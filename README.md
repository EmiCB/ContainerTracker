# WHIMC-Container-Tracker

**Version:** 1.4.0  
**Minecraft:** 1.21.11 (Spigot/Paper API `1.21.11-R0.1-SNAPSHOT`)

A WHIMC Minecraft plugin that logs container inventories and player physical interactions to MySQL. It is used alongside other WHIMC data-collection plugins (for example WHIMC-StudentFeedback) that read the tables this plugin writes.

## Download

Grab the latest built jar from the [latest release](https://github.com/whimc/WHIMC-ContainerTracker/releases/latest). A new release is published automatically on every push to `main`, and the version number in `pom.xml` is bumped by 0.1 after each release (for example 1.4.0 → 1.4.1).

The release artifact is named `WHIMC-Container-Tracker-<version>.jar`. On the server, plugin configuration is stored in `plugins/WHIMC-Container-Tracker/`.

## Prerequisites

- Minecraft **1.21.11** server (Spigot or Paper)
- **JDK 21** on the build machine; the server runtime also requires Java 21 for 1.21.x
- **ProtocolLib** (required)
- **WorldGuard** (required at runtime for region names on logged events)
- **MySQL** database reachable from the server

See also [WHIMC Server Documentation](https://docs.google.com/document/d/1T7UQParX9wVa3cVV-rwUFM5pJ7poIgMXGPR2lR-aAuo/edit?tab=t.0#bookmark=id.725lg2utykbb).

## What This Plugin Tracks

| Category | Trigger | Stored in |
|---|---|---|
| Barrel / shulker contents | Player closes a barrel or shulker box inventory | `whimc_containers` |
| Physical blocks | Player steps on or clicks pressure plates, levers, or buttons | `whimc_action_physical` |
| Air clicks | Player left-clicks empty space (each click is a separate row) | `whimc_action_physical` (`type`: `AIR CLICK`) |
| Player punches | Player damages another player (requires PvP enabled) | `whimc_action_physical` (`type`: `PUNCH <target>`) |
| Player deaths | Player dies | `whimc_action_physical` (`type`: `DEATH <cause>`) |
| Barrelbot puzzle success | Server chat message containing `[Barrelbot]` (via ProtocolLib) | `whimc_barrelbot_outcome` |
| In-session puzzle counts | Barrelbot success messages while the server is running | In-memory only (used by `/puzzle-progress`) |

For container slots, barrelbot instruction items store the instruction name (for example `move_forward`). Other items store serialized item metadata from the Bukkit YAML representation, or the material name when no meta is present.

## Commands

| Command | Usage | Description |
|---|---|---|
| `/ct-toggle-debug` | `/ct-toggle-debug` | Toggles debug mode in `config.yml` (also logs extra detail to the server console) |
| `/puzzle-progress` | `/puzzle-progress <player>` | Lists barrelbot puzzles the given online player has completed on the current world during this server session |

## Config

Config file: `plugins/WHIMC-Container-Tracker/config.yml`

### General

| Key | Type | Description |
|---|---|---|
| `debug` | `boolean` | Enable / disable debug mode |

Example:

```yaml
debug: true
```

### MySQL

| Key | Type | Description |
|---|---|---|
| `mysql.host` | `string` | Database host |
| `mysql.port` | `integer` | Database port |
| `mysql.database` | `string` | Database name |
| `mysql.username` | `string` | Database username |
| `mysql.password` | `string` | Database password |

Example:

```yaml
mysql:
  host: localhost
  port: 3306
  database: minecraft
  username: user
  password: pass
```

## Database Tables

### whimc_action_physical

Tracks player physical interactions and related events.

| Column | Type | Description |
|---|---|---|
| `rowid` | `int` | Unique row id |
| `uuid` | `string` | Player UUID |
| `username` | `string` | Player name |
| `world` | `string` | World name |
| `x` | `double` | Player x position |
| `y` | `double` | Player y position |
| `z` | `double` | Player z position |
| `time` | `bigint` | Unix timestamp (ms) |
| `type` | `string` | Block type, `AIR CLICK`, `DEATH <cause>`, or `PUNCH <target>` |
| `region_name` | `string` | WorldGuard region ids (space-separated) |

### whimc_barrelbot_outcome

Tracks barrelbot puzzle completions detected from chat.

| Column | Type | Description |
|---|---|---|
| `rowid` | `int` | Unique row id |
| `uuid` | `string` | Player UUID |
| `username` | `string` | Player name |
| `world` | `string` | World name |
| `x` | `double` | Player x position |
| `y` | `double` | Player y position |
| `z` | `double` | Player z position |
| `time` | `bigint` | Unix timestamp (ms) |
| `outcome` | `string` | Puzzle outcome (`Success` or `Failure`) |
| `inventory_row_id` | `int` | Latest `whimc_containers.rowid` for the player before completion |
| `puzzle_name` | `string` | Puzzle name from the barrelbot chat message |

### whimc_containers

Tracks barrel and shulker box contents when the inventory is closed.

| Column | Type | Description |
|---|---|---|
| `rowid` | `int` | Unique row id |
| `uuid` | `string` | Player UUID |
| `username` | `string` | Player name |
| `world` | `string` | World name |
| `x` | `double` | Container x position |
| `y` | `double` | Container y position |
| `z` | `double` | Container z position |
| `time` | `bigint` | Unix timestamp (ms) |
| `slot1`–`slot27` | `string` | Item data per slot (top-left = slot1, bottom-right = slot27) |
| `inventory_type` | `string` | Inventory type (`BARREL` or shulker box type) |
| `region_name` | `string` | WorldGuard region ids (space-separated) |

## Building

Requires JDK 21+.

```bash
mvn clean package
```

Output: `target/WHIMC-Container-Tracker-1.4.0.jar` (version matches `pom.xml`).

## Releases

Pushes to `main` trigger the GitHub Actions **Build and Release** workflow:

1. Builds the jar at the current `pom.xml` version
2. Creates a GitHub release tagged `v<version>` with the jar attached
3. Commits a version bump (+0.1) to `pom.xml` for the next release

To cut a release manually, run the workflow from the Actions tab (**workflow_dispatch**).

## Known Issues

- Weighted pressure plates can spam the database
- Citizens NPCs can trigger physical interactions (optional toggle planned)
- Double chests are not supported
- Containers with fewer than 27 slots may error and contents may not be stored
