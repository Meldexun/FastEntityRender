package meldexun.fastentityrender.mixin.mobends;

import java.util.Collections;
import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import goblinbob.mobends.core.client.model.ModelPartTransform;
import goblinbob.mobends.core.math.SmoothOrientation;
import goblinbob.mobends.core.math.vector.Vec3f;
import it.unimi.dsi.fastutil.Stack;
import meldexun.fastentityrender.api.IModelBox;
import meldexun.fastentityrender.api.IModelRenderer;
import meldexun.matrixutil.MatrixStack;
import meldexun.matrixutil.Quaternion;

@Mixin(ModelPartTransform.class)
public abstract class ModelPartTransformMixin implements IModelRenderer {

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
    @Final
    public ModelPartTransform parent;

	@Override
	public boolean shouldRender() {
		return true;
	}

	@Override
	public List<IModelBox> cubes() {
		return Collections.emptyList();
	}

	@Override
	public void pushChildren(Stack<IModelRenderer> stack) {

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
