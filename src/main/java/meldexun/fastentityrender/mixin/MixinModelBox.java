package meldexun.fastentityrender.mixin;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import meldexun.fastentityrender.util.CubeData;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;

@Mixin(ModelBox.class)
public class MixinModelBox implements Supplier<CubeData> {

	@Unique
	private CubeData boxData;

	@Inject(method = "<init>(Lnet/minecraft/client/model/ModelRenderer;IIFFFIIIFZ)V", at = @At("RETURN"))
	public void init(ModelRenderer renderer, int texU, int texV, float x, float y, float z, int dx, int dy, int dz, float delta, boolean mirror, CallbackInfo info) {
		this.boxData = new CubeData(renderer, texU, texV, mirror ? x - dx - delta : x + delta, y + delta, z + delta, dx, dy, dz, delta, mirror);
	}

	@Override
	public CubeData get() {
		return boxData;
	}

}
