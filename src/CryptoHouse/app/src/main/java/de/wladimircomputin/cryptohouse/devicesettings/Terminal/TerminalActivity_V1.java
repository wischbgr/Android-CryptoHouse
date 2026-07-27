package de.wladimircomputin.cryptohouse.devicesettings.Terminal;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

import de.wladimircomputin.cryptohouse.R;
import de.wladimircomputin.cryptohouse.devicemanager.DeviceManagerDevice;
import de.wladimircomputin.libcryptoiot.v1.protocol.Content;
import de.wladimircomputin.libcryptoiot.v1.protocol.CryptCon;
import de.wladimircomputin.libcryptoiot.v1.protocol.CryptConReceiver;

public class TerminalActivity_V1 extends AppCompatActivity {
    Toolbar toolbar;
    SharedPreferences sharedPref;
    DeviceManagerDevice device;
    CryptCon cc;
    ScrollView terminalScroll;
    TextView terminalText;
    AutoCompleteTextView terminalPromptText;
    Button terminalLeft;
    Button terminalRight;
    Button terminalDown;
    Button terminalUp;
    Button terminalColon;

    List<String> history;
    int historyPos = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_terminal);
        //toolbar = findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        terminalScroll = findViewById(R.id.terminalScroll);
        terminalText = findViewById(R.id.terminalText);
        terminalPromptText = findViewById(R.id.terminalPromptText);
        terminalLeft = findViewById(R.id.terminalLeft);
        terminalRight = findViewById(R.id.terminalRight);
        terminalDown = findViewById(R.id.terminalDown);
        terminalUp = findViewById(R.id.terminalUp);
        terminalColon = findViewById(R.id.terminalColon);

        history = new ArrayList<>();
        history.add("");

        Intent intent = getIntent();
        String id = intent.getStringExtra("id");
        String name = intent.getStringExtra("name");
        String ip = intent.getStringExtra("ip");
        String pass = intent.getStringExtra("pass");
        String type = intent.getStringExtra("type");
        device = new DeviceManagerDevice(id, name, type, name, ip, "", pass, true, "");
        this.cc = new CryptCon(pass, device.ip);

        setTitle(device.name + " " + getString(R.string.terminal));
        terminalText.setText(device.name + "-># ");

        terminalPromptText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    AutoCompleteTextView editText = ((AutoCompleteTextView) v);
                    terminalText.append(editText.getText().toString());
                    cc.sendMessageEncrypted(editText.getText().toString(), CryptCon.Mode.UDP, new CryptConReceiver() {
                        @Override
                        public void onSuccess(Content response) {
                            runOnUiThread(() -> {
                                terminalText.append(editText.getText().toString() + "\n" + response.data);
                            });

                        }

                        @Override
                        public void onFail() {

                        }

                        @Override
                        public void onFinished() {
                            runOnUiThread(() -> {
                                terminalText.append("\n\n" + device.name + "-># ");
                                terminalScroll.post(() -> {
                                    terminalScroll.fullScroll(View.FOCUS_DOWN);
                                });
                            });
                        }

                        @Override
                        public void onProgress(String sprogress, int iprogress) {

                        }
                    });
                    history.remove(terminalPromptText.getText().toString());
                    history.add(1, terminalPromptText.getText().toString());
                    historyPos = 0;
                    terminalPromptText.setText("");

                    return true;
                }
                return false;
            }
        });

        terminalLeft.setOnClickListener(v -> {
            terminalPromptText.setSelection(terminalPromptText.getSelectionStart()-1);
        });

        terminalRight.setOnClickListener(v -> {
            terminalPromptText.setSelection(terminalPromptText.getSelectionStart()+1);
        });

        terminalDown.setOnClickListener(v -> {
            if(historyPos > 0) {
                terminalPromptText.setText(history.get(--historyPos));
            }
        });

        terminalUp.setOnClickListener(v -> {
            if(historyPos < history.size()-1) {
                terminalPromptText.setText(history.get(++historyPos));
            }
        });

        terminalColon.setOnClickListener(v -> {
            terminalPromptText.append(":");
        });
    }

    @Override
    public void onResume(){
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected

            case android.R.id.home:
                finish();
                break;

            default:
                break;
        }
        return true;
    }
}