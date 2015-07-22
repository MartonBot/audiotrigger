package io.martonbot.audiotrigger;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {

    private static final long TICK_DELAY = 75;

    private boolean isAudioEnabled;
    private int threshold;
    private int cooldown;
    private int pollInterval;

    private View settingsButton;
    private View settingsButton2;
    private View resetButton;
    private TextView minutesText;
    private TextView secondsText;
    private TextView hundredthsText;
    private View ampDisc;
    private View chronoView;

    private Runnable pollTask;
    private Runnable tickTask;
    private Handler taskHandler;
    private AudioMonitor monitor;

    private SharedPreferences sharedPreferences;
    private ScaleAnimation anim;
    private float currentScale = 1f;

    private boolean isChronometerRunning = false;
    private long elapsedTime = 0;
    private long triggerTime;

    private long chronoBase;
    private int minutes;
    private int seconds;
    private int hundredths;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        monitor = new AudioMonitor();
        taskHandler = new Handler();
        sharedPreferences = getSharedPreferences(Preferences.SHARED_PREFS, MODE_PRIVATE);

        resetButton = findViewById(R.id.reset_button);
        minutesText = (TextView) findViewById(R.id.minutes_text);
        secondsText = (TextView) findViewById(R.id.seconds_text);
        hundredthsText = (TextView) findViewById(R.id.hundredths_text);
        ampDisc = findViewById(R.id.amp_disc);
        settingsButton = findViewById(R.id.settings_button);
        settingsButton2 = findViewById(R.id.settings_button2);
        chronoView = findViewById(R.id.chrono_view);

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reset();
            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settingsActivity = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(settingsActivity);
            }
        });

        settingsButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settingsActivity = new Intent(MainActivity.this, SettingsActivity2.class);
                startActivity(settingsActivity);
            }
        });

        chronoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleChronometer();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // shared preferences
        isAudioEnabled = sharedPreferences.getBoolean(Preferences.PREF_AUDIO_ENABLED, true);
        threshold = sharedPreferences.getInt(Preferences.PREF_THRESHOLD, Preferences.DEFAULT_THRESHOLD);
        cooldown = sharedPreferences.getInt(Preferences.PREF_COOLDOWN, Preferences.DEFAULT_COOLDOWN);
        pollInterval = sharedPreferences.getInt(Preferences.PREF_POLL_INTERVAL, Preferences.DEFAULT_POLL_INTERVAL);

        // start monitoring
        if (isAudioEnabled) {
            startAudioMonitoring();
        } else {
            stopAudioMonitoring();
        }

        if (isChronometerRunning) {
            taskHandler.postDelayed(getTickTask(), TICK_DELAY);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // stop monitoring
        stopAudioMonitoring();

        // cancel Handler callback
        taskHandler.removeCallbacks(getPollTask());
        taskHandler.removeCallbacks(getTickTask());

        if (anim != null) {
            anim.cancel();
        }

        // TODO consider saving elapsed time and running status to storage on Save Instance State
    }

    private void startChronometer() {
        chronoBase = SystemClock.elapsedRealtime() - elapsedTime;
        isChronometerRunning = true;
        taskHandler.postDelayed(getTickTask(), TICK_DELAY);
        updateResetButton();
    }

    private void stopChronometer() {
        elapsedTime = SystemClock.elapsedRealtime() - chronoBase;
        taskHandler.removeCallbacks(getTickTask());
        isChronometerRunning = false;
        updateResetButton();
    }

    private void toggleChronometer() {
        if (isChronometerRunning) {
            stopChronometer();
        } else {
            startChronometer();
        }
    }

    private void reset() {
        elapsedTime = 0;
        updateResetButton();
        hundredthsText.setText("00");
        secondsText.setText("00");
        minutesText.setText("00");
    }

    private void updateResetButton() {
        if (elapsedTime > 0 && !isChronometerRunning) {
            resetButton.setVisibility(View.VISIBLE);
        } else {
            resetButton.setVisibility(View.GONE);
        }
    }

    private Runnable getPollTask() {
        if (pollTask == null) {
            pollTask = new Runnable() {
                @Override
                public void run() {

                    int ampLog = monitor.getLogMaxAmplitude();
                    float ratio = ampLog / (float) threshold;

                    anim = new ScaleAnimation(currentScale, (2 + ratio) / 3, currentScale, (2 + ratio) / 3, Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, .5f);
                    anim.setDuration((long) (pollInterval * .75f));
                    anim.setFillEnabled(true);
                    anim.setFillAfter(true);
                    ampDisc.startAnimation(anim);

                    currentScale = (2 + ratio) / 3;

                    long time = SystemClock.elapsedRealtime();
                    if (ampLog >= threshold && time - triggerTime > cooldown) {
                        triggerTime = time;
                        toggleChronometer();
                    }

                    taskHandler.postDelayed(this, pollInterval);
                }
            };
        }
        return pollTask;
    }

    private Runnable getTickTask() {
        if (tickTask == null) {
            tickTask = new Runnable() {

                @Override
                public void run() {

                    // update the second hundredths
                    long time = SystemClock.elapsedRealtime();
                    hundredths = (int) (time - chronoBase) / 10;
                    seconds = hundredths / 100;
                    minutes = seconds / 60;

                    hundredthsText.setText(format(hundredths % 100));
                    secondsText.setText(format(seconds % 60));
                    minutesText.setText(format(minutes % 60));

                    taskHandler.postDelayed(this, minutes);

                }
            };
        }
        return tickTask;
    }

    private String format(int n) {
        return String.format("%02d", n);
    }

    private void startAudioMonitoring() {
        boolean isAudioAvailable = monitor.startMonitoring();
        if (isAudioAvailable) {
            taskHandler.postDelayed(getPollTask(), pollInterval);
        } else {
            monitor.stopMonitoring();
            Toast.makeText(MainActivity.this, "Audio monitoring is not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopAudioMonitoring() {
        monitor.stopMonitoring();
    }

}
