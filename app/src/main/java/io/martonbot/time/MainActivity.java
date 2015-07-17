package io.martonbot.time;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Toast;

import java.io.IOException;


public class MainActivity extends Activity {

    private static final long POLL_DELAY = 4000;

    private Chronometer chronometer;
    private Button resetButton;

    private Runnable pollTask;
    private Handler taskHandler;
    private AudioMonitor monitor;

    private boolean isChronometerRunning = false;
    private long elapsedTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        monitor = new AudioMonitor();
        taskHandler = new Handler();

        chronometer = (Chronometer) findViewById(R.id.chronometer);
        resetButton = (Button) findViewById(R.id.reset_button);

        chronometer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isChronometerRunning) {
                    stopChronometer();
                } else {
                    startChronometer();
                }
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

        Log.i("io.martonbot.time", "onResume");

        // start monitoring
        try {
            monitor.startMonitoring();
            taskHandler.postDelayed(getPollTask(), POLL_DELAY);

        } catch (IOException e) {
            monitor.stopMonitoring();
            Toast errorToast = Toast.makeText(MainActivity.this, "Audio monitoring is not available", Toast.LENGTH_SHORT);
            errorToast.show();
        }

    }

    @Override
    protected void onPause() {

        Log.i("io.martonbot.time", "onPause 1");

        // stop monitoring
        monitor.stopMonitoring();

        Log.i("io.martonbot.time", "onPause  2");

        // cancel Handler callback
        taskHandler.removeCallbacks(getPollTask());

        Log.i("io.martonbot.time", "onPause 3");

        // stop chronometer updates on pause
        chronometer.stop();

        Log.i("io.martonbot.time", "onPause 4");

        super.onPause();
        Log.i("io.martonbot.time", "onPause 5");
    }

    @Override
    protected void onStop() {

        Log.i("io.martonbot.time", "onStop 1");
        super.onStop();
        Log.i("io.martonbot.time", "onStop 2");
    }

    private void startChronometer() {
        chronometer.setBase(SystemClock.elapsedRealtime() - elapsedTime);
        chronometer.start();
        isChronometerRunning = true;
    }

    private void stopChronometer() {
        elapsedTime = SystemClock.elapsedRealtime() - chronometer.getBase();
        chronometer.stop();
        isChronometerRunning = false;
        updateResetButton();
    }

    private void reset() {
        chronometer.setBase(SystemClock.elapsedRealtime());
        elapsedTime = 0;
        updateResetButton();
    }

    private void updateResetButton() {
        if (elapsedTime > 0) {
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
                    // TODO poll audio
                    Toast.makeText(MainActivity.this, "Polling...", Toast.LENGTH_SHORT).show();
                    taskHandler.postDelayed(this, POLL_DELAY);
                }
            };
        }
        return pollTask;
    }


}
