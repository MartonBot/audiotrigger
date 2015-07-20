package io.martonbot.time;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.content.res.ResourcesCompat;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;


public class MainActivity extends Activity {

    private static final long POLL_DELAY = 200;
    private static final long TICK_DELAY = 75;
    private static final long COOLDOWN = 500;
    private static final int BASE = 5;
    private static final int THRESHOLD = 10;
    private float density;
    private Chronometer chronometer;
    private Button resetButton;
    private TextView hundredthsText;
    private View ampDisc;

    private Runnable pollTask;
    private Runnable tickTask;
    private Handler taskHandler;
    private AudioMonitor monitor;

    private boolean isChronometerRunning = false;
    private long elapsedTime = 0;
    private long triggerTime;

    private long chronoBase;
    private int hundredths;
    private int tickCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        density = getResources().getDisplayMetrics().density;

        monitor = new AudioMonitor();
        taskHandler = new Handler();

        chronometer = (Chronometer) findViewById(R.id.chronometer);
        resetButton = (Button) findViewById(R.id.reset_button);
        hundredthsText = (TextView) findViewById(R.id.hundredths_text);
        ampDisc = findViewById(R.id.amp_disc);

        chronometer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleChronometer();
            }
        });

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reset();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // start monitoring
        try {
            monitor.startMonitoring();
            taskHandler.postDelayed(getPollTask(), POLL_DELAY);

        } catch (IOException e) {
            monitor.stopMonitoring();
            Toast errorToast = Toast.makeText(MainActivity.this, "Audio monitoring is not available", Toast.LENGTH_SHORT);
            errorToast.show();
        }

        if (isChronometerRunning) {
            chronometer.start();
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

        // stop chronometer updates on pause
        // but don't stop the time counting
        if (isChronometerRunning) {
            chronometer.stop();
        }

        // TODO consider saving elapsed time and running status to storage on Save Instance State
    }

    private void startChronometer() {
        chronoBase = SystemClock.elapsedRealtime() - elapsedTime;
        chronometer.setBase(chronoBase);
        chronometer.start();
        isChronometerRunning = true;
        taskHandler.postDelayed(getTickTask(), TICK_DELAY);
        updateResetButton();
    }

    private void stopChronometer() {
        elapsedTime = SystemClock.elapsedRealtime() - chronoBase;
        chronometer.stop();
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
        chronometer.setBase(SystemClock.elapsedRealtime());
        elapsedTime = 0;
        updateResetButton();
        hundredthsText.setText("00");
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

                    int amp = monitor.getLogMaxAmplitude();

                    int diameter = (int) ((((amp - BASE) * (100 / (THRESHOLD - BASE))) + 50) * density + .5f);
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) ampDisc.getLayoutParams();
                    params.width = diameter;
                    params.height = diameter;
                    ampDisc.setLayoutParams(params);

                    int drawableId = R.drawable.black_circle;

                    if (amp >= THRESHOLD && SystemClock.elapsedRealtime() - triggerTime > COOLDOWN) {
                        triggerTime = SystemClock.elapsedRealtime();
                        toggleChronometer();
                        drawableId = R.drawable.red_circle;

                    }

                    ampDisc.setBackground(ResourcesCompat.getDrawable(getResources(), drawableId, null));

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
                    hundredths = (int) (((SystemClock.elapsedRealtime() - chronoBase) / 10) % 100);
                    hundredthsText.setText(String.format("%02d", hundredths));

                    taskHandler.postDelayed(this, TICK_DELAY);

                }
            };
        }
        return tickTask;
    }


}
