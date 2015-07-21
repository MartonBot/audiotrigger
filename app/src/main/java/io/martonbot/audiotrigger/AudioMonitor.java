package io.martonbot.audiotrigger;

import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;

public class AudioMonitor {

    private final static int AMP_FLOOR = 500;
    private final static int AMP_CEIL = 30000;
    private final static float CONST = 2.442f;

    private int maxAmplitude;

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
            mediaRecorder.setOutputFile("/dev/null");
            mediaRecorder.prepare();
            mediaRecorder.start();
        }

    }

    public void stopMonitoring() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (Exception e) {
                Log.e("io.martonbot.at", "Error while calling MediaRecorder.stop()", e);
            }
            try {
                mediaRecorder.reset();
            } catch (Exception e) {
                Log.e("io.martonbot.at", "Error while calling MediaRecorder.reset()", e);
            }
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    public int getMaxAmplitude() {
        if (mediaRecorder != null) {
            maxAmplitude = mediaRecorder.getMaxAmplitude();
            return maxAmplitude;
        }
        return AMP_FLOOR;
    }

    public int getLogMaxAmplitude() {
        return (int) (CONST * Math.log(maxAmplitude / ((float) AMP_FLOOR)));
    }


}
