package com.vweatherstation.mcreport;

import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * VWeatherStationReport — reports your Minecraft server's IN-GAME weather to
 * VWeatherStation so players can view it on a live dashboard (Direction 2).
 * The reverse of VWeatherConnect (which pulls real weather into the game).
 *
 * On enable it registers a session and logs a dashboard URL to share with
 * players. It then reports the world's weather + time every 30s.
 */
public class VWeatherStationReport extends JavaPlugin {
    private static final String API = "https://vweatherstation.com/api/v1";
    private String sessionToken = null;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        final String worldName = getConfig().getString("world", "");
        // register session
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            String resp = httpPost("/game-sessions/", "{\"game\":\"minecraft\",\"mode\":\"" +
                getServer().getName() + "\",\"platform\":\"minecraft\"}");
            if (resp != null) {
                sessionToken = extract(resp, "session_token");
                String manage = extract(resp, "manage_token");
                getLogger().info("=========================================");
                getLogger().info("VWeatherStation connected!");
                getLogger().info("Dashboard: https://vweatherstation.com/games/minecraft/live/?s=" + manage);
                getLogger().info("Share that URL with your players.");
                getLogger().info("=========================================");
            }
        });
        // report loop every 30s
        new BukkitRunnable() {
            @Override public void run() {
                if (sessionToken == null) return;
                World w = worldName != null && !worldName.isEmpty() ? Bukkit.getWorld(worldName)
                        : (Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0));
                if (w == null) return;
                String weather = w.hasStorm() ? (w.isThundering() ? "storm" : "rain") : "clear";
                long time = w.getTime();
                String gameTime = String.format("%02d:%02d", (int)(((time/1000)+6)%24), (int)((time%1000)*60/1000));
                int players = Bukkit.getOnlinePlayers().size();
                String body = "{\"session_token\":\"" + sessionToken + "\",\"weather\":\"" + weather +
                    "\",\"raw_weather\":\"" + weather + "\",\"game_time\":\"" + gameTime +
                    "\",\"zone\":\"" + w.getName() + "\",\"players\":" + players + "}";
                final String b = body;
                Bukkit.getScheduler().runTaskAsynchronously(VWeatherStationReport.this,
                    () -> httpPost("/game-telemetry/", b));
            }
        }.runTaskTimer(this, 100L, 600L); // every 30s
    }

    private String httpPost(String path, String json) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(API + path).openConnection();
            c.setRequestMethod("POST");
            c.setRequestProperty("Content-Type", "application/json");
            c.setRequestProperty("User-Agent", "VWeatherStation-MC/1.0");
            c.setConnectTimeout(8000); c.setReadTimeout(8000);
            c.setDoOutput(true);
            try (OutputStream os = c.getOutputStream()) { os.write(json.getBytes("UTF-8")); }
            if (c.getResponseCode() != 200) return null;
            StringBuilder sb = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream()))) {
                String line; while ((line = in.readLine()) != null) sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) { getLogger().warning("report failed: " + e.getMessage()); return null; }
    }
    private String extract(String json, String key) {
        String n = "\"" + key + "\""; int i = json.indexOf(n); if (i < 0) return null;
        i = json.indexOf(':', i); if (i < 0) return null; i++;
        while (i < json.length() && (json.charAt(i)==' '||json.charAt(i)=='"')) i++;
        int s = i; while (i < json.length() && json.charAt(i)!='"' && json.charAt(i)!=',' && json.charAt(i)!='}') i++;
        return json.substring(s, i);
    }
}
