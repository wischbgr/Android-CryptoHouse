package de.wladimircomputin.cryptohouse.device;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

import de.wladimircomputin.cryptohouse.R;
import de.wladimircomputin.cryptohouse.devicemanager.DeviceManagerDevice;
import de.wladimircomputin.libcryptoiot.v2.protocol.Content;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptConReceiver;

public class CryptoSML extends ACryptoDevice{

    TextView powerText;
    TextView frequencyText;
    ProgressBar voltageP1Progress;
    TextView voltageP1Text;
    ProgressBar voltageP2Progress;
    TextView voltageP2Text;
    ProgressBar voltageP3Progress;
    TextView voltageP3Text;
    ProgressBar currentP1Progress;
    TextView currentP1Text;
    ProgressBar currentP2Progress;
    TextView currentP2Text;
    ProgressBar currentP3Progress;
    TextView currentP3Text;
    TextView totalConsumptionText;

    public CryptoSML(DeviceManagerDevice device, Context context) {
        super(device, context, R.layout.device_cryptosml);

        powerText = rootview.findViewById(R.id.power_text);
        frequencyText = rootview.findViewById(R.id.frequency_text);
        voltageP1Progress = rootview.findViewById(R.id.voltage_p1_progress);
        voltageP1Text = rootview.findViewById(R.id.voltage_p1_text);
        voltageP2Progress = rootview.findViewById(R.id.voltage_p2_progress);
        voltageP2Text = rootview.findViewById(R.id.voltage_p2_text);
        voltageP3Progress = rootview.findViewById(R.id.voltage_p3_progress);
        voltageP3Text = rootview.findViewById(R.id.voltage_p3_text);
        currentP1Progress = rootview.findViewById(R.id.current_p1_progress);
        currentP1Text = rootview.findViewById(R.id.current_p1_text);
        currentP2Progress = rootview.findViewById(R.id.current_p2_progress);
        currentP2Text = rootview.findViewById(R.id.current_p2_text);
        currentP3Progress = rootview.findViewById(R.id.current_p3_progress);
        currentP3Text = rootview.findViewById(R.id.current_p3_text);
        totalConsumptionText = rootview.findViewById(R.id.total_consumption_text);
    }

    @Override
    public void update() {
        cc.sendMessageEncrypted("SML:parsedsml", new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    titleText.setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
                    try {
                        JSONArray jsonArray = new JSONArray(response.data);
                        for(int i = 0; i < jsonArray.length(); i++){
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            String unit = "";
                            try {
                                unit = jsonObject.getString("unit");
                            } catch (Exception x){}

                            switch (jsonObject.getString("id")){
                                case "1-0:96.50.1/1":
                                    break;
                                case "1-0:96.1.0/255":
                                    break;
                                case "1-0:1.8.0/255": {
                                    totalConsumptionText.setText(String.format(Locale.GERMAN, "%.4f k%s", jsonObject.getDouble("val") / 1000, unit));
                                }
                                    break;
                                case "1-0:2.8.0/255":
                                    break;
                                case "1-0:16.7.0/255": {
                                    powerText.setText(String.format(Locale.GERMAN, "%.0f %s", jsonObject.getDouble("val"), unit));
                                }
                                    break;
                                case "1-0:32.7.0/255": {
                                    double value = jsonObject.getDouble("val");
                                    double middle = value - 230.0;
                                    int progress = 50 + (int)Math.round(middle);
                                    setProgressAnimate(voltageP1Progress, progress);
                                    voltageP1Text.setText(String.format(Locale.GERMAN, "%.1f %s", value, unit));
                                }
                                    break;
                                case "1-0:52.7.0/255": {
                                    double value = jsonObject.getDouble("val");
                                    double middle = value - 230.0;
                                    int progress = 50 + (int)Math.round(middle);
                                    setProgressAnimate(voltageP2Progress, progress);
                                    voltageP2Text.setText(String.format(Locale.GERMAN, "%.1f %s", jsonObject.getDouble("val"), unit));
                                }
                                    break;
                                case "1-0:72.7.0/255": {
                                    double value = jsonObject.getDouble("val");
                                    double middle = value - 230.0;
                                    int progress = 50 + (int)Math.round(middle);
                                    setProgressAnimate(voltageP3Progress, progress);
                                    voltageP3Text.setText(String.format(Locale.GERMAN, "%.1f %s", jsonObject.getDouble("val"), unit));
                                }
                                    break;
                                case "1-0:31.7.0/255": {
                                    double value = jsonObject.getDouble("val");
                                    int progress = (int)(Math.round(value / 16.0 * 100.0));
                                    setProgressAnimate(currentP1Progress, progress);
                                    currentP1Text.setText(String.format(Locale.GERMAN, "%.2f %s", jsonObject.getDouble("val"), unit));
                                }
                                    break;
                                case "1-0:51.7.0/255": {
                                    double value = jsonObject.getDouble("val");
                                    int progress = (int)(Math.round(value / 16.0 * 100.0));
                                    setProgressAnimate(currentP2Progress, progress);
                                    currentP2Text.setText(String.format(Locale.GERMAN, "%.2f %s", jsonObject.getDouble("val"), unit));
                                }
                                    break;
                                case "1-0:71.7.0/255": {
                                    double value = jsonObject.getDouble("val");
                                    int progress = (int)(Math.round(value / 16.0 * 100.0));
                                    currentP3Progress.setProgress(progress, true);
                                    setProgressAnimate(currentP3Progress, progress);
                                    currentP3Text.setText(String.format(Locale.GERMAN, "%.2f %s", jsonObject.getDouble("val"), unit));
                                }
                                    break;
                                case "1-0:81.7.1/255":
                                    break;
                                case "1-0:81.7.2/255":
                                    break;
                                case "1-0:81.7.4/255":
                                    break;
                                case "1-0:81.7.15/255":
                                    break;
                                case "1-0:81.7.26/255":
                                    break;
                                case "1-0:14.7.0/255": {
                                    frequencyText.setText(String.format(Locale.GERMAN, "%.1f %s", jsonObject.getDouble("val"), unit));
                                }
                                    break;
                                case "1-0:0.2.0/0":
                                    break;
                                case "1-0:96.90.2/1":
                                    break;
                                case "1-0:96.5.0/255":
                                    break;
                            }
                        }
                    } catch (Exception x){}
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
        new Handler(Looper.getMainLooper()).post(() -> {
            ObjectAnimator animation = ObjectAnimator.ofInt(pb, "progress", pb.getProgress(), progressTo);
            animation.setDuration(300);
            animation.setInterpolator(new DecelerateInterpolator());
            animation.start();
        });
    }
}
