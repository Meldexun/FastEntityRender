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
import meldexun.fastentityrender.asm.util.DeobfuscationUtil;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;

public class FastEntityRenderClassTransformer extends HashMapClassNodeClassTransformer implements IClassTransformer {

	static final ClassUtil REMAPPING_CLASS_UTIL;
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
		registry.add("net.minecraft.client.model.ModelRenderer", ClassWriter.COMPUTE_FRAMES, classNode -> {
			classNode.interfaces.add("meldexun/fastentityrender/api/IModelRenderer");

			ASMUtil.findObf(classNode, "render", "func_78785_a").instructions.insert(ASMUtil.listWithLabel(label -> ASMUtil.listOf(
					new InsnNode(Opcodes.ICONST_1),
					new JumpInsnNode(Opcodes.IFEQ, label),
					new VarInsnNode(Opcodes.ALOAD, 0),
					new VarInsnNode(Opcodes.FLOAD, 1),
					new MethodInsnNode(Opcodes.INVOKESTATIC, "meldexun/fastentityrender/EntityRenderer", "render", "(Lnet/minecraft/client/model/ModelRenderer;F)V", false),
					new InsnNode(Opcodes.RETURN),
					label)));
			ASMUtil.findObf(classNode, "renderWithRotation", "func_78791_b").instructions.insert(ASMUtil.listWithLabel(label -> ASMUtil.listOf(
					new InsnNode(Opcodes.ICONST_1),
					new JumpInsnNode(Opcodes.IFEQ, label),
					new VarInsnNode(Opcodes.ALOAD, 0),
					new VarInsnNode(Opcodes.FLOAD, 1),
					new MethodInsnNode(Opcodes.INVOKESTATIC, "meldexun/fastentityrender/EntityRenderer", "render", "(Lnet/minecraft/client/model/ModelRenderer;F)V", false),
					new InsnNode(Opcodes.RETURN),
					label)));

			MethodNode shouldRender = new MethodNode(Opcodes.ACC_PUBLIC, "shouldRender", "()Z", null, null);
			shouldRender.instructions.insert(ASMUtil.listWithLabel(label -> ASMUtil.listOf(
					new VarInsnNode(Opcodes.ALOAD, 0),
					DeobfuscationUtil.createObfFieldInsn(Opcodes.GETFIELD, classNode.name, "field_78807_k", "Z"), // isHidden
					new JumpInsnNode(Opcodes.IFNE, label),
					new VarInsnNode(Opcodes.ALOAD, 0),
					DeobfuscationUtil.createObfFieldInsn(Opcodes.GETFIELD, classNode.name, "field_78806_j", "Z"), // showModel
					new InsnNode(Opcodes.IRETURN),
					label,
					new InsnNode(Opcodes.ICONST_0),
					new InsnNode(Opcodes.IRETURN))));
			classNode.methods.add(shouldRender);

			MethodNode cubes = new MethodNode(Opcodes.ACC_PUBLIC, "cubes", "()Ljava/util/List;", null, null);
			cubes.instructions.insert(ASMUtil.listOf(
					new VarInsnNode(Opcodes.ALOAD, 0),
					DeobfuscationUtil.createObfFieldInsn(Opcodes.GETFIELD, classNode.name, "field_78804_l", "Ljava/util/List;"), // cubeList
					new InsnNode(Opcodes.ARETURN)));
			classNode.methods.add(cubes);

			MethodNode pushChildren = new MethodNode(Opcodes.ACC_PUBLIC, "pushChildren", "(Lit/unimi/dsi/fastutil/Stack;)V", null, null);
			pushChildren.instructions.insert(ASMUtil.listWithLabels((label1, label2) -> ASMUtil.listOf(
					new VarInsnNode(Opcodes.ALOAD, 0),
					DeobfuscationUtil.createObfFieldInsn(Opcodes.GETFIELD, classNode.name, "field_78805_m", "Ljava/util/List;"), // childModels
					new JumpInsnNode(Opcodes.IFNULL, label2),

					new InsnNode(Opcodes.ICONST_0),
					new VarInsnNode(Opcodes.ISTORE, 2),
					label1,
					new VarInsnNode(Opcodes.ILOAD, 2),
					new VarInsnNode(Opcodes.ALOAD, 0),
					DeobfuscationUtil.createObfFieldInsn(Opcodes.GETFIELD, classNode.name, "field_78805_m", "Ljava/util/List;"), // childModels
					new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/Collection", "size", "()I", true),
					new JumpInsnNode(Opcodes.IF_ICMPGE, label2),

					new VarInsnNode(Opcodes.ALOAD, 1),
					new VarInsnNode(Opcodes.ALOAD, 0),
					DeobfuscationUtil.createObfFieldInsn(Opcodes.GETFIELD, classNode.name, "field_78805_m", "Ljava/util/List;"), // childModels
					new VarInsnNode(Opcodes.ILOAD, 2),
					new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/List", "get", "(I)Ljava/lang/Object;", true),
					new MethodInsnNode(Opcodes.INVOKEINTERFACE, "it/unimi/dsi/fastutil/Stack", "push", "(Ljava/lang/Object;)V", true),

					new VarInsnNode(Opcodes.ILOAD, 2),
					new InsnNode(Opcodes.ICONST_1),
					new InsnNode(Opcodes.IADD),
					new VarInsnNode(Opcodes.ISTORE, 2),
					new JumpInsnNode(Opcodes.GOTO, label1),
					label2,
					new InsnNode(Opcodes.RETURN))));
			classNode.methods.add(pushChildren);

			MethodNode applyTransformation = new MethodNode(Opcodes.ACC_PUBLIC, "applyTransformation", "(Lmeldexun/matrixutil/MatrixStack;FZ)V", null, null);
			applyTransformation.instructions.insert(ASMUtil.listWithLabels((label1, label2, label3) -> ASMUtil.listOf(
					new VarInsnNode(Opcodes.ALOAD, 1),
					new VarInsnNode(Opcodes.ALOAD, 0),
					DeobfuscationUtil.createObfFieldInsn(Opcodes.GETFIELD, classNode.name, "field_82906_o", "F"), // offsetX
					new VarInsnNode(Opcodes.ALOAD, 0),
					DeobfuscationUtil.createObfFieldInsn(Opcodes.GETFIELD, classNode.name, "field_78800_c", "F"), // rotationPointX
					new VarInsnNode(Opcodes.FLOAD, 2),
					new InsnNode(Opcodes.FMUL),
					new InsnNode(Opcodes.FADD),
					new VarInsnNode(Opcodes.ALOAD, 0),
					DeobfuscationUtil.createObfFieldInsn(Opcodes.GETFIELD, classNode.name, "field_82908_p", "F"), // offsetY
					new VarInsnNode(Opcodes.ALOAD, 0),
					DeobfuscationUtil.createObfFieldInsn(Opcodes.GETFIELD, classNode.name, "field_78797_d", "F"), // rotationPointY
					new VarInsnNode(Opcodes.FLOAD, 2),
					new InsnNode(Opcodes.FMUL),
					new InsnNode(Opcodes.FADD),
					new VarInsnNode(Opcodes.ALOAD, 0),
					DeobfuscationUtil.createObfFieldInsn(Opcodes.GETFIELD, classNode.name, "field_82907_q", "F"), // offsetZ
					new VarInsnNode(Opcodes.ALOAD, 0),
					DeobfuscationUtil.createObfFieldInsn(Opcodes.GETFIELD, classNode.name, "field_78798_e", "F"), // rotationPointZ
					new VarInsnNode(Opcodes.FLOAD, 2),
					new InsnNode(Opcodes.FMUL),
					new InsnNode(Opcodes.FADD),
					new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "meldexun/matrixutil/MatrixStack", "translate", "(FFF)V", false),

					new VarInsnNode(Opcodes.ALOAD, 0),
					DeobfuscationUtil.createObfFieldInsn(Opcodes.GETFIELD, classNode.name, "field_78808_h", "F"), // rotateAngleZ
					new InsnNode(Opcodes.FCONST_0),
					new InsnNode(Opcodes.FCMPL),
					new JumpInsnNode(Opcodes.IFEQ, label1),
					new VarInsnNode(Opcodes.ALOAD, 1),
					new VarInsnNode(Opcodes.ALOAD, 0),
					DeobfuscationUtil.createObfFieldInsn(Opcodes.GETFIELD, classNode.name, "field_78808_h", "F"), // rotateAngleZ
					new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "meldexun/matrixutil/MatrixStack", "rotateZ", "(F)V", false),
					label1,

					new VarInsnNode(Opcodes.ALOAD, 0),
					DeobfuscationUtil.createObfFieldInsn(Opcodes.GETFIELD, classNode.name, "field_78796_g", "F"), // rotateAngleY
					new InsnNode(Opcodes.FCONST_0),
					new InsnNode(Opcodes.FCMPL),
					new JumpInsnNode(Opcodes.IFEQ, label2),
					new VarInsnNode(Opcodes.ALOAD, 1),
					new VarInsnNode(Opcodes.ALOAD, 0),
					DeobfuscationUtil.createObfFieldInsn(Opcodes.GETFIELD, classNode.name, "field_78796_g", "F"), // rotateAngleY
					new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "meldexun/matrixutil/MatrixStack", "rotateY", "(F)V", false),
					label2,

					new VarInsnNode(Opcodes.ALOAD, 0),
					DeobfuscationUtil.createObfFieldInsn(Opcodes.GETFIELD, classNode.name, "field_78795_f", "F"), // rotateAngleX
					new InsnNode(Opcodes.FCONST_0),
					new InsnNode(Opcodes.FCMPL),
					new JumpInsnNode(Opcodes.IFEQ, label3),
					new VarInsnNode(Opcodes.ALOAD, 1),
					new VarInsnNode(Opcodes.ALOAD, 0),
					DeobfuscationUtil.createObfFieldInsn(Opcodes.GETFIELD, classNode.name, "field_78795_f", "F"), // rotateAngleX
					new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "meldexun/matrixutil/MatrixStack", "rotateX", "(F)V", false),
					label3,

					new InsnNode(Opcodes.RETURN))));
			classNode.methods.add(applyTransformation);
		});
		registry.add("net.minecraft.client.model.ModelBox", ClassWriter.COMPUTE_FRAMES, classNode -> {
			classNode.interfaces.add("meldexun/fastentityrender/api/IModelBox");

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
