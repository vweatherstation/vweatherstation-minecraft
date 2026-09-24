package com.vweatherstation.mcconnect;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * VWeatherStation MC Connect — a Bukkit/Spigot/Paper plugin that pulls
 * real-world weather from VWeatherStation and applies it to your server world.
 *
 * BUILD:
 *   - Standard Bukkit/Paper plugin. Add the Paper/Spigot API dependency in
 *     Maven/Gradle, build a jar, drop it in your server's /plugins folder.
 *   - Requires a plugin.yml (provided alongside this file).
 *
 * CONFIG (config.yml, auto-created):
 *   latitude: 40.71
 *   longitude: -74.01
 *   updateSeconds: 600
 *   world: world          # which world to control (default: first world)
 *
 * Data: https://vweatherstation.com/api/v1/game-weather
 */
public class VWeatherConnect extends JavaPlugin {

    private static final String ENDPOINT = "https://vweatherstation.com/api/v1/game-weather";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        final double lat = getConfig().getDouble("latitude", 51.51);
        final double lon = getConfig().getDouble("longitude", -0.13);
        long period = Math.max(120, getConfig().getLong("updateSeconds", 600)) * 20L; // ticks
        final String worldName = getConfig().getString("world", "");

        getLogger().info("VWeatherConnect enabled — syncing weather from VWeatherStation.");

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    String json = httpGet(ENDPOINT + "?lat=" + lat + "&lon=" + lon);
                    if (json == null) return;
                    String condition = extract(json, "condition");
                    final boolean isDay = "1".equals(extract(json, "is_day"));
                    if (condition == null) return;

                    final String cond = condition;
                    // Apply on the main server thread (Bukkit API is not thread-safe)
                    Bukkit.getScheduler().runTask(VWeatherConnect.this, () -> applyWeather(worldName, cond, isDay));
                } catch (Exception e) {
                    getLogger().warning("VWeatherConnect fetch failed: " + e.getMessage());
                }
            }
        }.runTaskTimerAsynchronously(this, 20L, period);
    }

    private void applyWeather(String worldName, String condition, boolean isDay) {
        World world = worldName != null && !worldName.isEmpty()
                ? Bukkit.getWorld(worldName)
                : (Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0));
        if (world == null) return;

        switch (condition) {
            case "thunderstorm":
                world.setStorm(true);
                world.setThundering(true);
                break;
            case "rain":
            case "drizzle":
            case "snow": // snow shows as precipitation in cold biomes
                world.setStorm(true);
                world.setThundering(false);
                break;
            case "clear":
            case "cloudy":
            case "overcast":
            case "fog":
            default:
                world.setStorm(false);
                world.setThundering(false);
                break;
        }
        // rough day/night sync (optional)
        world.setTime(isDay ? 6000L : 18000L);
    }

    private String httpGet(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "VWeatherStation-MC/1.0");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        if (conn.getResponseCode() != 200) return null;
        StringBuilder sb = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    /** Minimal JSON string/number field extractor (no dependency needed). */
    private String extract(String json, String key) {
        String needle = "\"" + key + "\"";
        int i = json.indexOf(needle);
        if (i < 0) return null;
        i = json.indexOf(':', i);
        if (i < 0) return null;
        i++;
        while (i < json.length() && (json.charAt(i) == ' ' || json.charAt(i) == '"')) i++;
        int start = i;
        while (i < json.length() && json.charAt(i) != '"' && json.charAt(i) != ',' && json.charAt(i) != '}') i++;
        return json.substring(start, i).trim();
    }
}
