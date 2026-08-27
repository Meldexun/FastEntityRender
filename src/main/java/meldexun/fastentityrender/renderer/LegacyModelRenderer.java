package meldexun.fastentityrender.renderer;

import static meldexun.memoryutil.UnsafeUtil.UNSAFE;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;

import meldexun.memoryutil.MemoryUtil;
import meldexun.memoryutil.NIOBufferUtil;

public class LegacyModelRenderer extends FastModelRenderer {

	public LegacyModelRenderer(long initialCapacity) {
		super(initialCapacity);
	}

	@Override
	public void dispose() {
		UNSAFE.freeMemory(address);
		address = 0L;
	}

	@Override
	protected void _initBuffers(long capacity) {
		address = UNSAFE.allocateMemory(capacity);
	}

	@Override
	protected void ensureCapacity(long minCapacity) {
		if (capacity < minCapacity) {
			long old = address;
			initBuffers(Math.max(capacity + (capacity >> 1), minCapacity));
			MemoryUtil.copyMemory(old + (verticesTotal - verticesBatch) * vertexSize, address, verticesBatch * vertexSize);
			UNSAFE.freeMemory(old);
			verticesTotal = verticesBatch;
		}
	}

	@Override
	public void startBatch() {
		super.startBatch();
		verticesTotal = 0;
	}

	@Override
	protected void renderBatch() {
		this.setupClientState();
		this.setupAttributePointers(NIOBufferUtil.asByteBuffer(address, capacity));

		GL11.glDrawArrays(GL11.GL_QUADS, 0, verticesBatch);

		this.resetClientState();
	}

	protected void setupClientState() {
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
	}

	protected void setupAttributePointers(ByteBuffer buffer) {
		GL11.glVertexPointer(3, GL11.GL_FLOAT, vertexSize, (ByteBuffer) buffer.position(0));
		GL11.glTexCoordPointer(2, GL11.GL_FLOAT, vertexSize, (ByteBuffer) buffer.position(12));
		GL11.glNormalPointer(GL11.GL_BYTE, vertexSize, (ByteBuffer) buffer.position(20));
	}

	protected void resetClientState() {
		GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
		GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
	}

}
