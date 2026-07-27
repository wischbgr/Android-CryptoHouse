package de.wladimircomputin.cryptohouse.devicemanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.List;
import java.util.UUID;

import de.wladimircomputin.cryptohouse.MainActivity;
import de.wladimircomputin.cryptohouse.R;
import de.wladimircomputin.cryptohouse.databinding.FragmentDevicemanagerBinding;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptCon;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptConDiscoverReceiver;
import de.wladimircomputin.libcryptoiot.v2.protocol.DiscoveryDevice;

public class DeviceManagerFragment extends Fragment {

    private ItemTouchHelper mItemTouchHelper;
    SharedPreferences sharedPrefs;
    DeviceRecyclerListAdapter listAdapter;

    FragmentDevicemanagerBinding binding;

    String current_profile;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = FragmentDevicemanagerBinding.inflate(inflater, container, false);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(R.string.device_manager);

        MainActivity mainActivity = (MainActivity)getActivity();
        listAdapter = new DeviceRecyclerListAdapter(getContext(), viewHolder -> mItemTouchHelper.startDrag(viewHolder));

        current_profile = mainActivity.getCurrentProfile().id;
        sharedPrefs = getContext().getSharedPreferences("de.wladimircomputin.cryptohouse.devices", Context.MODE_PRIVATE);
        String devicesjson = sharedPrefs.getString(current_profile, "{}");
        try {
            JSONObject devicesObj = new JSONObject(devicesjson);
            JSONArray devicesArr = devicesObj.toJSONArray(devicesObj.names());
            for(int i = 0; i < devicesArr.length(); i++){
                listAdapter.list.add(new DeviceManagerDevice(devicesArr.getJSONObject(i)));
            }
        } catch (Exception x){}

        binding.devicesRecycler.setHasFixedSize(true);
        binding.devicesRecycler.setAdapter(listAdapter);
        binding.devicesRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        listAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                onChanged();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                onChanged();
            }

            @Override
            public void onChanged() {
                super.onChanged();
                if(listAdapter.list.isEmpty() && binding.devicesManagerHint.getVisibility() != View.VISIBLE){
                    binding.devicesManagerHint.postDelayed(() -> {
                        binding.devicesManagerHint.setAlpha(0f);
                        binding.devicesManagerHint.setVisibility(View.VISIBLE);
                        binding.devicesManagerHint.animate().alpha(1f).setDuration(500);
                    }, 500);
                } else {
                    binding.devicesManagerHint.setVisibility(View.GONE);
                }
            }
        });
        binding.devicesManagerHint.setOnClickListener(view1 -> {
            scan();
        });
        listAdapter.notifyDataSetChanged();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(listAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(binding.devicesRecycler);

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear();
                menuInflater.inflate(R.menu.options_devicemanager, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.menu_apply) {
                    apply();
                    return true;
                } else if (menuItem.getItemId() == R.id.menu_add_device) {
                    add_device();
                    return true;
                } else if (menuItem.getItemId() == R.id.menu_add_divider) {
                    add_divider();
                    return true;
                } else if (menuItem.getItemId() == R.id.menu_scan) {
                    scan();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner());
    }

    private void apply(){
        File prefsdir = new File(getContext().getApplicationInfo().dataDir,"shared_prefs");


        if(prefsdir.exists() && prefsdir.isDirectory()) {
            String[] list = prefsdir.list();
            for (String prefname : list) {
                if (prefname.startsWith("de.wladimircomputin.cryptohouse.device.")) {
                    boolean contains = false;
                    for (DeviceManagerDevice item : listAdapter.list) {
                        if (prefname.contains(item.id)) {
                            contains = true;
                            break;
                        }
                    }
                    if (!contains) {
                        getContext().deleteSharedPreferences(prefname.replace(".xml", ""));
                    }
                }
            }
        }

        String currentSegment = "";
        JSONObject devicesObj = new JSONObject();
        for(int i = 0; i < listAdapter.list.size(); i++){
            try {
                DeviceManagerDevice d = listAdapter.list.get(i);
                if(d.type.equals("divider")){
                    currentSegment = d.name;
                }
                d.group = currentSegment;
                devicesObj.put(d.id, d.toJSON());
            } catch (Exception x){}
        }

        sharedPrefs.edit().putString(current_profile, devicesObj.toString()).commit();
        ((MainActivity)getActivity()).switchFragment(R.id.nav_devicecontrols);
    }

    private void add(DeviceManagerDevice device){
        int position;
        LinearLayoutManager layoutManager = ((LinearLayoutManager) binding.devicesRecycler.getLayoutManager());
        if(!listAdapter.list.isEmpty()) {
            if(!binding.devicesRecycler.canScrollVertically(1)){
                position = layoutManager.getItemCount();
            } else {
                position = layoutManager.findFirstCompletelyVisibleItemPosition();
            }
        } else {
            position = 0;
        }
        listAdapter.list.add(position, device);
        listAdapter.notifyItemInserted(position);
        if(position==0){
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                binding.devicesRecycler.smoothScrollToPosition(0);
            }, 500);
        } else if (position == layoutManager.getItemCount() - 1){
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                binding.devicesRecycler.smoothScrollToPosition(listAdapter.list.size() - 1);
            }, 500);
        }
    }

    private void add_device(){
        DeviceManagerDevice device = new DeviceManagerDevice(UUID.randomUUID().toString(), "", "CryptoGeneric", "", "", "", "", true, "");
        device.settingsVisible = true;
        add(device);
    }

    private void add_divider(){
        add(new DeviceManagerDevice(UUID.randomUUID().toString(), "", "divider", "", "", "", "", true, ""));
    }

    private void scan(){
        CryptCon.discoverDevices(new CryptConDiscoverReceiver() {
            @Override
            public void onSuccess(List<DiscoveryDevice> results) {
                for (DiscoveryDevice d : results) {
                    DeviceManagerDevice device = new DeviceManagerDevice(UUID.randomUUID().toString(), d.name, d.type, d.name, d.ip, d.mac, "", true, "");
                    boolean skip_existing = false;
                    for (int i1 = 0; i1 < listAdapter.list.size(); i1++) {
                        if (listAdapter.list.get(i1).equalsToDiscoveryDevice(d)) {
                            skip_existing = true;
                            break;
                        }
                    }
                    if (!skip_existing) {
                        getActivity().runOnUiThread(() -> {
                            device.settingsVisible = true;
                            listAdapter.list.add(device);
                            listAdapter.notifyItemInserted(listAdapter.list.size() - 1);
                        });
                    }
                }
                if(!listAdapter.list.isEmpty()) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        getActivity().runOnUiThread(() -> {
                            binding.devicesRecycler.smoothScrollToPosition(listAdapter.list.size() - 1);
                        });
                    }, 500);
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
}