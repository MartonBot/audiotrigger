package io.martonbot.audiotrigger;

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
            return mediaRecorder.getMaxAmplitude();
        }
        return 0;
    }

    public int getLogMaxAmplitude() {
        return (int) Math.round(Math.log(getMaxAmplitude() + 1));
    }


}
