package com.ikramkheshgi.popupdetector;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class DetectionStore {
    static final String PREFS = "popup_detector_v2";
    static final String HISTORY = "history";
    static final int MAX_ITEMS = 30;

    private DetectionStore() {}

    static synchronized void add(Context context, String pkg, String label,
                                  String reason, long time) {
        try {
            JSONArray old = read(context);
            JSONArray next = new JSONArray();
            JSONObject item = new JSONObject();
            item.put("package", pkg);
            item.put("label", label);
            item.put("reason", reason == null ? "Popup-like window detected" : reason);
            item.put("time", time);
            next.put(item);
            for (int i = 0; i < old.length() && next.length() < MAX_ITEMS; i++) {
                next.put(old.getJSONObject(i));
            }
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putString(HISTORY, next.toString()).apply();
        } catch (Exception ignored) {}
    }

    static JSONArray read(Context context) {
        try {
            return new JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getString(HISTORY, "[]"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    static JSONObject latest(Context context) {
        JSONArray a = read(context);
        return a.length() == 0 ? null : a.optJSONObject(0);
    }

    static void clear(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .remove(HISTORY).apply();
    }
}
