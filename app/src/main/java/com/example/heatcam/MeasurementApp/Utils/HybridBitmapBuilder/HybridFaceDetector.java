package com.example.heatcam.MeasurementApp.Utils.HybridBitmapBuilder;

import androidx.camera.core.ImageProxy;

import com.example.heatcam.MeasurementApp.Utils.HybridBitmapBuilder.HybridBitmapBuilder;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;

class HybridFaceDetector {

    FaceDetectorOptions options =
            new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setMinFaceSize(0.35f)
                    .enableTracking()
                    .build();

    FaceDetector faceDetector;
    HybridBitmapBuilder imagebuilder;

    private volatile boolean isProcessing = false;

    public HybridFaceDetector(HybridBitmapBuilder imagebuilder){
        faceDetector = FaceDetection.getClient(options);
        this.imagebuilder = imagebuilder;
    }

    public void processImage(InputImage image, ImageProxy imageProxy) {

        isProcessing = true;

        Task<List<Face>> result =
                faceDetector.process(image)
                        .addOnSuccessListener(
                                faces -> {
                                    //faces.sort(((a, b) -> Integer.compare(a.getBoundingBox().width(), b.getBoundingBox().width())));
                                    if (faces.size() > 0)
                                        imagebuilder.updateDetectedFace(faces.get(0));
                                    else
                                        imagebuilder.updateDetectedFace(null);

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