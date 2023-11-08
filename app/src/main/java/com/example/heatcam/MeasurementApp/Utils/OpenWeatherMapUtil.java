package com.example.heatcam.MeasurementApp.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class OpenWeatherMapUtil {

    private static final String API_KEY = null; // replace with correct API key or use BuildConfig.API_KEY
    private static final String BASE_URL = "http://api.openweathermap.org/data/2.5/weather?q=";
    private HttpURLConnection conn = null;
    private InputStream is = null;

    public String getWeatherInfoForCity(String city) {
        try {
            URL url = new URL(BASE_URL + city + "&APPID=" + API_KEY);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();

            StringBuffer buffer = new StringBuffer();
            is = conn.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                buffer.append(line);
            }
            is.close();
            conn.disconnect();
            return buffer.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                is.close();
                conn.disconnect();
            } catch (Throwable t) {
            }
        }
        return null;
    }

}
