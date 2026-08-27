package meldexun.fastentityrender;

import meldexun.fastentityrender.integration.Optifine;
import meldexun.fastentityrender.integration.OptifineLegacyModelRenderer;
import meldexun.fastentityrender.integration.OptifinePersistentModelRenderer;
import meldexun.fastentityrender.renderer.FastModelRenderer;
import meldexun.fastentityrender.renderer.LegacyModelRenderer;
import meldexun.fastentityrender.renderer.PersistentModelRenderer;
import net.minecraft.client.model.ModelRenderer;

public class EntityRenderer {

	private static FastModelRenderer renderer;

	public static FastModelRenderer getRenderer() {
		return renderer;
	}

	public static void startFrame() {
		if (renderer != null && PersistentModelRenderer.isSupported() && (FastEntityRenderConfig.forceLegacyRenderer != renderer instanceof LegacyModelRenderer)) {
			renderer.dispose();
			renderer = null;
		}
		if (renderer == null) {
			if (Optifine.OPTIFINE_DETECTED) {
				if (PersistentModelRenderer.isSupported() && !FastEntityRenderConfig.forceLegacyRenderer) {
					renderer = new OptifinePersistentModelRenderer(1 << 21);
				} else {
					renderer = new OptifineLegacyModelRenderer(1 << 17);
				}
			} else {
				if (PersistentModelRenderer.isSupported() && !FastEntityRenderConfig.forceLegacyRenderer) {
					renderer = new PersistentModelRenderer(1 << 20);
				} else {
					renderer = new LegacyModelRenderer(1 << 16);
				}
			}
		}

		renderer.startFrame();
	}

	public static void endFrame() {
		renderer.endFrame();
	}

	public static void startBatch() {
		renderer.startBatch();
	}

	public static void endBatch() {
		renderer.endBatch();
	}

	public static void render(ModelRenderer modelRenderer, float scale) {
		renderer.render(modelRenderer, scale);
	}

	public static void pushMatrix() {
		renderer.pushMatrix();
	}

	public static void popMatrix() {
		renderer.popMatrix();
	}

	public static void translate(double x, double y, double z) {
		translate((float) x, (float) y, (float) z);
	}

	public static void translate(float x, float y, float z) {
		renderer.translate(x, y, z);
	}

	public static void scale(double x, double y, double z) {
		scale((float) x, (float) y, (float) z);
	}

	public static void scale(float x, float y, float z) {
		renderer.scale(x, y, z);
	}

	public static void rotate(float angle, float x, float y, float z) {
		renderer.rotate((float) Math.toRadians(angle), x, y, z);
	}

}
