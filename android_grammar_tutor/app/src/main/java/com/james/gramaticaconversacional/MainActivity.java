package com.james.gramaticaconversacional;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Locale;

public final class MainActivity extends Activity implements TextToSpeech.OnInitListener {
    private static final int AUDIO_PERMISSION = 7001;
    private static final String[] KEYS={"yo","tu","el","nosotros","ellos"};
    private static final String[] LABELS={"yo","tú","él / ella / usted","nosotros","ustedes / ellos / ellas"};
    private static final String[] FORMS={"tengo","tienes","tiene","tenemos","tienen"};
    private final int[] mastery=new int[5];
    private final Handler handler=new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private TextView stageText, masteryText, promptText, hintText, heardText, feedbackText, statusText;
    private ProgressBar masteryBar;
    private EditText typed;
    private Switch autoSwitch;
    private Button startButton, micButton;
    private TextToSpeech tts;
    private boolean ttsReady=false, running=false, listening=false;
    private SpeechRecognizer recognizer;
    private Intent speechIntent;
    private int promptCounter=0, target=0, recovery=-1, transfer=-1, rapidStreak=0;
    private String expected="tengo", model="Yo tengo tiempo hoy.";

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences("grammar_progress",MODE_PRIVATE);
        for(int i=0;i<5;i++) mastery[i]=prefs.getInt(KEYS[i],0);
        buildUi();
        tts=new TextToSpeech(this,this);
        setupRecognizer();
        updateProgress();
    }

    private TextView tv(String text,int sp,int color){ TextView v=new TextView(this); v.setText(text); v.setTextSize(sp); v.setTextColor(color); v.setPadding(0,6,0,6); return v; }
    private Button btn(String text){ Button b=new Button(this); b.setText(text); b.setTextAllCaps(false); return b; }
    private void buildUi(){
        int bg=Color.rgb(16,19,24), white=Color.rgb(244,246,248), sub=Color.rgb(183,192,204), accent=Color.rgb(127,179,255);
        ScrollView sv=new ScrollView(this); sv.setBackgroundColor(bg);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18),dp(28),dp(18),dp(28)); sv.addView(root);
        TextView title=tv("Gramática Conversacional",28,white); title.setTypeface(Typeface.DEFAULT,Typeface.BOLD); root.addView(title);
        root.addView(tv("Tutor verbal · español latinoamericano",15,sub));
        TextView topic=tv("TENER · presente de indicativo",21,accent); topic.setTypeface(Typeface.DEFAULT,Typeface.BOLD); topic.setPadding(0,dp(18),0,dp(6)); root.addView(topic);
        root.addView(tv("yo tengo · tú tienes · él/ella/usted tiene · nosotros tenemos · ustedes/ellos/ellas tienen\n\nPatrón: yo usa teng-, varias formas usan tien-, y nosotros vuelve a ten-. También practicaremos tener que + infinitivo.",16,white));
        stageText=tv("",14,sub); root.addView(stageText);
        masteryBar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); masteryBar.setMax(100); root.addView(masteryBar,new LinearLayout.LayoutParams(-1,dp(12)));
        masteryText=tv("",13,sub); root.addView(masteryText);
        autoSwitch=new Switch(this); autoSwitch.setText("Modo conversación automática"); autoSwitch.setTextColor(white); autoSwitch.setChecked(true); root.addView(autoSwitch);
        TextView turn=tv("TU TURNO",12,sub); turn.setTypeface(Typeface.DEFAULT,Typeface.BOLD); turn.setPadding(0,dp(16),0,0); root.addView(turn);
        promptText=tv("Pulsa Comenzar. Trabajaremos este tema hasta que las formas salgan automáticamente.",23,white); promptText.setTypeface(Typeface.DEFAULT,Typeface.BOLD); root.addView(promptText);
        hintText=tv("Si fallas una forma, vuelve inmediatamente y después reaparece en otro contexto.",14,sub); root.addView(hintText);
        heardText=tv("",15,accent); root.addView(heardText);
        feedbackText=tv("",16,white); root.addView(feedbackText);
        statusText=tv("Preparado.",13,sub); root.addView(statusText);
        startButton=btn("Comenzar"); root.addView(startButton); startButton.setOnClickListener(v->{ if(running) pause(); else start(); });
        LinearLayout row1=new LinearLayout(this); row1.setOrientation(LinearLayout.HORIZONTAL); root.addView(row1);
        Button listen=btn("Escuchar"); micButton=btn("Responder 🎙"); row1.addView(listen,new LinearLayout.LayoutParams(0,-2,1)); row1.addView(micButton,new LinearLayout.LayoutParams(0,-2,1));
        listen.setOnClickListener(v->speak(promptText.getText().toString(),"MANUAL")); micButton.setOnClickListener(v->listen());
        LinearLayout row2=new LinearLayout(this); row2.setOrientation(LinearLayout.HORIZONTAL); root.addView(row2);
        Button show=btn("Mostrar respuesta"), next=btn("Siguiente"); row2.addView(show,new LinearLayout.LayoutParams(0,-2,1)); row2.addView(next,new LinearLayout.LayoutParams(0,-2,1));
        show.setOnClickListener(v->reveal()); next.setOnClickListener(v->nextPrompt(false));
        typed=new EditText(this); typed.setHint("Respuesta escrita"); typed.setTextColor(white); typed.setHintTextColor(sub); typed.setSingleLine(true); root.addView(typed);
        Button check=btn("Comprobar"); root.addView(check); check.setOnClickListener(v->{String s=typed.getText().toString(); if(!s.trim().isEmpty()) evaluate(s);});
        Button reset=btn("Reiniciar dominio de este tema"); root.addView(reset); reset.setOnClickListener(v->reset());
        setContentView(sv);
    }
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}

    private void start(){ running=true; startButton.setText("Pausar"); statusText.setText("Sesión activa."); speak("Vamos a trabajar un solo tema hasta que sea automático: el presente de tener. Yo tengo, tú tienes, él, ella o usted tiene, nosotros tenemos, ustedes, ellos o ellas tienen.","INTRO"); if(!ttsReady) nextPrompt(false); }
    private void pause(){ running=false; startButton.setText("Continuar"); stopListening(); if(tts!=null)tts.stop(); statusText.setText("Sesión pausada."); }

    private int stage(){ int min=5; for(int x:mastery)min=Math.min(min,x); if(min<=0)return 0; if(min==1)return 1; if(min==2)return 2; if(min==3)return 3; if(min==4)return 4; return 5; }
    private String stageName(){String[] s={"Formas","Transformación","Contexto","Conversación","Fluidez","Dominado"}; return s[stage()];}
    private int weakest(){int m=99,idx=0; for(int i=0;i<5;i++){if(mastery[i]<m){m=mastery[i];idx=i;}} return idx;}
    private void nextPrompt(boolean speakIt){
        if(!running){running=true;startButton.setText("Pausar");}
        stopListening(); typed.setText(""); heardText.setText(""); feedbackText.setText(""); promptCounter++;
        int st=stage(); if(st==5){promptText.setText("Tema dominado. Reinícialo cuando quieras otra vuelta de consolidación."); hintText.setText(""); updateProgress(); return;}
        if(recovery>=0){target=recovery; expected=FORMS[target]; model=LABELS[target]+" "+FORMS[target]; promptText.setText("Corrección: "+correction(target)+" Repite: "+model+"."); hintText.setText("Recuperación inmediata.");}
        else if(transfer>=0){target=transfer; transfer=-1; setTransferPrompt(target); hintText.setText("Transferencia: la misma forma vuelve en otro contexto.");}
        else {target=weakest(); setNormalPrompt(target,st); hintText.setText(st>=3?"Responde con una frase natural.":"Responde en voz alta.");}
        statusText.setText("Listo para tu respuesta."); updateProgress(); if(speakIt&&ttsReady)speak(promptText.getText().toString(),"PROMPT");
    }
    private void setNormalPrompt(int i,int st){ expected=FORMS[i]; String l=LABELS[i];
        if(st==0){promptText.setText(promptCounter%2==0?"Di la forma de tener para "+l+".":"Completa: "+capital(l)+" ___ tiempo hoy."); model=capital(l)+" "+expected+" tiempo hoy.";}
        else if(st==1){String from=i==0?"Ella tiene una reunión.":"Yo tengo una reunión."; promptText.setText("Cambia la frase para hablar de "+l+": "+from); model=capital(l)+" "+expected+" una reunión.";}
        else if(st==2){promptText.setText("Completa en contexto: "+capital(l)+" ___ una cita mañana."); model=capital(l)+" "+expected+" una cita mañana.";}
        else if(st==3){ if(i==0){promptText.setText("¿Qué tienes que hacer mañana? Responde usando tener que."); model="Mañana tengo que hacer ejercicio.";} else {promptText.setText("Usa tener en una frase natural hablando de "+l+"."); model=capital(l)+" "+expected+" tiempo mañana.";} }
        else {promptText.setText(capital(l)+": tener."); model=capital(l)+" "+expected+".";}
    }
    private void setTransferPrompt(int i){expected=FORMS[i]; promptText.setText("Ahora úsalo en otra frase: "+capital(LABELS[i])+" ___ una pregunta."); model=capital(LABELS[i])+" "+expected+" una pregunta.";}
    private String correction(int i){ if(i==0)return "Con yo, tener es irregular: yo tengo."; if(i==1)return "Con tú: tú tienes."; if(i==2)return "Con él, ella o usted: tiene."; if(i==3)return "Con nosotros: tenemos; aquí no hay cambio de raíz."; return "Con ustedes, ellos o ellas: tienen."; }
    private String capital(String s){return s.substring(0,1).toUpperCase(new Locale("es"))+s.substring(1);}

    private void evaluate(String answer){String a=norm(answer), token=norm(expected); heardText.setText("Te entendí: "+answer); boolean ok=containsWord(a,token); if(target==0 && promptText.getText().toString().contains("tener que")) ok=a.contains("tengo que");
        if(ok){ if(recovery==target){recovery=-1; transfer=target; feedbackText.setText("Correcto. Ahora comprobaré que puedas transferir esa forma.");} else {mastery[target]=Math.min(5,mastery[target]+1); if(stage()==4)rapidStreak++; feedbackText.setText("Correcto: "+model); } save(); updateProgress(); speak(feedbackText.getText().toString(),autoSwitch.isChecked()&&running?"FEEDBACK":"MANUAL"); }
        else {mastery[target]=Math.max(0,mastery[target]-1); recovery=target; rapidStreak=0; feedbackText.setText("Aún no. "+correction(target)+" La respuesta modelo es: "+model); save(); updateProgress(); speak(feedbackText.getText().toString(),autoSwitch.isChecked()&&running?"FEEDBACK":"MANUAL"); }
    }
    private boolean containsWord(String a,String token){for(String w:a.split("[^a-z]+"))if(w.equals(token))return true;return false;}
    private String norm(String s){String n=Normalizer.normalize(s.toLowerCase(new Locale("es")),Normalizer.Form.NFD).replaceAll("\\p{M}+",""); return n.replace('ñ','n').replaceAll("[^a-z ]"," ").replaceAll("\\s+"," ").trim();}
    private void reveal(){if(!running||stage()==5)return; mastery[target]=Math.max(0,mastery[target]-1); recovery=target; rapidStreak=0; feedbackText.setText("Respuesta: "+model+". "+correction(target)); save(); updateProgress(); speak(feedbackText.getText().toString(),autoSwitch.isChecked()?"FEEDBACK":"MANUAL");}
    private void updateProgress(){int total=0;for(int x:mastery)total+=x;int pct=(int)Math.round(total/25.0*100);masteryBar.setProgress(pct);stageText.setText("Etapa: "+stageName());masteryText.setText(pct+"% de dominio · punto más débil: "+LABELS[weakest()]);}
    private void save(){SharedPreferences.Editor e=prefs.edit();for(int i=0;i<5;i++)e.putInt(KEYS[i],mastery[i]);e.apply();}
    private void reset(){pause();for(int i=0;i<5;i++)mastery[i]=0;recovery=transfer=-1;rapidStreak=0;save();updateProgress();promptText.setText("Pulsa Comenzar para iniciar de nuevo.");feedbackText.setText("");heardText.setText("");Toast.makeText(this,"Progreso reiniciado",Toast.LENGTH_SHORT).show();}

    private void setupRecognizer(){if(!SpeechRecognizer.isRecognitionAvailable(this)){micButton.setEnabled(false);statusText.setText("Reconocimiento de voz no disponible. Usa respuesta escrita.");return;} recognizer=SpeechRecognizer.createSpeechRecognizer(this);speechIntent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"es-PA");speechIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3);recognizer.setRecognitionListener(new RecognitionListener(){public void onReadyForSpeech(Bundle p){listening=true;micButton.setText("Escuchando…");}public void onBeginningOfSpeech(){}public void onRmsChanged(float r){}public void onBufferReceived(byte[] b){}public void onEndOfSpeech(){statusText.setText("Procesando…");}public void onError(int e){listening=false;micButton.setText("Responder 🎙");statusText.setText("No pude reconocer la respuesta. Intenta otra vez o escribe.");}public void onResults(Bundle b){listening=false;micButton.setText("Responder 🎙");ArrayList<String> m=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(m!=null&&!m.isEmpty())evaluate(m.get(0));}public void onPartialResults(Bundle b){}public void onEvent(int t,Bundle b){}});}
    private void listen(){if(!running||stage()==5)return;if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},AUDIO_PERMISSION);return;}startListening();}
    private void startListening(){if(recognizer==null||listening)return;if(tts!=null&&tts.isSpeaking())tts.stop();try{recognizer.startListening(speechIntent);}catch(Exception e){statusText.setText("No pude iniciar el micrófono.");}}
    private void stopListening(){if(recognizer!=null&&listening)try{recognizer.cancel();}catch(Exception ignored){}listening=false;if(micButton!=null)micButton.setText("Responder 🎙");}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==AUDIO_PERMISSION&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)startListening();}

    @Override public void onInit(int status){if(status!=TextToSpeech.SUCCESS){statusText.setText("La voz no pudo iniciarse; puedes practicar por escrito.");return;}int r=tts.setLanguage(new Locale("es","PA"));if(r<0)r=tts.setLanguage(new Locale("es","MX"));if(r<0)tts.setLanguage(new Locale("es"));tts.setSpeechRate(.92f);ttsReady=true;tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){public void onStart(String id){}public void onError(String id){}public void onDone(String id){runOnUiThread(()->speechDone(id));}});}
    private void speechDone(String id){if(!running)return;if("INTRO".equals(id))nextPrompt(autoSwitch.isChecked());else if("PROMPT".equals(id)&&autoSwitch.isChecked())handler.postDelayed(this::listen,400);else if("FEEDBACK".equals(id)&&autoSwitch.isChecked())handler.postDelayed(()->nextPrompt(true),650);}
    private void speak(String text,String id){if(!ttsReady||text==null)return;stopListening();tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,id);}
    @Override protected void onDestroy(){running=false;if(recognizer!=null)try{recognizer.destroy();}catch(Exception ignored){}if(tts!=null){tts.stop();tts.shutdown();}super.onDestroy();}
}
