package io.github.traelmyx.steamshaftfix.test;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import io.github.traelmyx.steamshaftfix.SteamShaftFix;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.IntFunction;

/**
 * One boiler tank with a steam engine on its north and one on its south side. Each engine drives its own shaft line, and a
 * row of cogwheels joins both lines into one network. A test-only hook sets how much power the boiler gives each tick.
 */
@GameTestHolder(SteamTests.MOD_ID)
@PrefixGameTestTemplate(false)
public class SteamTests {
	public static final String MOD_ID = "steam_shaft_fix_gametest";

	/** Boiler power for the running test; null leaves the real boiler alone. */
	public static volatile Float EFFICIENCY = null;

	private static final BlockPos TANK = new BlockPos(3, 1, 3), ENGINE_N = new BlockPos(3, 1, 2), ENGINE_S = new BlockPos(3, 1, 4);
	private static final BlockPos SHAFT_N = new BlockPos(3, 1, 0);
	private static final List<BlockPos> KINETIC = new ArrayList<>();

	static {
		for (int x = 3; x <= 6; x++) {
			KINETIC.add(new BlockPos(x, 1, 0));
			KINETIC.add(new BlockPos(x, 1, 6));
		}
		for (int z = 0; z <= 6; z++) KINETIC.add(new BlockPos(7, 1, z));
	}

	private static void build(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
		helper.setBlock(TANK, AllBlocks.FLUID_TANK.getDefaultState());
		for (int x = 3; x <= 6; x++) {
			helper.setBlock(new BlockPos(x, 1, 0), AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.X));
			helper.setBlock(new BlockPos(x, 1, 6), AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.X));
		}
		for (int z = 0; z <= 6; z++)
			helper.setBlock(new BlockPos(7, 1, z), AllBlocks.COGWHEEL.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.X));
		// placing an engine turns the shaft two blocks in front of it into its powered shaft
		helper.setBlock(ENGINE_N, AllBlocks.STEAM_ENGINE.getDefaultState()
			.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.WALL).setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
		helper.setBlock(ENGINE_S, AllBlocks.STEAM_ENGINE.getDefaultState()
			.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.WALL).setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
	}

	private static int broken(GameTestHelper helper) {
		int broken = 0;
		for (BlockPos p : KINETIC)
			if (!(helper.getLevel().getBlockEntity(helper.absolutePos(p)) instanceof KineticBlockEntity)) broken++;
		return broken;
	}

	private static float speed(GameTestHelper helper) {
		return helper.getLevel().getBlockEntity(helper.absolutePos(SHAFT_N)) instanceof KineticBlockEntity k ? k.getSpeed() : 0;
	}

	/** Runs the boiler through a power pattern; reports how often the network reversed and how many blocks broke. */
	private static void run(GameTestHelper helper, String name, boolean fixed, int ticks, IntFunction<Float> pattern, Check check) {
		SteamShaftFix.keepDirection = fixed;
		SteamShaftFix.noFlickerForPowerChanges = fixed;
		build(helper);
		int[] reversals = {0};
		float[] lastSign = {0}, fastest = {0};
		for (int t = 1; t <= ticks; t++) {
			final int tick = t;
			helper.runAtTickTime(t, () -> {
				EFFICIENCY = pattern.apply(tick);
				float speed = speed(helper);
				fastest[0] = Math.max(fastest[0], Math.abs(speed));
				if (speed != 0) {
					if (lastSign[0] != 0 && Math.signum(speed) != lastSign[0]) reversals[0]++;
					lastSign[0] = Math.signum(speed);
				}
			});
		}
		helper.runAtTickTime(ticks + 5, () -> {
			EFFICIENCY = null;
			SteamShaftFix.keepDirection = true;
			SteamShaftFix.noFlickerForPowerChanges = true;
			int broken = broken(helper);
			System.out.printf("[steam_shaft_fix] %s, fix %s: %d reversals, %d of %d blocks broken, top speed %.0f rpm%n",
				name, fixed ? "on" : "off", reversals[0], broken, KINETIC.size(), fastest[0]);
			check.check(reversals[0], broken, fastest[0]);
			helper.succeed();
		});
	}

	private interface Check {
		void check(int reversals, int broken, float fastest);
	}

	/** Boiler power jumping between 0, 25, 50, 75 and 100% every 4 ticks, like a boiler on a flickering heat source. */
	private static Float fluctuating(int tick) {
		return FLUCTUATING[tick];
	}

	private static final float[] FLUCTUATING = new float[501];

	static {
		Random random = new Random(3701);
		float[] levels = {0f, 0.25f, 0.5f, 0.75f, 1f};
		for (int i = 0; i < FLUCTUATING.length; i++)
			FLUCTUATING[i] = i % 4 == 0 ? levels[random.nextInt(levels.length)] : FLUCTUATING[i - 1];
	}

	@GameTest(template = "empty", timeoutTicks = 500, batch = "fluctuating_fixed")
	public static void fluctuatingBoilerKeepsShafts(GameTestHelper helper) {
		run(helper, "fluctuating boiler", true, 400, SteamTests::fluctuating, (reversals, broken, fastest) -> {
			helper.assertTrue(fastest > 0, "the engines never turned");
			helper.assertTrue(broken == 0 && reversals == 0, broken + " blocks broke and the network reversed " + reversals + " times");
		});
	}

	@GameTest(template = "empty", timeoutTicks = 500, batch = "fluctuating_control")
	public static void controlReproducesBug(GameTestHelper helper) {
		run(helper, "fluctuating boiler", false, 400, SteamTests::fluctuating, (reversals, broken, fastest) ->
			// without the fix the same run must break shafts or reverse, or this test can't tell the fix works
			helper.assertTrue(broken > 0 || reversals > 0, "nothing broke and nothing reversed without the fix: the bug did not reproduce"));
	}

	/** Heat on and off every 2 seconds, ending with it off: the engines still start, run at full speed and stop. */
	@GameTest(template = "empty", timeoutTicks = 500, batch = "on_off")
	public static void onOffCyclesStillWork(GameTestHelper helper) {
		run(helper, "on/off cycles", true, 390, t -> t < 10 ? 0f : ((t / 40) % 2 == 0 ? 1f : 0f), (reversals, broken, fastest) -> {
			helper.assertTrue(fastest == 64, "top speed " + fastest + " rpm, expected 64");
			helper.assertTrue(broken == 0 && reversals == 0, broken + " blocks broke and the network reversed " + reversals + " times");
			helper.assertTrue(speed(helper) == 0, "the network still turns with the boiler off");
		});
	}

	/** Heat dying down step by step and coming back: speeds follow the boiler. */
	@GameTest(template = "empty", timeoutTicks = 400, batch = "ramp")
	public static void slowShutdownAndRestart(GameTestHelper helper) {
		float[] ramp = {1f, 0.75f, 0.5f, 0.25f, 0f, 0.25f, 0.5f, 0.75f, 1f};
		run(helper, "slow shutdown", true, 270, t -> ramp[Math.min(ramp.length - 1, t / 30)], (reversals, broken, fastest) -> {
			helper.assertTrue(broken == 0 && reversals == 0, broken + " blocks broke and the network reversed " + reversals + " times");
			helper.assertTrue(Math.abs(speed(helper)) == 64, "speed after the restart " + speed(helper) + " rpm, expected 64");
		});
	}
}
