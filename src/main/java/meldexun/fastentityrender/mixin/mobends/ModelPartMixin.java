package meldexun.fastentityrender.mixin.mobends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import goblinbob.mobends.core.client.model.IModelPart;
import goblinbob.mobends.core.client.model.ModelPart;
import goblinbob.mobends.core.math.SmoothOrientation;
import goblinbob.mobends.core.math.vector.Vec3f;
import meldexun.fastentityrender.EntityRenderer;
import meldexun.fastentityrender.api.IModelRenderer;
import meldexun.fastentityrender.mixin.minecraft.ModelRendererMixin;
import meldexun.matrixutil.MatrixStack;
import meldexun.matrixutil.Quaternion;

@Mixin(ModelPart.class)
public abstract class ModelPartMixin extends ModelRendererMixin implements IModelPart {

	@Shadow(remap = false)
    public Vec3f position;
	@Shadow(remap = false)
    public Vec3f scale;
	@Shadow(remap = false)
    public Vec3f offset;
	@Shadow(remap = false)
    public SmoothOrientation rotation;
	@Shadow(remap = false)
    public float offsetScale;
	@Shadow(remap = false)
    public Vec3f globalOffset;
	@Shadow(remap = false)
    protected IModelPart parent;

	@Override
	public void render(float scale) {
		EntityRenderer.render(this, scale);
	}

	@Override
	public void renderWithRotation(float scale) {
		EntityRenderer.render(this, scale);
	}

	@Override
	public void renderPart(float scale) {
		EntityRenderer.render(this, scale);
	}

	@Override
	public void renderJustPart(float scale) {
		EntityRenderer.render(this, scale, false);
	}

	@Override
	public void applyTransformation(MatrixStack matrixStack, float scale, boolean isRoot) {
		matrixStack.translate(this.globalOffset.x * scale, this.globalOffset.y * scale, this.globalOffset.z * scale);
		if (isRoot && this.parent instanceof IModelRenderer) {
			((IModelRenderer) this.parent).applyTransformation(matrixStack, scale * this.offsetScale, isRoot);
		}
		matrixStack.translate((this.offset.x + this.position.x) * scale * this.offsetScale, (this.offset.y + this.position.y) * scale * this.offsetScale, (this.offset.z + this.position.z) * scale * this.offsetScale);
		matrixStack.rotate(new Quaternion(this.rotation.getSmooth().x, this.rotation.getSmooth().y, this.rotation.getSmooth().z, this.rotation.getSmooth().w));
		matrixStack.scale(this.scale.x, this.scale.y, this.scale.z);
	}

}
