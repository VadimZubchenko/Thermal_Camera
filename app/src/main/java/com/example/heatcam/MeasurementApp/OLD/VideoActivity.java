package com.example.heatcam.MeasurementApp.OLD;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.annotation.Nullable;

import com.example.heatcam.R;

public class VideoActivity extends Activity {

    private VideoView videoView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.old_activity_video);
        videoView = (VideoView) findViewById(R.id.videoView);
        videoView.setMediaController(new MediaController(this));

        // video looppaa
        videoView.setOnCompletionListener(mp -> videoView.start());
    }

    @Override
    protected void onResume() {
        super.onResume();
        //String path = "android.resource://" + getPackageName() + "/" + R.raw.test;
        //videoView.setVideoURI(Uri.parse(path));
        videoView.start();
    }
}
