package com.example.heatcam.MeasurementApp.Fragments.Measurement;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.InitializationException;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.example.heatcam.MeasurementApp.Fragments.CameraListener;
import com.example.heatcam.MeasurementApp.Fragments.IntroFragment.IntroFragment;
import com.example.heatcam.MeasurementApp.Fragments.Result.ResultFragment;
import com.example.heatcam.MeasurementApp.FrontCamera.FrontCameraProperties;
import com.example.heatcam.MeasurementApp.ThermalCamera.SerialListeners.LowResolution16BitCamera;
import com.example.heatcam.MeasurementApp.ThermalCamera.SerialPort.SerialPortModel;
import com.example.heatcam.MeasurementApp.Utils.HybridBitmapBuilder.HybridBitmapBuilder;
import com.example.heatcam.MeasurementApp.Utils.HybridBitmapBuilder.HybridImageListener;
import com.example.heatcam.MeasurementApp.Utils.TiltAngleHandler;
import com.example.heatcam.R;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceLandmark;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MeasurementStartFragment extends Fragment implements CameraListener, HybridImageListener {

    private final String TAG = "MeasurementStartFragment";

    private int facePositionCheckCounter = 0;
    private final int checkLimit = 10;

    private float preferred_measure_distance;

    private Animation scanAnimation;
    private Button animBtn;
    private View scanBar;

    private boolean ready;
    private Rect naamarajat;

    private double userTemp = 0;
    private double avgUserTemp = 0;
    private List<Double> userTempList;

    private int laskuri = 0;

    private TextView txtDebug;

    SerialPortModel serialPortModel;

    private MeasurementAccessObject measurementAccessObject;
    private TiltAngleHandler angleHandler = new TiltAngleHandler();

    private ScheduledThreadPoolExecutor idleExecutor;

    private AnimatedOval animatedOval;
    private ImageView heatkuva;
    private TextView comecloserText;
    private HybridBitmapBuilder hbb;

    private SharedPreferences sharedPrefs;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferred_measure_distance = Float.parseFloat(PreferenceManager.getDefaultSharedPreferences(getContext())
                .getString(getString(R.string.preference_measure_distance), "300"));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.heatcam_measurement_start_layout, container, false);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(view.getContext());
        int timerDelay = Integer.parseInt(sharedPrefs.getString("PREFERENCE_TILT_CORRECTION_DELAY", "800"));
        angleHandler.setTimerDelay(timerDelay);
        // prevent app from dimming
        view.setKeepScreenOn(true);
        ConstraintLayout cl = (ConstraintLayout)view.findViewById(R.id.ConstraintLayout);
        cl.setBackgroundColor(Color.BLACK);
        hbb = new HybridBitmapBuilder(this, view);
        animBtn = view.findViewById(R.id.animBtn);
        scanBar = view.findViewById(R.id.scanBar);
        Bitmap ovalOverlay = createFaceOval(sharedPrefs);
        ((ImageView) view.findViewById(R.id.start_layout_oval_overlay)).setImageBitmap(ovalOverlay);

        comecloserText = view.findViewById(R.id.comecloser);
        comecloserText.setVisibility(view.INVISIBLE);
        heatkuva = view.findViewById(R.id.heatkuva);
        animatedOval = view.findViewById(R.id.animatedOval);


        measurementAccessObject = new MeasurementAccessObject();

        txtDebug = view.findViewById(R.id.txtDebugit);

        serialPortModel = SerialPortModel.getInstance();
        serialPortModel.setCamListener(this);

        scanAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.scan_animation);

        scanAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
    public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                scanBar.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        animBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // can add here some functionality for debugging
                changeToResultLayout();
                /*
                scanBar.setVisibility(View.VISIBLE);
                scanBar.startAnimation(scanAnimation);

                changeToResultLayout();

                animatedOval.init();
                animatedOval.setVisibility(View.VISIBLE);
                 */
            }
        });
        ProgressBar bar = view.findViewById(R.id.face_check_prog);
        bar.setMax(checkLimit);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        angleHandler.stop();
        stopIdleExecutor();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    private Bitmap createFaceOval(SharedPreferences sp) {
        Paint paint = new Paint();
        paint.setShader(new LinearGradient(0, 0, 600, 960, Color.parseColor("#871019FF"), Color.parseColor("#85FB3A1F"), Shader.TileMode.MIRROR));
        paint.setShader(new LinearGradient(600, 960, 1200, 1920, Color.parseColor("#85FB3A1F"), Color.parseColor("#871019FF"), Shader.TileMode.MIRROR));
        paint.setAlpha(230);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);

        int ovalWidth = Integer.parseInt(sp.getString(getString(R.string.preference_oval_width), "750")) / 2;
        int ovalHeight = Integer.parseInt(sp.getString(getString(R.string.preference_oval_height), "1300"));
        int fromTop = Integer.parseInt(sp.getString(getString(R.string.preference_oval_pos_from_top), "200"));

        return FaceOvalBuilder.create()
                .setOverlaySize(new Size(1200, 1920))
                .setPaint(paint)
                .setOvalSize(new Size(ovalWidth, ovalHeight))
                .setOvalPosition(new PointF(600, 960 + fromTop))
                .build();
    }

    public float facePositionCheck(Face face, int imgWidth, int imgHeight) {

        float middleX = imgWidth / 2f;
        float middleY =  imgHeight / 2.2f; //2.146f; //imgHeight / 2.05f; // joutuu sit säätää tabletille tää ja deviation
        float maxDeviationX = 25f; // eli max +- pixel heitto sijaintiin
        float maxDeviationY = 15f;
        PointF noseP = face.getLandmark(FaceLandmark.NOSE_BASE).getPosition();
        PointF leftEyeP = face.getLandmark(FaceLandmark.LEFT_EYE).getPosition();
        PointF rightEyeP = face.getLandmark(FaceLandmark.RIGHT_EYE).getPosition();

        float dist = 0;
        try {
            dist = FrontCameraProperties.getProperties().getDistance(new Size(imgWidth, imgHeight), leftEyeP, rightEyeP);
        } catch (InitializationException e) {
            e.printStackTrace();
        }

        //System.out.println(imgWidth+" "+imgWidth + " dist");

        boolean xOK = noseP.x > (middleX - maxDeviationX) && noseP.x < (middleX + maxDeviationX);
        boolean yOK = noseP.y > (middleY - maxDeviationY) && noseP.y < (middleY + maxDeviationY);

        int offset = 50;
        float et = dist;
        if(getActivity() != null)
            getActivity().runOnUiThread(() -> txtDebug.setText(et+""));
        boolean distanceOK = dist < preferred_measure_distance + offset && dist > preferred_measure_distance - offset;
        if (xOK && yOK && distanceOK) {
            facePositionCheckCounter++;
            startScanAnimation();
            ready = true;
            comecloserText.setText(R.string.start_layout_hold_position);
        } else {
            comecloserText.setText(R.string.start_layout_come_closer);
            comecloserText.setVisibility(View.INVISIBLE);
            animatedOval.stopAnimation();
            animatedOval.setVisibility(View.INVISIBLE);
            scanBar.clearAnimation();
            scanBar.setVisibility(View.INVISIBLE);
            indicateOvalDistance(dist); // makes oval's borders thicker if user is far away
            ready = false;
            laskuri = 0;
            userTempList = null;

            userTemp = 0;
            facePositionCheckCounter--;
            if (facePositionCheckCounter < 0) facePositionCheckCounter = 0;
        }

        // angle correction if target is in specified distance and y position is not OK.
        if(dist < 500 && !yOK) {
            synchronized (this) {
                int newAngle = angleHandler.newCorrection(dist, imgHeight, middleY, noseP.y);
                System.out.println("tilted "+ newAngle);
                if (newAngle != -1) serialPortModel.changeTiltAngle(newAngle);
            }
        }

        updateProgress();
        return et;
    }

    private void indicateOvalDistance(float dist) {
        if (dist > preferred_measure_distance && dist < 600) {
            float pWidth = ((60 - 12) * (dist - 250) / (600 - 250)) + 12;
            animatedOval.init(pWidth, false);
            comecloserText.setVisibility(View.VISIBLE);
            animatedOval.setVisibility(View.VISIBLE);
            animatedOval.invalidate();
        }
    }

    private void updateProgress() {
        try {
            ProgressBar bar = getActivity().findViewById(R.id.face_check_prog);
            bar.setProgress(facePositionCheckCounter);
        } catch (Exception ignored) {

        }
    }


    public void faceDetected(Face face) {
        int width = 144;
        int height = 144;
        Bitmap live = hbb.getLiveMap();
        if(live != null){
            width = live.getWidth(); height = live.getHeight();
        }
        float dist = facePositionCheck(face, width, height);
        hbb.setDistance(dist);
        // getActivity().runOnUiThread(() -> txtDebug.setText(String.valueOf(laskuri)));
        if (ready) {
            if (userTempList == null) {
                userTempList = new ArrayList<>();
            }
            if (laskuri < 35) {
                userTempList.add(hbb.getHighestFaceTemperature());

                if (hbb.getHighestFaceTemperature() > userTemp) {
                    userTemp = hbb.getHighestFaceTemperature();
                }
                laskuri++;
            } else {
                ready = false;
                laskuri = 0;
                saveMeasurementToJson();
                changeToResultLayout();
            }
        }
        stopIdleExecutor();
    }

    public void faceNotDetected() {
        scanBar.clearAnimation();
        animatedOval.stopAnimation();
        animatedOval.setVisibility(View.INVISIBLE);
        comecloserText.setVisibility(View.INVISIBLE);
        startIdleExecutor();
    }


    private void startIdleExecutor() {
        if (idleExecutor == null) {
            idleExecutor = new ScheduledThreadPoolExecutor(1);
        }
        // schedule the layout change if there isn't already a task going for it
        if (idleExecutor.getTaskCount() == 0) {
            int seconds = Integer.parseInt(sharedPrefs.getString("PREFERENCE_SECONDS_TO_SWITCH_INTRO_MEASUREMENT", "10"));
            idleExecutor.schedule(new Runnable() {
                @Override
                public void run() {
                    changeLayout();
                }
            }, seconds, TimeUnit.SECONDS);
        }
    }

    private void stopIdleExecutor() {
        if (idleExecutor != null) {
            idleExecutor.shutdownNow();
            idleExecutor = null;
        }
    }

    private void changeLayout() {
        hbb.setMsfNull();
        Fragment f = new IntroFragment();
        getActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.animator.slide_in_left, R.animator.slide_in_right, 0, 0)
                .replace(R.id.fragmentCamera, f, "menu").commit();
    }

    private void changeToResultLayout() {
        hbb.setMsfNull();

        Fragment f = new ResultFragment();
        Bundle args = new Bundle();
        args.putDouble("user_temp", userTemp);
        args.putDouble("avg_user_temp", avgUserTemp);
        f.setArguments(args);

        getActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.animator.slide_in_left, R.animator.slide_in_right, 0, 0)
                .replace(R.id.fragmentCamera, f, "default").commit();
    }

    private void startScanAnimation() {

        if (!ready) {
            animatedOval.init(12, true);
            animatedOval.setVisibility(View.VISIBLE);
            comecloserText.setVisibility(View.VISIBLE);
            scanBar.setVisibility(View.VISIBLE);
            scanBar.bringToFront();
            scanBar.invalidate();
            scanBar.startAnimation(scanAnimation);
        }
    }

    private void saveMeasurementToJson() {
        if (userTempList != null) {
            avgUserTemp = userTempList.stream()
                    .mapToDouble(v -> v)
                    .average()
                    .getAsDouble();
        }

        try {
            JSONObject obj = measurementAccessObject.newEntry(avgUserTemp, new Date());
            measurementAccessObject.write(getContext(), obj, true);
        } catch (JSONException | IOException e) {
            Log.e(TAG, "Something went wrong while saving measurement to JSON " + e);
            e.printStackTrace();
        }
    }

    @Override
    public void setConnectingImage() {
    }


    @Override
    public void setNoFeedImage() {
    }

    @Override
    public void updateImage(Bitmap image) {
        //System.out.println("MeasurementStartFragment heatmap update "+hbb.getHighestFaceTemperature());
        sendHeatmap(image);
    }

    @Override

    public void updateText(String text) {
    }

    @Override
    public void disconnect() {
    }

    @Override
    public void maxCelsiusValue(double max) {

    }

    @Override
    public void minCelsiusValue(double min) {
    }

    @Override
    public void detectFace(Bitmap image) {
    }

    @Override
    public void writeToFile(byte[] data) {
    }

    public void updateData(LowResolution16BitCamera.TelemetryData data) {
        if (angleHandler.getTargetTiltAngle() == 0) {
            angleHandler.setTargetTiltAngle(data.tiltAngle);
            angleHandler.setIsAtTargetAngle(true);
        }
        angleHandler.setCurrentTiltAngle(data.tiltAngle);
    }


    @Override
    public void onNewHybridImage(Bitmap image) {
        if(getActivity() != null)
            getActivity().runOnUiThread(() -> heatkuva.setImageBitmap(image));
    }

    @Override
    public void sendHeatmap(Bitmap image) {
        hbb.setHeatmap(image);
    }
}
