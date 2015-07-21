package io.martonbot.audiotrigger;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {

    private static final long POLL_DELAY = 200;
    private static final long TICK_DELAY = 75;
    private static final long COOLDOWN = 500;

    private Button resetButton;
    private TextView minutesText;
    private TextView secondsText;
    private TextView hundredthsText;
    private View ampDisc;
    private View settings;
    private View chronoView;

    private Runnable pollTask;
    private Runnable tickTask;
    private Handler taskHandler;
    private AudioMonitor monitor;

    private SharedPreferences sharedPreferences;
    private ScaleAnimation anim;
    private float currentScale = 1f;
    private int currentThreshold;
    private boolean isAudioEnabled;

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
        sharedPreferences = getSharedPreferences(CalibrateAudioActivity.SHARED_PREFS, MODE_PRIVATE);

        currentThreshold = sharedPreferences.getInt(CalibrateAudioActivity.PREF_THRESHOLD, CalibrateAudioActivity.DEFAULT_THRESHOLD);
        isAudioEnabled = sharedPreferences.getBoolean(CalibrateAudioActivity.PREF_AUDIO_ENABLED, true);

        resetButton = (Button) findViewById(R.id.reset_button);
        minutesText = (TextView) findViewById(R.id.minutes_text);
        secondsText = (TextView) findViewById(R.id.seconds_text);
        hundredthsText = (TextView) findViewById(R.id.hundredths_text);
        ampDisc = findViewById(R.id.amp_disc);
        settings = findViewById(R.id.settings);
        chronoView = findViewById(R.id.chrono_view);

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reset();
            }
        });

        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settingsActivity = new Intent(MainActivity.this, CalibrateAudioActivity.class);
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

        // start monitoring
        boolean success = monitor.startMonitoring();
        if (success) {
            taskHandler.postDelayed(getPollTask(), POLL_DELAY);
        } else {
            Toast errorToast = Toast.makeText(MainActivity.this, "Audio monitoring is not available", Toast.LENGTH_SHORT);
            errorToast.show();
            // TODO disable relevant UI parts
        }

        if (isChronometerRunning) {
            taskHandler.postDelayed(getTickTask(), TICK_DELAY);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        // stop monitoring
        monitor.stopMonitoring();

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

                    anim = new ScaleAnimation(currentScale, ampLog / 10f, currentScale, ampLog / 10f);
                    anim.setDuration((long) (POLL_DELAY * .75));
                    anim.setFillEnabled(true);
                    anim.setFillAfter(true);
                    currentScale = ampLog / 10f;
                    ampDisc.startAnimation(anim);

                    int colorId = ampLog >= currentThreshold ? R.color.primary_light : R.color.primary_dark;
                    ampDisc.setBackgroundColor(getResources().getColor(colorId));

                    if (ampLog >= currentThreshold && SystemClock.elapsedRealtime() - triggerTime > COOLDOWN) {
                        triggerTime = SystemClock.elapsedRealtime();
                        toggleChronometer();
                    }

                    taskHandler.postDelayed(this, POLL_DELAY);
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


}
