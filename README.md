# GUIBan

A professional Minecraft (Paper) moderation plugin with a graphical interface for banning, muting, jailing, warning, and kicking players. Supports SQLite, YAML, and MySQL storage with optional integrations.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21+-green.svg)](https://papermc.io/)
[![License](https://img.shields.io/badge/License-Use%20freely-blue.svg)](https://github.com/TVDOfficial/GuiBan)

---

## Features

- **GUI-based moderation** – Player selection, action menus, pagination, search
- **Punishment types** – Ban, Mute, Jail, Kick, Warn
- **Time-based punishments** – Temporary or permanent (e.g. `30s`, `1h`, `7d`, `perm`)
- **Multiple storage backends** – SQLite, YAML, MySQL
- **Caching & async storage** – Cached punishment lookups, async DB reads/writes for better performance
- **IP bans** – Ban by IP address or player (kicks all players on that IP)
- **Warn system** – Optional auto-punish after X warnings
- **Permission levels** – Levels 1–10; higher can punish lower
- **Customizable messages** – All text via `messages.yml`
- **Proxy support** – Velocity module (optional)
- **Integrations** – PlaceholderAPI, Discord webhook

---

## Requirements

- **Server:** Paper (or compatible) 1.21+
- **Java:** 21

---

## Installation

1. Download the latest `GUIBAN.jar` from [Releases](https://github.com/TVDOfficial/GuiBan/releases).
2. Place the JAR in your server's `plugins` folder.
3. Restart the server.
4. Edit `plugins/GUIBan/config.yml`, `plugins/GUIBan/messages.yml`, and `plugins/GUIBan/gui.yml` as needed.
5. Reload with `/guiban reload` (requires `guiban.admin`) or restart.

---

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/guiban` | Open the player selection GUI | `guiban.use` |
| `/guiban help` | Show command help | — |
| `/guiban ban <player> [time] [reason]` | Ban a player | `guiban.ban` |
| `/guiban mute <player> [time] [reason]` | Mute a player | `guiban.mute` |
| `/guiban jail <player> [time] [reason]` | Jail a player | `guiban.jail` |
| `/guiban kick <player> [reason]` | Kick a player | `guiban.kick` |
| `/guiban warn <player> [reason]` | Warn a player | `guiban.warn` |
| `/guiban ipban <player\|ip> [time] [reason]` | IP ban (by player or IP) | `guiban.ipban` |
| `/guiban unban <player>` | Remove a ban | `guiban.unban` |
| `/guiban unmute <player>` | Remove a mute | `guiban.unmute` |
| `/guiban unjail <player>` | Remove a jail | `guiban.unjail` |
| `/guiban list <ban\|mute\|jail>` | View active punishments | `guiban.view` |
| `/guiban reload` | Reload configs | `guiban.admin` |

**Aliases:** `gb` (e.g. `/gb`, `/gb ban Steve 1d Cheating`, `/gb list ban`)

### Time format

- **Permanent:** `perm` or `permanent`
- **Temporary:** `30s`, `5m`, `1h`, `7d`, `2w` (seconds, minutes, hours, days, weeks)

**Examples:**

- `/guiban ban Steve 7d Hacking`
- `/guiban mute Alex 1h Spam`
- `/guiban warn Bob Rule violation`
- `/guiban ipban 192.168.1.1 perm Evading ban`
- `/guiban kick Bob Left the game`

---

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `guiban.*` | All GUIBan permissions (level 10 + everything below) | OP |
| `guiban.use` | Open GUIBan GUI and use GUI actions | OP |
| `guiban.ban` | Ban players (command and GUI) | OP |
| `guiban.mute` | Mute players (command and GUI) | OP |
| `guiban.jail` | Jail players (command and GUI) | OP |
| `guiban.kick` | Kick players (command and GUI) | OP |
| `guiban.warn` | Warn players | OP |
| `guiban.ipban` | IP ban players or IPs | OP |
| `guiban.unban` | Unban players | OP |
| `guiban.unmute` | Unmute players | OP |
| `guiban.unjail` | Unjail players | OP |
| `guiban.view` | View active punishments and history (GUI + `/gb list`) | OP |
| `guiban.broadcast` | Receive staff broadcast when someone is banned | OP |
| `guiban.admin` | Reload configs and admin access | OP |
| `guiban.bypass` | Cannot be punished (e.g. server owner) | OP |
| `guiban.level.1` … `guiban.level.10` | Permission level 1 (lowest) to 10 (highest). Higher level can punish lower; equal or lower cannot punish. | false |

---

## GUI

- **Player list:** Pagination with **green wool** (next) and **red wool** (previous). **Name tag** = search: type a player name in chat to filter (includes offline players). **Book** = Punishments section.
- **Punishments section:** Choose Active Bans, Active Mutes, or Active Jails. Lists show reason, time left, punisher. Click a head to view history in chat.
- **Manage player:** Ban, Mute, Jail, Kick (opens duration menu, then reason in chat); **Unban**, **Unmute**, **Unjail**; **Back**.
- **Duration menu:** 1m, 5m, 15m, 30m, 1h, 6h, 12h, 1d, 1w, 2w, or Permanent. Then type reason in chat.
- **Player head hover:** Shows current status (Banned, Muted, Jailed) with reason and time left.

---

## Integrations

### PlaceholderAPI

If [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) is installed, GUIBan registers placeholders:

| Placeholder | Description |
|-------------|-------------|
| `%guiban_BAN_reason%` | Active ban reason (or `none`) |
| `%guiban_BAN_duration%` | Time left on ban |
| `%guiban_BAN_punisher%` | Who banned |
| `%guiban_MUTE_reason%` | Active mute reason |
| `%guiban_MUTE_duration%` | Time left on mute |
| `%guiban_MUTE_punisher%` | Who muted |
| `%guiban_JAIL_reason%` | Active jail reason |
| `%guiban_JAIL_duration%` | Time left on jail |
| `%guiban_JAIL_punisher%` | Who jailed |

### Discord webhook

Configure in `config.yml`:

```yaml
discord:
  webhook-url: "https://discord.com/api/webhooks/..."
  ban-message: "**{player}** was banned by **{punisher}** for `{reason}`"
  mute-message: "**{player}** was muted by **{punisher}** for `{reason}`"
  jail-message: "**{player}** was jailed by **{punisher}** for `{reason}`"
```

Placeholders: `{player}`, `{punisher}`, `{reason}`

### Velocity proxy

See [Working with Velocity](#working-with-velocity) below.

---

## Configuration

### config.yml

```yaml
storage:
  type: SQLITE   # SQLITE | YAML | MYSQL
  mysql:
    host: localhost
    port: 3306
    database: guiban
    username: root
    password: ""

ban-appeal-url: ""   # Shown in ban message (leave empty to hide)

jail:
  world: "world"
  x: 0
  y: 64
  z: 0
  use: false   # true = teleport jailed players here

sounds:
  open-menu: "BLOCK_NOTE_BLOCK_PLING"
  punishment-applied: "ENTITY_VILLAGER_NO"
  error: "ENTITY_VILLAGER_NO"

staff-broadcast: true   # Broadcast to guiban.broadcast when someone is banned
rate-limit: 10         # Max punishments per minute per player (0 = disabled)
audit-log: true        # Log to audit.log

discord:
  webhook-url: ""
  ban-message: "**{player}** was banned by **{punisher}** for `{reason}`"
  mute-message: "**{player}** was muted by **{punisher}** for `{reason}`"
  jail-message: "**{player}** was jailed by **{punisher}** for `{reason}`"

warns:
  enabled: false
  max-before-punish: 3
  punish-type: MUTE
  punish-duration: "1h"
```

### messages.yml

All plugin messages are configurable. Use `{player}`, `{reason}`, `{duration}`, `{punisher}`, `{appeal}` as placeholders. Supports `&` color codes.

### gui.yml

Customize menu titles, materials, and lore for player selection and moderation items (ban, mute, jail, kick).

---

## Working with Velocity

### Option 1: GUIBan only on Paper (recommended)

- Install **GUIBan** on each Paper server (no plugin on Velocity).
- Set **`storage.type: MYSQL`** and use the **same MySQL** on each server.
- Bans are shared across all Paper servers.

### Option 2: Enforce bans at the proxy

1. Build the Velocity plugin: `cd guiban-velocity && mvn clean package`
2. Put `guiban-velocity/target/guiban-velocity-2.0.jar` in Velocity's `plugins` folder.
3. Edit `plugins/guiban/config.properties` with the same MySQL settings.
4. Ensure player information forwarding is configured.

---

## Building from source

**Requirements:** Maven 3.6+, Java 21

```bash
git clone https://github.com/TVDOfficial/GuiBan.git
cd GuiBan
mvn clean package
```

The built JAR is at `target/GUIBAN.jar`.

---

## Creating a GitHub release

1. Build the JAR: `mvn clean package`
2. Go to your repo on GitHub → **Releases** → **Create a new release**
3. Choose a tag (e.g. `v2.0`) or create one
4. Add a title and release notes
5. Under **Attach binaries**, drag and drop `target/GUIBAN.jar` or click to upload
6. Click **Publish release**

Users can then download `GUIBAN.jar` from the Releases page.

---

## Project structure

```
GUIBan/
├── src/main/java/com/enterprise/guiban/
│   ├── GUIBAN.java
│   ├── command/
│   │   └── GuiBanCommand.java
│   ├── listener/
│   │   ├── GUIListener.java
│   │   ├── JoinListener.java
│   │   ├── ChatListener.java
│   │   ├── JailListener.java
│   │   └── ReasonInputListener.java
│   ├── storage/
│   │   ├── StorageProvider.java
│   │   ├── Punishment.java
│   │   ├── PunishmentType.java
│   │   ├── PunishmentCache.java
│   │   ├── CachedStorageProvider.java
│   │   ├── AsyncStorageHelper.java
│   │   ├── SQLiteProvider.java
│   │   ├── YamlProvider.java
│   │   └── MySQLProvider.java
│   ├── ui/
│   │   ├── GUIHandler.java
│   │   └── ReasonPending.java
│   ├── placeholders/
│   │   └── GUIBanPlaceholders.java
│   └── utils/
│       ├── TimeUtil.java
│       ├── PermissionHelper.java
│       ├── MessageHelper.java
│       ├── PlayerLookup.java
│       ├── RateLimiter.java
│       ├── SoundHelper.java
│       ├── AuditLogger.java
│       └── DiscordWebhook.java
├── src/main/resources/
│   ├── plugin.yml
│   ├── config.yml
│   ├── messages.yml
│   └── gui.yml
├── guiban-velocity/          # Velocity plugin module
├── pom.xml
└── README.md
```

---

## Support & links

- **Repository:** [https://github.com/TVDOfficial/GuiBan](https://github.com/TVDOfficial/GuiBan)
- **Author:** Mathew Pittard
- **API version:** 1.21

---

## License

Use freely. Check the repository for any license file.
