package com.james.gramaticaconversacional;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Locale;

public final class MainActivity extends Activity implements TextToSpeech.OnInitListener {
    private static final int AUDIO_PERMISSION = 7001;
    private static final int BG = Color.rgb(15, 18, 23);
    private static final int PANEL = Color.rgb(27, 32, 40);
    private static final int TEXT = Color.rgb(244, 246, 248);
    private static final int MUTED = Color.rgb(179, 188, 201);
    private static final int ACCENT = Color.rgb(93, 146, 230);
    private static final int BORDER = Color.rgb(63, 72, 86);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ArrayDeque<Integer> weakPrompts = new ArrayDeque<>();
    private final StringBuilder answerBuffer = new StringBuilder();

    private SharedPreferences prefs;
    private TextToSpeech tts;
    private SpeechRecognizer recognizer;
    private Intent speechIntent;

    private boolean ttsReady = false;
    private boolean speechAvailable = false;
    private boolean sessionActive = false;
    private boolean listening = false;
    private boolean pendingSessionStart = false;
    private boolean answerCaptureActive = false;
    private boolean answerFinishing = false;

    private int promptCursor = 0;
    private int sessionTurns = 0;
    private int sessionCorrections = 0;
    private int sessionGrammar = 0;
    private int sessionTransfer = 0;
    private int sessionTarget = 0;
    private int sessionNatural = 0;
    private int recognitionErrors = 0;
    private int captureGeneration = 0;

    private String lastPartial = "";
    private String lastCommittedChunk = "";
    private TutorContent.Prompt currentPrompt;

    private TextView statusText;
    private TextView fallbackQuestion;
    private LinearLayout correctionList;
    private Button micButton;
    private EditText typedFallback;
    private Button typedSubmit;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("conversation_tutor_progress", MODE_PRIVATE);
        buildMainScreen();
        tts = new TextToSpeech(this, this);
        setupRecognizer();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, int sp, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setLineSpacing(0f, 1.12f);
        return v;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(TEXT);
        b.setTextSize(15);
        b.setMinHeight(dp(48));
        b.setBackgroundTintList(ColorStateList.valueOf(ACCENT));
        return b;
    }

    private GradientDrawable panelBackground() {
        GradientDrawable g = new GradientDrawable();
        g.setColor(PANEL);
        g.setCornerRadius(dp(14));
        g.setStroke(dp(1), BORDER);
        return g;
    }

    private void addSpacer(LinearLayout root, int height) {
        View spacer = new View(this);
        root.addView(spacer, new LinearLayout.LayoutParams(1, dp(height)));
    }

    private LinearLayout baseRoot() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(28), dp(18), dp(28));
        root.setBackgroundColor(BG);
        return root;
    }

    private ScrollView scrollWith(LinearLayout root) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        scroll.addView(root);
        return scroll;
    }

    private void buildMainScreen() {
        sessionActive = false;
        stopListening();
        if (tts != null) tts.stop();

        LinearLayout root = baseRoot();
        TextView title = text("Gramática Conversacional", 29, TEXT);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);
        TextView sub = text("Conversational Latin American Spanish tutor", 15, MUTED);
        sub.setPadding(0, dp(3), 0, 0);
        root.addView(sub);

        addSpacer(root, 22);
        LinearLayout focus = new LinearLayout(this);
        focus.setOrientation(LinearLayout.VERTICAL);
        focus.setPadding(dp(16), dp(15), dp(16), dp(15));
        focus.setBackground(panelBackground());
        TextView focusTitle = text("CURRENT LEARNING FOCUS", 12, MUTED);
        focusTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        focus.addView(focusTitle);
        addSpacer(focus, 7);
        focus.addView(text(
            "Use Spanish in real conversation while the tutor quietly trains verb tense, mood and person. Current verb system: TENER across present, preterite, imperfect, perfect, future, conditional and subjunctive.",
            16, TEXT));
        addSpacer(focus, 10);
        focus.addView(text(
            "Persons: yo · tú · él · ella · usted · nosotros/nosotras · ustedes · ellos/ellas. Vosotros is intentionally omitted.",
            14, MUTED));
        addSpacer(focus, 10);
        focus.addView(text(
            "Every answer is also checked for articles, agreement, tense/mood, person agreement, common English-to-Spanish transfer patterns and more natural conversational structure.",
            14, MUTED));
        root.addView(focus);

        addSpacer(root, 18);
        root.addView(text("Questions are in Spanish. Grammar explanations and learning instructions are in English.", 14, MUTED));

        int lastTurns = prefs.getInt("last_turns", 0);
        if (lastTurns > 0) {
            addSpacer(root, 20);
            TextView lastTitle = text("LAST SESSION", 12, MUTED);
            lastTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            root.addView(lastTitle);
            String summary = lastTurns + " conversational turns · " + prefs.getInt("last_corrections", 0) + " corrections\n"
                + prefs.getInt("last_target", 0) + " verb/tense · "
                + prefs.getInt("last_grammar", 0) + " grammar · "
                + prefs.getInt("last_transfer", 0) + " English-transfer/native structure";
            TextView summaryView = text(summary, 15, TEXT);
            summaryView.setPadding(0, dp(8), 0, 0);
            root.addView(summaryView);
        }

        addSpacer(root, 24);
        Button start = button("Start Conversation");
        start.setOnClickListener(v -> startSession());
        root.addView(start);

        addSpacer(root, 12);
        root.addView(text(
            "During each spoken answer, pause as often as you need. The tutor keeps the answer open until you press Finish Answer. Correct answers leave the session screen clear; only meaningful corrections remain visible.",
            13, MUTED));

        setContentView(scrollWith(root));
    }

    private void buildSessionScreen() {
        LinearLayout outer = baseRoot();

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView active = text("Conversation active", 16, TEXT);
        active.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(active, new LinearLayout.LayoutParams(0, -2, 1f));
        Button stop = button("Stop Session");
        stop.setMinHeight(dp(42));
        stop.setOnClickListener(v -> endSession());
        header.addView(stop, new LinearLayout.LayoutParams(-2, dp(46)));
        outer.addView(header);

        statusText = text("Starting…", 13, MUTED);
        statusText.setPadding(0, dp(9), 0, dp(8));
        outer.addView(statusText);

        fallbackQuestion = text("", 19, TEXT);
        fallbackQuestion.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        fallbackQuestion.setVisibility(View.GONE);
        fallbackQuestion.setPadding(0, dp(8), 0, dp(12));
        outer.addView(fallbackQuestion);

        ScrollView correctionsScroll = new ScrollView(this);
        correctionList = new LinearLayout(this);
        correctionList.setOrientation(LinearLayout.VERTICAL);
        correctionsScroll.addView(correctionList);
        outer.addView(correctionsScroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        Button repeat = button("Repeat question");
        repeat.setOnClickListener(v -> repeatQuestion());
        micButton = button("Start Answer");
        micButton.setOnClickListener(v -> {
            if (answerCaptureActive) finishAnswerCapture();
            else if (!answerFinishing) beginAnswerCapture();
        });
        controls.addView(repeat, new LinearLayout.LayoutParams(0, dp(50), 1f));
        LinearLayout.LayoutParams micParams = new LinearLayout.LayoutParams(0, dp(50), 1f);
        micParams.setMargins(dp(8), 0, 0, 0);
        controls.addView(micButton, micParams);
        outer.addView(controls);

        typedFallback = new EditText(this);
        typedFallback.setHint("Type your Spanish answer");
        typedFallback.setTextColor(TEXT);
        typedFallback.setHintTextColor(MUTED);
        typedFallback.setTextSize(16);
        typedFallback.setSingleLine(false);
        typedFallback.setMinHeight(dp(56));
        typedFallback.setVisibility(View.GONE);
        outer.addView(typedFallback);

        typedSubmit = button("Submit typed answer");
        typedSubmit.setVisibility(View.GONE);
        typedSubmit.setOnClickListener(v -> {
            String value = typedFallback.getText().toString().trim();
            if (!value.isEmpty()) {
                typedFallback.setText("");
                handleAnswer(value);
            }
        });
        outer.addView(typedSubmit);

        setContentView(outer);
    }

    private void startSession() {
        sessionActive = true;
        sessionTurns = 0;
        sessionCorrections = 0;
        sessionGrammar = 0;
        sessionTransfer = 0;
        sessionTarget = 0;
        sessionNatural = 0;
        recognitionErrors = 0;
        promptCursor = prefs.getInt("prompt_cursor", 0) % TutorContent.PROMPTS.length;
        weakPrompts.clear();
        buildSessionScreen();

        if (ttsReady) {
            speakEnglish(
                "Conversation started. I will ask questions in Spanish. Answer naturally in Spanish. Pause whenever you need to think. I will keep listening across pauses and will not submit your answer until you press Finish Answer. Meaningful corrections will appear on the screen with explanations in English.",
                "SESSION_INTRO");
        } else {
            pendingSessionStart = true;
            statusText.setText("Preparing voice…");
            handler.postDelayed(() -> {
                if (sessionActive && !ttsReady && pendingSessionStart) {
                    pendingSessionStart = false;
                    statusText.setText("Voice is unavailable. The conversation can continue with displayed questions and typed answers.");
                    showTypedFallback();
                    nextQuestion();
                }
            }, 3500);
        }
    }

    private void endSession() {
        if (!sessionActive) return;
        sessionActive = false;
        pendingSessionStart = false;
        stopListening();
        if (tts != null) tts.stop();
        prefs.edit()
            .putInt("last_turns", sessionTurns)
            .putInt("last_corrections", sessionCorrections)
            .putInt("last_grammar", sessionGrammar)
            .putInt("last_transfer", sessionTransfer + sessionNatural)
            .putInt("last_target", sessionTarget)
            .putInt("prompt_cursor", promptCursor)
            .apply();
        buildMainScreen();
    }

    private void nextQuestion() {
        if (!sessionActive) return;
        stopListening();
        int index;
        if (!weakPrompts.isEmpty() && sessionTurns > 0 && sessionTurns % 3 == 0) {
            index = weakPrompts.removeFirst();
        } else {
            index = promptCursor % TutorContent.PROMPTS.length;
            promptCursor = (promptCursor + 1) % TutorContent.PROMPTS.length;
        }
        currentPrompt = TutorContent.PROMPTS[index];
        statusText.setText("Tutor speaking…");
        fallbackQuestion.setVisibility(View.GONE);
        if (ttsReady) {
            speakSpanish(currentPrompt.question, "PROMPT");
        } else {
            fallbackQuestion.setText(currentPrompt.question);
            fallbackQuestion.setVisibility(View.VISIBLE);
            statusText.setText("Answer in Spanish.");
            showTypedFallback();
        }
    }

    private void repeatQuestion() {
        if (!sessionActive || currentPrompt == null) return;
        stopListening();
        if (ttsReady) {
            statusText.setText("Repeating…");
            speakSpanish(currentPrompt.question, "REPEAT");
        } else {
            fallbackQuestion.setText(currentPrompt.question);
            fallbackQuestion.setVisibility(View.VISIBLE);
            showTypedFallback();
        }
    }

    private void handleAnswer(String answer) {
        if (!sessionActive || currentPrompt == null) return;
        stopListening();
        sessionTurns++;
        GrammarEvaluator.Result result = GrammarEvaluator.evaluate(answer, currentPrompt);

        if (result.corrections.isEmpty()) {
            statusText.setText("Tutor continuing…");
            handler.postDelayed(this::nextQuestion, 450);
            return;
        }

        sessionCorrections += result.corrections.size();
        for (GrammarEvaluator.Correction c : result.corrections) {
            if (GrammarEvaluator.TARGET.equals(c.category)) sessionTarget++;
            else if (GrammarEvaluator.GRAMMAR.equals(c.category)) sessionGrammar++;
            else if (GrammarEvaluator.TRANSFER.equals(c.category)) sessionTransfer++;
            else sessionNatural++;
            addCorrectionCard(c);
        }
        if (result.hasTargetCorrection() && weakPrompts.size() < 8) weakPrompts.addLast(currentPrompt.index);

        statusText.setText("Correction shown. Review it while we continue.");
        if (ttsReady) {
            String message = result.corrections.size() == 1
                ? "I flagged one correction on the screen. Notice what you said, the better Spanish, and why."
                : "I flagged several corrections on the screen. Review the corrected Spanish and the explanations.";
            speakEnglish(message, "FEEDBACK");
        } else {
            handler.postDelayed(this::nextQuestion, 1100);
        }
    }

    private void addCorrectionCard(GrammarEvaluator.Correction c) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(15), dp(14), dp(15), dp(14));
        card.setBackground(panelBackground());

        TextView saidLabel = text("YOU SAID", 11, MUTED);
        saidLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(saidLabel);
        TextView said = text(c.said, 18, TEXT);
        said.setPadding(0, dp(4), 0, dp(12));
        card.addView(said);

        TextView betterLabel = text("SAY INSTEAD", 11, MUTED);
        betterLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(betterLabel);
        TextView better = text(c.better, 19, TEXT);
        better.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        better.setPadding(0, dp(4), 0, dp(12));
        card.addView(better);

        TextView whyLabel = text("WHY", 11, MUTED);
        whyLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(whyLabel);
        TextView why = text(c.explanation, 15, MUTED);
        why.setPadding(0, dp(4), 0, 0);
        card.addView(why);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(12));
        correctionList.addView(card, 0, lp);
    }

    private void setupRecognizer() {
        speechAvailable = SpeechRecognizer.isRecognitionAvailable(this);
        if (!speechAvailable) return;
        try {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-PA");
            speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-PA");
            speechIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
            speechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            speechIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L);
            speechIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L);

            recognizer.setRecognitionListener(new RecognitionListener() {
                public void onReadyForSpeech(Bundle params) {
                    listening = true;
                    recognitionErrors = 0;
                    if (statusText != null && answerCaptureActive) {
                        statusText.setText("Listening — pause as needed. Press Finish Answer when you are done.");
                    }
                    updateMicButton();
                }
                public void onBeginningOfSpeech() {}
                public void onRmsChanged(float rmsdB) {}
                public void onBufferReceived(byte[] buffer) {}
                public void onEndOfSpeech() {
                    if (statusText != null && answerCaptureActive && !answerFinishing) {
                        statusText.setText("Pause detected — your answer is still open.");
                    }
                }
                public void onError(int error) {
                    listening = false;
                    if (!lastPartial.isEmpty()) {
                        appendChunk(lastPartial);
                        lastPartial = "";
                    }

                    if (answerFinishing) {
                        finalizeAnswerCapture();
                        return;
                    }
                    if (!answerCaptureActive) {
                        updateMicButton();
                        return;
                    }

                    if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        if (statusText != null) {
                            statusText.setText("Still waiting — take your time. Press Finish Answer when done.");
                        }
                        scheduleRecognitionRestart(250);
                        return;
                    }

                    recognitionErrors++;
                    if (recognitionErrors >= 3) {
                        answerCaptureActive = false;
                        captureGeneration++;
                        updateMicButton();
                        showTypedFallback();
                        if (statusText != null) {
                            statusText.setText("The speech service had repeated errors. Press Start Answer to retry or type your answer.");
                        }
                    } else {
                        if (statusText != null) {
                            statusText.setText("Reconnecting the microphone — your captured answer is still open.");
                        }
                        scheduleRecognitionRestart(600);
                    }
                }
                public void onResults(Bundle results) {
                    listening = false;
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) appendChunk(matches.get(0));
                    lastPartial = "";
                    recognitionErrors = 0;

                    if (answerFinishing) {
                        finalizeAnswerCapture();
                    } else if (answerCaptureActive) {
                        if (statusText != null) {
                            statusText.setText("Pause captured — continue speaking when ready, or press Finish Answer.");
                        }
                        scheduleRecognitionRestart(300);
                    } else {
                        updateMicButton();
                    }
                }
                public void onPartialResults(Bundle partialResults) {
                    ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    lastPartial = (matches != null && !matches.isEmpty()) ? matches.get(0).trim() : "";
                }
                public void onEvent(int eventType, Bundle params) {}
            });
        } catch (Exception ignored) {
            speechAvailable = false;
            recognizer = null;
        }
    }

    private void beginAnswerCapture() {
        if (!sessionActive || answerFinishing) return;
        if (!speechAvailable || recognizer == null) {
            showTypedFallback();
            if (statusText != null) statusText.setText("Speech recognition is unavailable. Type your Spanish answer.");
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { Manifest.permission.RECORD_AUDIO }, AUDIO_PERMISSION);
            return;
        }

        stopListening();
        answerBuffer.setLength(0);
        lastCommittedChunk = "";
        lastPartial = "";
        recognitionErrors = 0;
        answerCaptureActive = true;
        answerFinishing = false;
        captureGeneration++;
        updateMicButton();
        if (statusText != null) {
            statusText.setText("Listening — pause as needed. Press Finish Answer when you are done.");
        }
        startRecognitionChunk();
    }

    private void finishAnswerCapture() {
        if (!sessionActive || !answerCaptureActive || answerFinishing) return;
        answerCaptureActive = false;
        answerFinishing = true;
        captureGeneration++;
        updateMicButton();
        if (statusText != null) statusText.setText("Finishing your answer…");

        if (recognizer != null && listening) {
            try {
                recognizer.stopListening();
            } catch (Exception ignored) {
                finalizeAnswerCapture();
                return;
            }
            handler.postDelayed(() -> {
                if (sessionActive && answerFinishing) finalizeAnswerCapture();
            }, 1600);
        } else {
            handler.postDelayed(() -> {
                if (sessionActive && answerFinishing) finalizeAnswerCapture();
            }, 100);
        }
    }

    private void finalizeAnswerCapture() {
        if (!answerFinishing) return;
        if (!lastPartial.isEmpty()) appendChunk(lastPartial);
        lastPartial = "";
        listening = false;
        answerFinishing = false;
        answerCaptureActive = false;
        captureGeneration++;
        updateMicButton();

        String answer = answerBuffer.toString().trim();
        answerBuffer.setLength(0);
        lastCommittedChunk = "";

        if (answer.isEmpty()) {
            if (statusText != null) statusText.setText("I did not capture any words. Press Start Answer and try again.");
            return;
        }

        if (statusText != null) statusText.setText("Checking your full answer…");
        handleAnswer(answer);
    }

    private void appendChunk(String value) {
        if (value == null) return;
        String chunk = value.trim();
        if (chunk.isEmpty()) return;
        if (chunk.equalsIgnoreCase(lastCommittedChunk)) return;
        if (answerBuffer.length() > 0) answerBuffer.append(' ');
        answerBuffer.append(chunk);
        lastCommittedChunk = chunk;
    }

    private void scheduleRecognitionRestart(long delayMs) {
        final int generation = captureGeneration;
        handler.postDelayed(() -> {
            if (!sessionActive || !answerCaptureActive || answerFinishing || listening) return;
            if (generation != captureGeneration) return;
            startRecognitionChunk();
        }, delayMs);
    }

    private void startRecognitionChunk() {
        if (!sessionActive || !answerCaptureActive || answerFinishing || recognizer == null || listening) return;
        if (tts != null && tts.isSpeaking()) tts.stop();
        lastPartial = "";
        try {
            recognizer.startListening(speechIntent);
        } catch (Exception e) {
            recognitionErrors++;
            if (recognitionErrors >= 3) {
                answerCaptureActive = false;
                captureGeneration++;
                updateMicButton();
                showTypedFallback();
                if (statusText != null) statusText.setText("The microphone could not restart. Press Start Answer to retry or type your answer.");
            } else {
                scheduleRecognitionRestart(600);
            }
        }
    }

    private void stopListening() {
        captureGeneration++;
        answerCaptureActive = false;
        answerFinishing = false;
        if (recognizer != null && listening) {
            try { recognizer.cancel(); } catch (Exception ignored) {}
        }
        listening = false;
        answerBuffer.setLength(0);
        lastPartial = "";
        lastCommittedChunk = "";
        updateMicButton();
    }

    private void updateMicButton() {
        if (micButton == null) return;
        if (answerFinishing) {
            micButton.setText("Finishing…");
            micButton.setEnabled(false);
        } else if (answerCaptureActive) {
            micButton.setText("Finish Answer");
            micButton.setEnabled(true);
        } else {
            micButton.setText("Start Answer");
            micButton.setEnabled(true);
        }
    }

    private void showTypedFallback() {
        if (typedFallback != null) typedFallback.setVisibility(View.VISIBLE);
        if (typedSubmit != null) typedSubmit.setVisibility(View.VISIBLE);
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) beginAnswerCapture();
            else {
                showTypedFallback();
                if (statusText != null) statusText.setText("Microphone permission was denied. Type your Spanish answer instead.");
            }
        }
    }

    @Override public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS) {
            ttsReady = false;
            if (sessionActive) {
                pendingSessionStart = false;
                showTypedFallback();
                if (statusText != null) statusText.setText("Voice is unavailable. Questions will be displayed.");
                nextQuestion();
            }
            return;
        }
        ttsReady = true;
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            public void onStart(String utteranceId) {}
            public void onError(String utteranceId) {}
            public void onDone(String utteranceId) { runOnUiThread(() -> speechFinished(utteranceId)); }
        });
        if (sessionActive && pendingSessionStart) {
            pendingSessionStart = false;
            speakEnglish(
                "Conversation started. I will ask questions in Spanish. Answer naturally in Spanish. Pause whenever you need to think. I will keep listening across pauses and will not submit your answer until you press Finish Answer. Meaningful corrections will appear on the screen with explanations in English.",
                "SESSION_INTRO");
        }
    }

    private void speechFinished(String id) {
        if (!sessionActive) return;
        if ("SESSION_INTRO".equals(id)) {
            handler.postDelayed(this::nextQuestion, 350);
        } else if ("PROMPT".equals(id) || "REPEAT".equals(id)) {
            if (statusText != null) statusText.setText("Preparing to listen…");
            handler.postDelayed(this::beginAnswerCapture, 450);
        } else if ("FEEDBACK".equals(id)) {
            handler.postDelayed(this::nextQuestion, 650);
        }
    }

    private void speakSpanish(String value, String id) {
        if (!ttsReady || tts == null) return;
        int result = tts.setLanguage(new Locale("es", "PA"));
        if (result < 0) result = tts.setLanguage(new Locale("es", "MX"));
        if (result < 0) tts.setLanguage(new Locale("es"));
        tts.setSpeechRate(0.92f);
        tts.speak(value, TextToSpeech.QUEUE_FLUSH, null, id);
    }

    private void speakEnglish(String value, String id) {
        if (!ttsReady || tts == null) return;
        int result = tts.setLanguage(Locale.US);
        if (result < 0) tts.setLanguage(Locale.ENGLISH);
        tts.setSpeechRate(0.96f);
        tts.speak(value, TextToSpeech.QUEUE_FLUSH, null, id);
    }

    @Override public void onBackPressed() {
        if (sessionActive) endSession();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        sessionActive = false;
        pendingSessionStart = false;
        stopListening();
        handler.removeCallbacksAndMessages(null);
        if (recognizer != null) {
            try { recognizer.destroy(); } catch (Exception ignored) {}
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
