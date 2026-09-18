package meldexun.fastentityrender.mixin.mobends;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import goblinbob.mobends.core.client.model.BoxFactory;
import goblinbob.mobends.core.client.model.MutatedBox;
import goblinbob.mobends.core.math.vector.IVec3fRead;
import meldexun.fastentityrender.api.IVertexConsumer;
import meldexun.fastentityrender.mixin.minecraft.ModelBoxMixin;
import meldexun.fastentityrender.api.IModelBox;
import meldexun.fastentityrender.util.CubeData;
import meldexun.matrixutil.Matrix3f;
import meldexun.matrixutil.Matrix4f;
import meldexun.matrixutil.MatrixStack;
import net.minecraft.client.model.ModelRenderer;

@Mixin(MutatedBox.class)
public abstract class MutatedBoxMixin extends ModelBoxMixin implements IModelBox {

	@Shadow(remap = false)
	@Final
	protected byte faceVisibilityFlag;

	@Inject(method = "<init>(Lnet/minecraft/client/model/ModelRenderer;IIFFFIIIFZB)V", at = @At("RETURN"))
	public void init(ModelRenderer renderer, int texU, int texV, float x, float y, float z, int dx, int dy, int dz, float delta, boolean mirror, byte faceVisibilityFlag, CallbackInfo info) {
		this.cubeData = new CubeData(renderer, texU, texV, mirror ? (x - dx - delta) : (x + delta), y + delta, z + delta, dx, dy, dz, delta, mirror);
	}

	@Inject(method = "<init>(Lnet/minecraft/client/model/ModelRenderer;Lgoblinbob/mobends/core/math/vector/IVec3fRead;Lgoblinbob/mobends/core/math/vector/IVec3fRead;[Lgoblinbob/mobends/core/client/model/BoxFactory$TextureFace;B)V", at = @At("RETURN"))
	public void init(ModelRenderer renderer, IVec3fRead min, IVec3fRead max, BoxFactory.TextureFace[] faces, byte faceVisibilityFlag, CallbackInfo info) {
		this.cubeData = new CubeData(renderer, min, max, faces);
	}

	@Override
	public void bufferCube(MatrixStack matrixStack, float scale, IVertexConsumer vertexConsumer) {
		CubeData cubeData = getCubeData();

		Matrix4f modelMatrix = matrixStack.modelMatrix();
		float x000 = modelMatrix.m00 * (cubeData.x0 * scale) + modelMatrix.m01 * (cubeData.y0 * scale) + modelMatrix.m02 * (cubeData.z0 * scale) + modelMatrix.m03;
		float x001 = modelMatrix.m00 * (cubeData.x0 * scale) + modelMatrix.m01 * (cubeData.y0 * scale) + modelMatrix.m02 * (cubeData.z1 * scale) + modelMatrix.m03;
		float x010 = modelMatrix.m00 * (cubeData.x0 * scale) + modelMatrix.m01 * (cubeData.y1 * scale) + modelMatrix.m02 * (cubeData.z0 * scale) + modelMatrix.m03;
		float x011 = modelMatrix.m00 * (cubeData.x0 * scale) + modelMatrix.m01 * (cubeData.y1 * scale) + modelMatrix.m02 * (cubeData.z1 * scale) + modelMatrix.m03;
		float x100 = modelMatrix.m00 * (cubeData.x1 * scale) + modelMatrix.m01 * (cubeData.y0 * scale) + modelMatrix.m02 * (cubeData.z0 * scale) + modelMatrix.m03;
		float x101 = modelMatrix.m00 * (cubeData.x1 * scale) + modelMatrix.m01 * (cubeData.y0 * scale) + modelMatrix.m02 * (cubeData.z1 * scale) + modelMatrix.m03;
		float x110 = modelMatrix.m00 * (cubeData.x1 * scale) + modelMatrix.m01 * (cubeData.y1 * scale) + modelMatrix.m02 * (cubeData.z0 * scale) + modelMatrix.m03;
		float x111 = modelMatrix.m00 * (cubeData.x1 * scale) + modelMatrix.m01 * (cubeData.y1 * scale) + modelMatrix.m02 * (cubeData.z1 * scale) + modelMatrix.m03;

		float y000 = modelMatrix.m10 * (cubeData.x0 * scale) + modelMatrix.m11 * (cubeData.y0 * scale) + modelMatrix.m12 * (cubeData.z0 * scale) + modelMatrix.m13;
		float y001 = modelMatrix.m10 * (cubeData.x0 * scale) + modelMatrix.m11 * (cubeData.y0 * scale) + modelMatrix.m12 * (cubeData.z1 * scale) + modelMatrix.m13;
		float y010 = modelMatrix.m10 * (cubeData.x0 * scale) + modelMatrix.m11 * (cubeData.y1 * scale) + modelMatrix.m12 * (cubeData.z0 * scale) + modelMatrix.m13;
		float y011 = modelMatrix.m10 * (cubeData.x0 * scale) + modelMatrix.m11 * (cubeData.y1 * scale) + modelMatrix.m12 * (cubeData.z1 * scale) + modelMatrix.m13;
		float y100 = modelMatrix.m10 * (cubeData.x1 * scale) + modelMatrix.m11 * (cubeData.y0 * scale) + modelMatrix.m12 * (cubeData.z0 * scale) + modelMatrix.m13;
		float y101 = modelMatrix.m10 * (cubeData.x1 * scale) + modelMatrix.m11 * (cubeData.y0 * scale) + modelMatrix.m12 * (cubeData.z1 * scale) + modelMatrix.m13;
		float y110 = modelMatrix.m10 * (cubeData.x1 * scale) + modelMatrix.m11 * (cubeData.y1 * scale) + modelMatrix.m12 * (cubeData.z0 * scale) + modelMatrix.m13;
		float y111 = modelMatrix.m10 * (cubeData.x1 * scale) + modelMatrix.m11 * (cubeData.y1 * scale) + modelMatrix.m12 * (cubeData.z1 * scale) + modelMatrix.m13;

		float z000 = modelMatrix.m20 * (cubeData.x0 * scale) + modelMatrix.m21 * (cubeData.y0 * scale) + modelMatrix.m22 * (cubeData.z0 * scale) + modelMatrix.m23;
		float z001 = modelMatrix.m20 * (cubeData.x0 * scale) + modelMatrix.m21 * (cubeData.y0 * scale) + modelMatrix.m22 * (cubeData.z1 * scale) + modelMatrix.m23;
		float z010 = modelMatrix.m20 * (cubeData.x0 * scale) + modelMatrix.m21 * (cubeData.y1 * scale) + modelMatrix.m22 * (cubeData.z0 * scale) + modelMatrix.m23;
		float z011 = modelMatrix.m20 * (cubeData.x0 * scale) + modelMatrix.m21 * (cubeData.y1 * scale) + modelMatrix.m22 * (cubeData.z1 * scale) + modelMatrix.m23;
		float z100 = modelMatrix.m20 * (cubeData.x1 * scale) + modelMatrix.m21 * (cubeData.y0 * scale) + modelMatrix.m22 * (cubeData.z0 * scale) + modelMatrix.m23;
		float z101 = modelMatrix.m20 * (cubeData.x1 * scale) + modelMatrix.m21 * (cubeData.y0 * scale) + modelMatrix.m22 * (cubeData.z1 * scale) + modelMatrix.m23;
		float z110 = modelMatrix.m20 * (cubeData.x1 * scale) + modelMatrix.m21 * (cubeData.y1 * scale) + modelMatrix.m22 * (cubeData.z0 * scale) + modelMatrix.m23;
		float z111 = modelMatrix.m20 * (cubeData.x1 * scale) + modelMatrix.m21 * (cubeData.y1 * scale) + modelMatrix.m22 * (cubeData.z1 * scale) + modelMatrix.m23;

		Matrix3f normalMatrix = matrixStack.normalMatrix();
		if ((this.faceVisibilityFlag & (1 << MutatedBox.LEFT)) != 0)
			vertexConsumer.bufferQuad(x101, y101, z101, x100, y100, z100, x110, y110, z110, x111, y111, z111, cubeData.upx1, cubeData.vpx0, cubeData.upx0, cubeData.vpx1, normalMatrix.m00, normalMatrix.m10, normalMatrix.m20);
		if ((this.faceVisibilityFlag & (1 << MutatedBox.RIGHT)) != 0)
			vertexConsumer.bufferQuad(x000, y000, z000, x001, y001, z001, x011, y011, z011, x010, y010, z010, cubeData.unx1, cubeData.vnx0, cubeData.unx0, cubeData.vnx1, -normalMatrix.m00, -normalMatrix.m10, -normalMatrix.m20);
		if ((this.faceVisibilityFlag & (1 << MutatedBox.BOTTOM)) != 0)
			vertexConsumer.bufferQuad(x011, y011, z011, x111, y111, z111, x110, y110, z110, x010, y010, z010, cubeData.upy0, cubeData.vpy1, cubeData.upy1, cubeData.vpy0, normalMatrix.m01, normalMatrix.m11, normalMatrix.m21);
		if ((this.faceVisibilityFlag & (1 << MutatedBox.TOP)) != 0)
			vertexConsumer.bufferQuad(x000, y000, z000, x100, y100, z100, x101, y101, z101, x001, y001, z001, cubeData.uny0, cubeData.vny1, cubeData.uny1, cubeData.vny0, -normalMatrix.m01, -normalMatrix.m11, -normalMatrix.m21);
		if ((this.faceVisibilityFlag & (1 << MutatedBox.FRONT)) != 0)
			vertexConsumer.bufferQuad(x001, y001, z001, x101, y101, z101, x111, y111, z111, x011, y011, z011, cubeData.upz1, cubeData.vpz0, cubeData.upz0, cubeData.vpz1, normalMatrix.m02, normalMatrix.m12, normalMatrix.m22);
		if ((this.faceVisibilityFlag & (1 << MutatedBox.BACK)) != 0)
			vertexConsumer.bufferQuad(x100, y100, z100, x000, y000, z000, x010, y010, z010, x110, y110, z110, cubeData.unz1, cubeData.vnz0, cubeData.unz0, cubeData.vnz1, -normalMatrix.m02, -normalMatrix.m12, -normalMatrix.m22);
	}

}
