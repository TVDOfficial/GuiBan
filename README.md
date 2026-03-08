# GUIBan

A professional Minecraft (Paper) moderation plugin with a graphical interface for banning, muting, jailing, and kicking players. Supports SQLite, YAML, and MySQL storage.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21+-green.svg)](https://papermc.io/)
[![License](https://img.shields.io/badge/License-Use%20freely-blue.svg)](https://github.com/TVDOfficial/GuiBan)

---

## Features

- **GUI-based moderation** – Player selection and action menus
- **Punishment types** – Ban, Mute, Jail, Kick
- **Time-based punishments** – Temporary or permanent (e.g. `1h`, `7d`, `perm`)
- **Multiple storage backends** – SQLite, YAML, MySQL
- **Proxy support** – BungeeCord and Velocity modules (optional)
- **Customizable menus** – Titles, items, and lore via `gui.yml`

---

## Requirements

- **Server:** Paper (or compatible) 1.21+
- **Java:** 21

---

## Installation

1. Download the latest `GUIBAN-2.0.jar` from [Releases](https://github.com/TVDOfficial/GuiBan/releases).
2. Place the JAR in your server's `plugins` folder.
3. Restart the server (or use a plugin manager to load it).
4. Edit `plugins/GUIBan/config.yml` and `plugins/GUIBan/gui.yml` as needed.
5. Reload with `/guiban reload` (requires `guiban.admin`) or restart.

---

## Commands

| Command | Description | Permission |
|--------|-------------|------------|
| `/guiban` | Open the player selection GUI | `guiban.use` |
| `/guiban help` | Show command help | — |
| `/guiban ban <player> [time] [reason]` | Ban a player | `guiban.ban` |
| `/guiban mute <player> [time] [reason]` | Mute a player | `guiban.mute` |
| `/guiban jail <player> [time] [reason]` | Jail a player | `guiban.jail` |
| `/guiban kick <player> [reason]` | Kick a player | `guiban.kick` |
| `/guiban unban <player>` | Remove a ban | `guiban.unban` |
| `/guiban unmute <player>` | Remove a mute | `guiban.unmute` |
| `/guiban unjail <player>` | Remove a jail | `guiban.unjail` |
| `/guiban reload` | Reload config and gui config | `guiban.admin` |

**Aliases:** `gb` (e.g. `/gb`, `/gb ban Steve 1d Cheating`)

### Time format

- **Permanent:** `perm` or `permanent`
- **Temporary:** `30s`, `5m`, `1h`, `7d`, `2w` (seconds, minutes, hours, days, weeks)

**Examples:**

- `/guiban ban Steve 7d Hacking`
- `/guiban mute Alex 1h Spam`
- `/guiban kick Bob Left the game`

---

## Permissions

| Permission | Description | Default |
|------------|-------------|--------|
| `guiban.use` | Open GUIBan GUI and use GUI actions | OP |
| `guiban.ban` | Ban players (command and GUI) | OP |
| `guiban.mute` | Mute players (command and GUI) | OP |
| `guiban.jail` | Jail players (command and GUI) | OP |
| `guiban.kick` | Kick players (command and GUI) | OP |
| `guiban.unban` | Unban players | OP |
| `guiban.unmute` | Unmute players | OP |
| `guiban.unjail` | Unjail players | OP |
| `guiban.admin` | Reload configs and admin access | OP |

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
```

### gui.yml

Customize menu titles, materials, and lore for:

- **Player selection** – List of online players (skulls)
- **Moderation menu** – Ban (red), Mute (orange), Jail (yellow), Kick (iron door)

After editing, use `/guiban reload` (with `guiban.admin`).

---

## Building from source

**Requirements:** Maven 3.6+, Java 21

```bash
git clone https://github.com/TVDOfficial/GuiBan.git
cd GuiBan
mvn clean package
```

The built JAR is at `target/GUIBAN-2.0.jar`.

---

## Project structure

```
GUIBan/
├── src/main/java/com/enterprise/guiban/
│   ├── GUIBAN.java              # Plugin main class
│   ├── command/
│   │   └── GuiBanCommand.java   # Command handler
│   ├── listener/
│   │   ├── GUIListener.java     # GUI click handling
│   │   ├── JoinListener.java    # Ban check on join
│   │   ├── ChatListener.java    # Mute check
│   │   └── JailListener.java    # Jail logic
│   ├── storage/
│   │   ├── StorageProvider.java
│   │   ├── Punishment.java
│   │   ├── PunishmentType.java
│   │   ├── SQLiteProvider.java
│   │   ├── YamlProvider.java
│   │   └── MySQLProvider.java
│   ├── ui/
│   │   └── GUIHandler.java      # Menu creation
│   └── utils/
│       └── TimeUtil.java
├── src/main/resources/
│   ├── plugin.yml
│   ├── config.yml
│   └── gui.yml
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
