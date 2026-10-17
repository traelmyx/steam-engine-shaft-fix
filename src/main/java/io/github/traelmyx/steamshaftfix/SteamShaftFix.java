package io.github.traelmyx.steamshaftfix;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Create issue #3701: steam engines on one network break their shafts when the boiler's power changes.
 * <ul>
 * <li>An engine whose boiler gives no power still lines its remembered direction up with the shaft, which other engines keep
 * turning, but at zero power it compares against a direction that ignores which way the engine faces. Half the engines
 * flip, and when the heat comes back while the network is standing still, the network starts up backwards.</li>
 * <li>Every time the boiler's power steps up or down, the engine shaft that drives the network briefly detaches and
 * reattaches it. Each block counts that as stopping and starting, and after a few changes the network decides its speed
 * "changed too often" and breaks shafts.</li>
 * </ul>
 */
@Mod(SteamShaftFix.MOD_ID)
public class SteamShaftFix {
	public static final String MOD_ID = "steam_shaft_fix";
	public static final Logger LOGGER = LoggerFactory.getLogger("Steam Engine Shaft Fix");

	/** Engines without power keep their direction. */
	public static boolean keepDirection = true;
	/** A change of engine speed that never stops the network doesn't count towards the flicker limit. */
	public static boolean noFlickerForPowerChanges = true;

	/** Set while an engine shaft changes from one speed to another in the same direction. */
	public static final ThreadLocal<Boolean> CHANGING_SPEED = new ThreadLocal<>();

	public SteamShaftFix() {
		loadConfig(FMLPaths.CONFIGDIR.get());
		checkTargets();
	}

	/** The mixins are optional so a Create update can't crash a pack; say so when they no longer find their targets. */
	private static void checkTargets() {
		try {
			ClassLoader loader = SteamShaftFix.class.getClassLoader();
			Class.forName("com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity", false, loader).getDeclaredMethod("tick");
			Class.forName("com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity", false, loader).getDeclaredMethod("updateGeneratedRotation");
			Class.forName("com.simibubi.create.content.kinetics.base.KineticBlockEntity", false, loader).getDeclaredMethod("onSpeedChanged", float.class);
		} catch (ReflectiveOperationException | LinkageError e) {
			LOGGER.warn("This Create version changed the steam engine code; the fix may be inactive ({})", e.toString());
		}
	}

	static void loadConfig(Path configDir) {
		Path file = configDir.resolve(MOD_ID + ".properties");
		Properties props = new Properties();
		if (Files.exists(file)) {
			try (Reader reader = Files.newBufferedReader(file)) {
				props.load(reader);
			} catch (IOException e) {
				LOGGER.warn("Could not read {}, using defaults", file, e);
			}
		}
		keepDirection = Boolean.parseBoolean(props.getProperty("keepDirection", "true"));
		noFlickerForPowerChanges = Boolean.parseBoolean(props.getProperty("noFlickerForPowerChanges", "true"));
		props.setProperty("keepDirection", Boolean.toString(keepDirection));
		props.setProperty("noFlickerForPowerChanges", Boolean.toString(noFlickerForPowerChanges));
		try {
			Files.createDirectories(configDir);
			try (Writer writer = Files.newBufferedWriter(file)) {
				props.store(writer, "Steam Engine Shaft Fix. keepDirection: engines without power keep their direction. noFlickerForPowerChanges: boiler power changes don't count towards the shaft flicker limit.");
			}
		} catch (IOException e) {
			LOGGER.warn("Could not write {}", file, e);
		}
	}
}
