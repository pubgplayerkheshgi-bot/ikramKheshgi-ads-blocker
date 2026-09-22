package com.ikramkheshgi.popupdetector;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

final class DetectionStore {
    private static final String PREFS = "popup_detector_21";
    private static final String HISTORY = "history";
    private static final String WHITELIST = "whitelist";
    private static final int MAX_ITEMS = 50;

    private DetectionStore() {}

    static synchronized void add(Context c, String pkg, String label, String reason, long time) {
        if (isWhitelisted(c, pkg)) return;
        JSONArray old = read(c), next = new JSONArray();
        try {
            JSONObject item = new JSONObject();
            item.put("package", pkg);
            item.put("label", label);
            item.put("reason", reason);
            item.put("time", time);
            next.put(item);
            for (int i = 0; i < old.length() && next.length() < MAX_ITEMS; i++) {
                JSONObject o = old.optJSONObject(i);
                if (o != null) next.put(o);
            }
        } catch (Exception ignored) { return; }
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(HISTORY, next.toString()).apply();
    }

    static JSONArray read(Context c) {
        try { return new JSONArray(c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(HISTORY, "[]")); }
        catch (Exception e) { return new JSONArray(); }
    }

    static JSONObject latest(Context c) { JSONArray a = read(c); return a.length() == 0 ? null : a.optJSONObject(0); }

    static void clear(Context c) { c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(HISTORY).apply(); }

    static boolean isWhitelisted(Context c, String pkg) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet(WHITELIST, java.util.Collections.emptySet()).contains(pkg);
    }

    static void setWhitelisted(Context c, String pkg, boolean value) {
        android.content.SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        java.util.Set<String> s = new java.util.HashSet<>(p.getStringSet(WHITELIST, java.util.Collections.emptySet()));
        if (value) s.add(pkg); else s.remove(pkg);
        p.edit().putStringSet(WHITELIST, s).apply();
    }

    static java.util.Set<String> whitelist(Context c) {
        return new java.util.HashSet<>(c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet(WHITELIST, java.util.Collections.emptySet()));
    }
}
