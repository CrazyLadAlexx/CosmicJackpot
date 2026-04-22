# CosmicJackpot

CosmicJackpot is a Maven-based Bukkit/Spigot jackpot plugin using Vault for economy support.

## Plugin Info

- Name: `CosmicJackpot`
- Main: `me.alexdev.cosmicjackpot.CosmicJackpot`
- Author: `Alex`
- Dependency: `Vault`
- Java target: `Java 8`
- Intended server range: `1.16` through `1.21.11` Bukkit/Spigot-compatible versions

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
| `/jackpot buy` | Buy 1 jackpot ticket. |
| `/jackpot buy <amount>` | Buy a specific amount of jackpot tickets. |
| `/jackpot top` | View the top jackpot winners. |
| `/jackpot stats` | View your jackpot stats. |
| `/jackpot notifications` | Toggle jackpot countdown notifications. |

## Config

```yaml
tax: 0.10
drawTime: 14400
```

`drawTime` is in seconds.

## Support

If there are any problems, add me on Discord: `._._crazyladalex_.`
