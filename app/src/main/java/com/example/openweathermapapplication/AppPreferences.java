package com.example.openweathermapapplication;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPreferences {

    static AppPreferences instance;
    SharedPreferences preferences;
    SharedPreferences.Editor editor;
    Context context;
    private final String PREF_NAME = "WeatherAndroid";
    int private_mode = 0;
    private final String KEY_CITY = "KEY_CITY";

    public AppPreferences(Context context) {
        this.preferences = context.getSharedPreferences(PREF_NAME, private_mode);
        this.editor = preferences.edit();
        this.context = context;
    }

    public void setCityPreference(String value)
    {
        editor.putString(KEY_CITY, value);
        editor.commit();
    }

    public String getCityPreference() {
        String jsonString = preferences.getString(KEY_CITY, "");
        return jsonString;
    }
}
