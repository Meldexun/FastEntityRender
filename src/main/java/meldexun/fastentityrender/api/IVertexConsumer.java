package meldexun.fastentityrender.api;

public interface IVertexConsumer {

	default void bufferQuad(float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float u0, float v0, float u1, float v1, float nx, float ny, float nz) {
		int n = ((int) (nx * 127) & 255) | ((int) (ny * 127) & 255) << 8 | ((int) (nz * 127) & 255) << 16;
		bufferVertex(x0, y0, z0, u0, v0, n);
		bufferVertex(x1, y1, z1, u1, v0, n);
		bufferVertex(x2, y2, z2, u1, v1, n);
		bufferVertex(x3, y3, z3, u0, v1, n);
	}

	void bufferVertex(float x, float y, float z, float u, float v, int n);

}
