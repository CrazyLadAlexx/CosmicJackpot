# CosmicJackpot

CosmicJackpot is a Maven-based Bukkit/Spigot jackpot plugin using Vault for economy support.

## Plugin Info

- Name: `CosmicJackpot`
- Main: `me.alexdev.cosmicjackpot.CosmicJackpot`
- Author: `Alex`
- Dependency: `Vault`
- Java target: `Java 8`
- Intended server range: `1.8` through modern Bukkit/Spigot-compatible versions

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

## Config

```yaml
tax: 0.10
drawTime: 14400
```

`drawTime` is in seconds.