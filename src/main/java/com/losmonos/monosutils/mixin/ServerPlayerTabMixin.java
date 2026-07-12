package com.losmonos.monosutils.mixin;

import com.losmonos.monosutils.indicator.IndicatorManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Agrega el circulo de prank + la estrella de edicion despues del nombre en la lista de TAB.
 * (El hover del circulo no funciona en el TAB, solo en el chat: limitacion del juego.)
 */
@Mixin(ServerPlayer.class)
public class ServerPlayerTabMixin {

	@Inject(method = "getTabListDisplayName", at = @At("RETURN"), cancellable = true)
	private void monosutils$tab(CallbackInfoReturnable<Component> cir) {
		ServerPlayer self = (ServerPlayer) (Object) this;
		Component suffix = IndicatorManager.buildSuffix(self);
		if (suffix == null || suffix.getSiblings().isEmpty()) return;
		Component base = cir.getReturnValue();
		if (base == null) base = Component.literal(self.getGameProfile().getName());
		cir.setReturnValue(Component.empty().append(base).append(suffix));
	}
}
