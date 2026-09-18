package meldexun.fastentityrender.asm;

import static meldexun.fastentityrender.asm.FastEntityRenderClassTransformer.REMAPPING_CLASS_UTIL;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.MissingResourceException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import meldexun.asmutil2.ASMUtil;
import meldexun.asmutil2.AbstractClassTransformer;
import meldexun.asmutil2.NonLoadingClassWriter;

import meldexun.fastentityrender.FastEntityRenderConfig;
import meldexun.fastentityrender.asm.util.DeobfuscationUtil;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;

public class ModelClassTransformer extends AbstractClassTransformer implements IClassTransformer {

	private static boolean isRenderCall(AbstractInsnNode insn) {
		if (!(insn instanceof MethodInsnNode)) return false;
		MethodInsnNode i = (MethodInsnNode) insn;
		if (!i.owner.equals("net/minecraft/client/model/ModelRenderer")) return false;
		if (!i.desc.equals("(F)V")) return false;
		if (i.name.equals("func_78785_a")) return true;
		if (i.name.equals("render")) return true;
		if (i.name.equals("func_78791_b")) return true;
		if (i.name.equals("renderWithRotation")) return true;
		return false;
	}

	private static boolean isBatchable(AbstractInsnNode insn) {
		if (!(insn instanceof MethodInsnNode)) return true;
		MethodInsnNode i = (MethodInsnNode) insn;

		for (Pattern p : FastEntityRenderConfig.batchableMethodCalls) {
			if (p.matcher(i.owner + "." + i.name + i.desc).find()) {
				return true;
			}
		}

		ASMUtil.LOGGER.info("Illegal instruction in batch: {}.{}{}", i.owner, FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(i.owner, i.name, i.desc), i.desc);
		return false;
	}

	private static boolean isPush(AbstractInsnNode insn) {
		if (!(insn instanceof MethodInsnNode)) return false;
		MethodInsnNode i = (MethodInsnNode) insn;
		if (i.owner.equals("net/minecraft/client/renderer/GlStateManager") && Pattern.matches("pushMatrix|func_179094_E", i.name)) return true;
		if (i.owner.startsWith("org/lwjgl/opengl/GL") && i.name.equals("glPushMatrix")) return true;
		return false;
	}

	private static boolean isPop(AbstractInsnNode insn) {
		if (!(insn instanceof MethodInsnNode)) return false;
		MethodInsnNode i = (MethodInsnNode) insn;
		if (i.owner.equals("net/minecraft/client/renderer/GlStateManager") && Pattern.matches("popMatrix|func_179121_F", i.name)) return true;
		if (i.owner.startsWith("org/lwjgl/opengl/GL") && i.name.equals("glPopMatrix")) return true;
		return false;
	}

	private static boolean isTransformation(AbstractInsnNode insn) {
		if (!(insn instanceof MethodInsnNode)) return false;
		MethodInsnNode i = (MethodInsnNode) insn;
		if (i.owner.equals("net/minecraft/client/renderer/GlStateManager") && Pattern.matches("pushMatrix|func_179094_E|popMatrix|func_179121_F|translate|func_179109_b|func_179137_b|rotate|func_179114_b|func_187444_a|scale|func_179139_a|func_179152_a", i.name)) return true;
		if (i.owner.startsWith("org/lwjgl/opengl/GL") && Pattern.matches("gl(?:PushMatrix|PopMatrix|Translate|Rotate|Scale)[fd]?", i.name)) return true;
		return false;
	}

	@Override
	protected byte[] transformOrNull(String obfName, String name, byte[] basicClass) {
		if (basicClass == null) {
			return null;
		}
		try {
			if (REMAPPING_CLASS_UTIL.findInClassHierarchy(name.replace('.', '/'), "net/minecraft/client/model/ModelBase"::equals) == null) {
				return null;
			}
		} catch (MissingResourceException e) {
			return null;
		}

		ClassReader classReader = new ClassReader(basicClass);
		ClassNode classNode = new ClassNode();
		classReader.accept(classNode, 0);

		MethodNode renderMethod;
		try {
			renderMethod = ASMUtil.findObf(classNode, "render", "func_78088_a", "(Lnet/minecraft/entity/Entity;FFFFFF)V");
		} catch (NoSuchElementException e) {
			return null;
		}

		Deque<MethodInsnNode> renderCalls = ASMUtil.stream(renderMethod)
				.filter(MethodInsnNode.class::isInstance)
				.map(MethodInsnNode.class::cast)
				.filter(ModelClassTransformer::isRenderCall)
				.collect(Collectors.toCollection(ArrayDeque::new));
		if (renderCalls.size() <= 1) {
			return null; // no batching needed for zero or one render calls
		}

		ASMUtil.LOGGER.info("Transforming model class ({}.java:{}): detected {} render calls", name, ASMUtil.first(renderMethod).type(LineNumberNode.class).find().line, renderCalls.size());

		ControlFlowAnalysis controlFlowAnalysis;
		BatchAnalysis<AbstractInsnNode> matrixStackAnalysis;
		{
			ControlFlowAnalysis controlFlowAnalysis1 = new ControlFlowAnalysis(renderMethod);
			BatchAnalysis<AbstractInsnNode> matrixStackAnalysis1 = BatchAnalysis.of(controlFlowAnalysis1, ModelClassTransformer::isPush, ModelClassTransformer::isPop, ModelClassTransformer::isTransformation);
			
			// check for unguarded matrix stack transforms
			if (controlFlowAnalysis1.stream()
					.filter(((Predicate<AbstractInsnNode>) ModelClassTransformer::isPush).negate())
					.filter(((Predicate<AbstractInsnNode>) ModelClassTransformer::isPop).negate())
					.filter(ModelClassTransformer::isTransformation)
					.anyMatch(i -> matrixStackAnalysis1.getCorrespondingStarts(i).isEmpty() || matrixStackAnalysis1.getCorrespondingEnds(i).isEmpty())) {
				// insert push/pop at beginning/end
				renderMethod.instructions.insert(DeobfuscationUtil.createObfMethodInsn(Opcodes.INVOKESTATIC, "net/minecraft/client/renderer/GlStateManager", "func_179094_E", "()V", false)); // pushMatrix
				ASMUtil.stream(renderMethod).filter(i -> i.getOpcode() == Opcodes.RETURN).collect(Collectors.toList()).forEach(i -> {
					renderMethod.instructions.insertBefore(i, DeobfuscationUtil.createObfMethodInsn(Opcodes.INVOKESTATIC, "net/minecraft/client/renderer/GlStateManager", "func_179121_F", "()V", false)); // popMatrix
				});

				// method changed -> recompute
				controlFlowAnalysis = new ControlFlowAnalysis(renderMethod);
				matrixStackAnalysis = BatchAnalysis.of(controlFlowAnalysis, ModelClassTransformer::isPush, ModelClassTransformer::isPop, ModelClassTransformer::isTransformation);
			} else {
				controlFlowAnalysis = controlFlowAnalysis1;
				matrixStackAnalysis = matrixStackAnalysis1;
			}
		}

		Batch batch = null;
		AtomicInteger batchedRenderCalls = new AtomicInteger();
		BiPredicate<AbstractInsnNode, Predicate<AbstractInsnNode>> onAdd = (insn, add) -> {
			if (isPush(insn)) {
				return matrixStackAnalysis.getCorrespondingEnds(insn)
						.stream()
						.allMatch(add);
			}
			if (isPop(insn)) {
				return matrixStackAnalysis.getCorrespondingStarts(insn)
						.stream()
						.allMatch(add);
			}
			if (isTransformation(insn)) {
				return Stream.of(matrixStackAnalysis.getCorrespondingStarts(insn), matrixStackAnalysis.getCorrespondingEnds(insn))
						.flatMap(Collection::stream)
						.allMatch(add);
			}
			return isBatchable(insn);
		};
		MethodInsnNode renderCall;
		while ((renderCall = renderCalls.pollFirst()) != null) {
			if (batch != null) {
				Optional<Batch> newBatch = batch.add(controlFlowAnalysis, onAdd, renderCall);
				if (newBatch.isPresent()) {
					removeRenderCalls(controlFlowAnalysis, batch, newBatch.get(), renderCalls, batchedRenderCalls);
					batch = newBatch.get();
					batchedRenderCalls.getAndIncrement();
					continue;
				} else {
					finishBatch(renderMethod, controlFlowAnalysis, onAdd, batch, renderCalls, batchedRenderCalls);
				}
			}

			batch = Batch.of(renderCall);
			batchedRenderCalls.set(1);
		}
		finishBatch(renderMethod, controlFlowAnalysis, onAdd, batch, renderCalls, batchedRenderCalls);

		ClassWriter classWriter = new NonLoadingClassWriter(ClassWriter.COMPUTE_FRAMES, REMAPPING_CLASS_UTIL);
		classNode.accept(classWriter);
		return classWriter.toByteArray();
	}

	private static void removeRenderCalls(ControlFlowAnalysis controlFlow, Batch oldBatch, Batch newBatch, Collection<? extends AbstractInsnNode> renderCalls, AtomicInteger batchedRenderCalls) {
		controlFlow.stream(newBatch.dominator(), newBatch.postDominator(), true)
				.filter(ModelClassTransformer::isRenderCall)
				.filter(((Predicate<AbstractInsnNode>) controlFlow.stream(oldBatch.dominator(), oldBatch.postDominator(), true)
						.filter(ModelClassTransformer::isRenderCall)
						.collect(Collectors.toSet())::contains).negate())
				.filter(renderCalls::remove)
				.forEach(insn -> batchedRenderCalls.getAndIncrement());
	}

	private static void finishBatch(MethodNode method, ControlFlowAnalysis controlFlowAnalysis, BiPredicate<AbstractInsnNode, Predicate<AbstractInsnNode>> onAdd, Batch batch, Collection<? extends AbstractInsnNode> renderCalls, AtomicInteger batchedRenderCalls) {
		// detect if batch is inside a loop and try to extend batch to include the loop
		if (controlFlowAnalysis.isReachable(batch.dominator(), batch.dominator())) {
			AbstractInsnNode dominator = batch.dominator();
			while (controlFlowAnalysis.isReachable(dominator, dominator)) {
				dominator = controlFlowAnalysis.immediateDominator(dominator);
			}
			Optional<Batch> newBatch = batch.add(controlFlowAnalysis, onAdd, dominator);
			if (newBatch.isPresent()) {
				removeRenderCalls(controlFlowAnalysis, batch, newBatch.get(), renderCalls, batchedRenderCalls);
				batch = newBatch.get();
				batchedRenderCalls.getAndAdd(1000);
			}
		}

		if (batchedRenderCalls.get() > 1) {
			method.instructions.insertBefore(batch.dominator(), new MethodInsnNode(Opcodes.INVOKESTATIC, "meldexun/fastentityrender/EntityRenderer", "startBatch", "()V", false));
			method.instructions.insert(batch.postDominator(), new MethodInsnNode(Opcodes.INVOKESTATIC, "meldexun/fastentityrender/EntityRenderer", "endBatch", "()V", false));
			replaceGLCalls(controlFlowAnalysis, batch);
			ASMUtil.LOGGER.info("Batched {} render calls", batchedRenderCalls);
		}
	}

	private static void replaceGLCalls(ControlFlowAnalysis controlFlowAnalysis, Batch batch) {
		controlFlowAnalysis.stream(batch.dominator(), batch.postDominator(), false)
				.filter(MethodInsnNode.class::isInstance)
				.map(MethodInsnNode.class::cast)
				.forEach(insn -> {
					if (insn.owner.equals("net/minecraft/client/renderer/GlStateManager")) {
						switch (insn.name) {
						case "pushMatrix":
						case "func_179094_E":
							insn.owner = "meldexun/fastentityrender/EntityRenderer";
							insn.name = "pushMatrix";
							break;
						case "popMatrix":
						case "func_179121_F":
							insn.owner = "meldexun/fastentityrender/EntityRenderer";
							insn.name = "popMatrix";
							break;
						case "translate":
						case "func_179109_b":
						case "func_179137_b":
							insn.owner = "meldexun/fastentityrender/EntityRenderer";
							insn.name = "translate";
							break;
						case "rotate":
						case "func_179114_b":
							insn.owner = "meldexun/fastentityrender/EntityRenderer";
							insn.name = "rotate";
							break;
						case "scale":
						case "func_179139_a":
						case "func_179152_a":
							insn.owner = "meldexun/fastentityrender/EntityRenderer";
							insn.name = "scale";
							break;
						}
					} else if (insn.owner.startsWith("org/lwjgl/opengl/GL")) {
						switch (insn.name) {
						case "glPushMatrix":
							insn.owner = "meldexun/fastentityrender/EntityRenderer";
							insn.name = "pushMatrix";
							break;
						case "glPopMatrix":
						case "func_179121_F":
							insn.owner = "meldexun/fastentityrender/EntityRenderer";
							insn.name = "popMatrix";
							break;
						case "glTranslatef":
						case "glTranslated":
							insn.owner = "meldexun/fastentityrender/EntityRenderer";
							insn.name = "translate";
							break;
						case "glRotatef":
						case "glRotated":
							insn.owner = "meldexun/fastentityrender/EntityRenderer";
							insn.name = "rotate";
							break;
						case "glScalef":
						case "glScaled":
							insn.owner = "meldexun/fastentityrender/EntityRenderer";
							insn.name = "scale";
							break;
						}
					}
				});
	}

}
