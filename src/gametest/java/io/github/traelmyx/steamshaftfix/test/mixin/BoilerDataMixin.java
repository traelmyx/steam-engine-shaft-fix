package io.github.traelmyx.steamshaftfix.test.mixin;

import com.simibubi.create.content.fluids.tank.BoilerData;
import io.github.traelmyx.steamshaftfix.test.SteamTests;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Test only: lets a test set how much power the boiler gives, the way a fluctuating heat source would. */
@Mixin(value = BoilerData.class, remap = false)
public class BoilerDataMixin {
	@Inject(method = "getEngineEfficiency", at = @At("HEAD"), cancellable = true)
	private void steam_shaft_fix_gametest$efficiency(int boilerSize, CallbackInfoReturnable<Float> cir) {
		Float efficiency = SteamTests.EFFICIENCY;
		if (efficiency != null)
			cir.setReturnValue(efficiency);
	}
}
