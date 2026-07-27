package de.wladimircomputin.cryptohouse.device;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import de.wladimircomputin.cryptohouse.R;
import de.wladimircomputin.cryptohouse.devicemanager.DeviceManagerDevice;
import de.wladimircomputin.libcryptoiot.v2.protocol.Content;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptCon;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptConReceiver;

public class UranLight extends ACryptoDevice{

    public enum URANLIGHT_MODE {
        MODE_ON("ON"),
        MODE_RANDOM("RANDOM"),
        MODE_WAVE("WAVE"),
        MODE_CIRCLE("CIRCLE");

        private final String text;

        /**
         * @param text
         */
        URANLIGHT_MODE(final String text) {
            this.text = text;
        }

        static UranLight.URANLIGHT_MODE fromString(String in){
            for (UranLight.URANLIGHT_MODE val : UranLight.URANLIGHT_MODE.values()){
                if(val.text.equals(in)){
                    return val;
                }
            }
            return UranLight.URANLIGHT_MODE.MODE_ON;
        }

        @NonNull
        @Override
        public String toString() {
            return this.text;
        }
    }

    SwitchCompat stateSwitch;
    Spinner modeSpinner;
    URANLIGHT_MODE current_mode;


    public UranLight(DeviceManagerDevice device, Context context) {
        super(device, context, R.layout.device_uranlight);

        stateSwitch = rootview.findViewById(R.id.state_switch);
        modeSpinner = rootview.findViewById(R.id.mode_spinner);

        stateSwitch.setOnClickListener((view) -> {
            setState(((SwitchCompat)view).isChecked());
        });

        modeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                URANLIGHT_MODE mode = URANLIGHT_MODE.values()[position];
                if(current_mode != null && !mode.equals(current_mode)) {
                    setMode(mode);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }

    public void setState(boolean val){
        skipNextUpdate();
        cc.sendMessageEncrypted("UranLight:state:" + (val ? "1" : "0"), CryptCon.Mode.UDP, new CryptConReceiver(){
            @Override
            public void onSuccess(Content response) {

            }

            @Override
            public void onFail() {

            }

            @Override
            public void onFinished() {

            }

            @Override
            public void onProgress(String sprogress, int iprogress) {

            }
        });
    }

    public void setMode(URANLIGHT_MODE mode){
        skipNextUpdate();
        cc.sendMessageEncrypted("UranLight:mode:" + mode.toString(), CryptCon.Mode.UDP, new CryptConReceiver(){
            @Override
            public void onSuccess(Content response) {

            }

            @Override
            public void onFail() {

            }

            @Override
            public void onFinished() {

            }

            @Override
            public void onProgress(String sprogress, int iprogress) {

            }
        });
    }

    @Override
    public void update() {
        cc.sendMessageEncrypted("UranLight:get",  CryptCon.Mode.UDP, new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    titleText.setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
                    try {
                        JSONObject jsonObject = new JSONObject(response.data);
                        boolean current_state = jsonObject.getInt("state") == 1;
                        current_mode = URANLIGHT_MODE.fromString(jsonObject.getString("mode"));
                        stateSwitch.setChecked(current_state);

                        modeSpinner.setSelection(current_mode.ordinal(), true);
                    } catch (Exception x){
                        x.printStackTrace();
                    }
                });
            }

            @Override
            public void onFail() {
                new Handler(Looper.getMainLooper()).post(() -> {
                    titleText.setTextColor(ContextCompat.getColor(context, R.color.colorRed));
                });
            }

            @Override
            public void onFinished() {

            }

            @Override
            public void onProgress(String sprogress, int iprogress) {

            }
        });
    }
}
