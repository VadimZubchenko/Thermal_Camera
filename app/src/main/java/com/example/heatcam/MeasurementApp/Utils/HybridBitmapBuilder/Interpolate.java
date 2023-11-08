package com.example.heatcam.MeasurementApp.Utils.HybridBitmapBuilder;

/*https://rosettacode.org/wiki/Bilinear_interpolation#Java*/

import com.example.heatcam.MeasurementApp.ThermalCamera.SerialListeners.LeptonCamera;

public class Interpolate {
    public Interpolate(){}
    /* gets the 'n'th byte of a 4-byte integer */
    private static int get(int self, int n) {
        return self;
    }

    private static float lerp(float s, float e, float t) {
        return s + (e - s) * t;
    }

    private static float blerp(final Float c00, float c10, float c01, float c11, float tx, float ty) {
        return lerp(lerp(c00, c10, tx), lerp(c01, c11, tx), ty);
    }

    public static int[][] scale(int[][] kuva, int resoX, int resoY, float scaleX, float scaleY) {
        int newWidth = (int) (resoX * scaleX);
        int newHeight = (int) (resoY * scaleY);
        int[][] uusikuva = new int[newWidth][newHeight];
        //BufferedImage newImage = new BufferedImage(newWidth, newHeight, self.getType());
        for (int x = 0; x < newWidth; ++x) {
            for (int y = 0; y < newHeight; ++y) {
                float gx = ((float) x) / newWidth * (resoX - 1);
                float gy = ((float) y) / newHeight * (resoY - 1);
                int gxi = (int) gx;
                int gyi = (int) gy;
                int rgb = 0;
                int c00 = kuva[gxi][ gyi];
                int c10 = kuva[gxi + 1][ gyi];
                int c01 = kuva[gxi][ gyi + 1];
                int c11 = kuva[gxi + 1][ gyi + 1];
                for (int i = 0; i <= 2; ++i) {
                    float b00 = get(c00, i);
                    float b10 = get(c10, i);
                    float b01 = get(c01, i);
                    float b11 = get(c11, i);
                    int ble = ((int) blerp(b00, b10, b01, b11, gx - gxi, gy - gyi)) ;
                    rgb = rgb | ble;
                }
                uusikuva[x][y] = rgb;
                //newImage.setRGB(x, y, rgb);
            }
        }
        return uusikuva;
    }
    public static int[][] scale2(int[][] kuva, int currentResX, int currentResY, int newResX, float newResY) {
        int newWidth = (int) (newResX);
        int newHeight = (int) (newResY);
        int[][] uusikuva = new int[newWidth][newHeight];
        //BufferedImage newImage = new BufferedImage(newWidth, newHeight, self.getType());
        for (int x = 0; x < newWidth; ++x) {
            for (int y = 0; y < newHeight; ++y) {
                float gx = ((float) x) / newWidth * (currentResX - 1);
                float gy = ((float) y) / newHeight * (currentResY - 1);
                int gxi = (int) gx;
                int gyi = (int) gy;
                int rgb = 0;
                int c00 = kuva[gxi][ gyi];
                int c10 = kuva[gxi + 1][ gyi];
                int c01 = kuva[gxi][ gyi + 1];
                int c11 = kuva[gxi + 1][ gyi + 1];
                for (int i = 0; i <= 2; ++i) {
                    float b00 = get(c00, i);
                    float b10 = get(c10, i);
                    float b01 = get(c01, i);
                    float b11 = get(c11, i);
                    int ble = ((int) blerp(b00, b10, b01, b11, gx - gxi, gy - gyi)) ;
                    rgb = rgb | ble;
                }
                uusikuva[x][y] = rgb;
                //newImage.setRGB(x, y, rgb);
            }
        }
        return uusikuva;
    }
    public static int[][] testikuva() {
        int[][] kuva = new int[LeptonCamera.getHeight()][LeptonCamera.getWidth()];

        for(int y = 0; y < kuva.length; y++){
            for(int x = 0; x < kuva[kuva.length-1].length; x++){
                kuva[y][x] = (int) (30000+Math.round(Math.random()*1000));
            }
        }
        return kuva;
    }

    private static void testailija() {
        int[][] kuva = new int[5][5];

        kuva[0][0] = 31000;
        kuva[1][0] = 30000;
        kuva[2][0] = 30000;
        kuva[3][0] = 30000;
        kuva[4][0] = 30000;

        kuva[0][1] = 30000;
        kuva[1][1] = 30000;
        kuva[2][1] = 30000;
        kuva[3][1] = 30000;
        kuva[4][1] = 30000;

        kuva[0][2] = 30000;
        kuva[1][2] = 30000;
        kuva[2][2] = 30000;
        kuva[3][2] = 30000;
        kuva[4][2] = 30000;

        kuva[0][3] = 30000;
        kuva[1][3] = 30000;
        kuva[2][3] = 30000;
        kuva[3][3] = 30000;
        kuva[4][3] = 30000;

        kuva[0][4] = 30000;
        kuva[1][4] = 30000;
        kuva[2][4] = 30000;
        kuva[3][4] = 30000;
        kuva[4][4] = 31000;

        /*for(int y = 0; y < kuva.length; y++){
            for(int x = 0; x < kuva[kuva.length-1].length; x++){
                kuva[x][y] = (y+x)*100+30000;
            }
        }*/
        for(int y = 0; y < kuva.length; y++){
            for(int x = 0; x < kuva[kuva.length-1].length; x++){
                System.out.print(kuva[y][x]+"|");
            }
            System.out.println();
        }
        System.out.println("skaala");
        kuva = scale(kuva, 5, 5, 5.2f, 7.2f);

        for(int y = 0; y < kuva.length; y++){
            for(int x = 0; x < kuva[kuva.length-1].length; x++){
                System.out.print(kuva[y][x]+"|");
            }
            System.out.println();
        }
    }
}
