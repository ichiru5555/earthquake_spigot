package org.ichiru.earthquake;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONArray;
import org.json.JSONObject;

public final class Earthquake extends JavaPlugin {
    @Override
    public void onEnable() {
        new BukkitRunnable() {
            private String previousId;

            public void run() {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.p2pquake.net/v2/history?codes=551&limit=1"))
                        .build();

                client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(response -> {

                            try {
                                JSONArray jsonArray = new JSONArray(response.body());
                                if (!jsonArray.isEmpty()) {
                                    JSONObject earthquakeInfo = jsonArray.getJSONObject(0);

                                    String id = earthquakeInfo.getString("id");
                                    if (id.equals(previousId)) {
                                        getLogger().info("Skipping duplicate ID: " + id);
                                        return;
                                    }
                                    previousId = id;

                                    int maxScale = earthquakeInfo.getJSONObject("earthquake").getInt("maxScale");
                                    if (maxScale < 30){
                                        return;
                                    }
                                    String str_maxScale = convertMaxScale(maxScale);

                                    String domesticTsunami = convertDomesticTsunami(earthquakeInfo.getJSONObject("earthquake").getString("domesticTsunami"));
                                    JSONObject firstPoint = earthquakeInfo.getJSONArray("points").getJSONObject(0);
                                    String pref = firstPoint.getString("pref");
                                    String addr = firstPoint.getString("addr");

                                    broadcastToOnlinePlayers(maxScale, str_maxScale, domesticTsunami, pref, addr);
                                    saveToFile(id);
                                    if (maxScale >= 45) {
                                        showEarthquakeInfoOnScreen(str_maxScale, domesticTsunami, pref, addr);
                                    }
                                }
                            } catch (Exception e) {
                                getLogger().warning("Failed to parse JSON: " + e.getMessage());
                            }
                        }).exceptionally(e -> {
                            getLogger().warning("API request failed: " + e.getMessage());
                            return null;
                        });
            }

            private String convertMaxScale(int maxScale) {
                switch (maxScale) {
                    case 10: return "震度1"; // 震度1
                    case 20: return "震度2"; // 震度2
                    case 30: return "震度3"; // 震度3
                    case 40: return "震度4"; // 震度4
                    case 45: return "震度5弱"; // 震度5弱
                    case 50: return "震度5強"; // 震度5強
                    case 55: return "震度6弱"; // 震度6弱
                    case 60: return "震度6強"; // 震度6強
                    case 70: return "震度7"; // 震度7
                    default: return "震度情報なし"; // 震度情報なし
                }
            }

            private String convertDomesticTsunami(String domesticTsunami) {
                switch (domesticTsunami) {
                    case "None": return "なし";
                    case "Unknown": return "不明";
                    case "Checking": return "調査中";
                    case "NonEffective": return "若干の海面変動が予想されるが、被害の心配なし";
                    case "Watch": return "津波注意報";
                    case "Warning": return "津波予報(種類不明)";
                    default: return domesticTsunami;
                }
            }

            private void broadcastToOnlinePlayers(int maxScale,String str_maxScale, String domesticTsunami, String pref, String addr) {
                ChatColor color;
                if (maxScale <= 30) {
                    color = ChatColor.DARK_AQUA;
                } else if (maxScale <= 55) {
                    color = ChatColor.RED;
                } else {
                    color = ChatColor.DARK_PURPLE;
                }
                String message =
                        color + "震度: " + str_maxScale + "\n"
                        + color + "津波: " + domesticTsunami + "\n"
                        + color + "都道府県: " + pref + addr;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(message);
                }
            }

            private void saveToFile(String id) {
                File file = new File(getDataFolder(), "data.yml");
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

                config.set("id", id);

                try {
                    config.save(file);
                    getLogger().info("Data saved to data.yml");
                } catch (IOException e) {
                    getLogger().warning("Failed to save data to data.yml: " + e.getMessage());
                }
            }
            private void showEarthquakeInfoOnScreen(String str_maxScale, String domesticTsunami, String pref, String addr) {
                TextComponent message = new TextComponent(ChatColor.RED + "地震情報\n"
                        + ChatColor.DARK_PURPLE + "震度: " + str_maxScale + "\n"
                        + ChatColor.DARK_PURPLE + "津波情報: " + domesticTsunami + "\n"
                        + ChatColor.DARK_PURPLE + "発生場所: " + pref + " " + addr);
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, message);
                }
            }
        }.runTaskTimerAsynchronously(this, 0L, 200L);
    }

    @Override
    public void onDisable() {
        getLogger().info("EarthquakePlugin has been disabled.");
    }
}
