package com.smartkey.ai.keyboard;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.smartkey.ai.R;
import com.smartkey.ai.app.SettingsRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The keyboard itself. Renders a classic QWERTY layout plus a toolbar of
 * AI actions (rewrite / fix grammar / shorten / make friendlier) that send
 * the current text to Claude and replace it with the result.
 */
public class AIKeyboardService extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    private KeyboardView keyboardView;
    private Keyboard qwertyKeyboard;
    private boolean isShifted = false;

    private SettingsRepository settingsRepository;
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ProgressBar aiProgress;

    @Override
    public void onCreate() {
        super.onCreate();
        settingsRepository = new SettingsRepository(this);
    }

    @Override
    public View onCreateInputView() {
        View container = getLayoutInflater().inflate(R.layout.keyboard_container, null);

        keyboardView = container.findViewById(R.id.keyboardView);
        qwertyKeyboard = new Keyboard(this, R.xml.keyboard_qwerty);
        keyboardView.setKeyboard(qwertyKeyboard);
        keyboardView.setOnKeyboardActionListener(this);

        aiProgress = container.findViewById(R.id.aiProgress);

        Button btnRewrite = container.findViewById(R.id.btnAiRewrite);
        Button btnGrammar = container.findViewById(R.id.btnAiGrammar);
        Button btnShorter = container.findViewById(R.id.btnAiShorter);
        Button btnFriendly = container.findViewById(R.id.btnAiFriendly);

        btnRewrite.setOnClickListener(v -> runAiAction("Rewrite this text to sound clearer and more polished, keep the original language and meaning"));
        btnGrammar.setOnClickListener(v -> runAiAction("Fix all spelling and grammar mistakes in this text, keep the original language and meaning"));
        btnShorter.setOnClickListener(v -> runAiAction("Make this text noticeably shorter while keeping the key meaning, keep the original language"));
        btnFriendly.setOnClickListener(v -> runAiAction("Rewrite this text in a warmer, friendlier tone, keep the original language and meaning"));

        return container;
    }

    /**
     * Reads the current field's text (selection if present, otherwise the
     * whole field), sends it to Claude with the given instruction, and
     * replaces it with the result.
     */
    private void runAiAction(String instruction) {
        if (!settingsRepository.hasApiKey()) {
            Toast.makeText(this, R.string.api_key_missing, Toast.LENGTH_LONG).show();
            return;
        }

        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        String sourceText = extractTextToTransform(ic);
        if (sourceText == null || sourceText.trim().isEmpty()) {
            Toast.makeText(this, R.string.ai_no_text, Toast.LENGTH_SHORT).show();
            return;
        }

        setAiLoading(true);

        String apiKey = settingsRepository.getApiKey();
        String model = settingsRepository.getModel();
        AIClient client = new AIClient(apiKey, model);

        backgroundExecutor.execute(() -> {
            try {
                String result = client.transform(instruction, sourceText);
                mainHandler.post(() -> {
                    setAiLoading(false);
                    replaceAllText(getCurrentInputConnection(), result);
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    setAiLoading(false);
                    String msg = getString(R.string.ai_error, e.getMessage());
                    Toast.makeText(AIKeyboardService.this, msg, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private String extractTextToTransform(InputConnection ic) {
        CharSequence selected = ic.getSelectedText(0);
        if (selected != null && selected.length() > 0) {
            return selected.toString();
        }
        CharSequence before = ic.getTextBeforeCursor(4000, 0);
        CharSequence after = ic.getTextAfterCursor(4000, 0);
        String b = before != null ? before.toString() : "";
        String a = after != null ? after.toString() : "";
        return b + a;
    }

    private void replaceAllText(InputConnection ic, String newText) {
        if (ic == null) return;
        // Select everything we can see around the cursor, then replace it.
        CharSequence before = ic.getTextBeforeCursor(4000, 0);
        CharSequence after = ic.getTextAfterCursor(4000, 0);
        int beforeLen = before != null ? before.length() : 0;
        int afterLen = after != null ? after.length() : 0;

        ic.beginBatchEdit();
        ic.deleteSurroundingText(beforeLen, afterLen);
        ic.commitText(newText, 1);
        ic.endBatchEdit();
    }

    private void setAiLoading(boolean loading) {
        if (aiProgress != null) {
            aiProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    // ---- KeyboardView.OnKeyboardActionListener ----

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        switch (primaryCode) {
            case Keyboard.KEYCODE_DELETE:
                CharSequence selected = ic.getSelectedText(0);
                if (selected != null && selected.length() > 0) {
                    ic.commitText("", 1);
                } else {
                    ic.deleteSurroundingText(1, 0);
                }
                break;

            case Keyboard.KEYCODE_SHIFT:
                isShifted = !isShifted;
                qwertyKeyboard.setShifted(isShifted);
                keyboardView.invalidateAllKeys();
                break;

            case Keyboard.KEYCODE_MODE_CHANGE:
                // Placeholder: hook up a symbols keyboard here later.
                break;

            case -4: // "done"/enter
                ic.commitText("\n", 1);
                break;

            default:
                char code = (char) primaryCode;
                if (Character.isLetter(code) && isShifted) {
                    code = Character.toUpperCase(code);
                }
                ic.commitText(String.valueOf(code), 1);

                if (isShifted) {
                    isShifted = false;
                    qwertyKeyboard.setShifted(false);
                    keyboardView.invalidateAllKeys();
                }
        }
    }

    @Override
    public void onPress(int primaryCode) {}

    @Override
    public void onRelease(int primaryCode) {}

    @Override
    public void onText(CharSequence text) {}

    @Override
    public void swipeLeft() {}

    @Override
    public void swipeRight() {}

    @Override
    public void swipeDown() {}

    @Override
    public void swipeUp() {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        backgroundExecutor.shutdown();
    }
}
