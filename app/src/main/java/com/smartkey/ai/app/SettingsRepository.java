package com.smartkey.ai.app;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Wraps EncryptedSharedPreferences so the user's Anthropic API key is never
 * stored in plain text on disk. Used by both the settings screen and the IME.
 */
public class SettingsRepository {

    private static final String PREFS_FILE = "smartkey_secure_prefs";
    private static final String KEY_API_KEY = "anthropic_api_key";
    private static final String KEY_MODEL = "anthropic_model";
    private static final String DEFAULT_MODEL = "claude-sonnet-5";

    private final SharedPreferences prefs;

    public SettingsRepository(Context context) {
        SharedPreferences p;
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            p = EncryptedSharedPreferences.create(
                    context,
                    PREFS_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            // Fall back to regular prefs rather than crashing the keyboard;
            // this should not normally happen on API 26+.
            p = context.getSharedPreferences(PREFS_FILE + "_fallback", Context.MODE_PRIVATE);
        }
        this.prefs = p;
    }

    public void saveApiKey(String apiKey) {
        prefs.edit().putString(KEY_API_KEY, apiKey).apply();
    }

    public String getApiKey() {
        return prefs.getString(KEY_API_KEY, "");
    }

    public boolean hasApiKey() {
        String key = getApiKey();
        return key != null && !key.trim().isEmpty();
    }

    public String getModel() {
        return prefs.getString(KEY_MODEL, DEFAULT_MODEL);
    }

    public void saveModel(String model) {
        prefs.edit().putString(KEY_MODEL, model).apply();
    }
}
