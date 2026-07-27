package de.wladimircomputin.cryptohouse.device;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.util.Locale;

import de.wladimircomputin.cryptohouse.R;
import de.wladimircomputin.cryptohouse.devicemanager.DeviceManagerDevice;
import de.wladimircomputin.libcryptoiot.v2.protocol.Content;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptCon;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptConReceiver;

public class CryptoGeiger extends ACryptoDevice{

    TextView cpm;
    TextView dose;
    TextView acc_dose;
    SwitchCompat display_switch;

    int current_cpm = 0;
    double current_svh = 0;

    public CryptoGeiger(DeviceManagerDevice device, Context context) {
        super(device, context, R.layout.device_cryptogeiger);
        cpm = rootview.findViewById(R.id.cpm_text);
        dose = rootview.findViewById(R.id.dose_text);
        acc_dose = rootview.findViewById(R.id.total_dose_text);
        display_switch = rootview.findViewById(R.id.geiger_display_switch);

        display_switch.setOnClickListener((view) -> {
            if (((SwitchCompat)view).isChecked()) {
                cc.sendMessageEncrypted("Geiger:guimode:normal");
            } else {
                cc.sendMessageEncrypted("Geiger:guimode:off");
            }
        });
    }

    @Override
    public void update() {
        cc.sendMessageEncrypted("Geiger:getgeiger", CryptCon.Mode.UDP, new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    titleText.setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
                    try {
                        JSONObject jsonObject = new JSONObject(response.data);
                        int new_cpm = Integer.parseInt(jsonObject.getString("cpm_accurate"));
                        double new_svh = Double.parseDouble(jsonObject.getString("uSv_h_accurate"));
                        ValueAnimator animator1 = ValueAnimator.ofInt(current_cpm, new_cpm);
                        animator1.setDuration(500);
                        animator1.addUpdateListener(animation -> cpm.setText(animation.getAnimatedValue().toString()));
                        animator1.start();

                        ValueAnimator animator2 = ValueAnimator.ofFloat((float) current_svh, (float)new_svh);
                        animator2.setDuration(500);
                        animator2.addUpdateListener(animation -> dose.setText(String.format(Locale.GERMANY,"%.3f",(float)animation.getAnimatedValue())));
                        animator2.start();

                        current_cpm = new_cpm;
                        current_svh = new_svh;
                    } catch (Exception x){

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
