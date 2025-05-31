package meldexun.fastentityrender;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import meldexun.fastentityrender.renderer.FastModelRenderer;
import meldexun.matrixutil.MathUtil;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

@Mod(modid = FastEntityRender.MODID, acceptableRemoteVersions = "*")
public class FastEntityRender {

	public static final String MODID = "fastentityrender";
	public static final Logger LOGGER = LogManager.getLogger(MODID);

	@EventHandler
	public void onFMLConstructionEvent(FMLConstructionEvent event) {
		MathUtil.setSinFunc(a -> MathHelper.sin((float) a));
		MathUtil.setCosFunc(a -> MathHelper.cos((float) a));

		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void onRenderTickEvent(TickEvent.RenderTickEvent event) {
		if (event.phase == Phase.START) {
			FastModelRenderer.getInstance().startFrame();
		} else {
			FastModelRenderer.getInstance().endFrame();
		}
	}

}
