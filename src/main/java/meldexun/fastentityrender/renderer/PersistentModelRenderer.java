package meldexun.fastentityrender.renderer;

import java.util.stream.IntStream;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL44;

import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import meldexun.fastentityrender.FastEntityRenderConfig;
import meldexun.fastentityrender.opengl.BufferStorage;
import meldexun.fastentityrender.opengl.Sync;
import meldexun.fastentityrender.opengl.VertexArray;
import meldexun.memoryutil.MemoryUtil;
import meldexun.memoryutil.NIOBufferUtil;

public class PersistentModelRenderer extends FastModelRenderer {

	protected static final int BUFFERS = 3;

	protected boolean useFlushExplicit;
	protected boolean useVAOs;

	protected int[] vbos;
	protected int[] vaos;
	protected long[] addresses;
	protected final Object[] syncs = new Object[BUFFERS];
	@SuppressWarnings("unchecked")
	protected final Stack<Runnable>[] tasks = IntStream.range(0, BUFFERS).mapToObj(i -> new ObjectArrayList<>()).toArray(Stack[]::new);

	protected int index;
	protected int vbo;
	protected int vao;

	public PersistentModelRenderer(long initialCapacity) {
		super(initialCapacity);
	}

	public static boolean isSupported() {
		return BufferStorage.isSupported() && Sync.isSupported();
	}

	@Override
	public void dispose() {
		deleteVBOs();
		deleteVAOs();
		for (int i = 0; i < BUFFERS; i++) {
			while (!tasks[i].isEmpty()) {
				tasks[i].pop().run();
			}
		}
	}

	@Override
	protected void _initBuffers(long capacity) {
		useFlushExplicit = FastEntityRenderConfig.useExplicitFlush;
		useVAOs = FastEntityRenderConfig.useVAOs && VertexArray.isSupported();
		this.createVBOs();
		if (useVAOs) {
			this.createVAOs();
		}
	}

	protected void createVBOs() {
		if (vbos != null) {
			throw new IllegalStateException();
		}
		if (addresses != null) {
			throw new IllegalStateException();
		}
		vbos = new int[BUFFERS];
		addresses = new long[BUFFERS];
		for (int i = 0; i < BUFFERS; i++) {
			vbos[i] = BufferStorage.createBuffer();
			BufferStorage.bindBuffer(GL15.GL_ARRAY_BUFFER, vbos[i], false);
			BufferStorage.initBuffer(GL15.GL_ARRAY_BUFFER, vbos[i], capacity, GL30.GL_MAP_WRITE_BIT | GL44.GL_MAP_PERSISTENT_BIT);
			addresses[i] = NIOBufferUtil.getAddress(BufferStorage.mapBuffer(GL15.GL_ARRAY_BUFFER, vbos[i], 0L, capacity, GL30.GL_MAP_WRITE_BIT | GL30.GL_MAP_INVALIDATE_BUFFER_BIT | (useFlushExplicit ? GL30.GL_MAP_FLUSH_EXPLICIT_BIT : 0) | GL30.GL_MAP_UNSYNCHRONIZED_BIT | GL44.GL_MAP_PERSISTENT_BIT));
			BufferStorage.bindBuffer(GL15.GL_ARRAY_BUFFER, 0, false);
		}
	}

	protected void createVAOs() {
		if (vaos != null) {
			throw new IllegalStateException();
		}
		vaos = new int[BUFFERS];
		for (int i = 0; i < BUFFERS; i++) {
			vaos[i] = VertexArray.createVertexArray();
			VertexArray.bindVertexArray(vaos[i]);
			this.setupClientState();
			BufferStorage.bindBuffer(GL15.GL_ARRAY_BUFFER, vbos[i], true);
			this.setupAttributePointers();
			BufferStorage.bindBuffer(GL15.GL_ARRAY_BUFFER, 0, true);
			VertexArray.bindVertexArray(0);
		}
	}

	@Override
	protected void ensureCapacity(long minCapacity) {
		if (capacity < minCapacity) {
			this.deleteVBOs();
			if (useVAOs) {
				this.deleteVAOs();
			}

			long old = address;
			initBuffers(Math.max(capacity + (capacity >> 1), minCapacity));
			vbo = vbos[index];
			if (useVAOs) {
				vao = vaos[index];
			}
			address = addresses[index];
			MemoryUtil.copyMemory(old + (verticesTotal - verticesBatch) * vertexSize, address, verticesBatch * vertexSize);
			verticesTotal = verticesBatch;
		}
	}

	protected void deleteVBOs() {
		if (vbos == null) {
			throw new IllegalStateException();
		}
		for (int i = 0; i < BUFFERS; i++) {
			int vbo = vbos[i];
			tasks[i].push(() -> {
				BufferStorage.bindBuffer(GL15.GL_ARRAY_BUFFER, vbo, false);
				BufferStorage.unmapBuffer(GL15.GL_ARRAY_BUFFER, vbo);
				BufferStorage.bindBuffer(GL15.GL_ARRAY_BUFFER, 0, false);
				BufferStorage.deleteBuffer(vbo);
			});
		}
		vbos = null;
		addresses = null;
	}

	protected void deleteVAOs() {
		if (vaos == null) {
			throw new IllegalStateException();
		}
		for (int i = 0; i < BUFFERS; i++) {
			int vao = vaos[i];
			tasks[i].push(() -> {
				VertexArray.deleteVertexArray(vao);
			});
		}
		vaos = null;
	}

	@Override
	public void startFrame() {
		super.startFrame();
		index = (index + 1) % BUFFERS;
		vbo = vbos[index];
		if (useVAOs) {
			vao = vaos[index];
		}
		address = addresses[index];
		if (syncs[index] != null) {
			Sync.waitSync(syncs[index]);
			Sync.deleteSync(syncs[index]);
			syncs[index] = null;
		}
		while (!tasks[index].isEmpty()) {
			tasks[index].pop().run();
		}

		if (FastEntityRenderConfig.useExplicitFlush != useFlushExplicit) {
			useFlushExplicit = FastEntityRenderConfig.useExplicitFlush;
			for (int i = 0; i < BUFFERS; i++) {
				BufferStorage.bindBuffer(GL15.GL_ARRAY_BUFFER, vbos[i], false);
				BufferStorage.unmapBuffer(GL15.GL_ARRAY_BUFFER, vbos[i]);
				addresses[i] = NIOBufferUtil.getAddress(BufferStorage.mapBuffer(GL15.GL_ARRAY_BUFFER, vbos[i], 0L, capacity, GL30.GL_MAP_WRITE_BIT | GL30.GL_MAP_INVALIDATE_BUFFER_BIT | (useFlushExplicit ? GL30.GL_MAP_FLUSH_EXPLICIT_BIT : 0) | GL30.GL_MAP_UNSYNCHRONIZED_BIT | GL44.GL_MAP_PERSISTENT_BIT));
				BufferStorage.bindBuffer(GL15.GL_ARRAY_BUFFER, 0, false);
			}
			address = addresses[index];
		}
		if (FastEntityRenderConfig.useVAOs && VertexArray.isSupported() != useVAOs) {
			useVAOs = FastEntityRenderConfig.useVAOs && VertexArray.isSupported();
			if (useVAOs) {
				createVAOs();
			} else {
				deleteVAOs();
			}
		}
	}

	@Override
	public void endFrame() {
		super.endFrame();
		syncs[index] = Sync.createSync();
	}

	@Override
	protected void renderBatch() {
		if (useFlushExplicit) {
			BufferStorage.flushBuffer(GL15.GL_ARRAY_BUFFER, vbo, (verticesTotal - verticesBatch) * vertexSize, verticesBatch * vertexSize);
		}

		if (useVAOs) {
			VertexArray.bindVertexArray(vao);
		} else {
			this.setupClientState();
			BufferStorage.bindBuffer(GL15.GL_ARRAY_BUFFER, vbo, true);
			this.setupAttributePointers();
		}

		GL11.glDrawArrays(GL11.GL_QUADS, verticesTotal - verticesBatch, verticesBatch);

		if (useVAOs) {
			VertexArray.bindVertexArray(0);
		} else {
			this.resetClientState();
			BufferStorage.bindBuffer(GL15.GL_ARRAY_BUFFER, 0, true);
		}
	}

	protected void setupClientState() {
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
	}

	protected void setupAttributePointers() {
		GL11.glVertexPointer(3, GL11.GL_FLOAT, vertexSize, 0L);
		GL11.glTexCoordPointer(2, GL11.GL_FLOAT, vertexSize, 12L);
		GL11.glNormalPointer(GL11.GL_BYTE, vertexSize, 20L);
	}

	protected void resetClientState() {
		GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
		GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
	}

}
