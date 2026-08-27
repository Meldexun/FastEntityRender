package meldexun.fastentityrender.asm;

import java.lang.reflect.Field;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.google.common.collect.BiMap;

import meldexun.asmutil2.ASMUtil;
import meldexun.asmutil2.HashMapClassNodeClassTransformer;
import meldexun.asmutil2.IClassTransformerRegistry;
import meldexun.asmutil2.NonLoadingClassWriter;
import meldexun.asmutil2.reader.ClassUtil;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;

public class FastEntityRenderClassTransformer extends HashMapClassNodeClassTransformer implements IClassTransformer {

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
	protected void registerTransformers(IClassTransformerRegistry registry) {
		registry.addObf("net.minecraft.client.model.ModelRenderer", "render", "func_78785_a", ClassWriter.COMPUTE_FRAMES, methodNode -> {
			methodNode.instructions.insert(ASMUtil.listWithLabel(label -> ASMUtil.listOf(
					new InsnNode(Opcodes.ICONST_1),
					new JumpInsnNode(Opcodes.IFEQ, label),
					new MethodInsnNode(Opcodes.INVOKESTATIC, "meldexun/fastentityrender/EntityRenderer", "getRenderer", "()Lmeldexun/fastentityrender/renderer/FastModelRenderer;", false),
					new VarInsnNode(Opcodes.ALOAD, 0),
					new VarInsnNode(Opcodes.FLOAD, 1),
					new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "meldexun/fastentityrender/renderer/FastModelRenderer", "render", "(Lnet/minecraft/client/model/ModelRenderer;F)V", false),
					new InsnNode(Opcodes.RETURN),
					label)));
		});
		registry.addObf("net.minecraft.client.model.ModelRenderer", "renderWithRotation", "func_78791_b", ClassWriter.COMPUTE_FRAMES, methodNode -> {
			methodNode.instructions.insert(ASMUtil.listWithLabel(label -> ASMUtil.listOf(
					new InsnNode(Opcodes.ICONST_1),
					new JumpInsnNode(Opcodes.IFEQ, label),
					new MethodInsnNode(Opcodes.INVOKESTATIC, "meldexun/fastentityrender/EntityRenderer", "getRenderer", "()Lmeldexun/fastentityrender/renderer/FastModelRenderer;", false),
					new VarInsnNode(Opcodes.ALOAD, 0),
					new VarInsnNode(Opcodes.FLOAD, 1),
					new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "meldexun/fastentityrender/renderer/FastModelRenderer", "render", "(Lnet/minecraft/client/model/ModelRenderer;F)V", false),
					new InsnNode(Opcodes.RETURN),
					label)));
		});
		registry.add("net.minecraft.client.model.ModelBox", ClassWriter.COMPUTE_FRAMES, classNode -> {
			classNode.interfaces.add("meldexun/fastentityrender/renderer/CubeDataProvider");

			FieldNode cubeData = new FieldNode(Opcodes.ACC_PRIVATE, "cubeData", "Lmeldexun/fastentityrender/util/CubeData;", null, null);
			classNode.fields.add(cubeData);

			MethodNode init = ASMUtil.find(classNode, "<init>", "(Lnet/minecraft/client/model/ModelRenderer;IIFFFIIIFZ)V");
			init.instructions.insertBefore(ASMUtil.last(init).opcode(Opcodes.RETURN).find(), ASMUtil.listWithLabels((label1, label2) -> ASMUtil.listOf(
					new VarInsnNode(Opcodes.ALOAD, 0),
					new TypeInsnNode(Opcodes.NEW, "meldexun/fastentityrender/util/CubeData"),
					new InsnNode(Opcodes.DUP),
					new VarInsnNode(Opcodes.ALOAD, 1),
					new VarInsnNode(Opcodes.ILOAD, 2),
					new VarInsnNode(Opcodes.ILOAD, 3),
					new VarInsnNode(Opcodes.ILOAD, 11),
					new JumpInsnNode(Opcodes.IFEQ, label1),
					new VarInsnNode(Opcodes.FLOAD, 4),
					new VarInsnNode(Opcodes.ILOAD, 7),
					new InsnNode(Opcodes.I2F),
					new InsnNode(Opcodes.FSUB),
					new VarInsnNode(Opcodes.FLOAD, 10),
					new InsnNode(Opcodes.FSUB),
					new JumpInsnNode(Opcodes.GOTO, label2),
					label1,
					new VarInsnNode(Opcodes.FLOAD, 4),
					new VarInsnNode(Opcodes.FLOAD, 10),
					new InsnNode(Opcodes.FADD),
					label2,
					new VarInsnNode(Opcodes.FLOAD, 5),
					new VarInsnNode(Opcodes.FLOAD, 10),
					new InsnNode(Opcodes.FADD),
					new VarInsnNode(Opcodes.FLOAD, 6),
					new VarInsnNode(Opcodes.FLOAD, 10),
					new InsnNode(Opcodes.FADD),
					new VarInsnNode(Opcodes.ILOAD, 7),
					new VarInsnNode(Opcodes.ILOAD, 8),
					new VarInsnNode(Opcodes.ILOAD, 9),
					new VarInsnNode(Opcodes.FLOAD, 10),
					new VarInsnNode(Opcodes.ILOAD, 11),
					new MethodInsnNode(Opcodes.INVOKESPECIAL, "meldexun/fastentityrender/util/CubeData", "<init>", "(Lnet/minecraft/client/model/ModelRenderer;IIFFFIIIFZ)V", false),
					new FieldInsnNode(Opcodes.PUTFIELD, classNode.name, cubeData.name, cubeData.desc))));

			MethodNode getCubeData = new MethodNode(Opcodes.ACC_PUBLIC, "getCubeData", "()Lmeldexun/fastentityrender/util/CubeData;", null, null);
			getCubeData.instructions.insert(ASMUtil.listOf(
					new VarInsnNode(Opcodes.ALOAD, 0),
					new FieldInsnNode(Opcodes.GETFIELD, classNode.name, cubeData.name, cubeData.desc),
					new InsnNode(Opcodes.ARETURN)));
			classNode.methods.add(getCubeData);
		});
	}

	@Override
	protected ClassWriter createClassWriter(int flags) {
		return new NonLoadingClassWriter(flags, REMAPPING_CLASS_UTIL);
	}

}
