package com.losmonos.monosutils.config;

/**
 * Una region rectangular con ajustes de proteccion.
 * Todos los ajustes booleanos representan "proteccion activa" (true = la restriccion esta ON).
 */
public class Zone {
	public String name;
	public String world = "minecraft:overworld";
	public int x1, y1, z1;
	public int x2, y2, z2;

	// Ajustes (por defecto al crear: bloquea romper/poner, bloquea spawn hostil, PvP desactivado)
	public boolean spawnblock = true; // bloquea spawn de mobs hostiles
	public boolean breakBlocks = true; // bloquea romper bloques  (comando: "break")
	public boolean place = true;       // bloquea poner bloques
	public boolean pvp = true;         // true = PvP BLOQUEADO
	public boolean alldamage = false;  // true = bloquea TODO el dano dentro de la zona
	public boolean ignorey = false;    // true = la zona ignora la altura (columna completa)

	public Zone() {}

	public int minX() { return Math.min(x1, x2); }
	public int maxX() { return Math.max(x1, x2); }
	public int minY() { return Math.min(y1, y2); }
	public int maxY() { return Math.max(y1, y2); }
	public int minZ() { return Math.min(z1, z2); }
	public int maxZ() { return Math.max(z1, z2); }

	public boolean contains(String worldId, int x, int y, int z) {
		if (!worldId.equals(this.world)) return false;
		if (x < minX() || x > maxX()) return false;
		if (z < minZ() || z > maxZ()) return false;
		if (ignorey) return true;
		return y >= minY() && y <= maxY();
	}
}
