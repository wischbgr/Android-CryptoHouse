package de.wladimircomputin.cryptohouse.importexport;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import de.wladimircomputin.cryptohouse.MainActivity;
import de.wladimircomputin.cryptohouse.R;
import de.wladimircomputin.cryptohouse.actions.ActionItem;
import de.wladimircomputin.cryptohouse.databinding.FragmentImportBinding;
import de.wladimircomputin.cryptohouse.devicemanager.DeviceManagerDevice;
import de.wladimircomputin.cryptohouse.profile.ProfileItem;
import de.wladimircomputin.libcryptoiot.v2.protocol.Crypter;

public class ImportFragment extends Fragment implements FocusListener {

    List<ProfileItem> allProfiles;
    List<ProfileItem> importedProfiles;
    List<ProfileItem> profiles;

    FragmentImportBinding binding;

    ActivityResultLauncher<String[]> openDocumentLauncher;
    JSONObject rawJsonSettings;
    JSONObject verifiedJsonSettings;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPrefs = getContext().getSharedPreferences("de.wladimircomputin.cryptohouse.profiles", Context.MODE_PRIVATE);

        try {
            allProfiles = getProfiles(new JSONArray(sharedPrefs.getString("profiles", "[]")));
        } catch (Exception x){
            allProfiles = new ArrayList<>();
        }

        profiles = new ArrayList<>();
        importedProfiles = new ArrayList<>();

        openDocumentLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(), uri -> {
                    if (uri != null) {
                        loadFile(uri);
                    }
                }
        );

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = FragmentImportBinding.inflate(inflater, container, false);

        binding.importStatusText.setOnClickListener((v) -> {
            updateStatusText(1);
        });

        binding.importStatusText.setText("");

        binding.importProfileModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateStatusText(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        binding.importFilepathText.setOnClickListener((v) -> {
            binding.importBrowseButton.callOnClick();
        });

        binding.importBrowseButton.setOnClickListener((v) -> {
            binding.importButton.setEnabled(false);
            binding.importProfileModeSpinner.setVisibility(View.GONE);
            binding.importStatusText.setVisibility(View.GONE);
            binding.importOverwriteCheckbox.setVisibility(View.GONE);
            openDocumentLauncher.launch(new String[] {"application/json"});
        });

        binding.importButton.setOnClickListener((v) -> {
            try {
                if(importSettings(verifiedJsonSettings, binding.importOverwriteCheckbox.isChecked())){
                    Toast.makeText(getContext(), "Success", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(getContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    Toast.makeText(getContext(), "Fail", Toast.LENGTH_SHORT).show();
                }

            } catch (Exception x){

            }
        });



        return binding.getRoot();
    }

    private void loadSettings(JSONObject settings){
        verifiedJsonSettings = settings;
        updateStatusText(0);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            getActivity().runOnUiThread(new Runnable(){
                public void run() {
                    binding.importButton.setEnabled(true);
                    binding.importProfileModeSpinner.setVisibility(View.VISIBLE);
                    binding.importStatusText.setVisibility(View.VISIBLE);
                    binding.importOverwriteCheckbox.setVisibility(View.VISIBLE);
                }
            });
        }, 500);
    }


    public void loadFile(Uri uri){
        //importFilepathText.setText(getFileName(uri));
        try {
            rawJsonSettings = loadSettingsFromFile(uri);
            if(!rawJsonSettings.getBoolean("encrypted")){
                loadSettings(rawJsonSettings);
            } else {
                View v = getLayoutInflater().inflate(R.layout.dialog_importpassword, null);
                EditText textInputEditText = v.findViewById(R.id.import_password_edittext);

                // Build the dialog
                new AlertDialog.Builder(getContext())
                        .setTitle(getString(R.string.import_password_title))
                        .setMessage(getString(R.string.import_password_description))
                        .setView(v)  // Set the input field inside the dialog
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Handle the entered password here
                                String password = textInputEditText.getText().toString();
                                try {
                                    rawJsonSettings = decryptSettings(rawJsonSettings, password);
                                    loadSettings(rawJsonSettings);
                                } catch (Exception x){
                                }
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();  // Close the dialog
                            }
                        })
                        .show();

            }
        } catch (Exception x){
            Toast.makeText(getContext(), x.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateStatusText(int position){
        if(verifiedJsonSettings != null) {
            if(binding.importProfileModeSpinner.getSelectedItemPosition() != position){
                binding.importProfileModeSpinner.setSelection(position);
                return;
            }

            try {
                importedProfiles = getProfiles(verifiedJsonSettings.getJSONArray("profiles"));
            } catch (Exception x){}

            if (position == 1) {
                showMultiChoiceDialog(importedProfiles, profiles, new MultipleSelectionDialogReceiver<ProfileItem>() {
                    @Override
                    public void onFinish(List<ProfileItem> selectedItems) {
                        profiles.clear();
                        profiles.addAll(selectedItems);

                        binding.importStatusText.setText(profilesToString(profiles));
                    }

                    @Override
                    public void onAbort() {

                    }
                });
            } else {
                profiles.clear();
                profiles.addAll(importedProfiles);

                binding.importStatusText.setText(profilesToString(profiles));
            }
        }
    }

    private JSONObject loadSettingsFromFile(Uri settingsUri) throws Exception{
        InputStream inputStream = requireActivity().getContentResolver().openInputStream(settingsUri);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        inputStream.close();
        String jsonContent = stringBuilder.toString();
        return new JSONObject(jsonContent);
    }

    private String profilesToString(List<ProfileItem> profiles){
        String profiles_selected = "";
        for(ProfileItem profileItem : profiles){
            profiles_selected += profileItem.name;
            if(allProfiles.contains(profileItem)){
                profiles_selected += "*";
            }
            profiles_selected += ", ";
        }
        if(!profiles_selected.isEmpty()){
            profiles_selected = profiles_selected.substring(0, profiles_selected.length()-2);
        }
        return profiles_selected;
    }

    private void showMultiChoiceDialog(List<ProfileItem> profiles, List<ProfileItem> preselectedItems, MultipleSelectionDialogReceiver<ProfileItem> receiver) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Profiles to import");
        String[] items = new String[profiles.size()];
        List<ProfileItem> selectedItems = new ArrayList<>();
        for(int i = 0; i < profiles.size(); i++){
            items[i] = profiles.get(i).name;
        }
        boolean[] checkedItems = new boolean[profiles.size()];

        for(ProfileItem profileItem : preselectedItems) {
            checkedItems[profiles.indexOf(profileItem)] = true;
            selectedItems.add(profileItem);
        }


        builder.setMultiChoiceItems(items, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int indexSelected, boolean isChecked) {
                if (isChecked) {
                    selectedItems.add(profiles.get(indexSelected));
                } else {
                    selectedItems.remove(profiles.get(indexSelected));
                }
            }
        });

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                receiver.onFinish(selectedItems);
            }
        });

        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                receiver.onAbort();
            }
        });

        // Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public boolean importSettings(JSONObject settingsJson, boolean overwrite){
        try {
            SharedPreferences sharedPrefs = getContext().getSharedPreferences("de.wladimircomputin.cryptohouse.main", Context.MODE_PRIVATE);
            int version_code = Math.min(settingsJson.getJSONObject("main").getInt("version_code"), sharedPrefs.getInt("version_code", 0));

            sharedPrefs = getContext().getSharedPreferences("de.wladimircomputin.cryptohouse.profiles", Context.MODE_PRIVATE);
            List<ProfileItem> importedProfiles = getProfiles(settingsJson.getJSONArray("profiles"));
            if(importedProfiles == null){
                return false;
            }
            List<ProfileItem> existingProfiles = getProfiles(new JSONArray(sharedPrefs.getString("profiles", "[]")));

            for(ProfileItem profileItem : importedProfiles){
                if(overwrite && existingProfiles.contains(profileItem)){
                    existingProfiles.remove(profileItem);
                }
                if(!existingProfiles.contains(profileItem)) {
                    existingProfiles.add(profileItem);
                }
            }

            Map<String, Map<String, DeviceManagerDevice>> devicesToImport = new LinkedHashMap<>();
            sharedPrefs = getContext().getSharedPreferences("de.wladimircomputin.cryptohouse.devices", Context.MODE_PRIVATE);
            for(ProfileItem profileItem : importedProfiles){
                Map<String, DeviceManagerDevice> existingDevices = getDevices(new JSONObject(sharedPrefs.getString(profileItem.id, "{}")));
                Map<String, DeviceManagerDevice> importedDevices = getDevices(settingsJson.getJSONObject("devices").getJSONObject(profileItem.id));

                if(overwrite){
                    existingDevices = importedDevices;
                } else {
                    for(Map.Entry<String, DeviceManagerDevice> importedDevice : importedDevices.entrySet()){
                        if(!existingDevices.containsKey(importedDevice.getKey())){
                            boolean matchedAny = false;
                            for(Map.Entry<String, DeviceManagerDevice> existingDevice : existingDevices.entrySet()){
                                if(existingDevice.getValue().mac.equals(importedDevice.getValue().mac) && !existingDevice.getValue().mac.isEmpty()){
                                    matchedAny = true;
                                }
                            }
                            if(!matchedAny){
                                existingDevices.put(importedDevice.getKey(), importedDevice.getValue());
                            }
                        }
                    }
                }

                devicesToImport.put(profileItem.id, existingDevices);
            }

            Map<String, Map<String, ActionItem>> actionsToImport = new LinkedHashMap<>();
            sharedPrefs = getContext().getSharedPreferences("de.wladimircomputin.cryptohouse.actions", Context.MODE_PRIVATE);
            for(ProfileItem profileItem : importedProfiles){
                Map<String, ActionItem> existingActions = getActions(new JSONObject(sharedPrefs.getString(profileItem.id, "{}")), devicesToImport.get(profileItem.id));
                Map<String, ActionItem> importedActions = getActions(settingsJson.getJSONObject("actions").getJSONObject(profileItem.id), devicesToImport.get(profileItem.id));

                if(overwrite){
                    existingActions = importedActions;
                } else {
                    for(Map.Entry<String, ActionItem> importedAction : importedActions.entrySet()){
                        existingActions.putIfAbsent(importedAction.getKey(), importedAction.getValue());
                    }
                }
                actionsToImport.put(profileItem.id, existingActions);
            }

            sharedPrefs = getContext().getSharedPreferences("de.wladimircomputin.cryptohouse.main", Context.MODE_PRIVATE);
            sharedPrefs.edit().putInt("version_code", version_code).apply();

            sharedPrefs = getContext().getSharedPreferences("de.wladimircomputin.cryptohouse.profiles", Context.MODE_PRIVATE);
            JSONArray jsonArray = new JSONArray();
            for(ProfileItem profileItem : existingProfiles){
                jsonArray.put(profileItem.toJSON());
            }
            sharedPrefs.edit().putString("profiles", jsonArray.toString()).apply();

            Map <String, JSONObject> devicesToImportJson = new LinkedHashMap<>();
            for(Map.Entry<String, Map<String, DeviceManagerDevice>> id : devicesToImport.entrySet()){
                devicesToImportJson.put(id.getKey(), new JSONObject());
                for(Map.Entry<String, DeviceManagerDevice> device : id.getValue().entrySet()) {
                    devicesToImportJson.get(id.getKey()).put(device.getKey(), device.getValue().toJSON());
                }
            }
            importSharedPrefs("de.wladimircomputin.cryptohouse.devices", devicesToImportJson);

            Map <String, JSONObject> actionsToImportJson = new LinkedHashMap<>();
            for(Map.Entry<String, Map<String, ActionItem>> id : actionsToImport.entrySet()){
                actionsToImportJson.put(id.getKey(), new JSONObject());
                for(Map.Entry<String, ActionItem> action : id.getValue().entrySet()) {
                    actionsToImportJson.get(id.getKey()).put(action.getKey(), action.getValue().toJSON());
                }
            }
            importSharedPrefs("de.wladimircomputin.cryptohouse.actions", actionsToImportJson);


        } catch (Exception x){
            x.printStackTrace();
        }
        return true;
    }

    private void importSharedPrefs(String path, Map<String, JSONObject> map){
        SharedPreferences sharedPrefs = getContext().getSharedPreferences(path, Context.MODE_PRIVATE);
        for(Map.Entry<String, JSONObject> item : map.entrySet()){
            sharedPrefs.edit().putString(item.getKey(),item.getValue().toString()).apply();
        }
    }

    private JSONObject decryptSettings(JSONObject input, String password) throws Exception{
        Crypter crypter = new Crypter(password);

        byte iv[] = Base64.decode(input.getString("iv"), Base64.NO_WRAP);
        byte tag[] = Base64.decode(input.getString("tag"), Base64.NO_WRAP);
        byte data[] = Base64.decode(input.getString("data"), Base64.NO_WRAP);

        String plain = new String(crypter.decrypt(data, iv, tag), StandardCharsets.UTF_8);
        return new JSONObject(plain);
    }

    private Map<String, ActionItem> getActions(JSONObject jsonObject, Map<String, DeviceManagerDevice> deviceManagerDeviceMap) throws Exception{
        Pattern UUID_REGEX = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
        Map<String, ActionItem> out = new LinkedHashMap<>();
        for (Iterator<String> iter = jsonObject.keys(); iter.hasNext(); ) {
            String key = iter.next();
            if(UUID_REGEX.matcher(key).matches()) {
                out.put(key, new ActionItem(jsonObject.getJSONObject(key), deviceManagerDeviceMap));
            }
        }
        return out;
    }

    private Map<String, DeviceManagerDevice> getDevices(JSONObject jsonObject) throws Exception{
        Map<String, DeviceManagerDevice> out = new LinkedHashMap<>();
        Pattern UUID_REGEX = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
        for (Iterator<String> iter = jsonObject.keys(); iter.hasNext(); ) {
            String key = iter.next();
            if(UUID_REGEX.matcher(key).matches()) {
                out.put(key, new DeviceManagerDevice(jsonObject.getJSONObject(key)));
            }
        }
        return out;
    }

    private List<ProfileItem> getProfiles(JSONArray arr){
        ArrayList<ProfileItem> profiles = new ArrayList<>();
        try {
            for(int i = 0; i < arr.length(); i++) {
                ProfileItem profileItem = new ProfileItem(arr.getJSONObject(i));
                profiles.add(profileItem);
            }

        }catch (Exception x){
            return null;
        }
        return profiles;
    }

    @Override
    public void onSelected() {

    }

    @Override
    public void onUnselected() {

    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
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
    public void onResume() {
        super.onResume();
        MainActivity mainActivity = (MainActivity) getActivity();
        Uri importUri = mainActivity.getImportUri();
        if (importUri != null){
            loadFile(importUri);
            mainActivity.clearImportUri();
        }
    }
}
