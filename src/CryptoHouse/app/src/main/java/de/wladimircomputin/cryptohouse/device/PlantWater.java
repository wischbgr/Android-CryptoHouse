package de.wladimircomputin.cryptohouse.device;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import de.wladimircomputin.cryptohouse.R;
import de.wladimircomputin.cryptohouse.devicemanager.DeviceManagerDevice;
import de.wladimircomputin.libcryptoiot.v2.protocol.Content;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptCon;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptConReceiver;

public class PlantWater extends ACryptoDevice{

    public enum CONTROLL_MODE {
        NONE("NONE", 0),
        MANUAL("MANUAL", 1),
        SEMI("SEMI", 2),
        AUTO("AUTO", 3);

        private final String text;
        private final int pos;

        /**
         * @param text
         */
        CONTROLL_MODE(final String text, final int pos) {
            this.text = text;
            this.pos = pos;
        }

        static CONTROLL_MODE fromString(String in){
            for (CONTROLL_MODE val : CONTROLL_MODE.values()){
                if(val.text.equals(in)){
                    return val;
                }
            }
            return CONTROLL_MODE.MANUAL;
        }

        static CONTROLL_MODE fromInt(int pos){
            for (CONTROLL_MODE val : CONTROLL_MODE.values()){
                if(val.pos == pos){
                    return val;
                }
            }
            return CONTROLL_MODE.MANUAL;
        }

        @NonNull
        @Override
        public String toString() {
            return this.text;
        }

        public int toInt() {
            return this.pos;
        }
    }

    SeekBar seekBar;
    ProgressBar waterProgress;
    ProgressBar waterGlassProgress;
    SwitchCompat switch1;

    public PlantWater(DeviceManagerDevice device, Context context) {
        super(device, context, R.layout.device_plantwater);

        seekBar = rootview.findViewById(R.id.seekBar);
        switch1 = rootview.findViewById(R.id.switch1);
        waterProgress = rootview.findViewById(R.id.waterProgress);
        waterGlassProgress = rootview.findViewById(R.id.waterGlassProgress);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                skipNextUpdate();
                setMode(CONTROLL_MODE.values()[seekBar.getProgress()]);
            }
        });

        switch1.setOnClickListener((view) -> {
            String command = "Rain:state:";
            if(((SwitchCompat)view).isChecked()){
                command += "1";
            } else {
                command += "0";
            }
            skipNextUpdate();
            cc.sendMessageEncrypted(command);
        });
    }

    public void setMode(CONTROLL_MODE mode) {
        skipNextUpdate();
        cc.sendMessageEncrypted("Rain:mode:" + mode.toString());
    }

    @Override
    public void update() {
        cc.sendMessageEncrypted("Rain:state", CryptCon.Mode.UDP, new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    titleText.setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
                    boolean state = false;
                    CONTROLL_MODE mode = CONTROLL_MODE.MANUAL;
                    double water_lvl = 0;
                    double virt_water_glass = 0;
                    try {
                        JSONObject json = new JSONObject(response.data);
                        state = json.getBoolean("state");
                        mode = CONTROLL_MODE.fromString(json.getString("mode"));
                        water_lvl = json.getDouble("water_level");
                        virt_water_glass = json.getDouble("virt_water_glass");

                    } catch (Exception x){}
                    switch1.setChecked(state);
                    if (!seekBar.isPressed()) {
                        seekBar.setProgress(mode.ordinal(), true);
                    }
                    setProgressAnimate(waterProgress, (int)(water_lvl * 1000));
                    setProgressAnimate(waterGlassProgress, (int)(virt_water_glass * 1000));
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

    private void setProgressAnimate(ProgressBar pb, int progressTo) {
        //new Handler(Looper.getMainLooper()).post(() -> {
            ObjectAnimator animation = ObjectAnimator.ofInt(pb, "progress", pb.getProgress(), progressTo);
            animation.setDuration(300);
            animation.setInterpolator(new DecelerateInterpolator());
            animation.start();
        //});
    }
}
