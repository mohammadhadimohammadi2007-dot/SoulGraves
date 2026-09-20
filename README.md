# SoulGraves

A grave system for Paper 1.21+. When a player dies their inventory is packed
into a marker block or entity at the death location instead of being dropped
into the world. The player can walk back and open it, or teleport to it from a
`/graves` GUI for a configurable fee.

The design goal was to sit between two extremes: full keep-inventory (feels
weightless) and raw vanilla drops (loses everything to lava or despawn timers).
SoulGraves lets a server keep death consequential while removing the "you had
five minutes to sprint back before your netherite despawned" problem.

## Behavior

On death, if the world and PvP context allow it, the plugin spawns a marker at
the death spot. The player's items and (optionally) XP are stored inside. A
soul particle trail leads from the player back to their most recent grave
while it's active.

Each player can have up to `max_graves_per_player` active graves at once. When
the cap is reached, dying again drops items vanilla-style; the oldest graves
are not evicted.

## Expiry

Graves expire on the more permissive of two clocks:

- Playtime: `grave_lifetime_hours` hours of the player's own online time.
  A player who logs off does not "waste" their grave timer.
- Wall-clock hard cap: `hard_cap_days` days from creation, regardless of
  playtime, so a grave can't linger forever on an inactive account.

When a grave expires, its contents drop at the marker's location.

## World and PvP behavior

- `worlds.mode` accepts `WHITELIST` or `BLACKLIST` with a `list` of world names.
  Graves are only spawned in worlds the mode allows.
- `pvp_behavior.default` sets the fallback behavior; per-world overrides live
  under `pvp_behavior.overrides`. Values: `GRAVE` (create a grave on any death)
  or `DROP` (fall back to vanilla drops).

Resource worlds are usually set to `DROP` so PvP loot flows normally.

## Marker

By default the marker is an ItemsAdder block if a namespace:id is configured
in `marker.itemsadder_id`. If ItemsAdder is missing, unloaded, or the id is
blank, the plugin falls back to a chest, player head, or skeleton skull
(configurable). `spawn_in_unsafe: true` allows markers inside lava or the void
so graves still form on catastrophic deaths.

## GUI

`/graves` opens a chest inventory showing the player's active graves. Each
grave shows death time, world and coordinates, death cause, and time left
before expiry. Clicking teleports the player back for a Vault-based cost
(if Vault + an economy plugin is installed; otherwise teleport is free).

The GUI's slot layout, filler pane, per-grave item, and lore lines are all
config-driven with MiniMessage formatting.

## Commands and permissions

| Command | Description | Permission |
|---|---|---|
| `/graves` | Open the grave list | `soulgraves.use` (default: true) |
| `/soulgraves reload` | Reload config | `soulgraves.admin` (op) |
| `/soulgraves purge <player>` | Delete a player's graves | `soulgraves.admin` (op) |
| `/soulgraves list <player>` | List a player's graves in chat | `soulgraves.admin` (op) |

Aliases: `/grave`, `/tombs`, `/deaths` for `/graves`; `/sg` for `/soulgraves`.

## Storage

Grave records (owner UUID, marker location, contents, timestamps) are stored in
SQLite in the plugin's data folder. Server restarts, world unloads, and plugin
reloads all preserve grave state.

## XP handling

`core.xp_handling` accepts:

- `VANILLA_DROP` — XP orbs drop as normal on death
- `STORE` — XP is captured into the grave and refunded when opened
- `DELETE` — XP is discarded

## Build

```
mvn clean package
```

## Dependencies

- Paper API 1.21.1
- VaultAPI 1.7.1 (optional, for teleport cost)
- ItemsAdder (optional, for custom markers)
- PlaceholderAPI (optional)
- SQLite JDBC 3.46

## License

All rights reserved. Personal project.
