# Monos Utils

Mod de servidor para Fabric 1.21.11 (Los Monos). Mappings: Mojang. Java 21.

## Sistemas

**1. Zonas** (solo OP) — regiones con proteccion. Varita = hacha de oro (click izq = pos1, click der = pos2).
- `/zone wand`
- `/zone pos1` / `/zone pos2`
- `/zone create <nombre>` / `/zone redefine <nombre>` / `/zone remove <nombre>`
- `/zone list` / `/zone info <nombre>`
- `/zone set <nombre> <ajuste> <true|false>` — ajustes: `spawnblock`, `break`, `place`, `pvp`, `alldamage`, `ignorey` (true = restriccion activa; `pvp true` = PvP bloqueado)
- `/zone clear [nombre]` — mata hostiles dentro de la zona (o de todas)
- `/zone reload`

Los OP y quien tenga el tag `monos_exempt` estan exentos de la proteccion.

**2. Combate** — automatico, sin comandos. Al dar/recibir dano de otro jugador, ambos quedan en combate `combatTagSeconds` (10 s). Si te desconectas en combate, mueres y sueltas el inventario. Requiere `keepInventory=false` (asi el cadaver lo genera Player Corpses).

**3. Teletransporte** — lo maneja FTB Essentials (no incluido en este mod).

**4. Indicador social** (no ejecuta nada, solo muestra):
- Circulo de prank `①②③` despues del nombre (1 verde, 2 amarillo, 3 rojo), con descripcion al pasar el mouse en el chat.
- Estrella `★` de color segun cuantas ediciones de Made In Pitusa jugo el jugador (configurable; por defecto 2=bronce, 3=plata, 4=oro; 1=sin estrella).

Comandos:
- `/prank set <jugador> <1-3>` (OP)
- `/prank clear <jugador>` (OP)
- `/prank desc <1-3> <texto...>` (OP)
- `/prank check [jugador]`
- `/pitusa set <jugador> <ediciones>` (OP)
- `/pitusa clear <jugador>` (OP)
- `/pitusa check [jugador]`

## Configuracion

Todo se guarda en `config/monosutils.json` y se puede editar en caliente con `/zone reload`.

## Compilar

El `.jar` sale por GitHub Actions (pestana Actions -> ultimo build -> Artifacts). El archivo queda en `build/libs/`.
