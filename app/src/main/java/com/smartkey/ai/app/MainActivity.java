package com.smartkey.ai.app;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.smartkey.ai.R;

public class MainActivity extends AppCompatActivity {

    private SettingsRepository settingsRepository;
    private TextInputEditText editApiKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settingsRepository = new SettingsRepository(this);

        editApiKey = findViewById(R.id.editApiKey);
        Button btnEnableKeyboard = findViewById(R.id.btnEnableKeyboard);
        Button btnSwitchKeyboard = findViewById(R.id.btnSwitchKeyboard);
        Button btnSaveKey = findViewById(R.id.btnSaveKey);

        if (settingsRepository.hasApiKey()) {
            editApiKey.setText(settingsRepository.getApiKey());
        }

        // Step 1: system settings screen where the user enables our IME
        btnEnableKeyboard.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));

        // Step 2: system picker to make our IME the active one
        btnSwitchKeyboard.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showInputMethodPicker();
            }
        });

        // Step 3: save the user's own Anthropic API key, encrypted on-device
        btnSaveKey.setOnClickListener(v -> {
            String key = editApiKey.getText() != null ? editApiKey.getText().toString().trim() : "";
            if (key.isEmpty()) {
                Toast.makeText(this, R.string.api_key_missing, Toast.LENGTH_SHORT).show();
                return;
            }
            settingsRepository.saveApiKey(key);
            Toast.makeText(this, R.string.api_key_saved, Toast.LENGTH_SHORT).show();
        });
    }
}
