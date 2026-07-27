package de.wladimircomputin.cryptohouse.device;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.ncorti.slidetoact.SlideToActView;

import org.json.JSONObject;

import de.wladimircomputin.cryptohouse.R;
import de.wladimircomputin.cryptohouse.devicemanager.DeviceManagerDevice;
import de.wladimircomputin.libcryptoiot.v2.protocol.Content;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptConReceiver;

public class DoorLock extends ACryptoDevice{
    enum LockState {
        STATUSPENDING(-1, R.drawable.unknown),
        UNKNOWN(0, R.drawable.unknown),
        MOVING(1, R.drawable.baseline_autorenew_24),
        UNLOCKED(2, R.drawable.baseline_lock_open_24),
        LOCKED(3, R.drawable.baseline_lock_outline_24),
        OPENED(4, R.drawable.baseline_login_24),
        TIMEOUT(9, R.drawable.unknown);

        private int value;
        private int icon;

        LockState(int value, int icon) {
            this.value = value;
            this.icon = icon;
         }

        static LockState fromInt(int value){
            for(LockState lockState : LockState.values()){
                if(lockState.value == value){
                    return lockState;
                }
            }
            return UNKNOWN;
        }

        public int getIcon(){
            return icon;
        }
    }

    public enum AutoRelock {

        AUTO_RELOCK_DISABLED(0, "DOOR_STATUSPENDING", R.drawable.unknown),
        AUTO_RELOCK_ENABLED(1, "DOOR_UNKNOWN", R.drawable.unknown),
        AUTO_RELOCK_ENABLED_BY_PROG(2, "DOOR_MOVING", R.drawable.baseline_autorenew_24);

        private int code;
        private String value;
        private int icon;

        AutoRelock(int code, String value, int icon) {
            this.code = code;
            this.value = value;
            this.icon = icon;
        }

        public static AutoRelock fromCode(int code){
            for(AutoRelock d : AutoRelock.values()){
                if (d.code == code){
                    return d;
                }
            }
            return AUTO_RELOCK_DISABLED;
        }

        @Override
        public String toString() {
            return value;
        }

        public int getIcon(){
            return icon;
        }

        public int toCode(){
            return code;
        }
    }

    Button lockButton;
    Button unlockButton;
    SlideToActView openSlide;
    ImageView statusImageView;
    ImageView BatteryImageView;

    DoorLock.LockState currentLockState;

    public DoorLock(DeviceManagerDevice device, Context context) {
        super(device, context,  R.layout.device_doorlock);
        lockButton = rootview.findViewById(R.id.doorlockLockButton);
        unlockButton = rootview.findViewById(R.id.doorlockUnlockButton);
        openSlide = rootview.findViewById(R.id.doorlockOpenSlide);
        statusImageView = rootview.findViewById(R.id.doorlockStatusImage);
        BatteryImageView = rootview.findViewById(R.id.doorlockBatteryImage);

        lockButton.setOnClickListener(v -> {
            cc.sendMessageEncrypted("EQ3:lock", new CryptConReceiver() {
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

        unlockButton.setOnClickListener(v -> {
            cc.sendMessageEncrypted("EQ3:unlock", new CryptConReceiver() {
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

        openSlide.setOnSlideToActAnimationEventListener(new SlideToActView.OnSlideToActAnimationEventListener() {
            @Override
            public void onSlideCompleteAnimationStarted(@NonNull SlideToActView slideToActView, float v) {
                cc.sendMessageEncrypted("EQ3:open", new CryptConReceiver() {
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
            public void onSlideCompleteAnimationEnded(@NonNull SlideToActView slideToActView) {
                slideToActView.setCompleted(false, true);
            }

            @Override
            public void onSlideResetAnimationStarted(@NonNull SlideToActView slideToActView) {

            }

            @Override
            public void onSlideResetAnimationEnded(@NonNull SlideToActView slideToActView) {

            }
        });
    }

    @Override
    public void update() {
        cc.sendMessageEncrypted("EQ3:state", new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    try{
                        JSONObject jsonObject = new JSONObject(response.data);
                        titleText.setTextColor(context.getResources().getColor(R.color.colorAccent));
                        DoorLock.LockState lockState = DoorLock.LockState.fromInt(jsonObject.getInt("LockState"));
                        if(jsonObject.getInt("BatteryState") == 1){
                            BatteryImageView.setVisibility(View.VISIBLE);
                        } else {
                            BatteryImageView.setVisibility(View.GONE);
                        }
                        if(!lockState.equals(currentLockState)) {
                            AnimatorSet animatorSet = (AnimatorSet) AnimatorInflater.loadAnimator(context, R.animator.gate_state_transition_1);
                            animatorSet.setTarget(statusImageView);
                            animatorSet.start();
                            animatorSet.addListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(@NonNull Animator animation, boolean isReverse) {
                                    statusImageView.setImageDrawable(context.getDrawable(lockState.getIcon()));
                                    AnimatorSet animatorSet = (AnimatorSet) AnimatorInflater.loadAnimator(context, R.animator.gate_state_transition_2);
                                    animatorSet.setTarget(statusImageView);
                                    animatorSet.start();
                                }
                            });
                            currentLockState = lockState;
                        }
                    } catch (Exception x) {
                        x.printStackTrace();
                    }

                });
            }

            @Override
            public void onFail() {
                new Handler(Looper.getMainLooper()).post(() -> {
                    statusImageView.setImageDrawable(context.getDrawable(LockState.UNKNOWN.getIcon()));
                    titleText.setTextColor(context.getResources().getColor(R.color.colorRed));
                });
                currentLockState = LockState.UNKNOWN;
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
