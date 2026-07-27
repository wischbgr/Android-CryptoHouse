package de.wladimircomputin.cryptohouse.devicesettings.General;

import static de.wladimircomputin.libcryptoiot.v2.Constants.command_reboot;
import static de.wladimircomputin.libcryptoiot.v2.Constants.command_reset;
import static de.wladimircomputin.libcryptoiot.v2.Constants.command_status;
import static de.wladimircomputin.libcryptoiot.v2.Constants.command_writeSettings;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import de.wladimircomputin.cryptohouse.R;
import de.wladimircomputin.cryptohouse.assistant.FocusListener;
import de.wladimircomputin.cryptohouse.databinding.FragmentGeneralBinding;
import de.wladimircomputin.cryptohouse.devicemanager.DeviceManagerDevice;
import de.wladimircomputin.cryptohouse.devicesettings.DeviceSettingsActivity;
import de.wladimircomputin.libcryptoiot.v2.protocol.Content;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptCon;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptConBulkReceiver;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptConReceiver;

public class GeneralFragment extends Fragment implements FocusListener, GeneralSettingClickListener {

    FragmentGeneralBinding binding;
    List<GeneralSettingItem> items = new ArrayList<>();
    GeneralSettingRecyclerListAdapter adapter;

    ActivityResultLauncher<String> locationPermissionLauncher;
    Consumer<Location> pendingLocationRequest;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        locationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted && pendingLocationRequest != null) {
                fetchCoarseLocation(pendingLocationRequest);
            } else if (!granted) {
                Toast.makeText(getContext(), getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show();
            }
            pendingLocationRequest = null;
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGeneralBinding.inflate(inflater, container, false);

        items.add(new GeneralSettingItem(GeneralSettingItem.Type.SHOW_STATUS, R.string.status, R.string.status_description));
        items.add(new GeneralSettingItem(GeneralSettingItem.Type.CHANGE_HOSTNAME, R.string.change_hostname_title, R.string.change_hostname_description));
        items.add(new GeneralSettingItem(GeneralSettingItem.Type.CHANGE_DEVICE_PASSWORD, R.string.change_device_password_title, R.string.change_device_password_description));
        items.add(new GeneralSettingItem(GeneralSettingItem.Type.SET_LOCATION, R.string.set_location_title, R.string.set_location_description));
        items.add(new GeneralSettingItem(GeneralSettingItem.Type.REBOOT, R.string.reboot, R.string.reboot_description));
        items.add(new GeneralSettingItem(GeneralSettingItem.Type.RESET_FACTORY_DEFAULTS, R.string.reset_factory_defaults_title, R.string.reset_factory_defaults_description));

        adapter = new GeneralSettingRecyclerListAdapter(items, this, getContext());
        binding.generalRecycler.setHasFixedSize(true);
        binding.generalRecycler.setAdapter(adapter);
        binding.generalRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        return binding.getRoot();
    }

    @Override
    public void onGeneralSettingClicked(GeneralSettingItem item) {
        switch (item.type) {
            case SHOW_STATUS:
                showStatus();
                break;

            case CHANGE_HOSTNAME:
                showChangeHostnameDialog();
                break;

            case CHANGE_DEVICE_PASSWORD:
                showChangeDevicePasswordDialog();
                break;

            case SET_LOCATION:
                showSetLocationDialog();
                break;

            case REBOOT:
                confirmReboot();
                break;

            case RESET_FACTORY_DEFAULTS:
                confirmResetFactoryDefaults();
                break;
        }
        // the remaining actions are wired up to the device in a later step
    }

    private void showStatus() {
        DeviceSettingsActivity activity = (DeviceSettingsActivity) getActivity();
        activity.cc.sendMessageEncrypted(command_status, CryptCon.Mode.UDP, new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
                activity.runOnUiThread(() -> {
                    new AlertDialog.Builder(getContext())
                            .setTitle(activity.device.name + " " + getString(R.string.status))
                            .setMessage(response.data)
                            .setCancelable(true)
                            .show();
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
        });
    }

    private void showChangeHostnameDialog() {
        DeviceManagerDevice device = ((DeviceSettingsActivity) getActivity()).device;

        View v = getLayoutInflater().inflate(R.layout.dialog_changehostname, null);
        TextInputEditText hostnameEditText = v.findViewById(R.id.change_hostname_edittext);
        hostnameEditText.setText(device.name);

        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.change_hostname_title))
                .setMessage(getString(R.string.change_hostname_description))
                .setView(v)
                .setPositiveButton(getString(R.string.save), (dialog, which) -> {
                    String newHostname = hostnameEditText.getText().toString().trim();
                    if (!newHostname.isEmpty()) {
                        changeHostname(newHostname);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.cancel())
                .show();
    }

    private void changeHostname(String newHostname) {
        CryptCon cc = ((DeviceSettingsActivity) getActivity()).cc;
        cc.sendMessageEncrypted(command_writeSettings + ":system:hostname:" + newHostname, CryptCon.Mode.UDP, new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
                getActivity().runOnUiThread(() -> updateAppDeviceName(newHostname));
            }

            @Override
            public void onFail() {
                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), getString(R.string.hostname_change_failed), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onFinished() {

            }

            @Override
            public void onProgress(String sprogress, int iprogress) {

            }
        });
    }

    private void updateAppDeviceName(String newName) {
        DeviceSettingsActivity activity = (DeviceSettingsActivity) getActivity();
        activity.device.name = newName;
        activity.setTitle(newName + " " + getString(R.string.settings));
        persistDeviceField("name", newName);
        Toast.makeText(getContext(), getString(R.string.hostname_changed), Toast.LENGTH_SHORT).show();

        // the device keeps using the old hostname until it reboots
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                reboot();
            }
        };
        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.reboot_device))
                .setMessage(getString(R.string.reboot_device_text))
                .setPositiveButton(getString(R.string.yes), dialogClickListener)
                .setNegativeButton(getString(R.string.no), dialogClickListener)
                .show();
    }

    private void showChangeDevicePasswordDialog() {
        DeviceManagerDevice device = ((DeviceSettingsActivity) getActivity()).device;

        View v = getLayoutInflater().inflate(R.layout.dialog_changepassword, null);
        TextInputEditText passwordEditText = v.findViewById(R.id.change_password_edittext);
        passwordEditText.setText(device.pass);

        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.change_device_password_title))
                .setMessage(getString(R.string.change_device_password_description))
                .setView(v)
                .setPositiveButton(getString(R.string.save), (dialog, which) -> {
                    String newPassword = passwordEditText.getText().toString();
                    if (newPassword.length() < 8 || newPassword.length() > 64) {
                        Toast.makeText(getContext(), getString(R.string.device_password_short_long), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    changeDevicePassword(newPassword);
                })
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.cancel())
                .show();
    }

    private void changeDevicePassword(String newPassword) {
        CryptCon cc = ((DeviceSettingsActivity) getActivity()).cc;
        cc.sendMessageEncrypted(command_writeSettings + ":system:devicepass:" + newPassword, CryptCon.Mode.UDP, new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
                getActivity().runOnUiThread(() -> onDevicePasswordChanged(newPassword));
            }

            @Override
            public void onFail() {
                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), getString(R.string.password_change_failed), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onFinished() {

            }

            @Override
            public void onProgress(String sprogress, int iprogress) {

            }
        });
    }

    private void onDevicePasswordChanged(String newPassword) {
        ((DeviceSettingsActivity) getActivity()).device.pass = newPassword;
        persistDeviceField("pass", newPassword);
        Toast.makeText(getContext(), getString(R.string.password_changed), Toast.LENGTH_SHORT).show();

        // the device keeps using the old password to authenticate until it reboots
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                rebootWithNewPassword(newPassword);
            }
        };
        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.reboot_device))
                .setMessage(getString(R.string.reboot_device_text))
                .setPositiveButton(getString(R.string.yes), dialogClickListener)
                .setNegativeButton(getString(R.string.no), dialogClickListener)
                .show();
    }

    private void rebootWithNewPassword(String newPassword) {
        DeviceSettingsActivity activity = (DeviceSettingsActivity) getActivity();
        activity.cc.sendMessageEncrypted(command_reboot, CryptCon.Mode.UDP, 1, new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {

            }

            @Override
            public void onFail() {

            }

            @Override
            public void onFinished() {
                activity.cc = new CryptCon(newPassword, activity.device.ip);
                activity.runOnUiThread(() -> Toast.makeText(getContext(), getString(R.string.rebooting), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onProgress(String sprogress, int iprogress) {

            }
        });
    }

    private void showSetLocationDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_setlocation, null);
        Button currentLocationButton = v.findViewById(R.id.set_location_current_button);
        AutoCompleteTextView addressAutoComplete = v.findViewById(R.id.set_location_address_autocomplete);
        TextView coordinatesText = v.findViewById(R.id.set_location_coordinates_text);

        AddressAutoCompleteAdapter suggestionsAdapter = new AddressAutoCompleteAdapter(getContext());
        addressAutoComplete.setAdapter(suggestionsAdapter);

        List<NominatimGeocoder.Result> results = new ArrayList<>();
        double[] resolved = {Double.NaN, Double.NaN};
        AlertDialog[] dialogHolder = new AlertDialog[1];

        addressAutoComplete.addTextChangedListener(new TextWatcher() {
            Runnable pending;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (pending != null) {
                    addressAutoComplete.removeCallbacks(pending);
                }
                String query = s.toString().trim();
                if (query.length() < 3) {
                    return;
                }
                pending = () -> searchAddress(query, results, suggestionsAdapter, addressAutoComplete);
                addressAutoComplete.postDelayed(pending, 400);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        addressAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            NominatimGeocoder.Result selected = results.get(position);
            resolved[0] = selected.lat;
            resolved[1] = selected.lon;
            coordinatesText.setText(formatCoordinates(selected.lat, selected.lon));
            coordinatesText.setVisibility(View.VISIBLE);
            if (dialogHolder[0] != null) {
                dialogHolder[0].getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            }
        });

        currentLocationButton.setOnClickListener(v2 -> requestCurrentLocation(location -> {
            resolved[0] = location.getLatitude();
            resolved[1] = location.getLongitude();
            addressAutoComplete.setText("");
            coordinatesText.setText(formatCoordinates(resolved[0], resolved[1]));
            coordinatesText.setVisibility(View.VISIBLE);
            if (dialogHolder[0] != null) {
                dialogHolder[0].getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            }
        }));

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.set_location_title))
                .setMessage(getString(R.string.set_location_description))
                .setView(v)
                .setPositiveButton(getString(R.string.save), (d, which) -> {
                    if (!Double.isNaN(resolved[0])) {
                        setDeviceLocation(resolved[0], resolved[1]);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), (d, which) -> d.cancel())
                .create();
        dialogHolder[0] = dialog;
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
    }

    private void searchAddress(String query, List<NominatimGeocoder.Result> results, AddressAutoCompleteAdapter adapter, AutoCompleteTextView textView) {
        new Thread(() -> {
            try {
                List<NominatimGeocoder.Result> found = NominatimGeocoder.search(query);
                getActivity().runOnUiThread(() -> {
                    results.clear();
                    results.addAll(found);
                    List<String> displayNames = new ArrayList<>();
                    for (NominatimGeocoder.Result result : found) {
                        displayNames.add(result.displayName);
                    }
                    adapter.updateSuggestions(displayNames);
                    if (!displayNames.isEmpty()) {
                        textView.showDropDown();
                    }
                });
            } catch (Exception x) {

            }
        }).start();
    }

    private String formatCoordinates(double lat, double lon) {
        return String.format(Locale.US, "%.6f, %.6f", lat, lon);
    }

    private void requestCurrentLocation(Consumer<Location> callback) {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            pendingLocationRequest = callback;
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
            return;
        }
        fetchCoarseLocation(callback);
    }

    private void fetchCoarseLocation(Consumer<Location> callback) {
        LocationManager locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        try {
            Location lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (lastKnown != null) {
                callback.accept(lastKnown);
                return;
            }
            if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                Toast.makeText(getContext(), getString(R.string.location_unavailable), Toast.LENGTH_SHORT).show();
                return;
            }
            locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    callback.accept(location);
                }

                @Override
                public void onProviderEnabled(String provider) {
                }

                @Override
                public void onProviderDisabled(String provider) {
                }
            }, Looper.getMainLooper());
        } catch (SecurityException x) {
            Toast.makeText(getContext(), getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show();
        }
    }

    private void setDeviceLocation(double lat, double lon) {
        CryptCon cc = ((DeviceSettingsActivity) getActivity()).cc;
        String[] commands = {
                command_writeSettings + ":system:lat:" + lat,
                command_writeSettings + ":system:lon:" + lon
        };
        cc.sendMessageEncryptedBulk(commands, new CryptConBulkReceiver() {
            @Override
            public void onSuccess(Content response, int i) {

            }

            @Override
            public void onFail(int i) {
                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), getString(R.string.location_change_failed), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onFinished() {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), getString(R.string.location_changed), Toast.LENGTH_SHORT).show();
                    confirmRebootAfterLocationChange();
                });
            }

            @Override
            public void onProgress(String sprogress, int iprogress) {

            }
        });
    }

    private void confirmRebootAfterLocationChange() {
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                reboot();
            }
        };
        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.reboot_device))
                .setMessage(getString(R.string.reboot_device_text))
                .setPositiveButton(getString(R.string.yes), dialogClickListener)
                .setNegativeButton(getString(R.string.no), dialogClickListener)
                .show();
    }

    private void persistDeviceField(String jsonKey, String value) {
        try {
            DeviceSettingsActivity activity = (DeviceSettingsActivity) getActivity();
            SharedPreferences profilePrefs = getContext().getSharedPreferences("de.wladimircomputin.cryptohouse.profiles", Context.MODE_PRIVATE);
            String currentProfile = profilePrefs.getString("current_profile", "");

            SharedPreferences devicesPrefs = getContext().getSharedPreferences("de.wladimircomputin.cryptohouse.devices", Context.MODE_PRIVATE);
            JSONObject devicesObj = new JSONObject(devicesPrefs.getString(currentProfile, "{}"));
            JSONObject deviceObj = devicesObj.optJSONObject(activity.device.id);
            if (deviceObj != null) {
                deviceObj.put(jsonKey, value);
                devicesObj.put(activity.device.id, deviceObj);
                devicesPrefs.edit().putString(currentProfile, devicesObj.toString()).apply();
            }
        } catch (Exception x) {

        }
    }

    private void confirmReboot() {
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                reboot();
            }
        };
        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.reboot))
                .setMessage(getString(R.string.reboot_confirm_text))
                .setPositiveButton(getString(R.string.yes), dialogClickListener)
                .setNegativeButton(getString(R.string.no), dialogClickListener)
                .show();
    }

    private void reboot() {
        CryptCon cc = ((DeviceSettingsActivity) getActivity()).cc;
        cc.sendMessageEncrypted(command_reboot, CryptCon.Mode.UDP, 1, new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {

            }

            @Override
            public void onFail() {

            }

            @Override
            public void onFinished() {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), getString(R.string.rebooting), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onProgress(String sprogress, int iprogress) {

            }
        });
    }

    private void confirmResetFactoryDefaults() {
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                resetFactoryDefaults();
            }
        };
        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.reset_factory_defaults_title))
                .setMessage(getString(R.string.reset_factory_defaults_confirm_text))
                .setPositiveButton(getString(R.string.yes), dialogClickListener)
                .setNegativeButton(getString(R.string.no), dialogClickListener)
                .show();
    }

    private void resetFactoryDefaults() {
        CryptCon cc = ((DeviceSettingsActivity) getActivity()).cc;
        cc.sendMessageEncrypted(command_reset, CryptCon.Mode.UDP, new CryptConReceiver() {
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
    public void onSelected() {

    }

    @Override
    public void onUnselected() {

    }
}
