package de.wladimircomputin.cryptohouse.device;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import de.wladimircomputin.cryptohouse.R;
import de.wladimircomputin.cryptohouse.devicemanager.DeviceManagerDevice;
import de.wladimircomputin.libcryptoiot.v2.protocol.Content;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptCon;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptConReceiver;
import de.wladimircomputin.libcryptoiot.v2.protocol.MessageType;

public class CryptoGeneric extends ACryptoDevice{

    EditText editTextCommand;
    Button buttonCommandSend;

    public CryptoGeneric(DeviceManagerDevice device, Context context) {
        super(device, context, R.layout.device_cryptogeneric);

        editTextCommand = rootview.findViewById(R.id.editTextCommand);
        buttonCommandSend = rootview.findViewById(R.id.buttonCommandSend);

        buttonCommandSend.setOnClickListener((view) -> {
            cc.sendMessageEncrypted(editTextCommand.getText().toString(), new CryptConReceiver() {
                @Override
                public void onSuccess(Content response) {
                    if(response.code.equals(MessageType.DATA)) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
                            builder1.setMessage(response.data);
                            builder1.setCancelable(true);
                            builder1.setTitle(context.getString(R.string.response));
                            builder1.show();
                        });
                    } else if(response.code.equals(MessageType.ACK)) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(context, "ACK", Toast.LENGTH_SHORT).show();
                        });
                    }
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
    }

    @Override
    public void update() {
        cc.sendMessageEncrypted("ping", CryptCon.Mode.UDP, new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    titleText.setTextColor(context.getResources().getColor(R.color.colorAccent));
                });
            }

            @Override
            public void onFail() {
                new Handler(Looper.getMainLooper()).post(() -> {
                    titleText.setTextColor(context.getResources().getColor(R.color.colorRed));
                });
            }

            @Override
            public void onFinished() {}

            @Override
            public void onProgress(String sprogress, int iprogress) {}
        });
    }
}
