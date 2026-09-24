# VWeatherStation Minecraft Connect

Sync your Minecraft server's weather with the real world, or broadcast your server's weather to a live public dashboard. A Bukkit/Spigot/Paper plugin.

## Download (auto-built — no build tools needed)
GitHub Actions compiles the `.jar` automatically. Grab it from the **Actions** tab (artifact) or a **Release**.

## Two plugins
- **`VWeatherConnect`** — pulls real-world weather INTO your server (storms/rain/day-night follow the real world).
- **`VWeatherStationReport`** — broadcasts your server's weather TO a live dashboard.

The provided `plugin.yml` targets **VWeatherConnect**. To build the Report plugin, point `plugin.yml`'s `main` at `com.vweatherstation.mcreport.VWeatherStationReport`.

## Setup
Drop the `.jar` in `/plugins`, edit the auto-created `config.yml` (your latitude/longitude), restart.

Full docs & live demo: **https://vweatherstation.com/minecraft/**
