package meldexun.fastentityrender;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import meldexun.betterconfig.api.BetterConfig;
import meldexun.betterconfig.api.LoadEarly;

@BetterConfig(modid = FastEntityRender.MODID, addDefaultsToComments = false)
@LoadEarly
public class FastEntityRenderConfig {

	public static boolean forceLegacyRenderer = false;
	public static boolean useExplicitFlush = true;
	public static boolean useVAOs = true;
	public static List<Pattern> batchableMethodCalls = Stream.of(
			"^net/minecraft/client/model/ModelRenderer\\.(?:render|func_78785_a|renderWithRotation|func_78791_b)",
			"^net/minecraft/entity/Entity\\.(?:isSneaking|func_70093_af)",
			"^net/minecraft/entity/EntityLivingBase\\.(?:isChild|func_70631_g_)",
			"^net/minecraft/entity/monster/AbstractIllager\\.(?:getArmPose|func_193077_p)",
			"^net/minecraft/client/model/ModelDragon\\.(?:updateRotations|func_78214_a)",
			"^net/minecraft/entity/boss/EntityDragon\\.(?:getHeadPartYOffset|func_184667_a)",
			"^net/minecraft/entity/boss/EntityDragon\\.(?:getMovementOffsets|func_70974_a)",

			"^net/minecraft/util/math/MathHelper\\.",
			"^java/lang/Math\\.",

			"^net/minecraft/client/renderer/GlStateManager\\.(?:pushMatrix|func_179094_E|popMatrix|func_179121_F|translate|func_179109_b|func_179137_b|rotate|func_179114_b|func_187444_a|scale|func_179139_a|func_179152_a)",
			"^org/lwjgl/opengl/GL\\.gl(?:PushMatrix|PopMatrix|Translate|Rotate|Scale)[fd]?",

			"^com/dhanantry/scapeandrunparasites/entity/monster/ancient/EntityOronco\\.livingTEN(?:LA|RA|UL|UR)",
			"^familiarfauna/entities/EntityTurkey\\.getTurkeyType")
			.map(Pattern::compile)
			.collect(Collectors.toList());

}
