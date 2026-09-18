package meldexun.fastentityrender.mixin.mobends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import goblinbob.mobends.core.client.model.IModelPart;
import goblinbob.mobends.core.client.model.ModelPartExtended;
import it.unimi.dsi.fastutil.Stack;
import meldexun.fastentityrender.EntityRenderer;
import meldexun.fastentityrender.api.IModelRenderer;

@Mixin(ModelPartExtended.class)
public abstract class ModelPartExtendedMixin extends ModelPartMixin {

	@Shadow(remap = false)
    protected IModelPart extension;

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
	public void pushChildren(Stack<IModelRenderer> stack) {
		super.pushChildren(stack);
		if (this.extension instanceof IModelRenderer) {
			stack.push((IModelRenderer) this.extension);
		}
	}

}
