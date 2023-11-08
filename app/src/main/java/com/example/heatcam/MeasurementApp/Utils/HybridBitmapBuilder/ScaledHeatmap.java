package com.example.heatcam.MeasurementApp.Utils.HybridBitmapBuilder;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import com.example.heatcam.MeasurementApp.ThermalCamera.SerialListeners.LeptonCamera;

public class ScaledHeatmap {

    public static int[][] scaledTempFrame;
    public static Bitmap heatmap;
    public static Bitmap scaleHeatmap(int minRaw, int maxRaw, int minFilterKelvin, int maxFilterKelvin, int[][] rawFrame){
        int height = rawFrame.length;
        int width = rawFrame[rawFrame.length-1].length;

        int[][] tempFrame = Interpolate.scale2(rawFrame, height, width, HybridImageOptions.getScaledHeight()*HybridImageOptions.resolutionMultiplier, HybridImageOptions.getScaledWidth()*HybridImageOptions.resolutionMultiplier);
        scaledTempFrame = reverseArray(tempFrame);
        Bitmap bMap = convertTo8bit(minRaw, maxRaw, minFilterKelvin, maxFilterKelvin, tempFrame.length, tempFrame[tempFrame.length-1].length, tempFrame);
        Matrix m = new Matrix();
        //m.postRotate(180);
        heatmap = Bitmap.createBitmap(bMap, 0,0, bMap.getWidth(), bMap.getHeight(), m, false);
        return heatmap;
    }

    private static Bitmap convertTo8bit(int min, int max, int minFilter, int maxFilter, int height, int width, int[][] frame) {
        int pix;
        int ind = 0;
        int[] colors = new int[height * width];
        for(int i = 0; i < height; i++) {
            for(int j = 0; j < width; j++) {
                int pixelKelvin = frame[i][j];
                if(pixelKelvin > maxFilter) {
                    pix = 255;
                } else if (pixelKelvin < minFilter) {
                    pix = 0;
                } else {
                    pix = ((pixelKelvin-min) * 255 )/ (max-min);
                    if(pix > 255) pix = 255;
                    else if(pix < 0) pix = 0;
                }

                colors[ind++] = LeptonCamera.getColorTable().elementAt(pix);
            }
        }
        return Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888);
    }

    /*Interpolaus tekee ylösalaisen peilikuvan, korjataan se toistaiseksi tässä*/
    private static int[][] reverseArray(int[][] tempFrame){
        int temp;
        for (int y = 0; y < tempFrame.length; y++) {
            for (int x = 0; x < tempFrame[y].length/2; x++) {
                temp = tempFrame[y][x];
                tempFrame[y][x] = tempFrame[y][tempFrame[y].length - 1 - x];
                tempFrame[y][tempFrame[y].length - 1 - x] = temp;
            }
        }
        for (int y = 0; y < tempFrame.length / 2; y++) {
            for (int x = 0; x < tempFrame[y].length; x++) {
                temp = tempFrame[y][x];
                tempFrame[y][x] = tempFrame[tempFrame.length - 1 - y][x];
                tempFrame[tempFrame.length - 1 -y][x] = temp;
            }
        }
        return tempFrame;
    }
}