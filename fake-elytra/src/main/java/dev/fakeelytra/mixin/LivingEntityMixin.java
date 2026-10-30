package dev.fakeelytra.mixin;

import dev.fakeelytra.ModuleManager;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets the local singleplayer player glide (and use fireworks) without wearing an elytra.
 * The integrated server runs in the same game, so this one client-side mixin covers both sides.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "canGlide", at = @At("HEAD"), cancellable = true)
    private void fakeelytra$canGlide(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player
                && ModuleManager.isActiveFor(ModuleManager.FAKE_ELYTRA, self.getUUID())
                && !self.onGround()
                && !self.isPassenger()
                && !self.hasEffect(MobEffects.LEVITATION)) {
            cir.setReturnValue(true);
        }
    }
}
