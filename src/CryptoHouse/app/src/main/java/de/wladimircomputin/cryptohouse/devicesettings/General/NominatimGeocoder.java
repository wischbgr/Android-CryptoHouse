package de.wladimircomputin.cryptohouse.devicesettings.General;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Address search backed by the free OpenStreetMap Nominatim API (no API key required).
 */
public class NominatimGeocoder {

    private static final String ENDPOINT = "https://nominatim.openstreetmap.org/search";

    public static class Result {
        public final String displayName;
        public final double lat;
        public final double lon;

        public Result(String displayName, double lat, double lon) {
            this.displayName = displayName;
            this.lat = lat;
            this.lon = lon;
        }
    }

    // Blocking network call; run off the main thread.
    public static List<Result> search(String query) throws Exception {
        String url = ENDPOINT + "?format=json&limit=5&addressdetails=0&q=" + URLEncoder.encode(query, "UTF-8");
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", "CryptoHouse-Android-App");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        try (InputStream in = connection.getInputStream()) {
            JSONArray jsonArray = new JSONArray(readAll(in));
            List<Result> results = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                results.add(new Result(obj.getString("display_name"), obj.getDouble("lat"), obj.getDouble("lon")));
            }
            return results;
        } finally {
            connection.disconnect();
        }
    }

    private static String readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toString(StandardCharsets.UTF_8.name());
    }
}
