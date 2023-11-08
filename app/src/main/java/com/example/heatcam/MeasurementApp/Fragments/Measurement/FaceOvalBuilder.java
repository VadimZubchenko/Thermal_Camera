package com.example.heatcam.MeasurementApp.Fragments.Measurement;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.Size;

public class FaceOvalBuilder {

    private Paint paint;
    private Size overlaySize;
    private Size ovalSize;
    private PointF ovalPosition;
    private Bitmap.Config bMapConfig = Bitmap.Config.ARGB_8888;

    private FaceOvalBuilder() {
    }

    public static FaceOvalBuilder create() {
        return new FaceOvalBuilder();
    }

    public FaceOvalBuilder setOverlaySize(Size s) {
        this.overlaySize = s;
        return this;
    }

    public FaceOvalBuilder bitmapConfig(Bitmap.Config config) {
        this.bMapConfig = config;
        return this;
    }

    public FaceOvalBuilder setPaint(Paint p) {
        this.paint = p;
        return this;
    }

    public FaceOvalBuilder setOvalSize(Size s) {
        this.ovalSize = s;
        return this;
    }

    public FaceOvalBuilder setOvalPosition(PointF position) {
        this.ovalPosition = position;
        return this;
    }

    public Bitmap build() {
        Bitmap bitmap = Bitmap
                .createBitmap(overlaySize.getWidth(), overlaySize.getHeight(), bMapConfig);
        Canvas canvas = new Canvas(bitmap);
        Rect rect = new Rect(0, 0, overlaySize.getWidth(), overlaySize.getHeight());
        canvas.drawRect(rect, paint);
        Paint transparent = paint;
        transparent.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawOval(
                ovalPosition.x - ovalSize.getWidth(),
                ovalPosition.y - overlaySize.getHeight()/2f,
                ovalPosition.x + ovalSize.getWidth(),
                ovalSize.getHeight(),
                transparent
        );

        return bitmap;
    }

}
