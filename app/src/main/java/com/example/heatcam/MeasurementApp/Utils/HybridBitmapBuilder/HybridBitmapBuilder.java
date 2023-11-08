package com.example.heatcam.MeasurementApp.Utils.HybridBitmapBuilder;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import androidx.camera.core.ImageProxy;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceManager;

import com.example.heatcam.MeasurementApp.Fragments.Measurement.MeasurementStartFragment;
import com.example.heatcam.MeasurementApp.ThermalCamera.SerialListeners.LeptonCamera;
import com.example.heatcam.MeasurementApp.Utils.ImageUtils;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;

public class HybridBitmapBuilder{
    private CameraBitmap cameraBitmap;

    private Bitmap heatMap;
    private Bitmap liveMap;
    private HybridFaceDetector fTool;
    private Face face;
    private Rect faceBounds = new Rect();
    private HuippuLukema huiput = new HuippuLukema();
    private HybridImageListener listener;
    private MeasurementStartFragment msf;

    public HybridBitmapBuilder(LifecycleOwner owner, View view) {
        cameraBitmap = new CameraBitmap(owner, this, view);
        fTool = new HybridFaceDetector(this);
        if(owner instanceof MeasurementStartFragment)
            this.msf = (MeasurementStartFragment)owner;

        this.listener = (HybridImageListener)owner;
        SharedPreferences sp = view.getContext().getSharedPreferences("heatmapPrefs", Context.MODE_PRIVATE);

        HybridImageOptions.temperature = sp.getBoolean("temperature", true);
        HybridImageOptions.transparency = sp.getInt("transparency", 200);
        HybridImageOptions.smooth = sp.getBoolean("smooth", true);
        HybridImageOptions.facebounds = sp.getBoolean("facebounds", false);

        HybridImageOptions.scale = sp.getFloat("scale", 8.97f);
        HybridImageOptions.xOffset = sp.getInt("offsetx", -32);
        HybridImageOptions.yOffset = sp.getInt("offsety", -71);
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(view.getContext());
        HybridImageOptions.distanceCorrection = (double)Integer.parseInt(sharedPrefs.getString("PREFERENCE_TEMPERATURE_DISTANCE_CORRECTION", "33"))/10000;
        //HybridImageOptions.scaledWidth = sp.getInt("resx", LeptonCamera.getWidth());
        //HybridImageOptions.scaledHeight = sp.getInt("resy", LeptonCamera.getHeight());

    }

    public static Bitmap setOpacity(Bitmap image){
        Bitmap O = Bitmap.createBitmap(image.getWidth(),image.getHeight(), image.getConfig());
        for(int i=0; i<image.getWidth(); i++){
            for(int j=0; j<image.getHeight(); j++){
                int pixel = image.getPixel(i, j);
                int r = Color.red(pixel), g = Color.green(pixel), b = Color.blue(pixel);
                if (pixel > ImageUtils.LOWEST_COLOR) {
                    O.setPixel(i, j, Color.argb(HybridImageOptions.transparency, r, g, b));
                }
            }
        }
        return O;
    }

    long aika = System.currentTimeMillis();
    public void onNewBitmap(Bitmap image, ImageProxy proxy){
        liveMap = image.copy(image.getConfig(), true);
        InputImage inputImage = InputImage.fromBitmap(image, 0);
        if(System.currentTimeMillis() > aika){
            aika = System.currentTimeMillis()+100;
            fTool.processImage(inputImage, proxy); // face detection
        }
        updateLiveImage(liveMap);
    }

    public void setHeatmap(Bitmap image) {
        heatMap = image;
    }

    private void drawFaceBounds(Bitmap image){

        Canvas canvas = new Canvas(image);
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);

        Rect rect = new Rect();
        rect.set((int)(vasen*HybridImageOptions.scale + HybridImageOptions.xOffset), (int)(yla*HybridImageOptions.scale + HybridImageOptions.yOffset), (int)(oikea*HybridImageOptions.scale + HybridImageOptions.xOffset), (int)(ala*HybridImageOptions.scale + HybridImageOptions.yOffset));
        canvas.drawRect(rect, paint);
    }

    private void drawTemperature(Bitmap image){

        Canvas canvas = new Canvas(image);

        Paint paint2 = new Paint();
        paint2.setColor(Color.CYAN);
        paint2.setStyle(Paint.Style.FILL);

        Paint paint3 = new Paint();
        paint3.setColor(Color.GREEN);
        paint3.setStyle(Paint.Style.FILL);

        canvas.drawText(huiput.max+"", huiput.x, huiput.y, paint2);
        canvas.drawCircle(huiput.x, huiput.y, 2, paint3);
    }

    private Bitmap overlay(Bitmap live, Bitmap heat, boolean opacity) {

        if(opacity)
            heat = setOpacity(heat);

        Matrix m = new Matrix();
        //m.postScale(ModifyHeatmap.scale, ModifyHeatmap.scale);
        m.postTranslate(HybridImageOptions.xOffset, HybridImageOptions.yOffset);
        heat = Bitmap.createScaledBitmap(heat, (int)(heat.getWidth()*HybridImageOptions.scale), (int)(heat.getHeight()*HybridImageOptions.scale), HybridImageOptions.smooth);
        Canvas canvas = new Canvas(live);
        canvas.drawBitmap(heat, m, null);

        return live;
    }
    double distance = 0;
    public void setDistance(double distance){
        this.distance = distance;
    }
    private void updateLiveImage(Bitmap img){
        // copy image to make it mutable
        Bitmap image = img.copy(Bitmap.Config.ARGB_8888, true);
        if(heatMap != null && liveMap != null){
            huiput = laskeAlue();
            image = overlay(liveMap,heatMap,true);
            //else
            //    image = heatMap.copy(Bitmap.Config.ARGB_8888, true);

            if(HybridImageOptions.facebounds)
                drawFaceBounds(image);
            if(HybridImageOptions.temperature)
                drawTemperature(image);
            listener.onNewHybridImage(image);
        }
        else if(heatMap == null && liveMap != null){
            if(HybridImageOptions.facebounds)
                drawFaceBounds(image);
            listener.onNewHybridImage(image);
        }
    }

    public void setMsfNull(){
        msf = null;
    }
    protected void updateDetectedFace(Face face){
        if(face != null){
            if(msf != null)
                msf.faceDetected(face);
            this.face = face;
            faceBounds = face.getBoundingBox();
        }
        else{
            if(msf != null)
                msf.faceNotDetected();
            faceBounds = new Rect();
            faceBounds.set(0,0,0,0);
        }
    }

    int yla,vasen,oikea,ala = 0;
    //List<HuippuLukema> lukemat = new ArrayList<>();
    private HuippuLukema laskeAlue(){
        //lukemat.clear();
        int[][] scaledTempFrame = ScaledHeatmap.scaledTempFrame;
        //huiput = new HuippuLukema();
        if(scaledTempFrame == null)
            scaledTempFrame = LeptonCamera.getTempFrame();

        int livekorkeus = liveMap.getHeight();
        int liveleveys = liveMap.getWidth();
        int heatleveys = scaledTempFrame[scaledTempFrame.length-1].length;
        int heatkorkeus = scaledTempFrame.length;

        int top = (int)(((double)faceBounds.top / (double)livekorkeus) * heatkorkeus);
        int left = (int)(((double)faceBounds.left / (double)liveleveys) * heatleveys);
        int right = (int)(((double)faceBounds.right / (double)liveleveys) * heatleveys);
        int bottom = (int)(((double)faceBounds.bottom / (double)livekorkeus) * heatkorkeus);
        vasen = left; if(vasen < 0) vasen = 0; if(vasen > heatleveys) vasen = heatleveys;
        oikea = right; if(oikea < 0) oikea = 0; if(oikea > heatleveys) oikea = heatleveys;
        yla = top; if(yla < 0) yla = 0; if(yla > heatkorkeus) yla = heatkorkeus;
        ala = bottom; if(ala < 0) ala = 0; if(ala > heatkorkeus) ala = heatkorkeus;
        HuippuLukema temp = new HuippuLukema();
        try{
            if(scaledTempFrame != null){
                for(int y = yla; y < ala; y++){
                    for(int x = vasen; x < oikea; x++){
                        double lampo = (scaledTempFrame[y][x]- 27315)/100.0;
                        lampo = pyoristys(lampo+distance*HybridImageOptions.distanceCorrection);
                        if(lampo > temp.max){

                            temp.max = lampo;
                            temp.y = (int)(y*HybridImageOptions.scale) + HybridImageOptions.yOffset;
                            temp.x = (int)(x*HybridImageOptions.scale) + HybridImageOptions.xOffset;

                            //lukemat.add(temp);
                            //if(lukemat.size() > 5)
                             //   lukemat.remove(0);
                        }
                    }
                }
            }

        }catch (Exception e){
            //System.out.println(e.getMessage());
        }

        /*tulosten keskiarvotus
        if(lukemat.size() > 0){
            int x = 0, y = 0;
            double keskilampo = 0;
            for(int i = 0; i < lukemat.size(); i++){
                x += lukemat.get(i).x;
                y += lukemat.get(i).y;
                keskilampo += lukemat.get(i).max;
            }
            temp.x = x/lukemat.size();
            temp.y = y/lukemat.size();
            temp.max = Math.round(keskilampo/lukemat.size()*100.0)/100.0;
        }
*/
        if(temp.max >= huiput.max+0.1 || temp.max <= huiput.max-0.1)
            huiput = temp;

        return huiput;
    }

    public double pyoristys(double luku){
        return Math.round(luku*10.0)/10.0;
    }
    public double getHighestFaceTemperature(){
        return huiput.max;
    }
    public Bitmap getLiveMap(){
        return liveMap;
    }

    class HuippuLukema{
        int x = 0;
        int y = 0;
        double max = 0;
    }

    public static void setScale(double newscale) {
        HybridImageOptions.scale *= newscale;
        HybridImageOptions.scale = Math.round(HybridImageOptions.scale*100f)/100f;
        if(HybridImageOptions.scale < 1)
            HybridImageOptions.scale = 1f;
    }
    public static void setRes(double newres) {
        if(HybridImageOptions.scaledWidth*newres < LeptonCamera.getWidth() || HybridImageOptions.scaledHeight*newres < LeptonCamera.getHeight()){
            double oldscale = (double)HybridImageOptions.scaledHeight / (double)(LeptonCamera.getHeight());
            setScale(oldscale);

            HybridImageOptions.scaledWidth = LeptonCamera.getWidth();
            HybridImageOptions.scaledHeight = LeptonCamera.getHeight();
            return;
        }

        double oldscale = (double)HybridImageOptions.scaledHeight / (double)(HybridImageOptions.scaledHeight*newres);
        setScale(oldscale);

        HybridImageOptions.scaledHeight *= newres;
        HybridImageOptions.scaledWidth *= newres;

    }

    public static String teksti(){
        return "x: "+HybridImageOptions.xOffset+" y: "+HybridImageOptions.yOffset+" w/h: "+ HybridImageOptions.scaledWidth+"/"+HybridImageOptions.scaledHeight+" s: "+HybridImageOptions.scale;
    }
}
