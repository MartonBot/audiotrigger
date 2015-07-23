package io.martonbot.audiotrigger;

import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;

public class AudioMonitor {

    private final static int AMP_FLOOR = 50;
    private final static int AMP_CEIL = 30000;
    private final static double CONST = 10 / Math.log(AMP_CEIL / AMP_FLOOR);

    private MediaRecorder mediaRecorder;

    public boolean startMonitoring() {

        if (mediaRecorder == null) {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            // TODO consider changing the output format
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            // TODO consider changing the encoder
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            // TODO consider setting a different bit rate and sampling rate
            mediaRecorder.setOutputFile("/dev/null");
            try {
                mediaRecorder.prepare();
            } catch (IOException e) {
                stopMonitoring();
                return false;
            }
            mediaRecorder.start();
        }
        return true;
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

    public int getLogMaxAmplitude() {
        if (mediaRecorder != null) {
            int maxAmplitude = Math.max(mediaRecorder.getMaxAmplitude(), AMP_FLOOR);
            return (int) (CONST * Math.log(maxAmplitude / ((double) AMP_FLOOR)));
        }
        return AMP_FLOOR;
    }

}
