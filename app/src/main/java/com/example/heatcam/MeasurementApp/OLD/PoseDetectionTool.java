package com.example.heatcam.MeasurementApp.OLD;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import androidx.camera.core.ImageProxy;

import com.example.heatcam.MeasurementApp.OLD.LiveCameraActivity;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseDetectorOptions;
import com.google.mlkit.vision.pose.PoseLandmark;

import java.util.List;

public class PoseDetectionTool {

    PoseDetectorOptions options =
            new PoseDetectorOptions.Builder()
                    .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                    .setPerformanceMode(PoseDetectorOptions.PERFORMANCE_MODE_FAST)
                    .build();
    PoseDetector poseDetector;

    LiveCameraActivity a;


    public PoseDetectionTool (LiveCameraActivity a) {
        poseDetector = PoseDetection.getClient(options);
        this.a = a;
    }

    public void processImage(InputImage image, ImageProxy imageProxy) {

        Task<Pose> result =
                poseDetector.process(image)
                        .addOnSuccessListener(
                                pose -> {
                                    List<PoseLandmark> landmarks = pose.getAllPoseLandmarks();
                                    if (!landmarks.isEmpty()) {
                                        PoseLandmark rightWrist = landmarks.get(15);
                                        PoseLandmark rightElbow = landmarks.get(13);
                                        PoseLandmark rightShoulder = landmarks.get(11);
                                        PoseLandmark nose = landmarks.get(0);
                                        PoseLandmark rIndex = landmarks.get(19);

                                        boolean handUp = nose.getPosition().y > rIndex.getPosition().y;

                                        Canvas canvas = new Canvas(image.getBitmapInternal());
                                        Paint paint = new Paint();
                                        paint.setColor(Color.GREEN);
                                        paint.setStrokeWidth(20);
                                        canvas.drawLine(rightShoulder.getPosition().x, rightShoulder.getPosition().y,
                                                rightElbow.getPosition().x, rightElbow.getPosition().y, paint);
                                        canvas.drawLine(rightElbow.getPosition().x, rightElbow.getPosition().y,
                                                rightWrist.getPosition().x, rightWrist.getPosition().y, paint);
                                        canvas.drawCircle(nose.getPosition().x, nose.getPosition().y, 10, paint);
                                        canvas.drawCircle(rIndex.getPosition().x, rIndex.getPosition().y, 10, paint);
                                        a.updateText(handUp);
                                    }
                                    a.drawImage(image.getBitmapInternal());
                                    imageProxy.close();

                                })
                        .addOnFailureListener(
                                e -> {
                                    // Task failed with an exception
                                    // ...
                                    imageProxy.close();
                                });
    }

}
