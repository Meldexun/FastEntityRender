package meldexun.fastentityrender.util;

import goblinbob.mobends.core.client.model.BoxFactory;
import goblinbob.mobends.core.client.model.FaceRotation;
import goblinbob.mobends.core.math.vector.IVec3fRead;
import net.minecraft.client.model.ModelRenderer;

public class CubeData {

	public final float x0;
	public final float y0;
	public final float z0;
	public final float x1;
	public final float y1;
	public final float z1;

	public final float upx0;
	public final float upx1;
	public final float unx0;
	public final float unx1;
	public final float upy0;
	public final float upy1;
	public final float uny0;
	public final float uny1;
	public final float upz0;
	public final float upz1;
	public final float unz0;
	public final float unz1;

	public final float vpx0;
	public final float vpx1;
	public final float vnx0;
	public final float vnx1;
	public final float vpy0;
	public final float vpy1;
	public final float vny0;
	public final float vny1;
	public final float vpz0;
	public final float vpz1;
	public final float vnz0;
	public final float vnz1;

	public CubeData(ModelRenderer renderer, int texU, int texV, float x, float y, float z, int dx, int dy, int dz, float delta, boolean mirror) {
		this(renderer, texU, texV, x, y, z, (float) dx, (float) dy, (float) dz, delta, mirror);
	}

	public CubeData(ModelRenderer renderer, int texU, int texV, float x, float y, float z, float dx, float dy, float dz, float delta, boolean mirror) {
		this.x0 = mirror ? x + dx + delta : x - delta;
		this.y0 = y - delta;
		this.z0 = z - delta;
		this.x1 = mirror ? x - delta : x + dx + delta;
		this.y1 = y + dy + delta;
		this.z1 = z + dz + delta;

		this.upx0 = (texU + dz + dx) / renderer.textureWidth;
		this.upx1 = (texU + dz + dx + dz) / renderer.textureWidth;
		this.vpx0 = (texV + dz) / renderer.textureHeight;
		this.vpx1 = (texV + dz + dy) / renderer.textureHeight;

		this.unx0 = (texU) / renderer.textureWidth;
		this.unx1 = (texU + dz) / renderer.textureWidth;
		this.vnx0 = (texV + dz) / renderer.textureHeight;
		this.vnx1 = (texV + dz + dy) / renderer.textureHeight;

		this.upy0 = (texU + dz + dx) / renderer.textureWidth;
		this.upy1 = (texU + dz + dx + dx) / renderer.textureWidth;
		this.vpy0 = (texV + dz) / renderer.textureHeight;
		this.vpy1 = (texV) / renderer.textureHeight;

		this.uny0 = (texU + dz) / renderer.textureWidth;
		this.uny1 = (texU + dz + dx) / renderer.textureWidth;
		this.vny0 = (texV) / renderer.textureHeight;
		this.vny1 = (texV + dz) / renderer.textureHeight;

		this.upz0 = (texU + dz + dx + dz) / renderer.textureWidth;
		this.upz1 = (texU + dz + dx + dz + dx) / renderer.textureWidth;
		this.vpz0 = (texV + dz) / renderer.textureHeight;
		this.vpz1 = (texV + dz + dy) / renderer.textureHeight;

		this.unz0 = (texU + dz) / renderer.textureWidth;
		this.unz1 = (texU + dz + dx) / renderer.textureWidth;
		this.vnz0 = (texV + dz) / renderer.textureHeight;
		this.vnz1 = (texV + dz + dy) / renderer.textureHeight;
	}

	public CubeData(ModelRenderer renderer, IVec3fRead min, IVec3fRead max, BoxFactory.TextureFace[] faces) {
		float x0 = min.getX();
		float y0 = min.getY();
		float z0 = min.getZ();
		float x1 = max.getX();
		float y1 = max.getY();
		float z1 = max.getZ();

		this.x0 = renderer.mirror ? x1 : x0;
		this.y0 = y0;
		this.z0 = z0;
		this.x1 = renderer.mirror ? x0 : x1;
		this.y1 = y1;
		this.z1 = z1;

		BoxFactory.TextureFace face;
		float uSize;
		float vSize;

		face = faces[0];
		if (face.faceRotation == FaceRotation.CLOCKWISE || face.faceRotation == FaceRotation.COUNTER_CLOCKWISE) {
			uSize = face.vSize;
			vSize = face.uSize;
		} else {
			uSize = face.uSize;
			vSize = face.vSize;
		}
		this.upx0 = (face.uPos) / renderer.textureWidth;
		this.upx1 = (face.uPos + uSize) / renderer.textureWidth;
		this.vpx0 = (face.vPos) / renderer.textureHeight;
		this.vpx1 = (face.vPos + vSize) / renderer.textureHeight;

		face = faces[1];
		if (face.faceRotation == FaceRotation.CLOCKWISE || face.faceRotation == FaceRotation.COUNTER_CLOCKWISE) {
			uSize = face.vSize;
			vSize = face.uSize;
		} else {
			uSize = face.uSize;
			vSize = face.vSize;
		}
		this.unx0 = (face.uPos) / renderer.textureWidth;
		this.unx1 = (face.uPos + uSize) / renderer.textureWidth;
		this.vnx0 = (face.vPos) / renderer.textureHeight;
		this.vnx1 = (face.vPos + vSize) / renderer.textureHeight;

		face = faces[3];
		if (face.faceRotation == FaceRotation.CLOCKWISE || face.faceRotation == FaceRotation.COUNTER_CLOCKWISE) {
			uSize = face.vSize;
			vSize = face.uSize;
		} else {
			uSize = face.uSize;
			vSize = face.vSize;
		}
		this.upy0 = (face.uPos) / renderer.textureWidth;
		this.upy1 = (face.uPos + uSize) / renderer.textureWidth;
		this.vpy0 = (face.vPos) / renderer.textureHeight;
		this.vpy1 = (face.vPos + vSize) / renderer.textureHeight;

		face = faces[2];
		if (face.faceRotation == FaceRotation.CLOCKWISE || face.faceRotation == FaceRotation.COUNTER_CLOCKWISE) {
			uSize = face.vSize;
			vSize = face.uSize;
		} else {
			uSize = face.uSize;
			vSize = face.vSize;
		}
		this.uny0 = (face.uPos) / renderer.textureWidth;
		this.uny1 = (face.uPos + uSize) / renderer.textureWidth;
		this.vny0 = (face.vPos) / renderer.textureHeight;
		this.vny1 = (face.vPos + vSize) / renderer.textureHeight;

		face = faces[4];
		if (face.faceRotation == FaceRotation.CLOCKWISE || face.faceRotation == FaceRotation.COUNTER_CLOCKWISE) {
			uSize = face.vSize;
			vSize = face.uSize;
		} else {
			uSize = face.uSize;
			vSize = face.vSize;
		}
		this.upz0 = (face.uPos) / renderer.textureWidth;
		this.upz1 = (face.uPos + uSize) / renderer.textureWidth;
		this.vpz0 = (face.vPos) / renderer.textureHeight;
		this.vpz1 = (face.vPos + vSize) / renderer.textureHeight;

		face = faces[5];
		if (face.faceRotation == FaceRotation.CLOCKWISE || face.faceRotation == FaceRotation.COUNTER_CLOCKWISE) {
			uSize = face.vSize;
			vSize = face.uSize;
		} else {
			uSize = face.uSize;
			vSize = face.vSize;
		}
		this.unz0 = (face.uPos) / renderer.textureWidth;
		this.unz1 = (face.uPos + uSize) / renderer.textureWidth;
		this.vnz0 = (face.vPos) / renderer.textureHeight;
		this.vnz1 = (face.vPos + vSize) / renderer.textureHeight;
	}

}
