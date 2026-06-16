package com.eza.spicyex.beautifullyrics.entities;

import android.content.Context;
import android.content.SharedPreferences;

public final class LyricsResponseCache {
    private static final String PREFS_CACHE = "SpotifyPlusLyricsResponseCache";
    private static final long MAX_AGE_MS = 3L * 24L * 60L * 60L * 1000L;

    private LyricsResponseCache() {
    }

    public static String get(Context context, String trackId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_CACHE, Context.MODE_PRIVATE);
        String cacheKey = key(trackId);
        String response = prefs.getString(cacheKey, null);
        if (response == null) return null;

        String updatedKey = cacheKey + ":updated";
        long updatedAt = prefs.getLong(updatedKey, 0L);
        long now = System.currentTimeMillis();
        if (updatedAt <= 0L) {
            prefs.edit().putLong(updatedKey, now).apply();
            return response;
        }
        if (now - updatedAt > MAX_AGE_MS) {
            prefs.edit().remove(cacheKey).remove(updatedKey).apply();
            return null;
        }
        return response;
    }

    public static void put(Context context, String trackId, String response) {
        if (response == null || response.trim().isEmpty()) return;
        context.getSharedPreferences(PREFS_CACHE, Context.MODE_PRIVATE)
                .edit()
                .putString(key(trackId), response)
                .putLong(key(trackId) + ":updated", System.currentTimeMillis())
                .apply();
    }

    public static void clear(Context context) {
        context.getSharedPreferences(PREFS_CACHE, Context.MODE_PRIVATE).edit().clear().apply();
    }

    private static String key(String trackId) {
        return trackId == null ? "" : trackId;
    }
}
