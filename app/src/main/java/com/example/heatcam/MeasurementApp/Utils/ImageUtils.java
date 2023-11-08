package com.example.heatcam.MeasurementApp.Utils;

import android.graphics.Bitmap;

import java.util.Vector;

public final class ImageUtils {

    public static final int LOWEST_COLOR = -10197879;
    public static final int HIGHEST_COLOR = -65536;

    public static Vector<Integer> createColorTable() {
        Vector<Integer> table = new Vector<>();
        double a, b;
        int R, G, B;
        for(int i = 0; i < 256; i++){
            a = i * 0.01236846501;
            b = Math.cos(a - 1);
            R = (int)(Math.pow(2, Math.sin(a - 1.6)) * 200);
            G = (int) (Math.atan(a) * b * 155 + 100.0);
            B = (int) (b * 255);

            R   = Math.min(R, 255);
            G = Math.min(G, 255);
            B  = Math.min(B, 255);
            R  = Math.max(R, 0);
            G = Math.max(G, 0);
            B = Math.max(B, 0);
            table.add(0xff << 24 | (R & 0xff)  << 16 | (G & 0xff) << 8 | (B & 0xff));
        }
        return table;
    }

    // https://stackoverflow.com/a/18784216
    public static Bitmap bitmapFromArray(int[][] pixels2d){
        int imgHeight = pixels2d.length;
        int imgWidth = pixels2d[0].length;
        int[] pixels = new int[imgWidth * imgHeight];
        int pixelsIndex = 0;
        for (int[] ints : pixels2d) {
            for (int j = 0; j < imgWidth; j++) {
                pixels[pixelsIndex] = ints[j];
                pixelsIndex++;
            }
        }
        return Bitmap.createBitmap(pixels, imgWidth, imgHeight, Bitmap.Config.ARGB_8888);
    }
}
