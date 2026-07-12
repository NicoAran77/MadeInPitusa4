package com.losmonos.monosutils.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Estado persistente del mod. Se guarda al instante en config/monosutils.json.
 */
public class Config {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("monosutils.json");

	public static Config INSTANCE = new Config();

	// General
	public String exemptTag = "monos_exempt";

	// Zonas (nombre -> zona)
	public Map<String, Zone> zones = new LinkedHashMap<>();

	// Combate
	public int combatTagSeconds = 10;
	public boolean combatLogKill = true;

	// Prank (indicador social). Nivel 1-3, solo admin.
	public Map<Integer, String> prankDescriptions = new HashMap<>();
	public Map<String, Integer> prankLevels = new HashMap<>(); // uuid -> 1..3

	// Estrella por edicion de Made In Pitusa
	public int currentEdition = 4;
	public Map<String, Integer> editionCounts = new HashMap<>(); // uuid -> nº ediciones jugadas
	// Color de la estrella segun cuantas ediciones jugo (hex). Editable.
	public Map<Integer, String> editionColors = new LinkedHashMap<>();

	private void applyDefaults() {
		if (prankDescriptions.isEmpty()) {
			prankDescriptions.put(1, "solo pranks inofensivos. Nada que dane bloques o cosas.");
			prankDescriptions.put(2, "pranks moderados permitidos. No destruir su base.");
			prankDescriptions.put(3, "todo permitido (matarlo, llenarle la base de bloques, etc.).");
		}
		if (editionColors.isEmpty()) {
			// 1 edicion (recien llegado) -> sin estrella
			editionColors.put(2, "#CD7F32"); // bronce
			editionColors.put(3, "#C0C0C0"); // plata
			editionColors.put(4, "#FFD700"); // oro
		}
	}

	public static void load() {
		try {
			if (Files.exists(FILE)) {
				String json = Files.readString(FILE);
				Config loaded = GSON.fromJson(json, Config.class);
				if (loaded != null) INSTANCE = loaded;
			}
		} catch (Exception e) {
			System.err.println("[MonosUtils] No se pudo leer la config, usando valores por defecto: " + e.getMessage());
		}
		INSTANCE.applyDefaults();
		save();
	}

	public static void save() {
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, GSON.toJson(INSTANCE));
		} catch (IOException e) {
			System.err.println("[MonosUtils] No se pudo guardar la config: " + e.getMessage());
		}
	}
}
