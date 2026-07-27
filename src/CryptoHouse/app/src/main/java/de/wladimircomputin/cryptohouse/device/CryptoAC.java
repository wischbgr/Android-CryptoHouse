package de.wladimircomputin.cryptohouse.device;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.github.shchurov.horizontalwheelview.HorizontalWheelView;

import org.json.JSONObject;

import java.util.Locale;

import de.wladimircomputin.cryptohouse.R;
import de.wladimircomputin.cryptohouse.devicemanager.DeviceManagerDevice;
import de.wladimircomputin.libcryptoiot.v2.protocol.Content;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptCon;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptConReceiver;

public class CryptoAC extends ACryptoDevice{

    public enum LG_AC_MODE {
        COOL("COOL"),
        DRY("DRY"),
        FAN("FAN"),
        AUTO("AUTO"),
        HEAT("HEAT");

        private final String text;

        /**
         * @param text
         */
        LG_AC_MODE(final String text) {
            this.text = text;
        }

        @NonNull
        @Override
        public String toString() {
            return this.text;
        }
    }

    public enum LG_AC_FAN {
        F1("F1"),
        F2("F2"),
        F3("F3"),
        F4("F4"),
        F5("F5"),
        AUTO("AUTO");

        private final String text;

        /**
         * @param text
         */
        LG_AC_FAN(final String text) {
            this.text = text;
        }

        @NonNull
        @Override
        public String toString() {
            return this.text;
        }
    }

    public enum LG_AC_VERTICAL_SWING {
        S1("1"),
        S2("2"),
        S3("3"),
        S4("4"),
        S5("5"),
        S6("6"),
        AUTO("AUTO"),
        NONE("NONE");

        private final String text;

        /**
         * @param text
         */
        LG_AC_VERTICAL_SWING(final String text) {
            this.text = text;
        }

        static LG_AC_VERTICAL_SWING fromString(String in){
            for (LG_AC_VERTICAL_SWING val : LG_AC_VERTICAL_SWING.values()){
                if(val.text.equals(in)){
                    return val;
                }
            }
            return LG_AC_VERTICAL_SWING.NONE;
        }

        @NonNull
        @Override
        public String toString() {
            return this.text;
        }
    }

    public enum LG_AC_HORIZONTAL_SWING {
        S1("1"),
        S2("2"),
        S3("3"),
        S4("4"),
        S5("5"),
        AUTO_LEFT("AUTO_LEFT"),
        AUTO_RIGHT("AUTO_RIGHT"),
        AUTO("AUTO"),
        NONE("NONE");

        private final String text;

        /**
         * @param text
         */
        LG_AC_HORIZONTAL_SWING(final String text) {
            this.text = text;
        }

        static LG_AC_HORIZONTAL_SWING fromString(String in){
            for (LG_AC_HORIZONTAL_SWING val : LG_AC_HORIZONTAL_SWING.values()){
                if(val.text.equals(in)){
                    return val;
                }
            }
            return LG_AC_HORIZONTAL_SWING.NONE;
        }

        @NonNull
        @Override
        public String toString() {
            return this.text;
        }
    }

    public enum LG_AC_ENERGY {
        ENERGY_TURBO("TURBO"),
        ENERGY_100("100"),
        ENERGY_80("80"),
        ENERGY_60("60"),
        ENERGY_40("40");

        private final String text;

        /**
         * @param text
         */
        LG_AC_ENERGY(final String text) {
            this.text = text;
        }

        static LG_AC_ENERGY fromString(String in){
            for (LG_AC_ENERGY val : LG_AC_ENERGY.values()){
                if(val.text.equals(in)){
                    return val;
                }
            }
            return LG_AC_ENERGY.ENERGY_100;
        }

        @NonNull
        @Override
        public String toString() {
            return this.text;
        }
    }

    final int MIN_TEMP = 18;
    final int MAX_TEMP = 30;

    SwitchCompat stateSwitch;
    SeekBar fanSeekbar;
    HorizontalWheelView wheelView;
    TextView tempText;
    TextView tempInText;
    TextView tempOutText;
    Spinner energySpinner;
    RadioGroup acmodeRadiogroup;
    SeekBar swingVSeekbar;
    SeekBar swingHSeekbar;

    boolean current_state;
    int current_temp;
    LG_AC_MODE current_mode;
    LG_AC_ENERGY current_energy;
    LG_AC_FAN current_fan;
    LG_AC_VERTICAL_SWING current_swing_v;
    LG_AC_HORIZONTAL_SWING current_swing_h;


    @SuppressLint("ClickableViewAccessibility")
    public CryptoAC(DeviceManagerDevice device, Context context) {
        super(device, context, R.layout.device_cryptoac);
        stateSwitch = rootview.findViewById(R.id.state_switch);
        wheelView = rootview.findViewById(R.id.temp_wheel);
        tempText = rootview.findViewById(R.id.temp_text);
        tempInText = rootview.findViewById(R.id.temp_in_text);
        tempOutText = rootview.findViewById(R.id.temp_out_text);
        energySpinner = rootview.findViewById(R.id.energy_spinner);
        acmodeRadiogroup = rootview.findViewById(R.id.acmode_radiogroup);
        fanSeekbar = rootview.findViewById(R.id.fan_seekbar);
        swingVSeekbar = rootview.findViewById(R.id.swing_v_seekbar);
        swingHSeekbar = rootview.findViewById(R.id.swing_h_seekbar);

        stateSwitch.setOnClickListener((view) -> {
            setState(((SwitchCompat)view).isChecked());
        });

        wheelView.setOnTouchListener((v, event) -> {
            // Disallow the touch request for parent scroll on touch of  child view
            rootview.getParent().getParent().requestDisallowInterceptTouchEvent(true);
            wheelView.setPressed(true);
            return false;
        });

        wheelView.setListener(new HorizontalWheelView.Listener() {
            @Override
            public void onRotationChanged(double radians) {
                //super.onRotationChanged(radians);
                int value = (int)Math.round(MIN_TEMP + (wheelView.getCompleteTurnFraction()*(MAX_TEMP-MIN_TEMP)));
                tempText.setText(String.format(Locale.GERMANY,"%d°C", value));
                tempText.setTextColor(Color.HSVToColor(new float[]{240f - (float)wheelView.getCompleteTurnFraction() * 240, 1, 1}));
            }

            @Override
            public void onScrollStateChanged(int state) {
                super.onScrollStateChanged(state);
                if(state == 0){
                    int value = (int)Math.round(MIN_TEMP + (wheelView.getCompleteTurnFraction()*(MAX_TEMP-MIN_TEMP)));
                    setTemp(value);
                }
            }
        });
        energySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LG_AC_ENERGY energy = LG_AC_ENERGY.values()[position];
                if(current_energy != null && !energy.equals(current_energy)) {
                    if (energy.equals(LG_AC_ENERGY.ENERGY_TURBO)) {
                        setTurbo(true);
                    } else {
                        setEnergy(energy);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        acmodeRadiogroup.setOnCheckedChangeListener((group, checkedId) -> {
            LG_AC_MODE mode = LG_AC_MODE.valueOf(((RadioButton) group.findViewById(checkedId)).getText().toString());
            if(!mode.equals(current_mode)) {
                setMode(mode);
            }
        });
        fanSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                setFan(LG_AC_FAN.values()[seekBar.getProgress()]);
            }
        });

        swingVSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                setVerticalSwing(LG_AC_VERTICAL_SWING.values()[seekBar.getProgress()]);
            }
        });

        swingHSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                setHorizontalSwing(LG_AC_HORIZONTAL_SWING.values()[seekBar.getProgress()]);
            }
        });

    }

    public void setState(boolean val){
        skipNextUpdate();
        cc.sendMessageEncrypted("AC:state:" + (val ? "1" : "0"), CryptCon.Mode.UDP, new CryptConReceiver() {
            @Override
            public void onSuccess(Content response){

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

    public void setMode(LG_AC_MODE val){
        skipNextUpdate();
        cc.sendMessageEncrypted("AC:mode:" + val, CryptCon.Mode.UDP, new CryptConReceiver() {
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

    public void setTemp(int val){
        skipNextUpdate();
        cc.sendMessageEncrypted("AC:temp:" + val, CryptCon.Mode.UDP, new CryptConReceiver() {
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

    public void setFan(LG_AC_FAN val){
        skipNextUpdate();
        cc.sendMessageEncrypted("AC:fan:" + val, CryptCon.Mode.UDP, new CryptConReceiver() {
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

    public void setEnergy(LG_AC_ENERGY val){
        skipNextUpdate();
        cc.sendMessageEncrypted("AC:energy:" + val, CryptCon.Mode.UDP, new CryptConReceiver() {
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

    public void setTurbo(boolean val){
        skipNextUpdate();
        cc.sendMessageEncrypted("AC:turbo:" + (val ? "1" : "0"), CryptCon.Mode.UDP, new CryptConReceiver() {
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
        cc.sendMessageEncrypted("AC:get",  CryptCon.Mode.UDP, new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    titleText.setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
                    try {
                        JSONObject jsonObject = new JSONObject(response.data);
                        current_state = jsonObject.getInt("state") == 1;
                        current_temp = jsonObject.getInt("temp");
                        current_mode = LG_AC_MODE.valueOf(jsonObject.getString("mode"));
                        if(jsonObject.getInt("turbo") == 1){
                            current_energy = LG_AC_ENERGY.ENERGY_TURBO;
                        } else {
                            current_energy = LG_AC_ENERGY.fromString(jsonObject.getString("energy"));
                        }
                        current_fan = LG_AC_FAN.valueOf(jsonObject.getString("fan"));
                        current_swing_v = LG_AC_VERTICAL_SWING.fromString(jsonObject.getString("swing_v"));
                        current_swing_h = LG_AC_HORIZONTAL_SWING.fromString(jsonObject.getString("swing_h"));
                        double current_temp_in = jsonObject.getDouble("temp_in");
                        double current_temp_out = jsonObject.getDouble("temp_out");

                        stateSwitch.setChecked(current_state);
                        ((RadioButton)(acmodeRadiogroup.findViewWithTag(current_mode.toString()))).setChecked(true);
                        if(!wheelView.isPressed()) {
                            double temp_fraction = (double) (current_temp - MIN_TEMP) / (double) (MAX_TEMP - MIN_TEMP);
                            //wheelView.setCompleteTurnFraction(temp_fraction);
                            WheelAnimation animation = new WheelAnimation(wheelView, wheelView.getCompleteTurnFraction(), temp_fraction);
                            animation.setDuration(500);
                            wheelView.startAnimation(animation);
                        }

                        tempInText.setText(String.format(Locale.getDefault(), "%.1f°C", current_temp_in));
                        tempOutText.setText(String.format(Locale.getDefault(), "%.1f°C", current_temp_out));
                        tempInText.setTextColor(Color.HSVToColor(new float[]{clamp(240f - (float)(current_temp_in / 100) * 3 * 240, 0, 240), 1, 1}));
                        tempOutText.setTextColor(Color.HSVToColor(new float[]{clamp(240f - (float)(current_temp_out / 100) * 3 * 240,0,240), 1, 1}));


                        if(current_energy.equals(LG_AC_ENERGY.ENERGY_TURBO)) {
                            energySpinner.setSelection(0,true);
                        } else {
                            energySpinner.setSelection(current_energy.ordinal(), true);
                        }
                        if(!fanSeekbar.isPressed()) {
                            fanSeekbar.setProgress(current_fan.ordinal());
                        }
                        if(!swingVSeekbar.isPressed()) {
                            swingVSeekbar.setProgress(current_swing_v.ordinal());
                        }
                        if(!swingHSeekbar.isPressed()) {
                            swingHSeekbar.setProgress(current_swing_h.ordinal());
                        }
                    } catch (Exception x){
                        x.printStackTrace();
                    }
                });
            }

            @Override
            public void onFail() {
                new Handler(Looper.getMainLooper()).post(() -> titleText.setTextColor(ContextCompat.getColor(context, R.color.colorRed)));
            }

            @Override
            public void onFinished() {

            }

            @Override
            public void onProgress(String sprogress, int iprogress) {

            }
        });
    }

    public void setVerticalSwing(LG_AC_VERTICAL_SWING swing){
        skipNextUpdate();
        cc.sendMessageEncrypted("AC:swing_v:" + swing, CryptCon.Mode.UDP, new CryptConReceiver() {
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

    public void setHorizontalSwing(LG_AC_HORIZONTAL_SWING swing){
        skipNextUpdate();
        cc.sendMessageEncrypted("AC:swing_h:" + swing, CryptCon.Mode.UDP, new CryptConReceiver() {
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

    public static class WheelAnimation extends Animation {
        private HorizontalWheelView wheel;
        private double from;
        private double  to;

        public WheelAnimation(HorizontalWheelView wheel, double from, double to) {
            super();
            this.wheel = wheel;
            this.from = from;
            this.to = to;
            this.setInterpolator(new AccelerateDecelerateInterpolator());
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
            double value = from + (to - from) * interpolatedTime;
            wheel.setCompleteTurnFraction(value);
        }

    }
}
