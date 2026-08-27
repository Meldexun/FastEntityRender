package meldexun.fastentityrender.renderer;

import static meldexun.memoryutil.UnsafeUtil.UNSAFE;

import java.util.function.Supplier;

import meldexun.fastentityrender.util.ArrayStack;
import meldexun.fastentityrender.util.CubeData;
import meldexun.matrixutil.Matrix3f;
import meldexun.matrixutil.Matrix4f;
import meldexun.matrixutil.MatrixStack;
import net.minecraft.client.model.ModelRenderer;

public abstract class FastModelRenderer {

	public static final int VERTEX_SIZE = 24;

	private final ArrayStack<ModelRenderer> queue = new ArrayStack<>();
	private final MatrixStack matrixStack = new MatrixStack();

	protected long capacity;

	protected long address;
	protected int vertexSize = VERTEX_SIZE;
	protected int verticesBatch;
	protected int verticesTotal;
	protected boolean isBatching;

	protected FastModelRenderer(long initialCapacity) {
		initBuffers(initialCapacity);
	}

	public abstract void dispose();

	protected final void initBuffers(long capacity) {
		this.capacity = capacity;
		this._initBuffers(capacity);
	}

	protected abstract void _initBuffers(long capacity);

	protected abstract void ensureCapacity(long minCapacity);

	public void startFrame() {
		verticesTotal = 0;
	}

	public void endFrame() {

	}

	public void startBatch() {
		if (isBatching) {
			throw new IllegalStateException();
		}
		isBatching = true;
		verticesBatch = 0;
	}

	public void endBatch() {
		if (!isBatching) {
			throw new IllegalStateException();
		}
		isBatching = false;
		if (verticesBatch > 0) {
			this.renderBatch();
		}
	}

	protected abstract void renderBatch();

	public void render(ModelRenderer bone, float scale) {
		int vertices = vertices(bone);
		if (vertices <= 0) {
			return;
		}

		boolean batched = isBatching;
		if (!batched) {
			startBatch();
		}

		this.ensureCapacity((verticesTotal + vertices) * vertexSize);

		queue.add(bone);
		while (!queue.isEmpty()) {
			ModelRenderer bone1 = queue.remove();
			if (bone1 != null) {
				if (bone1.isHidden || !bone1.showModel) {
					continue;
				}

				matrixStack.push();
				matrixStack.translate(bone1.offsetX + bone1.rotationPointX * scale, bone1.offsetY + bone1.rotationPointY * scale, bone1.offsetZ + bone1.rotationPointZ * scale);
				if (bone1.rotateAngleZ != 0.0F)
					matrixStack.rotateZ(bone1.rotateAngleZ);
				if (bone1.rotateAngleY != 0.0F)
					matrixStack.rotateY(bone1.rotateAngleY);
				if (bone1.rotateAngleX != 0.0F)
					matrixStack.rotateX(bone1.rotateAngleX);

				for (int i = 0; i < bone1.cubeList.size(); i++) {
					@SuppressWarnings("unchecked")
					CubeData cubeData = ((Supplier<CubeData>) bone1.cubeList.get(i)).get();

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
					bufferQuad(x101, y101, z101, x100, y100, z100, x110, y110, z110, x111, y111, z111, cubeData.upx1, cubeData.vpx0, cubeData.upx0, cubeData.vpx1,  normalMatrix.m00,  normalMatrix.m10,  normalMatrix.m20);
					bufferQuad(x000, y000, z000, x001, y001, z001, x011, y011, z011, x010, y010, z010, cubeData.unx1, cubeData.vnx0, cubeData.unx0, cubeData.vnx1, -normalMatrix.m00, -normalMatrix.m10, -normalMatrix.m20);
					bufferQuad(x011, y011, z011, x111, y111, z111, x110, y110, z110, x010, y010, z010, cubeData.upy0, cubeData.vpy1, cubeData.upy1, cubeData.vpy0,  normalMatrix.m01,  normalMatrix.m11,  normalMatrix.m21);
					bufferQuad(x000, y000, z000, x100, y100, z100, x101, y101, z101, x001, y001, z001, cubeData.uny0, cubeData.vny1, cubeData.uny1, cubeData.vny0, -normalMatrix.m01, -normalMatrix.m11, -normalMatrix.m21);
					bufferQuad(x001, y001, z001, x101, y101, z101, x111, y111, z111, x011, y011, z011, cubeData.upz1, cubeData.vpz0, cubeData.upz0, cubeData.vpz1,  normalMatrix.m02,  normalMatrix.m12,  normalMatrix.m22);
					bufferQuad(x100, y100, z100, x000, y000, z000, x010, y010, z010, x110, y110, z110, cubeData.unz1, cubeData.vnz0, cubeData.unz0, cubeData.vnz1, -normalMatrix.m02, -normalMatrix.m12, -normalMatrix.m22);
				}

				queue.add(null);
				queue.addAll(bone1.childModels);
			} else {
				matrixStack.pop();
			}
		}

		if (!batched) {
			endBatch();
		}
	}

	private int vertices(ModelRenderer bone) {
		int cubes = 0;
		queue.add(bone);
		while (!queue.isEmpty()) {
			ModelRenderer bone1 = queue.remove();
			if (!bone1.isHidden && bone1.showModel) {
				cubes += bone1.cubeList.size();
				queue.addAll(bone1.childModels);
			}
		}
		return cubes * 6 * 4;
	}

	protected void bufferQuad(float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float u0, float v0, float u1, float v1, float nx, float ny, float nz) {
		int n = ((int) (nx * 127) & 255) | ((int) (ny * 127) & 255) << 8 | ((int) (nz * 127) & 255) << 16;
		bufferVertex(x0, y0, z0, u0, v0, n);
		bufferVertex(x1, y1, z1, u1, v0, n);
		bufferVertex(x2, y2, z2, u1, v1, n);
		bufferVertex(x3, y3, z3, u0, v1, n);
	}

	protected void bufferVertex(float x, float y, float z, float u, float v, int n) {
		long offset = address + verticesTotal * vertexSize;
		UNSAFE.putFloat(offset + 0, x);
		UNSAFE.putFloat(offset + 4, y);
		UNSAFE.putFloat(offset + 8, z);
		UNSAFE.putFloat(offset + 12, u);
		UNSAFE.putFloat(offset + 16, v);
		UNSAFE.putInt(offset + 20, n);
		verticesBatch++;
		verticesTotal++;
	}

	public void pushMatrix() {
		matrixStack.push();
	}

	public void popMatrix() {
		matrixStack.pop();
	}

	public void translate(float x, float y, float z) {
		matrixStack.translate(x, y, z);
	}

	public void scale(float x, float y, float z) {
		matrixStack.scale(x, y, z);
	}

	public void rotate(float radian, float x, float y, float z) {
		matrixStack.rotate(radian, x, y, z);
	}

}
