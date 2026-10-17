# Create: Steam Engine Shaft Fix

Fixes [Create issue #3701](https://github.com/Creators-of-Create/Create/issues/3701): steam engines that share a network break their shafts, or start turning the other way, when the boiler's power changes.

- **Engines without power keep their direction.** At zero boiler power an engine still lines its direction up with a shaft that other engines keep turning, but the comparison ignores which way the engine faces, so about half of them flip. When the heat comes back while everything stands still, the network starts up backwards.
- **Power changes don't count as flicker.** Each time the boiler's power steps up or down, the engine shaft that drives the network detaches it and attaches it again. Every block counts that as a stop and a start, and a boiler whose heat changes often soon makes the network think its speed "changed too often", which breaks shafts. Real stops, starts and direction changes still count.

- Server side (or singleplayer). Requires Create 6.0 for Minecraft 1.21.1 on NeoForge.
- `config/steam_shaft_fix.properties`: `keepDirection`, `noFlickerForPowerChanges`.
- If a Create update changes the code this hooks into, the fix switches itself off and logs a warning; it never stops the game from loading.

Tests: a boiler tank with an engine on its north and its south side, both driving one network through shafts and a row of cogwheels. A test-only hook sets the boiler's power each tick. Jumping between 0 and 100% every 4 ticks for 20 seconds, the fixed run must keep every block and never reverse; the same run without the fix must break shafts or reverse, so the test can't pass by accident. Two more tests check that on/off cycles and a slow shutdown and restart still run at the right speeds. A client smoke test builds a tank, engine and shaft and takes a screenshot.

Not affiliated with the Create team. MIT licensed.
