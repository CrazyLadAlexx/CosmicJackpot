# CosmicJackpot

CosmicJackpot is a Maven-based Bukkit/Spigot jackpot plugin using Vault for economy support.

## Plugin Info

- Name: `CosmicJackpot`
- Main: `me.alexdev.cosmicjackpot.CosmicJackpot`
- Author: `Alex`
- Dependency: `Vault`
- Compiled target: `Java 17` bytecode

## Supported Versions

| Server version | Supported | Java runtime | Notes |
| --- | --- | --- | --- |
| `1.20.x` | Yes | Java 17+ | Primary supported version range. Built against Spigot API `1.20.4`. |
| `1.21.x` | Yes | Java 21 recommended | Should work on modern Bukkit/Spigot/Paper-compatible servers. |
| `1.18.x` - `1.19.x` | Likely | Java 17+ | Expected to work because the plugin uses standard Bukkit/Vault APIs, but not the primary build target. |
| `1.17.x` and older | No | Java 16 or older | Not supported by this modernized build. |

The plugin is compiled with Java 17. GitHub Actions and local builds must use JDK 17 or newer.

## Build

```bash
mvn package
```

The compiled plugin jar will be created in `target/`.

## Install

1. Build the plugin with Maven.
2. Put `target/CosmicJackpot-1.0-SNAPSHOT.jar` into your server `plugins` folder.
3. Make sure Vault and an economy plugin are installed.
4. Start or restart the server.

## Commands

| Command | Description |
| --- | --- |
| `/jackpot` | View the current jackpot. |
| `/jackpot info` | View the current jackpot. |
| `/jackpot buy` | Buy 1 jackpot ticket. |
| `/jackpot buy <amount>` | Buy a specific amount of jackpot tickets. |
| `/jackpot top [page]` | View the top jackpot winners. |
| `/jackpot stats` | View your jackpot stats. |
| `/jackpot notifications` | Toggle jackpot countdown notifications. |

The `/jackpot` subcommands and common arguments tab-complete in game.

## Config

```yaml
tax: 0.10
drawTime: 14400
```

`drawTime` is in seconds.

## Support

If there are any problems, add me on Discord: `._._crazyladalex_.`
