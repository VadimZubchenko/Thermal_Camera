package com.example.heatcam.MeasurementApp.Fragments.Measurement;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PathMeasure;
import android.util.AttributeSet;
import android.view.View;

import androidx.preference.PreferenceManager;

import com.example.heatcam.R;

public class AnimatedOval extends View {

    private final int COLOR_RED = Color.rgb(255, 51,51);
    private final int COLOR_ORANGE = Color.rgb(255, 128, 51);
    private final int COLOR_YELLOW = Color.rgb(255, 218,51);
    private final int COLOR_GREEN = Color.rgb(76, 255, 51);
    private final int COLOR_WHITE_RED = Color.rgb(255, 154, 165);

    private Paint paint;
    private Path path;

    private int width = 1200;
    private int height = 1920;

    private PathMeasure pMeasure;
    private float length;

    private ObjectAnimator animator;

    private SharedPreferences sp;
    private float pWidth = 12;


    public AnimatedOval(Context context) {
        super(context);
        sp = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public AnimatedOval(Context context, AttributeSet attrs) {
        super(context, attrs);
        sp = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public AnimatedOval(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        sp = PreferenceManager.getDefaultSharedPreferences(context);
    }


    public void init(float paintWidth, boolean animate) {
        this.pWidth = paintWidth;
        // TODO: get these values from shared preferences like in createFaceOval()
        /*
        float x =  width / 2.0f;
        float w = (950/2.0f);
        float h = 1450.0f;
        float fromTop = 80.0f;
         */
        int ovalWidth = Integer.parseInt(sp.getString(getResources().getString(R.string.preference_oval_width), "750")) / 2;
        int ovalHeight = Integer.parseInt(sp.getString(getResources().getString(R.string.preference_oval_height), "1300"));
        int fromTop = Integer.parseInt(sp.getString(getResources().getString(R.string.preference_oval_pos_from_top), "200"));

        float ovalPosX = 600;
        float ovalPosY = 960 + fromTop;


        path = new Path();
        //path.addOval(x - w, fromTop, x + w, h, Path.Direction.CW);
        path.addOval(ovalPosX - ovalWidth,
                ovalPosY - height/2.0f,
                ovalPosX + ovalWidth,
                ovalHeight,
                Path.Direction.CW);

        paint = new Paint();
        paint.setColor(COLOR_RED);
        paint.setStrokeWidth(pWidth);
        paint.setStyle(Paint.Style.STROKE);

        pMeasure = new PathMeasure(path, false);
        length = pMeasure.getLength();


        if (animate) {
            animator = ObjectAnimator.ofFloat(AnimatedOval.this, "phase", 1.0f, 0.0f);
            animator.setDuration(4000);
            animator.start();
        }
    }

    public void setPhase(float phase) {
        // color changes from red to orange to yellow to green
        int color = Color.HSVToColor(new float[]{(float) (1 - phase) * 120f, 1f, 1f});
        paint.setColor(color);
        //paint.setPathEffect(createPathEffect(length, phase, 0.0f));
        invalidate();
    }

    private static PathEffect createPathEffect(float pathLength, float phase, float offset) {
        return new DashPathEffect(new float[] { pathLength, pathLength },
                Math.max(phase * pathLength, offset));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(path, paint);
    }

    public void stopAnimation() {
        if (animator != null) {
            animator.cancel();
        }
    }

    public void setPaintWidth (float pWidth) {
        this.pWidth = pWidth;
    }

}