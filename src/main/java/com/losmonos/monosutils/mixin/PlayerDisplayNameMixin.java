package com.losmonos.monosutils.mixin;

import com.losmonos.monosutils.indicator.IndicatorManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Agrega el circulo de prank + la estrella de edicion despues del nombre en el chat
 * (getDisplayName tambien afecta el nombre sobre la cabeza; si molesta, se puede restringir).
 */
@Mixin(Player.class)
public class PlayerDisplayNameMixin {

	@Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
	private void monosutils$appendIndicator(CallbackInfoReturnable<Component> cir) {
		Player self = (Player) (Object) this;
		Component suffix = IndicatorManager.buildSuffix(self);
		if (suffix == null || suffix.getSiblings().isEmpty()) return;
		Component base = cir.getReturnValue();
		if (base == null) base = Component.literal(self.getGameProfile().getName());
		cir.setReturnValue(Component.empty().append(base).append(suffix));
	}
}
