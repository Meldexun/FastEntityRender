package meldexun.fastentityrender.integration;

import static meldexun.memoryutil.UnsafeUtil.UNSAFE;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import meldexun.fastentityrender.opengl.VertexArray;
import meldexun.fastentityrender.renderer.PersistentModelRenderer;

public class OptifinePersistentModelRenderer extends PersistentModelRenderer {

	private boolean isShaders;

	public OptifinePersistentModelRenderer(long initialCapacity) {
		super(initialCapacity);
	}

	@Override
	public void startFrame() {
		super.startFrame();

		if (Optifine.isShaders() != isShaders) {
			isShaders = Optifine.isShaders();
			vertexSize = isShaders ? Optifine.vertexSizeShader : VERTEX_SIZE;

			if (VertexArray.isSupported()) {
				this.deleteVAOs();
				this.createVAOs();
				vao = vaos[index];
			}
		}
	}

	@Override
	public void bufferQuad(float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float u0, float v0, float u1, float v1, float nx, float ny, float nz) {
		if (!isShaders) {
			super.bufferQuad(x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, u0, v0, u1, v1, nx, ny, nz);
		} else {
			int n = ((int) (nx * 127) & 255) | ((int) (ny * 127) & 255) << 8 | ((int) (nz * 127) & 255) << 16;

			float avgU = (u0 + u1) * 0.5F;
			float avgV = (v0 + v1) * 0.5F;

			float f = 1.0F / (u1 - u0);
			float tanx = (x1 - x0) * f;
			float tany = (y1 - y0) * f;
			float tanz = (z1 - z0) * f;
			f = 1.0F / (float) Math.sqrt(tanx * tanx + tany * tany + tanz * tanz);
			tanx *= f;
			tany *= f;
			tanz *= f;

			f = 1.0F / (v1 - v0);
			float btanx = (x2 - x1) * f;
			float btany = (y2 - y1) * f;
			float btanz = (z2 - z1) * f;

			float ntx = nz * tany - ny * tanz;
			float nty = nx * tanz - nz * tanx;
			float ntz = ny * tanx - nx * tany;
			float tanw = ntx * btanx + nty * btany + ntz * btanz < 0.0F ? -1.0F : 1.0F;

			long tan = ((long) (tanx * 32767.0F) & 0xFFFF) << 0
					| ((long) (tany * 32767.0F) & 0xFFFF) << 16
					| ((long) (tanz * 32767.0F) & 0xFFFF) << 32
					| ((long) (tanw * 32767.0F) & 0xFFFF) << 48;

			bufferVertexShaders(x0, y0, z0, u0, v0, n, avgU, avgV, tan);
			bufferVertexShaders(x1, y1, z1, u1, v0, n, avgU, avgV, tan);
			bufferVertexShaders(x2, y2, z2, u1, v1, n, avgU, avgV, tan);
			bufferVertexShaders(x3, y3, z3, u0, v1, n, avgU, avgV, tan);
		}
	}

	protected void bufferVertexShaders(float x, float y, float z, float u, float v, int n, float avgU, float avgV, long tan) {
		long offset = address + verticesTotal * vertexSize;
		UNSAFE.putFloat(offset + 0, x);
		UNSAFE.putFloat(offset + 4, y);
		UNSAFE.putFloat(offset + 8, z);
		UNSAFE.putFloat(offset + 12, u);
		UNSAFE.putFloat(offset + 16, v);
		UNSAFE.putInt(offset + 20, n);
		UNSAFE.putFloat(offset + 24, avgU);
		UNSAFE.putFloat(offset + 28, avgV);
		UNSAFE.putLong(offset + 32, tan);
		verticesBatch++;
		verticesTotal++;
	}

	@Override
	protected void setupClientState() {
		super.setupClientState();
		if (isShaders) {
			GL20.glEnableVertexAttribArray(Optifine.midTexCoordAttrib);
			GL20.glEnableVertexAttribArray(Optifine.tangentAttrib);
		}
	}

	@Override
	protected void setupAttributePointers() {
		super.setupAttributePointers();
		if (isShaders) {
			GL20.glVertexAttribPointer(Optifine.midTexCoordAttrib, 2, GL11.GL_FLOAT, false, vertexSize, 24);
			GL20.glVertexAttribPointer(Optifine.tangentAttrib, 4, GL11.GL_SHORT, false, vertexSize, 32);
		}
	}

	@Override
	protected void resetClientState() {
		super.resetClientState();
		if (isShaders) {
			GL20.glDisableVertexAttribArray(Optifine.midTexCoordAttrib);
			GL20.glDisableVertexAttribArray(Optifine.tangentAttrib);
		}
	}

}
