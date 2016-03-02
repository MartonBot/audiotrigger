package io.martonbot.audiotrigger;

import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;

public class AudioMonitor {

    private AudioConfig config = AudioConfig.NORMAL;

    private MediaRecorder mediaRecorder;

    public boolean startMonitoring() {

        boolean ret = true;

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
                ret = false;
            }
            try {
                mediaRecorder.start();
            } catch (RuntimeException e) {
                stopMonitoring();
                ret = false;
            }
        }
        return ret;
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
            int maxAmplitude = Math.max(mediaRecorder.getMaxAmplitude(), config.ampFloor);
            return (int) (config.logRatio * Math.log(maxAmplitude / ((double) config.ampFloor)));
        }
        return 0;
    }

    public void setAudioConfig(AudioConfig c) {
        this.config = c;
    }

    public enum AudioConfig {

        NORMAL (100, 30000),
        NOISY (500, 40000);

        AudioConfig(int floor, int ceil) {
            this.ampFloor = floor;
            this.ampCeiling = ceil;
            this.logRatio = 10 / Math.log(ampCeiling / ((double) ampFloor));
        }

        private final int ampFloor;
        private final int ampCeiling;
        private final double logRatio;

    }

    public AudioMonitor(AudioConfig environment) {
        this.config = environment;
    }
}
