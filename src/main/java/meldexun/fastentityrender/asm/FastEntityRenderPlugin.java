package meldexun.fastentityrender.asm;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;

import meldexun.fastentityrender.asm.tweaker.FastEntityRenderTweaker;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.common.launcher.FMLInjectionAndSortingTweaker;
import net.minecraftforge.fml.relauncher.CoreModManager;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions({ "meldexun.fastentityrender.asm", "meldexun.asmutil2" })
public class FastEntityRenderPlugin implements IFMLLoadingPlugin {

	@SuppressWarnings("unchecked")
	public FastEntityRenderPlugin() {
		try {
			if (((List<ITweaker>) Launch.blackboard.get("Tweaks")).stream().noneMatch(FMLInjectionAndSortingTweaker.class::isInstance)) {
				((List<String>) Launch.blackboard.get("TweakClasses")).add(FastEntityRenderTweaker.class.getName());
			} else {
				((List<ITweaker>) Launch.blackboard.get("Tweaks")).add(new FastEntityRenderTweaker());
			}
			Field _tweakSorting = CoreModManager.class.getDeclaredField("tweakSorting");
			_tweakSorting.setAccessible(true);
			((Map<String, Integer>) _tweakSorting.get(null)).put(FastEntityRenderTweaker.class.getName(), 1001);
		} catch (ReflectiveOperationException e) {
			throw new UnsupportedOperationException(e);
		}

		addModsToClasspath("mobends");
	}

	private static void addModsToClasspath(String... modids) {
		try {
			Predicate<String> isTarget = Pattern.compile("^.*\"modid\"\\s*:\\s*\"" + Arrays.stream(modids).collect(Collectors.joining("|", "(?:", ")")) + "\".*$", Pattern.DOTALL).asPredicate();

			Path modsDir = Optional.ofNullable(Launch.minecraftHome).map(File::toPath).orElse(Paths.get(".")).resolve("mods");
			if (!Files.exists(modsDir)) return;
			if (!Files.isDirectory(modsDir)) return;

			Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
			addURL.setAccessible(true);

			for (Path file : Files.find(modsDir, 1, (p, a) -> a.isRegularFile() && p.getFileName().toString().endsWith(".jar")).collect(Collectors.toList())) {
				try (JarFile jar = new JarFile(file.toFile())) {
					JarEntry entry = jar.getJarEntry("mcmod.info");
					if (entry == null) continue;
					try (InputStream in = jar.getInputStream(entry)) {
						if (!isTarget.test(IOUtils.toString(in, StandardCharsets.UTF_8))) {
							continue;
						}
					}
				}

				URL url = file.toUri().toURL();
				addURL.invoke(Launch.classLoader.getClass().getClassLoader(), url);
				addURL.invoke(Launch.classLoader, url);
			}
		} catch (Exception e) {
			throw new UnsupportedOperationException(e);
		}
	}

	@Override
	public String[] getASMTransformerClass() {
		return null;
	}

	@Override
	public String getModContainerClass() {
		return null;
	}

	@Override
	public String getSetupClass() {
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data) {
		MixinBootstrap.init();
		if (Boolean.FALSE.equals(data.get("runtimeDeobfuscationEnabled"))) {
			MixinEnvironment.getDefaultEnvironment().setObfuscationContext("searge");
			CoreModManager.getIgnoredMods().add("mixin-0.8.7.jar");
			CoreModManager.getIgnoredMods().add("asm-util-6.2.jar");
			CoreModManager.getIgnoredMods().add("asm-analysis-6.2.jar");
			CoreModManager.getIgnoredMods().add("asm-tree-6.2.jar");
			CoreModManager.getIgnoredMods().add("asm-6.2.jar");
		}
	}

	@Override
	public String getAccessTransformerClass() {
		return null;
	}

}
