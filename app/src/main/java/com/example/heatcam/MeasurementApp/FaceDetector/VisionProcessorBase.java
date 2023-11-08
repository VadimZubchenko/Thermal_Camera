package com.example.heatcam.MeasurementApp.FaceDetector;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ImageProxy;

import com.example.heatcam.MeasurementApp.Utils.RenderScriptTools;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskExecutors;
import com.google.mlkit.vision.common.InputImage;

import java.nio.ByteBuffer;

/*
    Modified from https://github.com/googlesamples/mlkit/blob/master/android/vision-quickstart/app/src/main/java/com/google/mlkit/vision/demo/java/VisionProcessorBase.java
 */
public abstract class VisionProcessorBase<T> implements VisionImageProcessor {

    private static final String TAG = "VisionProcessorBase";
    protected static final String MANUAL_TESTING_LOG = "LogTagForTest";

    private final ScopedExecutor executor;

    // Whether this processor is already shut down
    private boolean isShutdown;

    // To keep the latest images and its metadata.
    @GuardedBy("this")
    private ByteBuffer latestImage;

    private RenderScriptTools rs;


    protected VisionProcessorBase(Context context) {
        executor = new ScopedExecutor(TaskExecutors.MAIN_THREAD);
        rs = new RenderScriptTools(context);
    }


    // -----------------Code for processing live preview frame from CameraX API-----------------------
    @SuppressLint("UnsafeExperimentalUsageError")
    public void processImageProxy(ImageProxy image) {
        if (isShutdown) {
            image.close();
            return;
        }

        // Bitmap bitmap = null;

        /*
        if (!PreferenceUtils.isCameraLiveViewportEnabled(graphicOverlay.getContext())) {
            bitmap = BitmapUtils.getBitmap(image);
        }

         */
        //bitmap = ImageUtils.getBitmap(image);
        Bitmap bitmap = rs.YUV_420_888_toRGB(image.getImage(), image.getImage().getWidth(), image.getImage().getHeight(), image.getImageInfo().getRotationDegrees());

        requestDetectInImage(
                InputImage.fromMediaImage(image.getImage(), image.getImageInfo().getRotationDegrees()),
                /* originalCameraImage= */ bitmap)
                // When the image is from CameraX analysis use case, must call image.close() on received
                // images when finished using them. Otherwise, new images may not be received or the camera
                // may stall.
                .addOnCompleteListener(results -> image.close());
    }

    // -----------------Common processing logic-------------------------------------------------------
    private Task<T> requestDetectInImage(
            final InputImage image,
            @Nullable final Bitmap originalCameraImage) {
        return detectInImage(image)
                .addOnSuccessListener(
                        executor,
                        results -> {


                            if (originalCameraImage != null) {
                                //graphicOverlay.add(new CameraImageGraphic(graphicOverlay, originalCameraImage));
                            }
                            /*
                            graphicOverlay.add(
                                    new InferenceInfoGraphic(
                                            graphicOverlay, currentLatencyMs, shouldShowFps ? framesPerSecond : null));
                            VisionProcessorBase.this.onSuccess(results, graphicOverlay);
                            graphicOverlay.postInvalidate();
                             */
                            VisionProcessorBase.this.onSuccess(results, originalCameraImage);
                        })
                .addOnFailureListener(
                        executor,
                        e -> {

                            String error = "Failed to process. Error: " + e.getLocalizedMessage();
                            Log.d(TAG, error);
                            e.printStackTrace();
                            VisionProcessorBase.this.onFailure(e);
                        });
    }


    public void stop() {
        executor.shutdown();
        isShutdown = true;
    }

    protected abstract Task<T> detectInImage(InputImage image);

    protected abstract void onSuccess(@NonNull T results, Bitmap originalCameraImage);

    protected abstract void onFailure(@NonNull Exception e);

}
