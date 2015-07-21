package io.martonbot.audiotrigger;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.ScaleAnimation;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;


public class CalibrateAudioActivity extends Activity {

    private static final long POLL_DELAY = 200;

    private View ampBar;
    private TextView ampText;
    private TextView ampLogText;

    private AudioMonitor monitor;

    private Runnable pollTask;
    private Handler taskHandler;

    private ScaleAnimation anim;
    private float currentScale = 1f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibrate_audio);

        monitor = new AudioMonitor();
        taskHandler = new Handler();

        ampBar = findViewById(R.id.amp_bar);
        ampText = (TextView) findViewById(R.id.amp_text);
        ampLogText = (TextView) findViewById(R.id.amp_log_text);

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
            Toast errorToast = Toast.makeText(CalibrateAudioActivity.this, "Audio monitoring is not available", Toast.LENGTH_SHORT);
            errorToast.show();
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

                    int amp = monitor.getMaxAmplitude();
                    int ampLog = monitor.getLogMaxAmplitude();

                    anim = new ScaleAnimation(currentScale, ampLog / 10f, 1f, 1f);
                    anim.setDuration((long) (POLL_DELAY * .75));
                    anim.setFillEnabled(true);
                    anim.setFillAfter(true);
                    currentScale = ampLog / 10f;
                    ampBar.startAnimation(anim);

                    ampText.setText(String.valueOf(amp));
                    ampLogText.setText(String.valueOf(ampLog));

                    taskHandler.postDelayed(this, POLL_DELAY);
                }
            };
        }
        return pollTask;
    }

}
