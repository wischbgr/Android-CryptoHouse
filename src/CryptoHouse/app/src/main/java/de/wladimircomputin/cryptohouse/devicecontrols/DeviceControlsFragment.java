package de.wladimircomputin.cryptohouse.devicecontrols;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import de.wladimircomputin.cryptohouse.MainActivity;
import de.wladimircomputin.cryptohouse.assistant.FocusListener;
import de.wladimircomputin.cryptohouse.databinding.FragmentDevicecontrolsBinding;
import de.wladimircomputin.cryptohouse.device.CIoTBell;
import de.wladimircomputin.cryptohouse.device.CryptoAC;
import de.wladimircomputin.cryptohouse.device.CryptoAC_TCL;
import de.wladimircomputin.cryptohouse.device.CryptoDimmer;
import de.wladimircomputin.cryptohouse.device.CryptoDimmer2;
import de.wladimircomputin.cryptohouse.device.CryptoDimmer4;
import de.wladimircomputin.cryptohouse.device.CryptoGarage;
import de.wladimircomputin.cryptohouse.device.CryptoGeiger;
import de.wladimircomputin.cryptohouse.device.CryptoGeneric;
import de.wladimircomputin.cryptohouse.device.CryptoKamin;
import de.wladimircomputin.cryptohouse.device.CryptoRollo;
import de.wladimircomputin.cryptohouse.device.CryptoSML;
import de.wladimircomputin.cryptohouse.device.CryptoTimer;
import de.wladimircomputin.cryptohouse.device.DoorLock;
import de.wladimircomputin.cryptohouse.device.ICryptoDevice;
import de.wladimircomputin.cryptohouse.device.LightSwitch;
import de.wladimircomputin.cryptohouse.device.PlantWater;
import de.wladimircomputin.cryptohouse.device.PlugSwitch;
import de.wladimircomputin.cryptohouse.device.UranLight;
import de.wladimircomputin.cryptohouse.devicemanager.DeviceManagerDevice;
import de.wladimircomputin.cryptohouse.profile.ProfileItem;
import de.wladimircomputin.cryptohouse.ui.PagerAdapterTitleProvider;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptCon;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptConDiscoverReceiver;
import de.wladimircomputin.libcryptoiot.v2.protocol.DiscoveryDevice;

public class DeviceControlsFragment extends Fragment {

    private CustomPagerAdapter pagerAdapter;

    private List<DeviceControlsSubFragment> segmentFragments = new ArrayList<>();

    FragmentDevicecontrolsBinding binding;
    Timer updateTimer;
    Timer updateIPTimer;
    Map<String, ArrayList<ICryptoDevice>> devices = new LinkedHashMap<>();
    SharedPreferences sharedPrefs;

    ProfileItem current_profile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = FragmentDevicecontrolsBinding.inflate(inflater, container, false);

        setHasOptionsMenu(true);
        //((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(R.string.app_name);
        MainActivity mainActivity = (MainActivity) getActivity();
        current_profile = mainActivity.getCurrentProfile();
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(current_profile.name);
        sharedPrefs = getContext().getSharedPreferences("de.wladimircomputin.cryptohouse.devices", Context.MODE_PRIVATE);
        String devicesjson = sharedPrefs.getString(current_profile.id, "{}");
        ArrayList<DeviceManagerDevice> deviceManagerDevices = new ArrayList<>();
        try {
            JSONObject devicesObj = new JSONObject(devicesjson);
            JSONArray devicesArr = devicesObj.toJSONArray(devicesObj.names());
            for (int i = 0; i < devicesArr.length(); i++) {
                if(!devicesArr.getJSONObject(i).optString("type", "").equals("divider")) {
                    DeviceManagerDevice device = new DeviceManagerDevice(devicesArr.getJSONObject(i));
                    deviceManagerDevices.add(device);
                }
            }
            sharedPrefs.edit().putString(current_profile.id, devicesObj.toString()).apply();
        } catch (Exception x) {
            x.printStackTrace();
        }
        for (DeviceManagerDevice device : deviceManagerDevices) {
            if(!device.type.equals("divider")) {
                if (!devices.containsKey(device.group)) {
                    devices.put(device.group, new ArrayList<>());
                }
                ICryptoDevice device1 = null;
                switch (device.type) {
                    case "CryptoDimmer":
                        device1 = new CryptoDimmer(device, getContext());
                        break;
                    case "CryptoDimmer2":
                        device1 = new CryptoDimmer2(device, getContext());
                        break;
                    case "CryptoDimmer4":
                        device1 = new CryptoDimmer4(device, getContext());
                        break;
                    case "CryptoGarage":
                        device1 = new CryptoGarage(device, getContext());
                        break;
                    case "CryptoGeneric":
                        device1 = new CryptoGeneric(device, getContext());
                        break;
                    case "CryptoRollo":
                        device1 = new CryptoRollo(device, getContext());
                        break;
                    case "CryptoTimer":
                        device1 = new CryptoTimer(device, getContext());
                        break;
                    case "PlugSwitch":
                        device1 = new PlugSwitch(device, getContext());
                        break;
                    case "CryptoSML":
                        device1 = new CryptoSML(device, getContext());
                        break;
                    case "CryptoAC":
                        device1 = new CryptoAC(device, getContext());
                        break;
                    case "CryptoAC-TCL":
                        device1 = new CryptoAC_TCL(device, getContext());
                        break;
                    case "CryptoGeiger":
                        device1 = new CryptoGeiger(device, getContext());
                        break;
                    case "UranLight":
                        device1 = new UranLight(device, getContext());
                        break;
                    case "CryptoKamin":
                        device1 = new CryptoKamin(device, getContext());
                        break;
                    case "PlantWater":
                        device1 = new PlantWater(device, getContext());
                        break;
                    case "DoorLock":
                        device1 = new DoorLock(device, getContext());
                        break;
                    case "LightSwitch":
                        device1 = new LightSwitch(device, getContext());
                        break;
                    case "CIoTBell":
                        device1 = new CIoTBell(device, getContext());
                        break;
                    case "divider":
                        break;
                    default:
                        device1 = new CryptoGeneric(device, getContext());
                        break;
                }
                devices.get(device.group).add(device1);
            }
        }

        for (Map.Entry<String, ArrayList<ICryptoDevice>> entry : devices.entrySet()) {
            segmentFragments.add(new DeviceControlsSubFragment(entry.getKey(), entry.getValue()));
        }

        if (devices.isEmpty()) {
            if (binding.devicesControlsHint.getVisibility() != View.VISIBLE) {
                binding.devicesControlsHint.postDelayed(() -> {
                    binding.devicesControlsHint.setAlpha(0f);
                    binding.devicesControlsHint.setVisibility(View.VISIBLE);
                    binding.devicesControlsHint.animate().alpha(1f).setDuration(500);
                }, 500);
            } else {
                binding.devicesControlsHint.setVisibility(View.GONE);
            }
        }
        // Create and set up the adapter
        pagerAdapter = new CustomPagerAdapter(getChildFragmentManager(), getLifecycle(), segmentFragments);
        binding.devicesControlsViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                ((FocusListener)pagerAdapter.createFragment(position)).onSelected();
                for(int page = 0; page < pagerAdapter.getItemCount(); page++){
                    if(page != position) {
                        ((FocusListener) pagerAdapter.createFragment(position)).onUnselected();
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });
        binding.devicesControlsViewPager.setOffscreenPageLimit(10);
        binding.devicesControlsViewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(binding.devicesControlsTabLayout, binding.devicesControlsViewPager, (tab, position) -> {
            tab.setText(((PagerAdapterTitleProvider) pagerAdapter).getTitle(position));
        }).attach();

        if(devices.size() < 2){
            binding.devicesControlsTabLayout.setVisibility(View.GONE);
        }

        pagerAdapter.notifyDataSetChanged();

        return binding.getRoot();
    }

    @Override
    public void onPause() {
        super.onPause();
        detachUpdateHandler();
    }

    @Override
    public void onResume() {
        super.onResume();
        attachUpdateHandler(500);
    }

    private void autoUpdateIPs(){
        CryptCon.discoverDevices(new CryptConDiscoverReceiver() {
            @Override
            public void onSuccess(List<DiscoveryDevice> results) {
                List<DeviceManagerDevice> changedDevices = new ArrayList<>();
                for (DiscoveryDevice d : results) {
                    for (Map.Entry<String, ArrayList<ICryptoDevice>> entry : devices.entrySet()) {
                        for (ICryptoDevice device : entry.getValue()){
                            DeviceManagerDevice item = device.getDeviceManagerItem();
                            if(item.update_ip) {
                                if (item.equalsToDiscoveryDevice(d)) {
                                    DeviceManagerDevice backup = item.clone();
                                    item.hostname = d.name;
                                    item.ip = d.ip;
                                    item.mac = d.mac;
                                    item.type = d.type;
                                    if (!backup.equals(item)) {
                                        changedDevices.add(item);
                                        new Handler(Looper.getMainLooper()).post(() -> {
                                            device.reloadSettings();
                                        });
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

                if(!changedDevices.isEmpty()) {
                    String devicesjson = sharedPrefs.getString(current_profile.id, "{}");
                    try {
                        JSONObject devicesObj = new JSONObject(devicesjson);
                        for (DeviceManagerDevice changedDevice : changedDevices) {
                            devicesObj.put(changedDevice.id, changedDevice.toJSON());
                        }
                        sharedPrefs.edit().putString(current_profile.id, devicesObj.toString()).commit();
                    } catch (Exception x) {
                        x.printStackTrace();
                    }
                }
            }

            @Override
            public void onFail() {

            }

            @Override
            public void onFinished() {

            }
        });
    }

    private void attachUpdateHandler(int after){
        detachUpdateHandler();

        updateIPTimer = new Timer();
        updateIPTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                autoUpdateIPs();
            }
        }, after);
    }

    private void detachUpdateHandler(){
        if(updateIPTimer != null){
            updateIPTimer.cancel();
        }
    }
}
