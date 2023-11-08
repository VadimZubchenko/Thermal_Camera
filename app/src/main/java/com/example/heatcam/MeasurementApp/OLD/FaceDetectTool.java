package com.example.heatcam.MeasurementApp.OLD;

import android.graphics.PointF;

import androidx.camera.core.ImageProxy;

import com.example.heatcam.MeasurementApp.OLD.User_result;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.List;

public class FaceDetectTool {

    FaceDetectorOptions options =
            new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setMinFaceSize(0.35f)
                    .enableTracking()
                    .build();

    FaceDetector faceDetector;
    User_result userResult;

    private volatile boolean isProcessing = false;

    public FaceDetectTool(User_result userResult){
        faceDetector = FaceDetection.getClient(options);
        this.userResult = userResult;
    }

    public void processImage(InputImage image, ImageProxy imageProxy) {

        isProcessing = true;

        Task<List<Face>> result =
                faceDetector.process(image)
                        .addOnSuccessListener(
                                faces -> {
                                    // Task completed successfully
                                    // [START_EXCLUDE]
                                    // [START get_face_info]
                                    if (faces.size() > 0) {
                                        for (Face face : faces) {

                                            int id = face.getTrackingId();
                                            PointF leftEyeP = face.getLandmark(FaceLandmark.LEFT_EYE).getPosition();
                                            PointF rightEyeP = face.getLandmark(FaceLandmark.RIGHT_EYE).getPosition();

                                           //float faceDist = userResult.calculateFaceDistance(leftEyeP, rightEyeP);

                                           userResult.updateDetectedFace(face);

                                        }
                                    } else {

                                    }
                                    // [END get_face_info]
                                    // [END_EXCLUDE]
                                })
                        .addOnCompleteListener(res -> {
                            imageProxy.close();
                            isProcessing = false;
                        })
                        .addOnFailureListener(
                                e -> {

                                    // Task failed with an exception
                                    // ...
                                    System.out.println("FAILURE");
                                    System.out.println(e.getMessage());
                                    e.printStackTrace();
                                    imageProxy.close();
                                    isProcessing = false;
                                });
    }

    public boolean isProcessing() {
        return isProcessing;
    }
}
