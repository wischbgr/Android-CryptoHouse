package de.wladimircomputin.cryptohouse.assistant;

import static de.wladimircomputin.libcryptoiot.v2.Constants.ap_ip_default;
import static de.wladimircomputin.libcryptoiot.v2.Constants.command_discover;
import static de.wladimircomputin.libcryptoiot.v2.Constants.command_reboot;
import static de.wladimircomputin.libcryptoiot.v2.Constants.command_wifiresults;
import static de.wladimircomputin.libcryptoiot.v2.Constants.command_wifiscan;
import static de.wladimircomputin.libcryptoiot.v2.Constants.command_writeSettings;
import static de.wladimircomputin.libcryptoiot.v2.Constants.devicepass_factory_default;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.wladimircomputin.cryptohouse.R;
import de.wladimircomputin.cryptohouse.databinding.FragmentAssistantSetupBinding;
import de.wladimircomputin.libcryptoiot.v2.protocol.Content;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptCon;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptConBulkReceiver;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptConReceiver;
import de.wladimircomputin.libcryptoiot.v2.protocol.DiscoveryDevice;
import de.wladimircomputin.libcryptoiot.v2.util.WifiScanItem;

public class SetupFragment extends Fragment implements FocusListener {

    FragmentAssistantSetupBinding binding;

    DiscoveryDevice device;

    CryptCon cc;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    // Inflate the view for the fragment based on layout XML
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAssistantSetupBinding.inflate(inflater, container, false);

        cc = new CryptCon(devicepass_factory_default, ap_ip_default);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {cc.sendMessageEncrypted(command_discover, new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    device = new DiscoveryDevice(response.data);

                    binding.assistantDevicetypeLabel.setText(device.type);
                    String[] devicetypes = getResources().getStringArray(R.array.devicetypes);
                    TranslateAnimation animate = new TranslateAnimation(
                            0,                 // fromXDelta
                            0,                 // toXDelta
                            binding.assistantDevicetypeImage.getHeight(),  // fromYDelta
                            0);                // toYDelta
                    animate.setDuration(500);
                    animate.setInterpolator(new DecelerateInterpolator());
                    if (Arrays.asList(devicetypes).contains(device.type)) {
                        binding.assistantDevicetypeImage.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_baseline_check_24));
                        binding.assistantDevicetypeImage.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorAccent)));
                    } else {
                        binding.assistantDevicetypeImage.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.baseline_question_mark_24));
                        binding.assistantDevicetypeImage.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.design_default_color_error)));
                    }
                    binding.assistantDevicetypeImage.startAnimation(animate);
                });
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
        });}, 10000);

        binding.assistantWifiScanButton.setOnClickListener((v) -> {
            scan();
        });

        binding.assistantWifiModeSpinner.setSelection(1);
        binding.assistantApplyButton.setOnClickListener((v) -> {
            apply();
        });
        return binding.getRoot();
    }

    @Override
    public void onSelected() {
    }

    @Override
    public void onUnselected() {
    }

    public void scan(){
        Toast.makeText(getContext(), getString(R.string.wifi_scanning), Toast.LENGTH_SHORT).show();

        binding.assistantWifiScanProgress.setVisibility(View.VISIBLE);
        binding.assistantWifiScanButton.setVisibility(View.INVISIBLE);
        cc.sendMessageEncrypted(command_wifiscan, new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
                Looper.prepare();
                (new Handler(Looper.getMainLooper())).postDelayed(() -> {
                    cc.sendMessageEncrypted(command_wifiresults, new CryptConReceiver() {
                        @Override
                        public void onSuccess(Content response) {
                            List<WifiScanItem> scanResults = new ArrayList<>();
                            try{
                                JSONArray jsonArray = new JSONArray(response.data);
                                for(int i = 0; i < jsonArray.length(); i++) {
                                    WifiScanItem scanItem = new WifiScanItem(jsonArray.getJSONObject(i));
                                    scanResults.add(scanItem);
                                }

                                scanResults.sort((lhs, rhs) -> rhs.rssi - lhs.rssi);


                                String[] arr = new String[scanResults.size()];
                                for (int i = 0; i < arr.length; i++) {
                                    if(!scanResults.get(i).hidden) {
                                        arr[i] = scanResults.get(i).ssid;
                                    } else {
                                        arr[i] = "<hidden> " + scanResults.get(i).bssid;
                                    }
                                }

                                new Handler(Looper.getMainLooper()).post(() -> {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                    builder.setTitle(getString(R.string.select_ssid));

                                    builder.setItems(arr, (dialog, which) -> binding.assistantWifiSsidEdittext.setText(arr[which]));
                                    AlertDialog dialog = builder.create();
                                    dialog.show();
                                });

                            } catch (Exception x){
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    Toast.makeText(getContext(), getString(R.string.wifi_scan_failed), Toast.LENGTH_SHORT).show();
                                });
                            }
                        }

                        @Override
                        public void onFail() {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                Toast.makeText(getContext(), getString(R.string.wifi_scan_failed), Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onFinished() {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                binding.assistantWifiScanButton.setVisibility(View.VISIBLE);
                                binding.assistantWifiScanProgress.setVisibility(View.INVISIBLE);
                            });
                        }

                        @Override
                        public void onProgress(String sprogress, int iprogress) {

                        }
                    });
                }, 3000);
            }

            @Override
            public void onFail() {
                new Handler(Looper.getMainLooper()).post(() -> {
                    binding.assistantWifiScanButton.setVisibility(View.VISIBLE);
                    binding.assistantWifiScanProgress.setVisibility(View.INVISIBLE);
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

    public void apply(){
        DeviceSetupPack pack = new DeviceSetupPack();

        pack.hostname = binding.assistantHostnameEdittext.getText().toString();
        pack.mac = device.mac;
        pack.type = device.type;
        pack.devicepass = binding.assistantDevicepassEdittext.getText().toString();
        pack.wifimode = String.valueOf(binding.assistantWifiModeSpinner.getSelectedItem());
        pack.ssid = binding.assistantWifiSsidEdittext.getText().toString();
        pack.wifipass = binding.assistantWifiPassEdittext.getText().toString();

        if(!(pack.devicepass.length() >= 8 && pack.devicepass.length() <= 64)){
            Toast.makeText(getContext(), getString(R.string.device_password_short_long), Toast.LENGTH_SHORT).show();
            return;
        }

        if(!(pack.ssid.length() >= 3 && pack.ssid.length() <= 32)) {
            Toast.makeText(getContext(), getString(R.string.ssid_long_short), Toast.LENGTH_SHORT).show();
            return;
        }

        if(!(pack.wifipass.length() >= 8 && pack.wifipass.length() <= 63)){
            Toast.makeText(getContext(), getString(R.string.wifipass_long_short), Toast.LENGTH_SHORT).show();
            return;
        }

        View view = getActivity().getCurrentFocus();
        if (view == null) {
            view = new View(getActivity());
        }
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

        ArrayList<String> commands = new ArrayList<>();
        commands.add(command_writeSettings + ":system:hostname:" + pack.hostname);
        commands.add(command_writeSettings + ":system:devicepass:" + pack.devicepass);
        commands.add(command_writeSettings + ":wifi:ssid:" + pack.ssid);
        commands.add(command_writeSettings + ":wifi:pass:" + pack.wifipass);
        commands.add(command_writeSettings + ":wifi:mode:" + pack.wifimode);

        cc.sendMessageEncryptedBulk(commands.toArray(new String[0]), new CryptConBulkReceiver() {
            @Override
            public void onSuccess(Content response, int i) {
                commands.remove(0);
            }

            @Override
            public void onFail(int i) {
                ((AssistantActivity)getActivity()).pack = null;
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getContext(), "Command failed: " + commands.get(0), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFinished() {
                if(commands.isEmpty()) {
                    ((AssistantActivity)getActivity()).pack = pack;
                    cc.sendMessageEncrypted(command_reboot, new CryptConReceiver() {
                        @Override
                        public void onSuccess(Content response) {
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                ((AssistantActivity)getActivity()).nextPage();
                            }, 1000);
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

            @Override
            public void onProgress(String sprogress, int iprogress) {

            }
        });
    }
}
