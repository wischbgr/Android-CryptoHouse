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

public class CryptoAC_TCL extends ACryptoDevice{

    public enum TCL_AC_MODE {
        COOL("COOL"),
        HEAT("HEAT"),
        DRY("DRY"),
        FAN("FAN"),
        AUTO("AUTO");
        private final String text;

        /**
         * @param text
         */
        TCL_AC_MODE(final String text) {
            this.text = text;
        }

        @NonNull
        @Override
        public String toString() {
            return this.text;
        }
    }

    public enum TCL_AC_FAN {
        F1("F1"),
        F2("F2"),
        F3("F3"),
        F4("F4"),
        F5("F5"),
        F6("F6"),
        F7("F7"),
        AUTO("AUTO");

        private final String text;

        /**
         * @param text
         */
        TCL_AC_FAN(final String text) {
            this.text = text;
        }

        @NonNull
        @Override
        public String toString() {
            return this.text;
        }
    }

    public enum TCL_AC_VERTICAL_SWING {
        S1("FIX1"),
        S2("FIX2"),
        S3("FIX3"),
        S4("FIX4"),
        S5("FIX5"),
        SU("UPPER"),
        SD("LOWER"),
        SF("FULL"),
        AUTO("AUTO"),
        NONE("NONE");

        private final String text;

        /**
         * @param text
         */
        TCL_AC_VERTICAL_SWING(final String text) {
            this.text = text;
        }

        static TCL_AC_VERTICAL_SWING fromString(String in){
            for (TCL_AC_VERTICAL_SWING val : TCL_AC_VERTICAL_SWING.values()){
                if(val.text.equals(in)){
                    return val;
                }
            }
            return TCL_AC_VERTICAL_SWING.NONE;
        }

        @NonNull
        @Override
        public String toString() {
            return this.text;
        }
    }

    public enum TCL_AC_HORIZONTAL_SWING {
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
        TCL_AC_HORIZONTAL_SWING(final String text) {
            this.text = text;
        }

        static TCL_AC_HORIZONTAL_SWING fromString(String in){
            for (TCL_AC_HORIZONTAL_SWING val : TCL_AC_HORIZONTAL_SWING.values()){
                if(val.text.equals(in)){
                    return val;
                }
            }
            return TCL_AC_HORIZONTAL_SWING.NONE;
        }

        @NonNull
        @Override
        public String toString() {
            return this.text;
        }
    }

    public enum TCL_AC_PRESET {
        PRESET_NONE("NONE"),
        PRESET_SILENT("SILENT"),
        PRESET_TURBO("TURBO"),
        PRESET_ECO("ECO"),
        PRESET_NO_FREEZE("NO-FREEZE");

        private final String text;

        /**
         * @param text
         */
        TCL_AC_PRESET(final String text) {
            this.text = text;
        }

        static TCL_AC_PRESET fromString(String in){
            for (TCL_AC_PRESET val : TCL_AC_PRESET.values()){
                if(val.text.equals(in)){
                    return val;
                }
            }
            return TCL_AC_PRESET.PRESET_NONE;
        }

        @NonNull
        @Override
        public String toString() {
            return this.text;
        }
    }

    final int MIN_TEMP = 16;
    final int MAX_TEMP = 31;

    SwitchCompat stateSwitch;
    SeekBar fanSeekbar;
    HorizontalWheelView wheelView;
    TextView tempText;
    TextView tempOutsideInText;
    TextView tempOutsideOutText;
    TextView tempInsideInText;
    TextView tempInsideOutText;
    Spinner presetSpinner;
    RadioGroup acmodeRadiogroup;
    SeekBar swingVSeekbar;

    boolean current_state;
    int current_temp;
    TCL_AC_MODE current_mode;
    TCL_AC_PRESET current_preset;
    TCL_AC_FAN current_fan;
    TCL_AC_VERTICAL_SWING current_swing_v;
    TCL_AC_HORIZONTAL_SWING current_swing_h;


    @SuppressLint("ClickableViewAccessibility")
    public CryptoAC_TCL(DeviceManagerDevice device, Context context) {
        super(device, context, R.layout.device_cryptoac_tcl);
        stateSwitch = rootview.findViewById(R.id.state_switch);
        wheelView = rootview.findViewById(R.id.temp_wheel);
        tempText = rootview.findViewById(R.id.temp_text);
        tempOutsideInText = rootview.findViewById(R.id.temp_outside_in_text);
        tempOutsideOutText = rootview.findViewById(R.id.temp_outside_out_text);
        tempInsideInText = rootview.findViewById(R.id.temp_inside_in_text);
        tempInsideOutText = rootview.findViewById(R.id.temp_inside_out_text);
        presetSpinner = rootview.findViewById(R.id.preset_spinner);
        acmodeRadiogroup = rootview.findViewById(R.id.acmode_radiogroup);
        fanSeekbar = rootview.findViewById(R.id.fan_seekbar);
        swingVSeekbar = rootview.findViewById(R.id.swing_v_seekbar);

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
        presetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TCL_AC_PRESET preset = TCL_AC_PRESET.values()[position];
                if(current_preset != null && !preset.equals(current_preset)) {
                    setPreset(preset);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        acmodeRadiogroup.setOnCheckedChangeListener((group, checkedId) -> {
            TCL_AC_MODE mode = TCL_AC_MODE.valueOf(((RadioButton) group.findViewById(checkedId)).getText().toString());
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
                setFan(TCL_AC_FAN.values()[seekBar.getProgress()]);
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
                setVerticalSwing(TCL_AC_VERTICAL_SWING.values()[seekBar.getProgress()]);
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

    public void setMode(TCL_AC_MODE val){
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

    public void setFan(TCL_AC_FAN val){
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

    public void setPreset(TCL_AC_PRESET val){
        skipNextUpdate();
        cc.sendMessageEncrypted("AC:preset:" + val, CryptCon.Mode.UDP, new CryptConReceiver() {
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

    public void setVerticalSwing(TCL_AC_VERTICAL_SWING swing){
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

    @Override
    public void update() {
        skipNextUpdate();
        cc.sendMessageEncrypted("AC:get",  CryptCon.Mode.UDP, new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    titleText.setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
                    try {
                        JSONObject jsonObject = new JSONObject(response.data);
                        current_state = jsonObject.getInt("state") == 1;
                        current_temp = jsonObject.getInt("temp");
                        current_mode = TCL_AC_MODE.valueOf(jsonObject.getString("mode"));
                        current_preset = TCL_AC_PRESET.fromString(jsonObject.getString("preset"));
                        current_fan = TCL_AC_FAN.valueOf(jsonObject.getString("fan"));
                        current_swing_v = TCL_AC_VERTICAL_SWING.fromString(jsonObject.getString("swing_v"));
                        //current_swing_h = TCL_AC_HORIZONTAL_SWING.fromString(jsonObject.getString("swing_h"));
                        int current_temp_outside_in = jsonObject.getJSONObject("info").getInt("temp_outside_in");
                        int current_temp_outside_out = jsonObject.getJSONObject("info").getInt("temp_outside_out");
                        int current_temp_inside_in = jsonObject.getJSONObject("info").getInt("temp_inside_in");
                        int current_temp_inside_out = jsonObject.getJSONObject("info").getInt("temp_inside_out");

                        stateSwitch.setChecked(current_state);
                        ((RadioButton)(acmodeRadiogroup.findViewWithTag(current_mode.toString()))).setChecked(true);
                        if(!wheelView.isPressed()) {
                            double temp_fraction = (double) (current_temp - MIN_TEMP) / (double) (MAX_TEMP - MIN_TEMP);
                            //wheelView.setCompleteTurnFraction(temp_fraction);
                            WheelAnimation animation = new WheelAnimation(wheelView, wheelView.getCompleteTurnFraction(), temp_fraction);
                            animation.setDuration(500);
                            wheelView.startAnimation(animation);
                        }

                        tempOutsideInText.setText(String.format(Locale.getDefault(), "%d°C", current_temp_outside_in));
                        tempOutsideOutText.setText(String.format(Locale.getDefault(), "%d°C", current_temp_outside_out));
                        tempInsideInText.setText(String.format(Locale.getDefault(), "%d°C", current_temp_inside_in));
                        tempInsideOutText.setText(String.format(Locale.getDefault(), "%d°C", current_temp_inside_out));
                        tempOutsideInText.setTextColor(Color.HSVToColor(new float[]{clamp(240f - ((float)(current_temp_outside_in) / 100) * 3 * 240, 0, 240), 1, 1}));
                        tempOutsideOutText.setTextColor(Color.HSVToColor(new float[]{clamp(240f - ((float)(current_temp_outside_out) / 100) * 3 * 240,0,240), 1, 1}));
                        tempInsideInText.setTextColor(Color.HSVToColor(new float[]{clamp(240f - ((float)(current_temp_inside_in) / 100) * 3 * 240, 0, 240), 1, 1}));
                        tempInsideOutText.setTextColor(Color.HSVToColor(new float[]{clamp(240f - ((float)(current_temp_inside_out) / 100) * 3 * 240,0,240), 1, 1}));

                        presetSpinner.setSelection(current_preset.ordinal(), true);
                        if(!fanSeekbar.isPressed()) {
                            fanSeekbar.setProgress(current_fan.ordinal());
                        }
                        if(!swingVSeekbar.isPressed()) {
                            swingVSeekbar.setProgress(current_swing_v.ordinal());
                        }
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
