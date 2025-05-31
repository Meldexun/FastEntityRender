package meldexun.fastentityrender.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import meldexun.fastentityrender.renderer.FastModelRenderer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mixin(ModelRenderer.class)
public class MixinModelRenderer {

	@Overwrite
	@SideOnly(Side.CLIENT)
	public void render(float scale) {
		FastModelRenderer.getInstance().render((ModelRenderer) (Object) this, scale);
	}

}
