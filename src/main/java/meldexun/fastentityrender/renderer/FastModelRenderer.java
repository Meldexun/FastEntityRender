package meldexun.fastentityrender.renderer;

import static meldexun.memoryutil.UnsafeUtil.UNSAFE;

import java.nio.ByteBuffer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL44;

import meldexun.fastentityrender.opengl.BufferStorage;
import meldexun.fastentityrender.opengl.Sync;
import meldexun.fastentityrender.opengl.VertexArray;
import meldexun.fastentityrender.util.ArrayStack;
import meldexun.fastentityrender.util.CubeData;
import meldexun.matrixutil.Matrix3f;
import meldexun.matrixutil.Matrix4f;
import meldexun.matrixutil.MatrixStack;
import meldexun.memoryutil.MemoryUtil;
import meldexun.memoryutil.NIOBufferUtil;
import net.minecraft.client.model.ModelRenderer;

public class FastModelRenderer {

	private static final int BUFFERS = 3;
	private static final int VERTEX_SIZE = 24;
	private static FastModelRenderer instance;

	private final int[] vbos = new int[BUFFERS];
	private final int[] vaos = new int[BUFFERS];
	private final long[] addresses = new long[BUFFERS];
	private final Object[] syncs = new Object[BUFFERS];
	@SuppressWarnings("unchecked")
	private final ArrayStack<Runnable>[] tasks = IntStream.range(0, BUFFERS).mapToObj(i -> new ArrayStack<>()).toArray(ArrayStack[]::new);
	private long buffer;
	private final ArrayStack<ModelRenderer> queue = new ArrayStack<>();
	private final MatrixStack matrixStack = new MatrixStack();

	private long capacity;

	private int index;
	private int vbo;
	private int vao;
	private long address;
	private int verticesBatch;
	private int verticesTotal;
	private boolean isBatching;

	public static FastModelRenderer getInstance() {
		if (instance == null) {
			instance = new FastModelRenderer(1 << 20);
		}
		return instance;
	}

	private FastModelRenderer(long initialCapacity) {
		initVBOs(initialCapacity);
	}

	private void initVBOs(long capacity) {
		this.capacity = capacity;
		if (BufferStorage.isSupported() && Sync.isSupported()) {
			for (int i = 0; i < BUFFERS; i++) {
				vbos[i] = BufferStorage.createBuffer();
				BufferStorage.bindBuffer(GL15.GL_ARRAY_BUFFER, vbos[i], false);
				BufferStorage.initBuffer(GL15.GL_ARRAY_BUFFER, vbos[i], capacity, GL30.GL_MAP_WRITE_BIT | GL44.GL_MAP_PERSISTENT_BIT);
				addresses[i] = NIOBufferUtil.getAddress(BufferStorage.mapBuffer(GL15.GL_ARRAY_BUFFER, vbos[i], 0L, capacity, GL30.GL_MAP_WRITE_BIT | GL30.GL_MAP_INVALIDATE_BUFFER_BIT | GL30.GL_MAP_FLUSH_EXPLICIT_BIT | GL30.GL_MAP_UNSYNCHRONIZED_BIT | GL44.GL_MAP_PERSISTENT_BIT));
				BufferStorage.bindBuffer(GL15.GL_ARRAY_BUFFER, 0, false);

				if (VertexArray.isSupported()) {
					vaos[i] = VertexArray.createVertexArray();
					VertexArray.bindVertexArray(vaos[i]);
					GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
					GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
					GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
					BufferStorage.bindBuffer(GL15.GL_ARRAY_BUFFER, vbos[i], true);
					GL11.glVertexPointer(3, GL11.GL_FLOAT, VERTEX_SIZE, 0L);
					GL11.glTexCoordPointer(2, GL11.GL_FLOAT, VERTEX_SIZE, 12L);
					GL11.glNormalPointer(GL11.GL_BYTE, VERTEX_SIZE, 20L);
					BufferStorage.bindBuffer(GL15.GL_ARRAY_BUFFER, 0, true);
					VertexArray.bindVertexArray(0);
				}
			}
		} else {
			if (buffer != 0L) {
				UNSAFE.freeMemory(buffer);
			}
			buffer = UNSAFE.allocateMemory(capacity);
		}
	}

	public void startFrame() {
		if (BufferStorage.isSupported() && Sync.isSupported()) {
			index = (index + 1) % BUFFERS;
			vbo = vbos[index];
			vao = vaos[index];
			address = addresses[index];
			if (syncs[index] != null) {
				Sync.waitSync(syncs[index]);
				Sync.deleteSync(syncs[index]);
				syncs[index] = null;
			}
			while (!tasks[index].isEmpty()) {
				tasks[index].remove().run();
			}
			verticesTotal = 0;
		} else {
			address = buffer;
		}
	}

	public void endFrame() {
		if (BufferStorage.isSupported() && Sync.isSupported()) {
			syncs[index] = Sync.createSync();
		}
	}

	public void startBatch() {
		isBatching = true;
		verticesBatch = 0;
		if (!BufferStorage.isSupported() || !Sync.isSupported()) {
			verticesTotal = 0;
		}
	}

	public void endBatch() {
		if (!isBatching) {
			throw new IllegalStateException();
		}
		isBatching = false;
		if (verticesBatch > 0) {
			if (BufferStorage.isSupported() && Sync.isSupported()) {
				BufferStorage.flushBuffer(GL15.GL_ARRAY_BUFFER, vbo, (verticesTotal - verticesBatch) * VERTEX_SIZE, verticesBatch * VERTEX_SIZE);

				if (VertexArray.isSupported()) {
					VertexArray.bindVertexArray(vao);
				} else {
					GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
					GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
					GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
					BufferStorage.bindBuffer(GL15.GL_ARRAY_BUFFER, vbo, true);
					GL11.glVertexPointer(3, GL11.GL_FLOAT, VERTEX_SIZE, 0L);
					GL11.glTexCoordPointer(2, GL11.GL_FLOAT, VERTEX_SIZE, 12L);
					GL11.glNormalPointer(GL11.GL_BYTE, VERTEX_SIZE, 20L);
				}

				GL11.glDrawArrays(GL11.GL_QUADS, verticesTotal - verticesBatch, verticesBatch);

				if (VertexArray.isSupported()) {
					VertexArray.bindVertexArray(0);
				} else {
					GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
					GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
					GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
					BufferStorage.bindBuffer(GL15.GL_ARRAY_BUFFER, 0, true);
				}
			} else {
				GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
				GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
				GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);

				ByteBuffer buffer = NIOBufferUtil.asByteBuffer(address, capacity);
				GL11.glVertexPointer(3, GL11.GL_FLOAT, VERTEX_SIZE, (ByteBuffer) buffer.position(0));
				GL11.glTexCoordPointer(2, GL11.GL_FLOAT, VERTEX_SIZE, (ByteBuffer) buffer.position(12));
				GL11.glNormalPointer(GL11.GL_BYTE, VERTEX_SIZE, (ByteBuffer) buffer.position(20));

				GL11.glDrawArrays(GL11.GL_QUADS, 0, verticesBatch);

				GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
				GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
				GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
			}
		}
	}

	public void render(ModelRenderer bone, float scale) {
		int vertices = vertices(bone);
		if (vertices <= 0) {
			return;
		}

		boolean batched = isBatching;
		if (!batched) {
			startBatch();
		}

		if (capacity < (verticesTotal + vertices) * VERTEX_SIZE) {
			if (BufferStorage.isSupported() && Sync.isSupported()) {
				long old = address;
				for (int i = 0; i < BUFFERS; i++) {
					int vbo = vbos[i];
					int vao = vaos[i];
					tasks[i].add(() -> {
						BufferStorage.bindBuffer(GL15.GL_ARRAY_BUFFER, vbo, false);
						BufferStorage.unmapBuffer(GL15.GL_ARRAY_BUFFER, vbo);
						BufferStorage.bindBuffer(GL15.GL_ARRAY_BUFFER, 0, false);
						if (VertexArray.isSupported()) {
							VertexArray.deleteVertexArray(vao);
						}
						BufferStorage.deleteBuffer(vbo);
					});
				}
				initVBOs(Math.max(capacity + (capacity >> 1), (verticesTotal + vertices) * VERTEX_SIZE));
				vbo = vbos[index];
				vao = vaos[index];
				address = addresses[index];
				MemoryUtil.copyMemory(old + (verticesTotal - verticesBatch) * VERTEX_SIZE, address, verticesBatch * VERTEX_SIZE);
				verticesTotal = verticesBatch;
			} else {
				long old = address;
				initVBOs(Math.max(capacity + (capacity >> 1), (verticesTotal + vertices) * VERTEX_SIZE));
				address = buffer;
				MemoryUtil.copyMemory(old + (verticesTotal - verticesBatch) * VERTEX_SIZE, address, verticesBatch * VERTEX_SIZE);
				verticesTotal = verticesBatch;
			}
		}

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
					int nx0 = ((int) ( normalMatrix.m00 * 127) & 255) | ((int) ( normalMatrix.m10 * 127) & 255) << 8 | ((int) ( normalMatrix.m20 * 127) & 255) << 16;
					int nx1 = ((int) (-normalMatrix.m00 * 127) & 255) | ((int) (-normalMatrix.m10 * 127) & 255) << 8 | ((int) (-normalMatrix.m20 * 127) & 255) << 16;
					int ny0 = ((int) ( normalMatrix.m01 * 127) & 255) | ((int) ( normalMatrix.m11 * 127) & 255) << 8 | ((int) ( normalMatrix.m21 * 127) & 255) << 16;
					int ny1 = ((int) (-normalMatrix.m01 * 127) & 255) | ((int) (-normalMatrix.m11 * 127) & 255) << 8 | ((int) (-normalMatrix.m21 * 127) & 255) << 16;
					int nz0 = ((int) ( normalMatrix.m02 * 127) & 255) | ((int) ( normalMatrix.m12 * 127) & 255) << 8 | ((int) ( normalMatrix.m22 * 127) & 255) << 16;
					int nz1 = ((int) (-normalMatrix.m02 * 127) & 255) | ((int) (-normalMatrix.m12 * 127) & 255) << 8 | ((int) (-normalMatrix.m22 * 127) & 255) << 16;

					bufferVertex(x101, y101, z101, cubeData.upx1, cubeData.vpx0, nx0);
					bufferVertex(x100, y100, z100, cubeData.upx0, cubeData.vpx0, nx0);
					bufferVertex(x110, y110, z110, cubeData.upx0, cubeData.vpx1, nx0);
					bufferVertex(x111, y111, z111, cubeData.upx1, cubeData.vpx1, nx0);

					bufferVertex(x000, y000, z000, cubeData.unx1, cubeData.vnx0, nx1);
					bufferVertex(x001, y001, z001, cubeData.unx0, cubeData.vnx0, nx1);
					bufferVertex(x011, y011, z011, cubeData.unx0, cubeData.vnx1, nx1);
					bufferVertex(x010, y010, z010, cubeData.unx1, cubeData.vnx1, nx1);

					bufferVertex(x011, y011, z011, cubeData.upy0, cubeData.vpy1, ny0);
					bufferVertex(x111, y111, z111, cubeData.upy1, cubeData.vpy1, ny0);
					bufferVertex(x110, y110, z110, cubeData.upy1, cubeData.vpy0, ny0);
					bufferVertex(x010, y010, z010, cubeData.upy0, cubeData.vpy0, ny0);

					bufferVertex(x000, y000, z000, cubeData.uny0, cubeData.vny1, ny1);
					bufferVertex(x100, y100, z100, cubeData.uny1, cubeData.vny1, ny1);
					bufferVertex(x101, y101, z101, cubeData.uny1, cubeData.vny0, ny1);
					bufferVertex(x001, y001, z001, cubeData.uny0, cubeData.vny0, ny1);

					bufferVertex(x001, y001, z001, cubeData.upz1, cubeData.vpz0, nz0);
					bufferVertex(x101, y101, z101, cubeData.upz0, cubeData.vpz0, nz0);
					bufferVertex(x111, y111, z111, cubeData.upz0, cubeData.vpz1, nz0);
					bufferVertex(x011, y011, z011, cubeData.upz1, cubeData.vpz1, nz0);

					bufferVertex(x100, y100, z100, cubeData.unz1, cubeData.vnz0, nz1);
					bufferVertex(x000, y000, z000, cubeData.unz0, cubeData.vnz0, nz1);
					bufferVertex(x010, y010, z010, cubeData.unz0, cubeData.vnz1, nz1);
					bufferVertex(x110, y110, z110, cubeData.unz1, cubeData.vnz1, nz1);
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

	private void bufferVertex(float x, float y, float z, float u, float v, int n) {
		long offset = address + verticesTotal * VERTEX_SIZE;
		UNSAFE.putFloat(offset + 0, x);
		UNSAFE.putFloat(offset + 4, y);
		UNSAFE.putFloat(offset + 8, z);
		UNSAFE.putFloat(offset + 12, u);
		UNSAFE.putFloat(offset + 16, v);
		UNSAFE.putInt(offset + 20, n);
		verticesBatch++;
		verticesTotal++;
	}

}
