package meldexun.fastentityrender.mixin.minecraft;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import it.unimi.dsi.fastutil.Stack;
import meldexun.fastentityrender.EntityRenderer;
import meldexun.fastentityrender.api.IModelBox;
import meldexun.fastentityrender.api.IModelRenderer;
import meldexun.matrixutil.MatrixStack;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;

@Mixin(ModelRenderer.class)
public class ModelRendererMixin implements IModelRenderer {

	@Shadow
	public float rotationPointX;
	@Shadow
	public float rotationPointY;
	@Shadow
	public float rotationPointZ;
	@Shadow
	public float rotateAngleX;
	@Shadow
	public float rotateAngleY;
	@Shadow
	public float rotateAngleZ;
	@Shadow
	public boolean showModel;
	@Shadow
	public boolean isHidden;
	@Shadow
	public List<ModelBox> cubeList;
	@Shadow
	public List<ModelRenderer> childModels;
	@Shadow
	public float offsetX;
	@Shadow
	public float offsetY;
	@Shadow
	public float offsetZ;

	@Overwrite
	public void render(float scale) {
		EntityRenderer.render(this, scale);
	}

	@Overwrite
	public void renderWithRotation(float scale) {
		EntityRenderer.render(this, scale);
	}

	@Override
	public boolean shouldRender() {
		return !this.isHidden && this.showModel;
	}

	@Override
	public void applyTransformation(MatrixStack matrixStack, float scale, boolean isRoot) {
		matrixStack.translate(this.offsetX + this.rotationPointX * scale, this.offsetY + this.rotationPointY * scale, this.offsetZ + this.rotationPointZ * scale);
		matrixStack.rotateZ(this.rotateAngleZ);
		matrixStack.rotateY(this.rotateAngleY);
		matrixStack.rotateX(this.rotateAngleX);
	}

	@Override
	public void pushChildren(Stack<IModelRenderer> stack) {
		if (this.childModels != null) {
			for (int i = 0; i < this.childModels.size(); i++) {
				stack.push((IModelRenderer) this.childModels.get(i));
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<IModelBox> cubes() {
		return (List<IModelBox>) (List<?>) this.cubeList;
	}

}
