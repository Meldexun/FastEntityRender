package meldexun.fastentityrender.integration;

import java.lang.invoke.MethodHandle;

import meldexun.fastentityrender.renderer.FastModelRenderer;
import meldexun.fastentityrender.util.MethodHandleUtil;

public class Optifine {

	public static final boolean OPTIFINE_DETECTED;
	static {
		boolean flag = false;
		try {
			Class.forName("optifine.OptiFineClassTransformer", false, Thread.currentThread().getContextClassLoader());
			flag = true;
		} catch (ClassNotFoundException e) {
			// ignore
		}
		OPTIFINE_DETECTED = flag;
	}
	private static final MethodHandle isShaders = MethodHandleUtil.method("Config", "isShaders");
	public static final int midTexCoordAttrib = MethodHandleUtil.get("net.optifine.shaders.Shaders", "midTexCoordAttrib", null, 0);
	public static final int tangentAttrib = MethodHandleUtil.get("net.optifine.shaders.Shaders", "tangentAttrib", null, 0);
	public static final int entityAttrib = MethodHandleUtil.get("net.optifine.shaders.Shaders", "entityAttrib", null, 0);
	public static final int vertexSizeShader = FastModelRenderer.VERTEX_SIZE + 16;

	public static boolean isShaders() {
		try {
			return (boolean) isShaders.invokeExact();
		} catch (Throwable e) {
			throw new UnsupportedOperationException(e);
		}
	}

}
