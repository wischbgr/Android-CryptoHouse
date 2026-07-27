package de.wladimircomputin.cryptohouse.device;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import de.wladimircomputin.cryptohouse.R;
import de.wladimircomputin.cryptohouse.devicemanager.DeviceManagerDevice;
import de.wladimircomputin.libcryptoiot.v2.protocol.Content;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptCon;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptConReceiver;

public class CryptoGarage extends ACryptoDevice{

    enum GateState {
        GATE_NONE("GATE_NONE", R.drawable.unknown),
        GATE_CLOSED("GATE_CLOSED", R.drawable.closed),
        GATE_OPENING("GATE_OPENING", R.drawable.opening),
        GATE_OPEN("GATE_OPEN", R.drawable.open),
        GATE_CLOSING("GATE_CLOSING", R.drawable.closing),
        GATE_STOPPED_OPENING("GATE_STOPPED_OPENING",R.drawable.opening_stopped),
        GATE_STOPPED_CLOSING("GATE_STOPPED_CLOSING", R.drawable.closing_stopped);

        private String value;
        private int icon;

        GateState(String value, int icon) {
            this.value = value;
            this.icon = icon;
        }

        @NonNull
        @Override
        public String toString() {
            return value;
        }

        public int getIcon(){
            return icon;
        }
    }

    Button triggerButton;
    ImageView statusImageView;

    GateState currentGateState;

    public CryptoGarage(DeviceManagerDevice device, Context context) {
        super(device, context, R.layout.device_cryptogarage);

        triggerButton = rootview.findViewById(R.id.button);
        statusImageView = rootview.findViewById(R.id.statusImageView);

        triggerButton.setOnClickListener((view) -> {
            trigger();
        });

        statusImageView.setOnClickListener((view) -> {
            update();
        });
    }


    @Override
    public void update() {
        cc.sendMessageEncrypted("Garage:gatestate", CryptCon.Mode.UDP, new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    titleText.setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
                    GateState gateState = GateState.valueOf(response.data);
                    if(!gateState.equals(currentGateState)) {
                        AnimatorSet animatorSet = (AnimatorSet) AnimatorInflater.loadAnimator(context, R.animator.gate_state_transition_1);
                        animatorSet.setTarget(statusImageView);
                        animatorSet.start();
                        animatorSet.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(@NonNull Animator animation, boolean isReverse) {
                                statusImageView.setImageDrawable(context.getDrawable(gateState.getIcon()));
                                AnimatorSet animatorSet = (AnimatorSet) AnimatorInflater.loadAnimator(context, R.animator.gate_state_transition_2);
                                animatorSet.setTarget(statusImageView);
                                animatorSet.start();
                            }
                        });
                        currentGateState = gateState;
                    }
                });
            }

            @Override
            public void onFail() {
                new Handler(Looper.getMainLooper()).post(() -> {
                    statusImageView.setImageDrawable(context.getDrawable(GateState.GATE_NONE.getIcon()));
                    titleText.setTextColor(ContextCompat.getColor(context, R.color.colorRed));
                });
                currentGateState = GateState.GATE_NONE;
            }

            @Override
            public void onFinished() {

            }

            @Override
            public void onProgress(String sprogress, int iprogress) {

            }
        });
    }

    public void trigger(){
        skipNextUpdate();
        cc.sendMessageEncrypted("Garage:trigger", CryptCon.Mode.UDP, new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
                update();
            }

            @Override
            public void onFail() {}

            @Override
            public void onFinished() {}

            @Override
            public void onProgress(String sprogress, int iprogress) {}
        });
    }
}
