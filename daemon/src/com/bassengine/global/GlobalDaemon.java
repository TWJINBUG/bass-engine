package com.bassengine.global;

import android.media.audiofx.DynamicsProcessing;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Root daemon that attaches a system DynamicsProcessing effect to audio session 0
 * and mirrors the BASS ENGINE control app config into it.
 *
 * v0.2.0
 *  - only reads global_config.json when mtime/size changed (no 3 Hz file reads)
 *  - status file is written on change plus a 2 s heartbeat (no 3 Hz rewrites)
 *  - keeps the last applied config in /data/adb/bassengine/last_config.json so
 *    DSP still works right after reboot, before the app is opened
 *  - maps mode / intensity / virtualBass / q / limiter instead of ignoring them
 *  - static headroom from the EQ curve so the limiter is not slammed
 */
public class GlobalDaemon {
    private static final String TAG = "GlobalDaemon";
    private static final float[] FREQ = {20f, 125f, 250f, 500f, 650f, 1000f, 2000f, 4000f, 8000f, 16000f};
    private static final String DEFAULT_CONFIG =
            "/sdcard/Android/data/com.bassengine.control/files/global_config.json";
    private static final String DEFAULT_STATUS =
            "/sdcard/Android/data/com.bassengine.control/files/global_status.json";
    private static final String STATE_DIR = "/data/adb/bassengine";
    private static final String SAVED_CONFIG = STATE_DIR + "/last_config.json";
    private static final long STATUS_HEARTBEAT_MS = 2000L;
    private static final long IDLE_SLEEP_MS = 400L;
    private static final long HEALTH_CHECK_MS = 30000L;

    private static long seenAppMod = -1L;
    private static long seenAppLen = -1L;
    private static long seenSavedMod = -1L;

    public static void main(String[] args) {
        String configPath = args.length > 0 ? args[0] : DEFAULT_CONFIG;
        String statusPath = args.length > 1 ? args[1] : DEFAULT_STATUS;
        log("start uid=" + android.os.Process.myUid() + " pid=" + android.os.Process.myPid());

        DynamicsProcessing dp = null;
        String applied = "";
        String lastError = "";
        String lastStatusKey = "";
        long lastStatusAt = 0L;
        long lastHealthAt = 0L;
        int failures = 0;

        while (true) {
            String statusKey;
            try {
                if (dp == null) {
                    dp = createEffect();
                    applied = "";
                    log("effect attached id=" + dp.getId());
                }

                boolean fromApp = false;
                String json = readIfChanged(configPath, true);
                if (json != null) {
                    fromApp = true;
                } else {
                    json = readIfChanged(SAVED_CONFIG, false);
                }

                if (json != null && !json.equals(applied)) {
                    JSONObject cfg = new JSONObject(json);
                    apply(dp, cfg);
                    applied = json;
                    lastError = "";
                    if (fromApp) {
                        saveConfig(json);
                    }
                    log("config applied");
                }

                String health = applied.isEmpty() ? lastError : "";
                statusKey = dp.getId() + "|" + (applied.isEmpty() ? 0 : 1) + "|" + health;
                long now = System.currentTimeMillis();
                if (!statusKey.equals(lastStatusKey) || now - lastStatusAt > STATUS_HEARTBEAT_MS) {
                    putStatus(statusPath, statusJson(dp.getId(), !applied.isEmpty(), health));
                    lastStatusKey = statusKey;
                    lastStatusAt = now;
                }

                if (now - lastHealthAt > HEALTH_CHECK_MS) {
                    // Touching the effect catches a dead audio server before the
                    // next config change would.
                    dp.setEnabled(applied.isEmpty() ? true
                            : new JSONObject(applied).optBoolean("enabled", true));
                    lastHealthAt = now;
                }
                failures = 0;
            } catch (Throwable t) {
                failures++;
                lastError = t.getClass().getSimpleName() + ": " + t.getMessage();
                log("error " + lastError);
                if (dp != null) {
                    try {
                        dp.release();
                    } catch (Throwable ignored) {
                    }
                    dp = null;
                }
                applied = "";
                statusKey = "0|0|" + lastError;
                lastStatusAt = writeStatusAlways(statusPath, statusKey, dp, lastError);
                try {
                    Thread.sleep(Math.min(5000L, 500L * failures));
                } catch (InterruptedException ignored) {
                }
                continue;
            }
            try {
                Thread.sleep(IDLE_SLEEP_MS);
            } catch (InterruptedException ignored) {
            }
        }
    }

    /** Reads {@code path} only when mtime or size changed; null when unchanged/missing. */
    private static String readIfChanged(String path, boolean isAppConfig) {
        File f = new File(path);
        if (!f.exists()) {
            if (isAppConfig) {
                seenAppMod = -1L;
                seenAppLen = -1L;
            }
            return null;
        }
        long mod = f.lastModified();
        long len = f.length();
        if (isAppConfig) {
            if (mod == seenAppMod && len == seenAppLen) {
                return null;
            }
            seenAppMod = mod;
            seenAppLen = len;
        } else {
            if (mod == seenSavedMod) {
                return null;
            }
            seenSavedMod = mod;
        }
        try {
            return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void saveConfig(String json) {
        try {
            File dir = new File(STATE_DIR);
            dir.mkdirs();
            File tmp = new File(SAVED_CONFIG + ".tmp");
            write(tmp, json);
            File dst = new File(SAVED_CONFIG);
            if (!tmp.renameTo(dst)) {
                dst.delete();
                tmp.renameTo(dst);
            }
            seenSavedMod = dst.lastModified();
        } catch (Throwable t) {
            log("save config failed " + t);
        }
    }

    private static DynamicsProcessing createEffect() {
        DynamicsProcessing.Config config = new DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                2,
                true, 10,
                true, 2,
                false, 0,
                true
        ).build();
        DynamicsProcessing dp = new DynamicsProcessing(0, 0, config);
        dp.setEnabled(true);
        return dp;
    }

    private static void apply(DynamicsProcessing dp, JSONObject cfg) {
        boolean enabled = cfg.optBoolean("enabled", true);
        boolean limiter = cfg.optBoolean("limiter", true);
        float preamp = clamp((float) cfg.optDouble("preampDb", 0.0), -12f, 12f);

        float[] gains = new float[10];
        JSONArray arr = cfg.optJSONArray("bands");
        for (int i = 0; i < 10; i++) {
            gains[i] = clamp(arr == null ? 0f : (float) arr.optDouble(i, 0.0), -15f, 15f);
        }

        JSONObject bass = cfg.optJSONObject("bass");
        boolean bassEnabled = bass == null || bass.optBoolean("enabled", true);
        boolean boost = bass != null && bass.optBoolean("boost", true);
        boolean adaptive = bass != null && bass.optBoolean("adaptive", true);
        boolean virtualBass = bass != null && bass.optBoolean("virtualBass", false);
        boolean subImpact = bass != null && bass.optBoolean("subImpact", false);
        boolean subwoofer = bass != null && bass.optBoolean("subwoofer", false);
        float q = clamp(bass == null ? 2.2f : (float) bass.optDouble("q", 2.2), 0.5f, 3.0f);

        if (bassEnabled) {
            // Approximates the LSPosed path's 70 Hz low shelf with bands 20/125/250.
            float shelf = 0f;
            if (boost) shelf += 3.5f + (q - 2.2f);
            if (adaptive) shelf += 1.0f; // no per-stream volume here; fixed mild lift
            if (subwoofer) shelf += 4.0f;
            shelf = clamp(shelf, -8f, 16f);
            if (shelf != 0f) {
                gains[0] += shelf * 0.30f;
                gains[1] += shelf * 0.55f;
                gains[2] += shelf * 0.25f;
            }
            if (virtualBass) {
                // DP cannot synthesise harmonics; emphasise the sub region instead.
                gains[0] += 2.5f;
                gains[1] += 1.5f;
                gains[2] += 1.0f;
            }
            if (subImpact) {
                gains[0] += 4.0f;
                gains[1] += 3.0f;
            }
            if (subwoofer) {
                gains[8] += 3.0f;
                gains[9] += 4.0f;
                gains[4] -= 2.0f;
                gains[5] -= 3.0f;
            }
        }

        JSONObject surround = cfg.optJSONObject("surround");
        if (surround != null) {
            float s = 0.5f + (clamp(surround.optInt("intensity", 45), 40, 70) - 40) / 30f * 0.5f;
            if (surround.optBoolean("air", true)) {
                gains[8] += 2.0f * s;
                gains[9] += 2.5f * s;
            }
            if (surround.optBoolean("vocal", true)) {
                gains[5] += 2.2f * s;
                gains[6] += 0.8f * s;
            }
            if (surround.optBoolean("focus", true)) {
                gains[6] += 1.2f * s;
                gains[7] += 0.6f * s;
            }
            String mode = surround.optString("mode", "wide");
            if ("cinema".equals(mode)) {
                gains[0] += 1.5f * s;
                gains[1] += 0.8f * s;
            } else if ("near".equals(mode)) {
                gains[4] += 0.6f * s;
                gains[5] += 1.0f * s;
                gains[6] += 0.6f * s;
            } else {
                gains[7] += 0.6f * s;
                gains[8] += 0.8f * s;
            }
        }

        for (int i = 0; i < 10; i++) {
            gains[i] = clamp(gains[i], -15f, 15f);
        }

        float headroom = 0f;
        for (int i = 0; i < 10; i++) {
            float stack = gains[i]
                    + (i > 0 ? Math.max(0f, gains[i - 1]) * 0.5f : 0f)
                    + (i < 9 ? Math.max(0f, gains[i + 1]) * 0.5f : 0f);
            headroom = Math.max(headroom, stack);
        }
        headroom = clamp(headroom - 2f, 0f, 10f);

        for (int i = 0; i < 10; i++) {
            dp.setPreEqBandAllChannelsTo(i, new DynamicsProcessing.EqBand(true, FREQ[i], gains[i]));
        }
        dp.setInputGainAllChannelsTo(clamp(preamp - headroom, -24f, 12f));

        float lowRatio = subwoofer ? 5f : (virtualBass ? 4f : 3f);
        float lowThreshold = subwoofer ? 0.18f : 0.25f;
        DynamicsProcessing.MbcBand low = new DynamicsProcessing.MbcBand(
                true, 120f, 5f, 80f, lowRatio, lowThreshold, 6f, 0f, 1f, 0f, 1f);
        DynamicsProcessing.MbcBand high = new DynamicsProcessing.MbcBand(
                true, 6000f, 10f, 100f, 2f, 0.5f, 6f, 0f, 1f, 0f, 0f);
        dp.setMbcBandAllChannelsTo(0, low);
        dp.setMbcBandAllChannelsTo(1, high);

        dp.setLimiterAllChannelsTo(new DynamicsProcessing.Limiter(
                true, limiter && enabled, 0, 1f, 60f, 10f, 0.9f, 0f));
        dp.setEnabled(enabled);
    }

    private static float clamp(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
    }

    private static String statusJson(int id, boolean attached, String error) {
        try {
            JSONObject o = new JSONObject();
            o.put("running", true);
            o.put("attached", attached);
            o.put("effectId", id);
            o.put("error", error == null ? "" : error);
            o.put("updated", System.currentTimeMillis());
            o.put("pid", android.os.Process.myPid());
            return o.toString();
        } catch (Throwable t) {
            return "{\"running\":true,\"attached\":false,\"error\":\"status error\"}";
        }
    }

    private static void putStatus(String path, String json) {
        try {
            File f = new File(path);
            File dir = f.getParentFile();
            if (dir != null) {
                dir.mkdirs();
            }
            File tmp = new File(path + ".tmp");
            write(tmp, json);
            if (!tmp.renameTo(f)) {
                f.delete();
                tmp.renameTo(f);
            }
        } catch (Throwable ignored) {
        }
    }

    private static long writeStatusAlways(String path, String key, DynamicsProcessing dp,
                                          String error) {
        putStatus(path, statusJson(dp == null ? -1 : dp.getId(), false, error));
        return System.currentTimeMillis();
    }

    private static void write(File f, String content) throws Exception {
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void log(String msg) {
        System.out.println(TAG + " " + msg);
    }
}
