package de.wladimircomputin.cryptohouse.actions.config;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Objects;

import de.wladimircomputin.libcryptoiot.v2.protocol.CryptCon;

public class ActionDeviceItem {
    public String device_id;
    public CryptCon cc;
    public String[] commands;

    public ActionDeviceItem(String device_id, CryptCon cc, String[] commands) {
        this.device_id = device_id;
        this.cc = cc;
        this.commands = commands;
    }

    public ActionDeviceItem(JSONObject jsonobj, CryptCon cc){
        try {
            device_id = jsonobj.getString("device_id");
            commands = toStringArray(jsonobj.optJSONArray("commands"));
            this.cc = cc;
        } catch (Exception x){
            x.printStackTrace();
        }
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject out = new JSONObject();
        out.put("device_id", device_id);
        out.put("commands", toJsonArray(commands));
        return out;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActionDeviceItem that = (ActionDeviceItem) o;
        return Objects.equals(device_id, that.device_id) && Arrays.equals(commands, that.commands);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(device_id);
        result = 31 * result + Arrays.hashCode(commands);
        return result;
    }

    @NonNull
    @Override
    public ActionDeviceItem clone(){
        return new ActionDeviceItem(device_id, cc, commands.clone());
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
