package com.losmonos.monosutils;

import com.losmonos.monosutils.combat.CombatManager;
import com.losmonos.monosutils.config.Config;
import com.losmonos.monosutils.indicator.IndicatorManager;
import com.losmonos.monosutils.zones.ZoneManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MonosUtils implements ModInitializer {
	public static final String MODID = "monosutils";
	public static final Logger LOGGER = LoggerFactory.getLogger("MonosUtils");

	@Override
	public void onInitialize() {
		Config.load();

		// Eventos
		ZoneManager.registerEvents();
		CombatManager.registerEvents();

		// Comandos
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			ZoneManager.registerCommands(dispatcher);
			IndicatorManager.registerCommands(dispatcher);
		});

		LOGGER.info("Monos Utils cargado.");
	}
}
