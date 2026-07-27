package de.wladimircomputin.cryptohouse.device;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.wladimircomputin.cryptohouse.R;
import de.wladimircomputin.cryptohouse.devicemanager.DeviceManagerDevice;
import de.wladimircomputin.libcryptoiot.v2.protocol.Content;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptCon;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptConBulkReceiver;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptConReceiver;

public abstract class ACryptoDimmer extends ACryptoDevice{

    List<SeekBar> seekBars;

    public ACryptoDimmer(DeviceManagerDevice device, Context context, int layoutfile) {
        super(device, context, layoutfile);

        seekBars = new ArrayList<>();
        // each SeekBar sits in its own row, alongside a "<" and ">" jump-to-min/max button
        int num = ((ViewGroup)genericDeviceView).getChildCount();
        for(int i = 0; i < num; i++){
            View row = ((ViewGroup) genericDeviceView).getChildAt(i);
            if (!(row instanceof ViewGroup)) {
                continue;
            }
            ViewGroup rowGroup = (ViewGroup) row;

            SeekBar seekBar = null;
            TextView minButton = null;
            TextView maxButton = null;
            for (int j = 0; j < rowGroup.getChildCount(); j++) {
                View child = rowGroup.getChildAt(j);
                if (child instanceof SeekBar) {
                    seekBar = (SeekBar) child;
                } else if (child instanceof TextView) {
                    if (minButton == null) {
                        minButton = (TextView) child;
                    } else {
                        maxButton = (TextView) child;
                    }
                }
            }

            if (seekBar == null) {
                continue;
            }
            seekBar.setTag("Dimmer" + (num == 1 ? "" : (i+1)));
            seekBars.add(seekBar);

            SeekBar finalSeekBar = seekBar;
            if (minButton != null) {
                minButton.setOnClickListener(v -> jumpTo(finalSeekBar, 0));
            }
            if (maxButton != null) {
                maxButton.setOnClickListener(v -> jumpTo(finalSeekBar, finalSeekBar.getMax()));
            }
        }

        for (SeekBar seekBar : seekBars) {
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    Timer timer = new Timer();
                    final int[] last_pos = {seekBar.getProgress()};
                    final boolean[] dimmed = {false};
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if (seekBar.isPressed()) {
                                if (Math.abs(last_pos[0] - seekBar.getProgress()) <= 10) {
                                    if (!dimmed[0]) {
                                        dimfade(seekBar.getTag().toString(), (double) seekBar.getProgress() / 1000, 1000);
                                    }
                                    dimmed[0] = true;
                                } else {
                                    dimmed[0] = false;
                                }
                            } else {
                                cancel();
                            }
                            last_pos[0] = seekBar.getProgress();
                        }
                    }, 100, 100);
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    dimfade(seekBar.getTag().toString(), (double) seekBar.getProgress() / 1000, 2000);
                }
            });
        }
    }

    @Override
    public void update() {
        List<SeekBar> toUpdate = new ArrayList<>();
        for (SeekBar seekBar : seekBars) {
            if (!seekBar.isPressed()) {
                toUpdate.add(seekBar);
            }
        }

        if(!toUpdate.isEmpty()) {
            String[] commands = new String[toUpdate.size()];
            for (int i = 0; i < commands.length; i++) {
                commands[i] = toUpdate.get(i).getTag().toString() + ":dimstate";
            }
            cc.sendMessageEncryptedBulk(commands, CryptCon.Mode.UDP, new CryptConBulkReceiver() {
                @Override
                public void onSuccess(Content response, int i) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        titleText.setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
                        if (!toUpdate.get(i).isPressed()) {
                            toUpdate.get(i).setProgress((int) (1000 * Double.parseDouble(response.data)), true);
                            toUpdate.get(i).jumpDrawablesToCurrentState();
                        }
                    });
                }

                @Override
                public void onFail(int i) {
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

    private void jumpTo(SeekBar seekBar, int progress) {
        seekBar.setProgress(progress, true);
        seekBar.setPressed(false);
        seekBar.jumpDrawablesToCurrentState();
        dimfade(seekBar.getTag().toString(), (double) progress / 1000, 2000);
    }

    public void dimfade(String device, double val, int millis){
        skipNextUpdate();
        cc.sendMessageEncrypted(device + ":" + "dimfade:" + val + ":" + millis, CryptCon.Mode.UDP, new CryptConReceiver() {
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
}
