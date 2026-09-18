package meldexun.fastentityrender.renderer;

import static meldexun.memoryutil.UnsafeUtil.UNSAFE;

import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import meldexun.fastentityrender.api.IModelRenderer;
import meldexun.fastentityrender.api.IVertexConsumer;
import meldexun.matrixutil.MatrixStack;

public abstract class FastModelRenderer implements IVertexConsumer {

	public static final int VERTEX_SIZE = 24;

	private final Stack<IModelRenderer> stack = new ObjectArrayList<>();
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

	public void render(IModelRenderer bone, float scale, boolean isRoot) {
		int vertices = vertices(bone);
		if (vertices <= 0) {
			return;
		}

		boolean batched = isBatching;
		if (!batched) {
			startBatch();
		}

		this.ensureCapacity((verticesTotal + vertices) * vertexSize);

		matrixStack.push();
		bone.applyTransformation(matrixStack, scale, isRoot);
		bone.render(matrixStack, scale, this);
		bone.pushChildren(stack);
		while (!stack.isEmpty()) {
			IModelRenderer bone1 = stack.pop();
			if (bone1 != null) {
				if (bone1.shouldRender()) {
					matrixStack.push();
					bone1.applyTransformation(matrixStack, scale, false);
					bone1.render(matrixStack, scale, this);
					stack.push(null);
					bone1.pushChildren(stack);
				}
			} else {
				matrixStack.pop();
			}
		}
		matrixStack.pop();

		if (!batched) {
			endBatch();
		}
	}

	private int vertices(IModelRenderer bone) {
		int vertices = 0;
		stack.push(bone);
		while (!stack.isEmpty()) {
			IModelRenderer bone1 = stack.pop();
			if (bone1.shouldRender()) {
				vertices += bone1.vertices();
				bone1.pushChildren(stack);
			}
		}
		return vertices;
	}

	@Override
	public void bufferVertex(float x, float y, float z, float u, float v, int n) {
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
