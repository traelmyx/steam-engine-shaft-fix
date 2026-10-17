package io.github.traelmyx.steamshaftfix.mixin;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlockEntity;
import io.github.traelmyx.steamshaftfix.SteamShaftFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Marks a steam engine shaft going from one speed to another in the same direction, see {@link KineticBlockEntityMixin}. */
@Mixin(value = GeneratingKineticBlockEntity.class, remap = false)
public abstract class GeneratingKineticBlockEntityMixin {
	@Inject(method = "updateGeneratedRotation", at = @At("HEAD"), require = 0)
	private void steam_shaft_fix$markSpeedChange(CallbackInfo ci) {
		GeneratingKineticBlockEntity self = (GeneratingKineticBlockEntity) (Object) this;
		if (!SteamShaftFix.noFlickerForPowerChanges || !(self instanceof PoweredShaftBlockEntity))
			return;
		float before = self.getSpeed(), after = self.getGeneratedSpeed();
		if (before != 0 && after != 0 && Math.signum(before) == Math.signum(after))
			SteamShaftFix.CHANGING_SPEED.set(Boolean.TRUE);
	}

	@Inject(method = "updateGeneratedRotation", at = @At("RETURN"), require = 0)
	private void steam_shaft_fix$clear(CallbackInfo ci) {
		SteamShaftFix.CHANGING_SPEED.remove();
	}
}
