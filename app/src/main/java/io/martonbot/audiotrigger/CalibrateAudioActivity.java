package io.martonbot.audiotrigger;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.ScaleAnimation;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;


public class CalibrateAudioActivity extends Activity {

    // TODO maybe put that in a class
    public final static String SHARED_PREFS = "AUDIO_TRIGGER_PREFERENCES";
    public final static String PREF_THRESHOLD = "PREF_THRESHOLD";
    public final static String PREF_AUDIO_ENABLED = "PREF_AUDIO_ENABLED";
    public final static int DEFAULT_THRESHOLD = 8;
    private static final long POLL_DELAY = 200;

    // TODO add cooldown setting
    private View ampBar;
    private Switch enableAudioSwitch;
    private SeekBar thresholdSeekBar;
    private View ampBarContainer;

    private AudioMonitor monitor;

    private Runnable pollTask;
    private Handler taskHandler;
    private SharedPreferences sharedPreferences;

    private ScaleAnimation anim;
    private float currentScale = 1f;
    private int currentThreshold;
    private boolean isAudioEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibrate_audio);

        monitor = new AudioMonitor();
        taskHandler = new Handler();
        sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);

        currentThreshold = sharedPreferences.getInt(PREF_THRESHOLD, DEFAULT_THRESHOLD);
        isAudioEnabled = sharedPreferences.getBoolean(PREF_AUDIO_ENABLED, true);

        ampBar = findViewById(R.id.amp_bar);
        enableAudioSwitch = (Switch) findViewById(R.id.switch_enable_audio);
        thresholdSeekBar = (SeekBar) findViewById(R.id.threshold_seekbar);
        ampBarContainer = findViewById(R.id.amp_bar_container);

        thresholdSeekBar.setProgress(currentThreshold);

        enableAudioSwitch.setChecked(isAudioEnabled);

        enableAudioSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                toggleAudioEnabled(isChecked);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(PREF_AUDIO_ENABLED, isChecked);
                editor.apply();
            }
        });

        thresholdSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentThreshold = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // nothing
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(PREF_THRESHOLD, currentThreshold);
                editor.apply();
            }

        });

        toggleAudioEnabled(isAudioEnabled);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // start monitoring
        boolean success = monitor.startMonitoring();
        if (success) {
            taskHandler.postDelayed(getPollTask(), POLL_DELAY);
        } else {
            Toast errorToast = Toast.makeText(CalibrateAudioActivity.this, "Audio monitoring is not available", Toast.LENGTH_SHORT);
            errorToast.show();
            // TODO disable relevant UI parts
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        // stop monitoring
        monitor.stopMonitoring();

        // cancel Handler callback
        taskHandler.removeCallbacks(getPollTask());

        if (anim != null) {
            anim.cancel();
        }

    }

    private Runnable getPollTask() {
        if (pollTask == null) {
            pollTask = new Runnable() {
                @Override
                public void run() {

                    int ampLog = monitor.getLogMaxAmplitude();

                    anim = new ScaleAnimation(currentScale, ampLog / 10f, 1f, 1f);
                    anim.setDuration((long) (POLL_DELAY * .75));
                    anim.setFillEnabled(true);
                    anim.setFillAfter(true);
                    currentScale = ampLog / 10f;
                    ampBar.startAnimation(anim);

                    int colorId = ampLog >= currentThreshold ? R.color.primary_dark : R.color.primary_light;
                    ampBar.setBackgroundColor(getResources().getColor(colorId));

                    taskHandler.postDelayed(this, POLL_DELAY);
                }
            };
        }
        return pollTask;
    }

    private void toggleAudioEnabled(boolean isAudioEnabled) {
        int state = View.VISIBLE;
        if (!isAudioEnabled) {
            state = View.INVISIBLE;
        }
        thresholdSeekBar.setVisibility(state);
        ampBarContainer.setVisibility(state);
    }

}
