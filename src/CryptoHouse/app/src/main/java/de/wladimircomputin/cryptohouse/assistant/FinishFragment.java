package de.wladimircomputin.cryptohouse.assistant;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import org.json.JSONObject;

import java.util.TimerTask;
import java.util.UUID;

import de.wladimircomputin.cryptohouse.R;
import de.wladimircomputin.cryptohouse.databinding.FragmentAssistantFinishBinding;
import de.wladimircomputin.cryptohouse.devicemanager.DeviceManagerDevice;

public class FinishFragment extends Fragment implements FocusListener {
    static class Timeout extends TimerTask{
        private boolean expired = false;
        public boolean expired(){
            return expired;
        }

        @Override
        public void run() {
            expired = true;
        }
    }

    FragmentAssistantFinishBinding binding;

    final int WAIT_AT_LEAST = 5000;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    // Inflate the view for the fragment based on layout XML
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAssistantFinishBinding.inflate(inflater, container, false);

        binding.assistantFinishButton.setOnClickListener((v) -> {
            finishAssistant();
        });
        return binding.getRoot();
    }

    @Override
    public void onSelected() {
    }

    @Override
    public void onUnselected() {
    }

    public void finishAssistant(){
        binding.assistantFinishProgress.setVisibility(View.VISIBLE);
        (new Handler(Looper.getMainLooper())).postDelayed(() -> {
            DeviceSetupPack pack = ((AssistantActivity)getActivity()).pack;
            DeviceManagerDevice device = new DeviceManagerDevice(UUID.randomUUID().toString(), pack.hostname, pack.type, pack.hostname, "127.0.0.1", pack.mac, pack.devicepass, true, "");
            SharedPreferences sharedPrefs = getContext().getSharedPreferences("de.wladimircomputin.cryptohouse.profiles", Context.MODE_PRIVATE);
            String current_profile = sharedPrefs.getString("current_profile", "0000");
            sharedPrefs = getContext().getSharedPreferences("de.wladimircomputin.cryptohouse.devices", Context.MODE_PRIVATE);
            String devicesjson = sharedPrefs.getString(current_profile, "{}");
            try {
                JSONObject devicesObj = new JSONObject(devicesjson);
                devicesObj.put(device.id, device.toJSON());
                sharedPrefs.edit().putString(current_profile, devicesObj.toString()).commit();
                Intent intent = getActivity().getBaseContext().getPackageManager().getLaunchIntentForPackage(getActivity().getBaseContext().getPackageName());
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                getActivity().finish();
                startActivity(intent);
            } catch (Exception x) {
                x.printStackTrace();
            }
        }, WAIT_AT_LEAST);

    }
}
