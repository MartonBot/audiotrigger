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
    private static final long COOLDOWN = 2000;

    private static final int BASE = 6;
    private static final int THRESHOLD = 10;

    private Chronometer chronometer;
    private Button resetButton;
    private TextView ampText;
    private View ampDisc;

    private Runnable pollTask;
    private Handler taskHandler;
    private AudioMonitor monitor;

    private boolean isChronometerRunning = false;
    private long elapsedTime = 0;
    private long triggerTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        monitor = new AudioMonitor();
        taskHandler = new Handler();

        chronometer = (Chronometer) findViewById(R.id.chronometer);
        resetButton = (Button) findViewById(R.id.reset_button);
        ampText = (TextView) findViewById(R.id.amp_text);
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
        }

    }

    @Override
    protected void onPause() {

        // stop monitoring
        monitor.stopMonitoring();

        // cancel Handler callback
        taskHandler.removeCallbacks(getPollTask());

        // stop chronometer updates on pause
        // but don't stop the time counting
        if (isChronometerRunning) {
            chronometer.stop();
        }

        // TODO consider saving elapsed time and running status to storage

        super.onPause();
    }

    private void startChronometer() {
        chronometer.setBase(SystemClock.elapsedRealtime() - elapsedTime);
        chronometer.start();
        isChronometerRunning = true;
        updateResetButton();
    }

    private void stopChronometer() {
        elapsedTime = SystemClock.elapsedRealtime() - chronometer.getBase();
        chronometer.stop();
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
                    ampText.setText(String.valueOf(amp));
                    int diameter = ((amp - BASE) * (100 / (THRESHOLD - BASE))) + 50;
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) ampDisc.getLayoutParams();
                    params.width = diameter;
                    params.height = diameter;
                    ampDisc.setLayoutParams(params);
                    int drawableId = amp >= THRESHOLD ? R.drawable.red_circle : R.drawable.black_circle;
                    ampDisc.setBackground(ResourcesCompat.getDrawable(getResources(), drawableId, null));
                    taskHandler.postDelayed(this, POLL_DELAY);

                    if (amp >= THRESHOLD && SystemClock.elapsedRealtime() - triggerTime > COOLDOWN) {
                        triggerTime = SystemClock.elapsedRealtime();
                        toggleChronometer();
                    }

                }
            };
        }
        return pollTask;
    }


}
