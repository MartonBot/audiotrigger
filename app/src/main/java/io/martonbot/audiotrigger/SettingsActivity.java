package io.martonbot.audiotrigger;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.view.animation.ScaleAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


public class SettingsActivity extends Activity {

    // TODO maybe put that in a class
    public final static String SHARED_PREFS = "AUDIO_TRIGGER_PREFERENCES";

    public final static String PREF_AUDIO_ENABLED = "PREF_AUDIO_ENABLED";
    public final static String PREF_THRESHOLD = "PREF_THRESHOLD";
    public final static String PREF_COOLDOWN = "PREF_COOLDOWN";
    public final static String PREF_POLL_INTERVAL = "PREF_POLL_INTERVAL";

    public final static int DEFAULT_THRESHOLD = 8;
    public final static int DEFAULT_COOLDOWN = 1000;
    public final static int DEFAULT_POLL_INTERVAL = 200;

    private View thresholdSection;
    private View cooldownSection;
    private View pollIntervalSection;

    private View ampBar;
    private TextView audioStatusText;
    private Switch enableAudioSwitch;
    private SeekBar thresholdSeekBar;
    private Spinner cooldownSpinner;
    private Spinner pollIntervalSpinner;

    private DropdownAdapter cooldownAdapter;
    private DropdownAdapter pollIntervalAdapter;

    private AudioMonitor monitor;

    private Runnable pollTask;
    private Handler taskHandler;
    private SharedPreferences sharedPreferences;

    private ScaleAnimation anim;
    private float currentScale = 1f;
    private long triggerTime;

    private boolean isAudioEnabled;
    private int threshold;
    private int cooldown;
    private int pollInterval;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        monitor = new AudioMonitor();
        taskHandler = new Handler();
        sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);

        thresholdSection = findViewById(R.id.threshold_level_section);
        cooldownSection = findViewById(R.id.trigger_cooldown_section);
        pollIntervalSection = findViewById(R.id.poll_interval_section);

        ampBar = findViewById(R.id.amp_bar);
        audioStatusText = (TextView) findViewById(R.id.audio_status_text);

        enableAudioSwitch = (Switch) findViewById(R.id.switch_enable_audio);
        thresholdSeekBar = (SeekBar) findViewById(R.id.threshold_seekbar);
        cooldownSpinner = (Spinner) findViewById(R.id.cooldown_spinner);
        pollIntervalSpinner = (Spinner) findViewById(R.id.poll_interval_spinner);

        cooldownAdapter = new DropdownAdapter();
        cooldownAdapter.add(500);
        cooldownAdapter.add(DEFAULT_COOLDOWN);
        cooldownAdapter.add(2000);
        cooldownAdapter.add(5000);
        cooldownSpinner.setAdapter(cooldownAdapter);

        pollIntervalAdapter = new DropdownAdapter();
        pollIntervalAdapter.add(DEFAULT_POLL_INTERVAL);
        pollIntervalAdapter.add(500);
        pollIntervalAdapter.add(1000);
        pollIntervalSpinner.setAdapter(pollIntervalAdapter);

        enableAudioSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isAudioEnabled = isChecked;
                toggleAudioEnabled();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(PREF_AUDIO_ENABLED, isChecked);
                editor.apply();
                int textId;
                if (isAudioEnabled) {
                    startAudioMonitoring();
                    textId = R.string.audio_trigger_enabled;
                } else {
                    stopAudioMonitoring();
                    textId = R.string.audio_trigger_disabled;
                }
                audioStatusText.setText(textId);
            }
        });

        thresholdSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                threshold = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // nothing
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(PREF_THRESHOLD, threshold);
                editor.apply();
            }

        });

        cooldownSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                cooldown = (int) cooldownSpinner.getSelectedItem();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(PREF_COOLDOWN, cooldown);
                editor.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // do nothing
            }
        });

        pollIntervalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                pollInterval = (int) pollIntervalSpinner.getSelectedItem();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(PREF_POLL_INTERVAL, pollInterval);
                editor.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // do nothing
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // shared preferences
        isAudioEnabled = sharedPreferences.getBoolean(PREF_AUDIO_ENABLED, true);
        threshold = sharedPreferences.getInt(PREF_THRESHOLD, DEFAULT_THRESHOLD);
        cooldown = sharedPreferences.getInt(PREF_COOLDOWN, DEFAULT_COOLDOWN);
        pollInterval = sharedPreferences.getInt(PREF_POLL_INTERVAL, DEFAULT_POLL_INTERVAL);

        // start monitoring
        if (isAudioEnabled) {
            startAudioMonitoring();
        } else {
            stopAudioMonitoring();
        }

        enableAudioSwitch.setChecked(isAudioEnabled);
        thresholdSeekBar.setProgress(threshold);
        cooldownSpinner.setSelection(cooldownAdapter.getPosition(cooldown));
        pollIntervalSpinner.setSelection(pollIntervalAdapter.getPosition(pollInterval));

        toggleAudioEnabled();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // stop monitoring
        stopAudioMonitoring();

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
                    anim.setDuration((long) (pollInterval * .80));
                    anim.setFillEnabled(true);
                    anim.setFillAfter(true);
                    ampBar.startAnimation(anim);
                    currentScale = ampLog / 10f;

                    long time = SystemClock.elapsedRealtime();
                    boolean isTriggered = ampLog >= threshold && time >= triggerTime + cooldown;
                    if (isTriggered) {
                        triggerTime = time;
                    }

                    int colorId = isTriggered ? R.color.primary_dark : R.color.primary_light;
                    ampBar.setBackgroundColor(getResources().getColor(colorId));

                    taskHandler.postDelayed(this, pollInterval);
                }
            };
        }
        return pollTask;
    }

    private void toggleAudioEnabled() {
        int state = View.VISIBLE;
        if (!isAudioEnabled) {
            state = View.INVISIBLE;
        }
        thresholdSection.setVisibility(state);
        cooldownSection.setVisibility(state);
        pollIntervalSection.setVisibility(state);
    }

    private void startAudioMonitoring() {
        boolean isAudioAvailable = monitor.startMonitoring();
        if (isAudioAvailable) {
            taskHandler.postDelayed(getPollTask(), pollInterval);
        } else {
            monitor.stopMonitoring();
            Toast errorToast = Toast.makeText(SettingsActivity.this, "Audio monitoring is not available", Toast.LENGTH_SHORT);
            errorToast.show();
        }
    }

    private void stopAudioMonitoring() {
        monitor.stopMonitoring();
    }

    private class DropdownAdapter extends ArrayAdapter<Integer> {

        public DropdownAdapter() {
            super(SettingsActivity.this, R.layout.dropdown_item);
        }

    }

}
