package de.wladimircomputin.cryptohouse.devicesettings.TimeEvents;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Objects;

import de.wladimircomputin.libcryptoiot.v2.protocol.api.TimeEventType;

public class TimeEvent {
    public TimeEventType timeEventType;
    public String time;
    public String[] commands;

    public TimeEvent(TimeEventType timeEventType, String time, String[] commands) {
        this.timeEventType = timeEventType;
        this.time = time;
        this.commands = commands;
    }

    public TimeEvent(JSONObject jsonobj){
        try {
            timeEventType = TimeEventType.fromString(jsonobj.names().optString(0));
            if(timeEventType == TimeEventType.TIME) {
                time = jsonobj.names().optString(0);
            } else {
                time = "00:00";
            }
            if(jsonobj.optJSONArray(jsonobj.names().optString(0)) != null) {
                commands = toStringArray(jsonobj.optJSONArray(jsonobj.names().optString(0)));
            } else {
                commands = new String[]{jsonobj.optString(jsonobj.names().optString(0))};
            }
        } catch (Exception x){}
    }

    public String toJSON() throws JSONException {
        JSONObject out = new JSONObject();
        if (timeEventType == TimeEventType.TIME) {
            out.put(time, toJsonArray(commands));
        } else {
            out.put(timeEventType.toString(), toJsonArray(commands));
        }
        return out.toString();
    }

    @NonNull
    @Override
    public TimeEvent clone(){
        return new TimeEvent(timeEventType, time, commands);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeEvent timeEvent = (TimeEvent) o;
        return timeEventType == timeEvent.timeEventType && Objects.equals(time, timeEvent.time) && Arrays.equals(commands, timeEvent.commands);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(timeEventType, time);
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
