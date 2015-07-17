package io.martonbot.time;

import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;

public class AudioMonitor {

    private MediaRecorder mediaRecorder;

    public void startMonitoring() throws IOException {

        if (mediaRecorder == null) {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            // TODO consider changing the output format
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            // TODO consider changing the encoder
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            // TODO consider setting a different bit rate and sampling rate
            mediaRecorder.setOutputFile("dev/null");
            mediaRecorder.prepare();
            mediaRecorder.start();
        }

    }

    public void stopMonitoring() {
        Log.i("io.martonbot.time", "stopMonitoring 1");
        if (mediaRecorder != null) {
            Log.i("io.martonbot.time", "stopMonitoring 2");
            try {
                mediaRecorder.stop();
            }
            catch (Exception e) {
                Log.w("io.martonbot.time", "exception on media recorder stop", e);
            }

            Log.i("io.martonbot.time", "stopMonitoring 3");
            mediaRecorder.reset();
            Log.i("io.martonbot.time", "stopMonitoring 4");
            mediaRecorder.release();
            Log.i("io.martonbot.time", "stopMonitoring 5");
        }
    }

    public int getMaxAmplitude() {
        if (mediaRecorder != null) {
            return mediaRecorder.getMaxAmplitude();
        }
        return 0;
    }



}
