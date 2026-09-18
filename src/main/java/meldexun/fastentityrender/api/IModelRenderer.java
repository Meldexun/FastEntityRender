package meldexun.fastentityrender.api;

import java.util.List;

import it.unimi.dsi.fastutil.Stack;
import meldexun.matrixutil.MatrixStack;

public interface IModelRenderer {

	boolean shouldRender();

	default int vertices() {
		return cubes().size() * 6 * 4;
	}

	void applyTransformation(MatrixStack matrixStack, float scale, boolean isRoot);

	default void render(MatrixStack matrixStack, float scale, IVertexConsumer vertexConsumer) {
		List<IModelBox> cubes = cubes();
		for (int i = 0; i < cubes.size(); i++) {
			cubes.get(i).bufferCube(matrixStack, scale, vertexConsumer);
		}
	}

	void pushChildren(Stack<IModelRenderer> stack);

	List<IModelBox> cubes();

}
