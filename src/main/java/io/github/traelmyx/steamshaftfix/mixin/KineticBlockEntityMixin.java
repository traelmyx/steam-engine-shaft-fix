package io.github.traelmyx.steamshaftfix.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import io.github.traelmyx.steamshaftfix.SteamShaftFix;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * When an engine shaft that drives the network changes speed, Create detaches the network and attaches it again, so every
 * block sees its speed drop to 0 and come back and adds to its flicker score. The network never really stopped, so those
 * two steps aren't counted while {@link GeneratingKineticBlockEntityMixin} marks such a change. Real stops, starts and
 * direction changes still count.
 */
@Mixin(value = KineticBlockEntity.class, remap = false)
public abstract class KineticBlockEntityMixin {
	@WrapOperation(method = "onSpeedChanged", require = 0,
		at = @At(value = "FIELD", target = "Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;flickerTally:I", opcode = Opcodes.PUTFIELD))
	private void steam_shaft_fix$skipFlicker(KineticBlockEntity self, int value, Operation<Void> original) {
		if (SteamShaftFix.CHANGING_SPEED.get() != null)
			return;
		original.call(self, value);
	}
}
