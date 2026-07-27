package de.wladimircomputin.cryptohouse.devicesettings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;

import de.wladimircomputin.cryptohouse.R;
import de.wladimircomputin.cryptohouse.assistant.FocusListener;
import de.wladimircomputin.cryptohouse.databinding.ActivityDeviceSettingsBinding;
import de.wladimircomputin.cryptohouse.devicemanager.DeviceManagerDevice;
import de.wladimircomputin.cryptohouse.devicesettings.Terminal.TerminalActivity_V1;
import de.wladimircomputin.cryptohouse.ui.PagerAdapterTitleProvider;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptCon;

public class DeviceSettingsActivity extends AppCompatActivity {
    public SharedPreferences sharedPref;
    public DeviceManagerDevice device;
    public CryptCon cc;
    private DeviceSettingsPagerAdapter deviceSettingsPagerAdapter;

    ActivityDeviceSettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDeviceSettingsBinding.inflate(getLayoutInflater());
        setSupportActionBar(binding.toolbar.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        String id = intent.getStringExtra("id");
        String name = intent.getStringExtra("name");
        String ip = intent.getStringExtra("ip");
        String pass = intent.getStringExtra("pass");
        String type = intent.getStringExtra("type");
        device = new DeviceManagerDevice(id, name, type, name, ip, "", pass, true, "");

        setTitle(device.name + " " + getString(R.string.settings));
        this.sharedPref = getSharedPreferences("de.wladimircomputin.cryptohouse.device." + device.id, Context.MODE_PRIVATE);
        this.cc = new CryptCon(pass, device.ip);

        // Create adapter for ViewPager2
        deviceSettingsPagerAdapter = new DeviceSettingsPagerAdapter(this);
        binding.deviceSettingsPager.setOffscreenPageLimit(10);
        binding.deviceSettingsPager.setAdapter(deviceSettingsPagerAdapter);

        new TabLayoutMediator(binding.deviceSettingsTabLayout, binding.deviceSettingsPager, (tab, position) -> {
            tab.setText(((PagerAdapterTitleProvider) deviceSettingsPagerAdapter).getTitle(position));
        }).attach();

        binding.deviceSettingsPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                ((FocusListener)deviceSettingsPagerAdapter.createFragment(position)).onSelected();
                for(int page = 0; page < deviceSettingsPagerAdapter.getItemCount(); page++){
                    if(page != position) {
                        ((FocusListener) deviceSettingsPagerAdapter.createFragment(position)).onUnselected();
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });
        setContentView(binding.getRoot());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected

            case android.R.id.home:
                finish();
                break;

            case R.id.menu_terminal_v1:
                terminal_v1();
                break;

            case R.id.menu_apply:
                return false;

            case R.id.menu_add:
                return false;

            default:
                break;
        }
        return true;
    }


    public void terminal_v1(){
        Intent intent = new Intent(this, TerminalActivity_V1.class);
        intent.putExtra("id", device.id);
        intent.putExtra("name", device.name);
        intent.putExtra("ip", device.ip);
        intent.putExtra("pass", device.pass);
        intent.putExtra("type", device.type);
        this.startActivity(intent);
    }
}