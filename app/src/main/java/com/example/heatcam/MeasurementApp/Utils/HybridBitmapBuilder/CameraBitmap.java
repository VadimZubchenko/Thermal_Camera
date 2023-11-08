package com.example.heatcam.MeasurementApp.Utils.HybridBitmapBuilder;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.Image;
import android.util.Size;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;

import com.example.heatcam.MeasurementApp.Utils.HybridBitmapBuilder.HybridBitmapBuilder;
import com.example.heatcam.MeasurementApp.Utils.RenderScriptTools;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CameraBitmap {
    private Executor executor = Executors.newSingleThreadExecutor();
    private final int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};

    private View view;
    private RenderScriptTools rs;
    private LifecycleOwner owner;
    private HybridBitmapBuilder listener;

    public CameraBitmap(LifecycleOwner owner, HybridBitmapBuilder listener, View view){
        this.view = view;
        this.owner = owner;
        this.listener = listener;

        rs = new RenderScriptTools(view.getContext());
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ((Fragment) owner).getActivity().requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void startCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(view.getContext());
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
            }
        }, ContextCompat.getMainExecutor(view.getContext()));
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        //Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size( 1, 1))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

        imageAnalysis.setAnalyzer(executor, new MyAnalyzer());
        //preview.setSurfaceProvider(liveFeed.createSurfaceProvider());
        //cameraProvider.bindToLifecycle(owner, cameraSelector, imageAnalysis, preview);
        Camera camera = cameraProvider.bindToLifecycle(owner, cameraSelector, imageAnalysis);
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(view.getContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private class MyAnalyzer implements ImageAnalysis.Analyzer {
        @Override
        public void analyze(@NonNull ImageProxy image) {
            int rotationDegrees = image.getImageInfo().getRotationDegrees();
            @SuppressLint("UnsafeExperimentalUsageError") Image img = image.getImage();
            if (img != null) {
                Bitmap bMap = rs.YUV_420_888_toRGB(img, img.getWidth(), img.getHeight(), rotationDegrees);
                Bitmap bOutput;
                Matrix matrix = new Matrix();
                matrix.preScale(-1.0f, 1.0f);
                bOutput = Bitmap.createBitmap(bMap, 0, 0, bMap.getWidth(), bMap.getHeight(), matrix, true);
                //InputImage inputImage = InputImage.fromBitmap(bMap, 0);
                listener.onNewBitmap(bOutput, image);
                //updateLiveImage(bOutput);
                image.close();
                //processImage(inputImage, image); // face detection
            }
        }
    }
}
