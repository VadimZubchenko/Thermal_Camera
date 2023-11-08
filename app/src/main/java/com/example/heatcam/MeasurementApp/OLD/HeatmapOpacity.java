package com.example.heatcam.MeasurementApp.OLD;

import android.graphics.Bitmap;
import android.graphics.Color;

public class HeatmapOpacity {
    public Bitmap setOpacity(Bitmap image){
        Bitmap O = Bitmap.createBitmap(image.getWidth(),image.getHeight(), image.getConfig());
        for(int i=0; i<image.getWidth(); i++){
            for(int j=0; j<image.getHeight(); j++){
                int pixel = image.getPixel(i, j);
                int r = Color.red(pixel), g = Color.green(pixel), b = Color.blue(pixel);
                if (b < 200)
                {
                    O.setPixel(i, j, Color.argb(20, i, j, pixel));
                }
            }
        }
        return O;
    }
}
