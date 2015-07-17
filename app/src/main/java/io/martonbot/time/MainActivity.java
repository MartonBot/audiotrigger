package io.martonbot.time;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;


public class MainActivity extends Activity {

    private Chronometer chronometer;
    private Button resetButton;
    private boolean isRunning = false;
    private long elapsedTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chronometer = (Chronometer) findViewById(R.id.chronometer);
        resetButton = (Button) findViewById(R.id.reset_button);

        chronometer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRunning) {
                    stop();
                }
                else {
                    start();
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

    private void start() {
        chronometer.setBase(SystemClock.elapsedRealtime() - elapsedTime);
        chronometer.start();
        isRunning = true;
    }

    private void stop() {
        elapsedTime = SystemClock.elapsedRealtime() - chronometer.getBase();
        chronometer.stop();
        isRunning = false;
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
        }
        else {
            resetButton.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onPause() {
        // stop Handler messages on pause
        chronometer.stop();
        super.onPause();
    }
}
