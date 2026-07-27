package de.wladimircomputin.cryptohouse.device;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.widget.SwitchCompat;

import org.json.JSONObject;

import de.wladimircomputin.cryptohouse.R;
import de.wladimircomputin.cryptohouse.devicemanager.DeviceManagerDevice;
import de.wladimircomputin.libcryptoiot.v2.protocol.Content;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptCon;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptConReceiver;

public class LightSwitch extends ACryptoDevice{
    SwitchCompat sw0;
    SwitchCompat sw1;

    public LightSwitch(DeviceManagerDevice device, Context context) {
        super(device, context, R.layout.device_lightswitch);
        sw0 = rootview.findViewById(R.id.lightswitch_switch0);
        sw1 = rootview.findViewById(R.id.lightswitch_switch1);

        sw0.setOnClickListener((view) -> {
            setState(0, sw0.isChecked());

        });
        sw1.setOnClickListener((view) -> {
            setState(1, sw1.isChecked());
        });
    }

    @Override
    public void update() {
        cc.sendMessageEncrypted("LightSwitch:state", CryptCon.Mode.UDP, new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        titleText.setTextColor(context.getResources().getColor(R.color.colorAccent));
                        JSONObject jsonObject = new JSONObject(response.data);
                        sw0.setChecked(jsonObject.getInt("0") == 1);
                        sw1.setChecked(jsonObject.getInt("1") == 1);
                    } catch (Exception x){

                    }
                });
            }

            @Override
            public void onFail() {
                new Handler(Looper.getMainLooper()).post(() -> {
                    titleText.setTextColor(context.getResources().getColor(R.color.colorRed));
                });
            }

            @Override
            public void onFinished() {}

            @Override
            public void onProgress(String sprogress, int iprogress) {}
        });
    }

    public void setState(int sw, boolean state){
        skipNextUpdate();
        cc.sendMessageEncrypted("LightSwitch:touch:" + sw + ":" + (state ? "1" : "0"), CryptCon.Mode.UDP, new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {}

            @Override
            public void onFail() {}

            @Override
            public void onFinished() {}

            @Override
            public void onProgress(String sprogress, int iprogress) {}
        });
    }
}
