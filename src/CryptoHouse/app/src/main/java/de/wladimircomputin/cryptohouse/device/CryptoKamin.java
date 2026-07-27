package de.wladimircomputin.cryptohouse.device;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import de.wladimircomputin.cryptohouse.R;
import de.wladimircomputin.cryptohouse.devicemanager.DeviceManagerDevice;
import de.wladimircomputin.libcryptoiot.v2.protocol.Content;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptCon;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptConReceiver;

public class CryptoKamin extends ACryptoDevice{

    public enum KAMIN_MODE {
        OFF("OFF", 0),
        LIGHTS("LIGHTS", 1),
        FIRE("FIRE", 2);

        private final String text;
        private final int pos;

        /**
         * @param text
         */
        KAMIN_MODE(final String text, final int pos) {
            this.text = text;
            this.pos = pos;
        }

        static KAMIN_MODE fromString(String in){
            for (KAMIN_MODE val : KAMIN_MODE.values()){
                if(val.text.equals(in)){
                    return val;
                }
            }
            return KAMIN_MODE.OFF;
        }

        static KAMIN_MODE fromInt(int pos){
            for (KAMIN_MODE val : KAMIN_MODE.values()){
                if(val.pos == pos){
                    return val;
                }
            }
            return KAMIN_MODE.OFF;
        }

        @NonNull
        @Override
        public String
        toString() {
            return this.text;
        }

        public int toInt() {
            return this.pos;
        }
    }

    SeekBar seekBar;
    ProgressBar waterProgress;

    public CryptoKamin(DeviceManagerDevice device, Context context) {
        super(device, context, R.layout.device_cryptokamin);

        seekBar = rootview.findViewById(R.id.seekBar);
        waterProgress = rootview.findViewById(R.id.waterProgress);

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
                setMode(KAMIN_MODE.values()[seekBar.getProgress()]);
            }
        });

        waterProgress.setOnLongClickListener(view -> {
            skipNextUpdate();
            cc.sendMessageEncrypted("Kamin:refilled");
            return true;
        });
    }

    public void setMode(KAMIN_MODE mode) {
        cc.sendMessageEncrypted("Kamin:mode:" + mode.toString());
    }

    @Override
    public void update() {
        cc.sendMessageEncrypted("Kamin:get", CryptCon.Mode.UDP, new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    titleText.setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
                    KAMIN_MODE mode = KAMIN_MODE.OFF;
                    int water = 0;
                    try {
                        JSONObject json = new JSONObject(response.data);
                        mode = KAMIN_MODE.fromString(json.getString("mode"));
                        water = json.getInt("water");

                    } catch (Exception x) {
                    }
                    if (!seekBar.isPressed()) {
                        seekBar.setProgress(mode.ordinal(), true);
                    }
                    setProgressAnimate(waterProgress, water * 10);
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
