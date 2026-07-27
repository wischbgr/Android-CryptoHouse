package de.wladimircomputin.cryptohouse.actions.config;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

import de.wladimircomputin.cryptohouse.R;
import de.wladimircomputin.cryptohouse.actions.ActionItem;
import de.wladimircomputin.cryptohouse.boilerplate.SimpleItemTouchHelperCallback;
import de.wladimircomputin.cryptohouse.databinding.ActivityActionConfigBinding;
import de.wladimircomputin.cryptohouse.devicemanager.DeviceManagerDevice;

public class ActionConfigActivity extends AppCompatActivity {

    String action_id;
    String current_profile;
    ActionItem actionItem;
    SharedPreferences sharedPrefs;
    HashMap<String, DeviceManagerDevice> deviceManagerItems;
    ActionDeviceItemRecyclerListAdapter actionDeviceItemRecyclerListAdapter;
    ItemTouchHelper mItemTouchHelper;

    ActivityActionConfigBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityActionConfigBinding.inflate(getLayoutInflater());
        setSupportActionBar(binding.toolbar.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.actionsDevicesHint.setOnClickListener(view1 -> {
            add();
        });

        Intent intent = getIntent();
        current_profile = intent.getStringExtra("current_profile");
        action_id = intent.getStringExtra("action_id");
        deviceManagerItems = new HashMap<>();

        sharedPrefs = getSharedPreferences("de.wladimircomputin.cryptohouse.devices", Context.MODE_PRIVATE);
        String devicesjson = sharedPrefs.getString(current_profile, "{}");
        try {
            JSONObject devicesObj = new JSONObject(devicesjson);
            JSONArray devicesArr = devicesObj.toJSONArray(devicesObj.names());
            if (devicesArr != null) {
                for (int i = 0; i < devicesArr.length(); i++) {
                    DeviceManagerDevice deviceManagerDevice = new DeviceManagerDevice(devicesArr.getJSONObject(i));
                    if(!deviceManagerDevice.type.equals("divider")) {
                        deviceManagerItems.put(deviceManagerDevice.id, deviceManagerDevice);
                    }
                }

                actionDeviceItemRecyclerListAdapter = new ActionDeviceItemRecyclerListAdapter(viewHolder -> mItemTouchHelper.startDrag(viewHolder), deviceManagerItems, this);
                ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(actionDeviceItemRecyclerListAdapter);
                mItemTouchHelper = new ItemTouchHelper(callback);
                mItemTouchHelper.attachToRecyclerView(binding.actionConfigRecycleview);
                binding.actionConfigRecycleview.setAdapter(actionDeviceItemRecyclerListAdapter);
                binding.actionConfigRecycleview.setLayoutManager(new LinearLayoutManager(this));
                actionDeviceItemRecyclerListAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
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
                        if(actionDeviceItemRecyclerListAdapter.list.isEmpty() && binding.actionsDevicesHint.getVisibility() != View.VISIBLE){
                            binding.actionsDevicesHint.postDelayed(() -> {
                                binding.actionsDevicesHint.setAlpha(0f);
                                binding.actionsDevicesHint.setVisibility(View.VISIBLE);
                                binding.actionsDevicesHint.animate().alpha(1f).setDuration(500);
                            }, 500);
                        } else {
                            binding.actionsDevicesHint.setVisibility(View.GONE);
                        }
                    }
                });
            }
            sharedPrefs = getSharedPreferences("de.wladimircomputin.cryptohouse.actions", MODE_PRIVATE);
            if (!action_id.isEmpty()) {
                String actionsjson = sharedPrefs.getString(current_profile, "{}");
                try {
                    JSONObject actions = new JSONObject(actionsjson);
                    actionItem = new ActionItem(actions.getJSONObject(action_id), deviceManagerItems);
                } catch (JSONException x) {
                    actionItem = new ActionItem(UUID.randomUUID().toString(), binding.actionNameEdittext.getText().toString(), new ActionDeviceItem[]{});
                    x.printStackTrace();
                }
                actionDeviceItemRecyclerListAdapter.list.addAll(Arrays.asList(actionItem.action_device_items));
            } else {
                actionItem = new ActionItem(UUID.randomUUID().toString(), binding.actionNameEdittext.getText().toString(), new ActionDeviceItem[]{});
            }
            actionDeviceItemRecyclerListAdapter.notifyDataSetChanged();

            binding.actionNameEdittext.setText(actionItem.name);
            getSupportActionBar().setTitle(actionItem.name);


        } catch (JSONException x){
            x.printStackTrace();
        }
        setContentView(binding.getRoot());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case android.R.id.home:
                finish();
                break;

            case R.id.menu_apply:
                apply();
                break;

            case R.id.menu_add:
                add();
                break;
        }
        return true;
    }

    private void add() {
        int position;
        LinearLayoutManager layoutManager = ((LinearLayoutManager) binding.actionConfigRecycleview.getLayoutManager());
        if(!actionDeviceItemRecyclerListAdapter.list.isEmpty()) {
            if(!binding.actionConfigRecycleview.canScrollVertically(1)){
                position = layoutManager.getItemCount();
            } else {
                position = layoutManager.findFirstCompletelyVisibleItemPosition();
            }
        } else {
            position = 0;
        }
        actionDeviceItemRecyclerListAdapter.list.add(position, new ActionDeviceItem("0", null, new String[]{""}));
        actionDeviceItemRecyclerListAdapter.notifyItemInserted(position);
        if(position==0){
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                binding.actionConfigRecycleview.smoothScrollToPosition(0);
            }, 500);
        } else if (position == layoutManager.getItemCount() - 1){
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                binding.actionConfigRecycleview.smoothScrollToPosition(actionDeviceItemRecyclerListAdapter.list.size() - 1);
            }, 500);
        }
    }

    private void apply() {
        actionItem.name = binding.actionNameEdittext.getText().toString();
        actionItem.action_device_items = actionDeviceItemRecyclerListAdapter.list.toArray(new ActionDeviceItem[0]);
        String actionsjson = sharedPrefs.getString(current_profile, "{}");
        try {
            JSONObject actions = new JSONObject(actionsjson);
            actions.put(actionItem.id, actionItem.toJSON());
            sharedPrefs.edit().putString(current_profile, actions.toString()).apply();
            finish();
        } catch (JSONException x){
            x.printStackTrace();
        }
    }
}
