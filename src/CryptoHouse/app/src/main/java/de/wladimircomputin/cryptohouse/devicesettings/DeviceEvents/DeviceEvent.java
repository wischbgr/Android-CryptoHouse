package de.wladimircomputin.cryptohouse.devicesettings.DeviceEvents;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Objects;

public class DeviceEvent {
    public String event;
    public String[] commands;

    public DeviceEvent(String event, String[] commands) {
        this.event = event;
        this.commands = commands;
    }

    public DeviceEvent(JSONObject jsonobj){
        try {
            event = jsonobj.names().optString(0);
            if(jsonobj.optJSONArray(event) != null) {
                commands = toStringArray(jsonobj.optJSONArray(event));
            } else {
                commands = new String[]{jsonobj.optString(event)};
            }
        } catch (Exception x){}
    }

    public String toJSON() throws JSONException {
        JSONObject out = new JSONObject();
        out.put(event, toJsonArray(commands));
        return out.toString();
    }

    @NonNull
    @Override
    public DeviceEvent clone(){
        return new DeviceEvent(event, commands);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeviceEvent deviceEvent = (DeviceEvent) o;
        return Objects.equals(event, deviceEvent.event) && Arrays.equals(commands, deviceEvent.commands);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(event);
        result = 31 * result + Arrays.hashCode(commands);
        return result;
    }

    private static String[] toStringArray(JSONArray array) {
        if(array==null)
            return new String[] {};

        String[] arr = new String[array.length()];
        for(int i=0; i<arr.length; i++) {
            arr[i] = array.optString(i);
        }
        return arr;
    }

    private static JSONArray toJsonArray(String[] array) {
        if(array==null)
            return new JSONArray();

        JSONArray jsonArray = new JSONArray();
        for (String s : array) {
            if (!array[0].isEmpty()) {
                jsonArray.put(s);
            }
        }
        return jsonArray;
    }

}
