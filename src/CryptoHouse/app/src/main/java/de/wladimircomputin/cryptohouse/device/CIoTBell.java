package de.wladimircomputin.cryptohouse.device;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;

import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import de.wladimircomputin.cryptohouse.R;
import de.wladimircomputin.cryptohouse.devicemanager.DeviceManagerDevice;
import de.wladimircomputin.libcryptoiot.v2.protocol.Content;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptCon;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptConReceiver;

public class CIoTBell extends ACryptoDevice{
    Button ringButton;

    public CIoTBell(DeviceManagerDevice device, Context context) {
        super(device, context, R.layout.device_ciotbell);
        ringButton = rootview.findViewById(R.id.ciotbell_button);
        ringButton.setOnClickListener(v -> {
            ring();
        });
    }

    @Override
    public void update() {
        cc.sendMessageEncrypted("ping", CryptCon.Mode.UDP, new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    titleText.setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
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

    public void ring(){
        skipNextUpdate();
        cc.sendMessageEncrypted("Bell:ring", CryptCon.Mode.UDP, new CryptConReceiver() {
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
}
