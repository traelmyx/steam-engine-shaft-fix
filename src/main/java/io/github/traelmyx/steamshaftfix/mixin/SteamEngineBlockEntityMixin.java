package io.github.traelmyx.steamshaftfix.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlockEntity;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity;
import io.github.traelmyx.steamshaftfix.SteamShaftFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = SteamEngineBlockEntity.class, remap = false)
public abstract class SteamEngineBlockEntityMixin {
	@Unique
	private float steam_shaft_fix$efficiency;

	// require = 0: if a Create update changes this method, the fix stops working instead of crashing the game
	@ModifyExpressionValue(method = "tick", require = 0,
		at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(FFF)F", ordinal = 0))
	private float steam_shaft_fix$rememberEfficiency(float efficiency) {
		steam_shaft_fix$efficiency = efficiency;
		return efficiency;
	}

	// The engine lines its direction up with the shaft only if the shaft is driven by something else; an engine without
	// power has nothing to line up, and its comparison ignores the way it faces, so it would flip for no reason.
	@WrapOperation(method = "tick", require = 0,
		at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/steamEngine/PoweredShaftBlockEntity;hasSource()Z", ordinal = 0))
	private boolean steam_shaft_fix$keepDirectionWithoutPower(PoweredShaftBlockEntity shaft, Operation<Boolean> original) {
		if (SteamShaftFix.keepDirection && steam_shaft_fix$efficiency == 0)
			return false;
		return original.call(shaft);
	}
}
