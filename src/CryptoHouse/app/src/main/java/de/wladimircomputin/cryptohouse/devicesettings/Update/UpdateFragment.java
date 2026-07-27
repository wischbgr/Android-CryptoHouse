package de.wladimircomputin.cryptohouse.devicesettings.Update;

import static de.wladimircomputin.libcryptoiot.v2.Constants.command_update;
import static de.wladimircomputin.libcryptoiot.v2.Constants.command_version;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Iterator;

import de.wladimircomputin.cryptohouse.R;
import de.wladimircomputin.cryptohouse.assistant.FocusListener;
import de.wladimircomputin.cryptohouse.databinding.FragmentUpdateBinding;
import de.wladimircomputin.cryptohouse.devicesettings.DeviceSettingsActivity;
import de.wladimircomputin.libcryptoiot.v2.protocol.Content;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptCon;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptConReceiver;

public class UpdateFragment extends Fragment implements FocusListener {

    FragmentUpdateBinding binding;
    ActivityResultLauncher<String[]> openDocumentLauncher;
    Uri selectedFirmwareUri;
    String webUpdaterUrl;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        openDocumentLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(), uri -> {
                    if (uri != null) {
                        selectedFirmwareUri = uri;
                        binding.updateOtaFilepathText.setText(getFileName(uri));
                        binding.updateOtaButton.setEnabled(true);
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentUpdateBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setControlsEnabled(false);

        binding.updateModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                enableUpdateMode();
            } else {
                webUpdaterUrl = null;
                setControlsEnabled(false);
            }
        });

        binding.updateWebButton.setOnClickListener(v -> openWebUpdater());

        binding.updateOtaFilepathText.setOnClickListener(v -> binding.updateOtaBrowseButton.callOnClick());
        binding.updateOtaBrowseButton.setOnClickListener(v -> openDocumentLauncher.launch(new String[]{"*/*"}));

        binding.updateOtaButton.setOnClickListener(v -> startOtaUpdate());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadVersionInfo();
    }

    private void loadVersionInfo() {
        CryptCon cc = ((DeviceSettingsActivity) getActivity()).cc;
        cc.sendMessageEncrypted(command_version, CryptCon.Mode.UDP, new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
                getActivity().runOnUiThread(() -> showVersionInfo(response.data));
            }

            @Override
            public void onFail() {
                getActivity().runOnUiThread(() -> binding.updateVersionText.setText(R.string.version_not_supported));
            }

            @Override
            public void onFinished() {

            }

            @Override
            public void onProgress(String sprogress, int iprogress) {

            }
        });
    }

    private void showVersionInfo(String json) {
        try {
            JSONObject versions = new JSONObject(json);
            StringBuilder text = new StringBuilder();

            String env = versions.optString("env", "");
            if (!env.isEmpty()) {
                text.append(env).append("\n\n");
            }

            String coreVersion = versions.optString("CryptoIoT", "");
            if (!coreVersion.isEmpty()) {
                text.append("CryptoIoT ").append(coreVersion);
            }

            Iterator<String> keys = versions.keys();
            while (keys.hasNext()) {
                String app = keys.next();
                if (app.equals("CryptoIoT") || app.equals("env")) {
                    continue;
                }
                if (text.length() > 0) {
                    text.append("\n");
                }
                text.append("  ").append(app).append(" ").append(versions.getString(app));
            }

            binding.updateVersionText.setText(text.toString());
        } catch (Exception x) {
            binding.updateVersionText.setText(R.string.version_not_supported);
        }
    }

    private void setControlsEnabled(boolean enabled) {
        binding.updateWebButton.setEnabled(enabled);
        binding.updateOtaFilepathText.setEnabled(enabled);
        binding.updateOtaBrowseButton.setEnabled(enabled);
        if (!enabled) {
            selectedFirmwareUri = null;
            binding.updateOtaFilepathText.setText(R.string.select_file);
        }
        binding.updateOtaButton.setEnabled(enabled && selectedFirmwareUri != null);
    }

    private void enableUpdateMode() {
        binding.updateModeSwitch.setEnabled(false);
        CryptCon cc = ((DeviceSettingsActivity) getActivity()).cc;
        cc.sendMessageEncrypted(command_update, CryptCon.Mode.UDP, new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
                webUpdaterUrl = response.data;
                getActivity().runOnUiThread(() -> {
                    binding.updateModeSwitch.setEnabled(true);
                    setControlsEnabled(true);
                    Toast.makeText(getContext(), getString(R.string.update_server_enabled) + "\n" + response.data, Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onFail() {
                getActivity().runOnUiThread(() -> {
                    binding.updateModeSwitch.setEnabled(true);
                    binding.updateModeSwitch.setChecked(false);
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

    private void openWebUpdater() {
        if (webUpdaterUrl == null) {
            return;
        }
        String url = webUpdaterUrl.startsWith("http") ? webUpdaterUrl : "http://" + webUpdaterUrl;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    private void startOtaUpdate() {
        if (selectedFirmwareUri == null) {
            return;
        }

        binding.updateOtaButton.setEnabled(false);
        binding.updateOtaBrowseButton.setEnabled(false);
        binding.updateOtaProgress.setProgress(0);
        binding.updateOtaProgress.setVisibility(View.VISIBLE);

        String ip = ((DeviceSettingsActivity) getActivity()).device.ip;
        Uri firmwareUri = selectedFirmwareUri;

        new Thread(() -> {
            byte[] firmware;
            try {
                firmware = readFile(firmwareUri);
            } catch (Exception x) {
                showResult(false, x.getMessage());
                return;
            }

            new ArduinoOTAClient(ip, new ArduinoOTAClient.Callback() {
                @Override
                public void onProgress(int progress) {
                    getActivity().runOnUiThread(() -> {
                        ObjectAnimator animation = ObjectAnimator.ofInt(binding.updateOtaProgress, "progress", binding.updateOtaProgress.getProgress(), progress);
                        animation.setDuration(300);
                        animation.setInterpolator(new DecelerateInterpolator());
                        animation.start();
                    });
                }

                @Override
                public void onSuccess() {
                    showResult(true, null);
                }

                @Override
                public void onError(String message) {
                    showResult(false, message);
                }
            }).update(firmware);
        }).start();
    }

    private void showResult(boolean success, String message) {
        getActivity().runOnUiThread(() -> {
            binding.updateOtaProgress.setVisibility(View.GONE);
            binding.updateOtaBrowseButton.setEnabled(true);
            binding.updateOtaButton.setEnabled(true);
            Toast.makeText(getContext(), success ? getString(R.string.update_success) : message, Toast.LENGTH_LONG).show();
        });
    }

    private byte[] readFile(Uri uri) throws Exception {
        InputStream inputStream = requireActivity().getContentResolver().openInputStream(uri);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        inputStream.close();
        return out.toByteArray();
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                }
            } catch (Exception ignored) {
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    @Override
    public void onSelected() {

    }

    @Override
    public void onUnselected() {

    }
}
