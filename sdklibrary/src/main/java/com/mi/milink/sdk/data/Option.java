
package com.mi.milink.sdk.data;

import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import com.mi.milink.sdk.base.Global;

//------------------------------------------------------------------------------
// 功能实现 via android.content.SharedPreferences
// ------------------------------------------------------------------------------
/**
 * 存储通用的配置信息
 *
 * @author MK
 */
public final class Option {

    private static final String TAG = "options.for." + Global.getPackageName();

    private static SharedPreferences preferences = Global.getSharedPreferences(TAG,
            Context.MODE_PRIVATE);

    private static SharedPreferences.Editor editor = preferences.edit();

    public static Map<String, ?> getAll() {
        return preferences.getAll();
    }

    public static String getString(String key, String defValue) {
        return preferences.getString(key, defValue);
    }

    public static int getInt(String key, int defValue) {
        return preferences.getInt(key, defValue);
    }

    public static long getLong(String key, long defValue) {

        return preferences.getLong(key, defValue);
    }

    public static float getFloat(String key, float defValue) {
        return preferences.getFloat(key, defValue);
    }

    public static boolean getBoolean(String key, boolean defValue) {
        return preferences.getBoolean(key, defValue);
    }

    public static boolean contains(String key) {
        return preferences.contains(key);
    }

    public static void startListen(OnSharedPreferenceChangeListener listener) {
        preferences.registerOnSharedPreferenceChangeListener(listener);
    }

    public static void stopListen(OnSharedPreferenceChangeListener listener) {
        preferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public static Editor putString(String key, String value) {
        return editor.putString(key, value);
    }

    public static Editor putInt(String key, int value) {
        return editor.putInt(key, value);
    }

    public static Editor putLong(String key, long value) {
        return editor.putLong(key, value);
    }

    public static Editor putFloat(String key, float value) {
        return editor.putFloat(key, value);
    }

    public static Editor putBoolean(String key, boolean value) {
        return editor.putBoolean(key, value);
    }

    public static Editor remove(String key) {
        return editor.remove(key);
    }

    public static Editor clear() {
        return editor.clear();
    }

    public static boolean commit() {
        return editor.commit();
    }

}
