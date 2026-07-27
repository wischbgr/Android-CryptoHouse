package de.wladimircomputin.cryptohouse.device;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import de.wladimircomputin.cryptohouse.R;
import de.wladimircomputin.cryptohouse.devicemanager.DeviceManagerDevice;
import de.wladimircomputin.libcryptoiot.v2.protocol.Content;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptCon;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptConBulkReceiver;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptConReceiver;

public class PlugSwitch extends ACryptoDevice{
    SwitchCompat sw;
    ProgressBar powerProgress;
    TextView powerText;

    private static final int N_A = 999999;
    enum PowerMeterMode {UNKNOWN, ON, OFF};
    PowerMeterMode powerMeterMode = PowerMeterMode.UNKNOWN;



    public PlugSwitch(DeviceManagerDevice device, Context context) {
        super(device, context, R.layout.device_plugswitch);
        sw = rootview.findViewById(R.id.switch1);
        powerProgress = rootview.findViewById(R.id.powerProgress);
        powerText = rootview.findViewById(R.id.powerText);

        sw.setOnClickListener((view) -> {
            setState(((SwitchCompat)view).isChecked());
        });

        powerText.setText("N/A\nW");
        powerProgress.setProgress(0);
    }

    @Override
    public void update() {
        String[] commands;
        if(powerMeterMode == PowerMeterMode.UNKNOWN) {
            commands = new String[]{"Switch:state", "Powermeter:get", "apps"};
        } else if (powerMeterMode == PowerMeterMode.ON){
            commands = new String[]{"Switch:state", "Powermeter:get"};
        } else {
            commands = new String[]{"Switch:state"};
        }

        cc.sendMessageEncryptedBulk(commands, CryptCon.Mode.UDP, new CryptConBulkReceiver() {
            @Override
            public void onSuccess(Content response, int i) {
                switch (i) {
                    case 0:
                        new Handler(Looper.getMainLooper()).post(() -> {
                            titleText.setTextColor(context.getResources().getColor(R.color.colorAccent));
                            sw.setChecked(response.data.equals("1"));
                        });
                    break;
                    case 1:
                        new Handler(Looper.getMainLooper()).post(() -> {
                            int power = N_A;
                            try {
                                JSONObject jsonObject = new JSONArray(response.data).getJSONObject(0);
                                power = (int)Math.round(jsonObject.getDouble("power"));
                            } catch (Exception x){}
                            if(power != N_A) {
                                setProgressAnimate(powerProgress, (power * 1000 / 3500));
                                setTextAnimate(powerText, power);
                            }
                        });
                    break;
                    case 2:
                        powerMeterMode = response.data.contains("Powermeter") ? PowerMeterMode.ON : PowerMeterMode.OFF;
                    break;
                }
            }

            @Override
            public void onFail(int i) {
                switch (i) {
                    case 0:
                        new Handler(Looper.getMainLooper()).post(() -> {
                            titleText.setTextColor(context.getResources().getColor(R.color.colorRed));
                        });
                    break;
                    default:
                    break;

                }
            }

            @Override
            public void onFinished() {}

            @Override
            public void onProgress(String sprogress, int iprogress) {}
        });
    }

    public void setState(boolean state){
        skipNextUpdate();
        cc.sendMessageEncrypted("Switch:switch:" + (state ? "1" : "0"), CryptCon.Mode.UDP, new CryptConReceiver() {
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

    private void setProgressAnimate(ProgressBar pb, int progressTo) {
        ObjectAnimator animation = ObjectAnimator.ofInt(pb, "progress", pb.getProgress(), progressTo);
        animation.setDuration(300);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();
    }

    private void setTextAnimate(TextView tv, int value) {
        int startValue = 0;
        try {
            String currentText = tv.getText().toString().split("\n")[0];
            startValue = Integer.parseInt(currentText);
        } catch (Exception e) {}
        if(value != N_A) {
            ValueAnimator animator = ValueAnimator.ofInt(startValue, value);
            animator.setDuration(300);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(animation -> tv.setText(String.format("%d\nW", (int) animation.getAnimatedValue())));
            animator.start();
        } else {
            tv.setText("N/A\nW");
        }
    }
}
