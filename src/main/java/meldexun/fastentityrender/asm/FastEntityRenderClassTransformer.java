package meldexun.fastentityrender.asm;

import java.lang.reflect.Field;
import java.util.MissingResourceException;
import java.util.NoSuchElementException;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.google.common.collect.BiMap;

import meldexun.asmutil2.ASMUtil;
import meldexun.asmutil2.AbstractClassTransformer;
import meldexun.asmutil2.NonLoadingClassWriter;
import meldexun.asmutil2.reader.ClassUtil;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;

public class FastEntityRenderClassTransformer extends AbstractClassTransformer implements IClassTransformer {

	private static final ClassUtil REMAPPING_CLASS_UTIL;
	static {
		try {
			Class<?> FMLDeobfuscatingRemapper = Class.forName("net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper", true, Launch.classLoader);
			Field _INSTANCE = FMLDeobfuscatingRemapper.getField("INSTANCE");
			Field _classNameBiMap = FMLDeobfuscatingRemapper.getDeclaredField("classNameBiMap");
			_classNameBiMap.setAccessible(true);
			@SuppressWarnings("unchecked")
			BiMap<String, String> deobfuscationMap = (BiMap<String, String>) _classNameBiMap.get(_INSTANCE.get(null));
			REMAPPING_CLASS_UTIL = ClassUtil.getInstance(new ClassUtil.Configuration(Launch.classLoader, deobfuscationMap.inverse(), deobfuscationMap));
		} catch (ReflectiveOperationException e) {
			throw new UnsupportedOperationException(e);
		}
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

		boolean transformed = false;
		int writeFlags = 0;
		for (AbstractInsnNode insn = renderMethod.instructions.getFirst(); insn != null; insn = insn.getNext()) {
			if (!(insn instanceof MethodInsnNode))
				continue;
			MethodInsnNode methodInsn = (MethodInsnNode) insn;
			if (!methodInsn.owner.equals("net/minecraft/client/model/ModelRenderer") || (!methodInsn.name.equals("func_78785_a") && !methodInsn.name.equals("render")) || !methodInsn.desc.equals("(F)V"))
				continue;

			AbstractInsnNode first = methodInsn;
			AbstractInsnNode last = methodInsn;
			for (AbstractInsnNode insn1 = last.getNext(); insn1 != null; insn1 = insn1.getNext()) {
				if (insn1 instanceof LineNumberNode)
					continue;
				if (insn1 instanceof LabelNode)
					continue;
				if (insn1 instanceof VarInsnNode)
					continue;
				if (insn1 instanceof FieldInsnNode)
					continue;
				if (insn1 instanceof LdcInsnNode)
					continue;
				if (insn1 instanceof InsnNode && insn1.getOpcode() != Opcodes.RETURN && insn1.getOpcode() != Opcodes.ATHROW)
					continue;
				if (insn1 instanceof MethodInsnNode) {
					MethodInsnNode methodInsn1 = (MethodInsnNode) insn1;
					if (methodInsn1.owner.equals("net/minecraft/client/model/ModelRenderer") && (methodInsn1.name.equals("func_78785_a") || methodInsn1.name.equals("render")) && methodInsn1.desc.equals("(F)V")) {
						last = methodInsn1;
						continue;
					}
				}
				break;
			}

			if (first.getPrevious() != null && first.getPrevious().getOpcode() == Opcodes.FLOAD
					&& first.getPrevious().getPrevious() != null && first.getPrevious().getPrevious().getOpcode() == Opcodes.GETFIELD
					&& first.getPrevious().getPrevious().getPrevious() != null && first.getPrevious().getPrevious().getPrevious().getOpcode() == Opcodes.ALOAD) {
				first = first.getPrevious().getPrevious().getPrevious();
			} else {
				writeFlags |= ClassWriter.COMPUTE_MAXS;
			}

			renderMethod.instructions.insertBefore(first, ASMUtil.listOf(
					new MethodInsnNode(Opcodes.INVOKESTATIC, "meldexun/fastentityrender/renderer/FastModelRenderer", "getInstance", "()Lmeldexun/fastentityrender/renderer/FastModelRenderer;", false),
					new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "meldexun/fastentityrender/renderer/FastModelRenderer", "startBatch", "()V", false)
			));
			renderMethod.instructions.insert(last, ASMUtil.listOf(
					new MethodInsnNode(Opcodes.INVOKESTATIC, "meldexun/fastentityrender/renderer/FastModelRenderer", "getInstance", "()Lmeldexun/fastentityrender/renderer/FastModelRenderer;", false),
					new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "meldexun/fastentityrender/renderer/FastModelRenderer", "endBatch", "()V", false)
			));
			transformed = true;

			insn = last;
		}
		if (!transformed) {
			return null;
		}

		ClassWriter classWriter = new NonLoadingClassWriter(writeFlags, REMAPPING_CLASS_UTIL);
		classNode.accept(classWriter);
		return classWriter.toByteArray();
	}

}
