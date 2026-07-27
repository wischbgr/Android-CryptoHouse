package de.wladimircomputin.cryptohouse.device;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ProgressBar;
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
import mobi.upod.timedurationpicker.TimeDurationPickerDialog;

public class CryptoTimer extends ACryptoDevice{

    ProgressBar timerProgress;
    SwitchCompat switch2;
    TextView timerText;
    TextView timerRemaining;
    Button timerButton;

    public CryptoTimer(DeviceManagerDevice device, Context context) {
        super(device, context, R.layout.device_cryptotimer);

        timerProgress = rootview.findViewById(R.id.timer_progress);
        switch2 = rootview.findViewById(R.id.switch2);
        timerText = rootview.findViewById(R.id.timerText);
        timerRemaining = rootview.findViewById(R.id.timer_remaining);
        timerButton = rootview.findViewById(R.id.timer_button);

        timerButton.setOnClickListener(this::timerButton_click);
        timerText.setOnClickListener(this::timerText_click);
        switch2.setOnClickListener((view) -> {
            String command = "TimerController:switch:";
            if(((SwitchCompat)view).isChecked()){
                command += "1";
            } else {
                command += "0";
            }
            skipNextUpdate();
            cc.sendMessageEncrypted(command, CryptCon.Mode.UDP, new CryptConReceiver() {
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
        });

        timerText.setText(toFormatTime((long)sharedPref.getInt("lasttimer", 0) * 1000));
    }

    @Override
    public void update() {
        cc.sendMessageEncrypted("TimerController:state", CryptCon.Mode.UDP, new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    titleText.setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
                    boolean state = false;
                    double progress = 0;
                    long seconds_remaining = 0;
                    try {
                        JSONObject json = new JSONObject(response.data);
                        state = json.getInt("state") == 1;
                        int seconds_set = json.getInt("seconds_set");
                        seconds_remaining = json.getInt("seconds_remaining");
                        if(seconds_set != 0){
                            progress = (double)seconds_remaining / (double)seconds_set;
                        }
                    } catch (Exception x){}
                    switch2.setChecked((boolean)state);
                    setProgressAnimate(timerProgress, (int)Math.round(progress * 1000));
                    timerRemaining.setText(toFormatTime(seconds_remaining * 1000));
                });
            }

            @Override
            public void onFail() {
                new Handler(Looper.getMainLooper()).post(() -> {
                    titleText.setTextColor(ContextCompat.getColor(context, R.color.colorRed));
                });
            }

            @Override
            public void onFinished() {}

            @Override
            public void onProgress(String sprogress, int iprogress) {}
        });
    }

    public void startTimer(int seconds){
        skipNextUpdate();
        cc.sendMessageEncrypted("TimerController:timer:" + "1" + ":" + seconds, CryptCon.Mode.UDP, new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
                setProgressAnimate(timerProgress, 1000);
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

    private String toFormatTime(long ms){
        int seconds = (int)(ms / 1000);
        int minutes = seconds / 60;
        int hours = minutes / 60;
        seconds %= 60;
        minutes %= 60;
        return String.format(Locale.GERMAN, "%02d:%02d:%02d", hours, minutes, seconds);
    }

    public void timerText_click(View view){
        //int lasttimer = sharedPref.getInt("lasttimer", 0);
        TimeDurationPickerDialog dialog = new TimeDurationPickerDialog(context, (view1, duration) -> {
            timerText.setText(toFormatTime(duration));
            sharedPref.edit().putInt("lasttimer", (int)(duration / 1000)).apply();
        }, 0);
        dialog.show();
    }

    public void timerButton_click(View view){
        startTimer(sharedPref.getInt("lasttimer", 0));
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
