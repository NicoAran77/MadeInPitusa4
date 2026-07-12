package com.losmonos.monosutils.combat;

import com.losmonos.monosutils.MonosUtils;
import com.losmonos.monosutils.config.Config;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Etiqueta de combate: al dar/recibir dano de OTRO jugador, ambos quedan "en combate".
 * Si te desconectas en combate, mueres y sueltas el inventario (keepInventory=false en el server).
 * Como el server tiene Player Corpses, el mod NO suelta items manualmente: solo mata al jugador
 * y deja que la muerte real genere el cadaver.
 */
public class CombatManager {

	private static final Map<UUID, Long> COMBAT_UNTIL = new HashMap<>();

	public static boolean inCombat(UUID uuid) {
		Long until = COMBAT_UNTIL.get(uuid);
		return until != null && until > System.currentTimeMillis();
	}

	private static void tag(ServerPlayer player) {
		COMBAT_UNTIL.put(player.getUUID(), System.currentTimeMillis() + Config.INSTANCE.combatTagSeconds * 1000L);
	}

	public static void registerEvents() {
		// Detecta dano entre jugadores y etiqueta a ambos
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> {
			if (!(entity instanceof ServerPlayer victim)) return;
			if (source.getEntity() instanceof ServerPlayer attacker && attacker != victim) {
				tag(victim);
				tag(attacker);
			}
		});

		// Combat-log: al desconectar en combate, morir
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayer player = handler.player;
			if (player == null) return;
			if (Config.INSTANCE.combatLogKill && inCombat(player.getUUID())) {
				try {
					player.hurtServer(player.serverLevel(), player.damageSources().genericKill(), Float.MAX_VALUE);
				} catch (Throwable t) {
					try {
						player.die(player.damageSources().genericKill());
					} catch (Throwable t2) {
						MonosUtils.LOGGER.warn("No se pudo matar al jugador en combat-log: {}", t2.getMessage());
					}
				}
			}
			COMBAT_UNTIL.remove(player.getUUID());
		});
	}
}
