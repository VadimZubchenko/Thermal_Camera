package com.example.heatcam.MeasurementApp.Fragments.Measurement;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
/*
    How to use:
    1. create accessObject
    2. create new entry
    3. write object returned from newEntry() function
    > or
    2. read JSONArray containing measurements
 */

public class MeasurementAccessObject {
    private static final String DEFAULT_FILE_NAME = "measurement_data.json";
    private String fileName;

    public MeasurementAccessObject() {
        this.fileName = DEFAULT_FILE_NAME;
    }

    public MeasurementAccessObject(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Creates and returns new JSONObject.
     * @param measured Measured temperature.
     * @param date Date of measurement.
     * @return new JSONObject
     * @throws JSONException
     */
    public JSONObject newEntry(double measured, Date date) throws JSONException {
        JSONObject o = new JSONObject();
        o.put("Date", date);
        o.put("Measured", measured);
        return o;
    }

    /**
     * Creates and returns new JSONObject.
     * @param measured Measured temperature.
     * @param date Date of measurement.
     * @param outsideTemperature Measured outside temperature.
     * @return new JSONObject
     * @throws JSONException
     */
    public JSONObject newEntry(double measured, Date date, double outsideTemperature) throws JSONException {
        JSONObject o = newEntry(measured, date);
        o.put("OutsideTemperature", outsideTemperature);
        return o;
    }

    /**
     * Write JSONObject into .json file. Appends into existing JSONArray or overwrite with
     * new JSONArray.
     * @param context Context this fragment is currently associated with.
     * @param jsonObject JSONObject to write.
     * @param append True to append, false to overwrite.
     * @throws IOException
     * @throws JSONException
     */
    public void write(Context context, JSONObject jsonObject, boolean append) throws IOException, JSONException {
        JSONArray jsonArray;
        if(append) {
            jsonArray = read(context);
        } else {
            jsonArray = new JSONArray();
        }
        jsonArray.put(jsonObject);
        String jsonString = jsonArray.toString();
        File file = new File(context.getFilesDir(), fileName);

        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
        bufferedWriter.write(jsonString);
        bufferedWriter.close();
    }

    /**
     * Read
     * @param context Context this fragment is currently associated with.
     * @return JSONArray containing measurements.
     * @throws IOException
     * @throws JSONException
     */
    public JSONArray read(Context context) throws IOException, JSONException {

        File file = new File(context.getFilesDir(), fileName);
        if(!file.exists()) {
            file.createNewFile();
        }

        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        StringBuilder stringBuilder = new StringBuilder();
        String line = bufferedReader.readLine();
        while (line != null){
            stringBuilder.append(line).append("\n");
            line = bufferedReader.readLine();
        }
        bufferedReader.close();
        String result = stringBuilder.toString();
        if(result.length() > 0) {
            return new JSONArray(result);
        }
        return new JSONArray();
    }

}
