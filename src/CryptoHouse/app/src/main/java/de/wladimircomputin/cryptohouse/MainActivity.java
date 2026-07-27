package de.wladimircomputin.cryptohouse;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.wladimircomputin.cryptohouse.actions.ActionFragment;
import de.wladimircomputin.cryptohouse.assistant.AssistantActivity;
import de.wladimircomputin.cryptohouse.devicecontrols.DeviceControlsFragment;
import de.wladimircomputin.cryptohouse.devicemanager.DeviceManagerDevice;
import de.wladimircomputin.cryptohouse.devicemanager.DeviceManagerFragment;
import de.wladimircomputin.cryptohouse.importexport.ImportExportFragment;
import de.wladimircomputin.cryptohouse.profile.ProfileFragment;
import de.wladimircomputin.cryptohouse.profile.ProfileItem;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout mDrawer;
    private Toolbar toolbar;
    private NavigationView nvDrawer;
    private ActionBarDrawerToggle drawerToggle;

    int currentFragment;
    List<ProfileItem> profiles;
    ProfileItem currentProfile = null;

    private Uri importUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(null);
        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mDrawer = findViewById(R.id.drawer_layout);
        drawerToggle = setupDrawerToggle();
        drawerToggle.setDrawerIndicatorEnabled(true);
        drawerToggle.syncState();
        mDrawer.addDrawerListener(drawerToggle);
        nvDrawer = findViewById(R.id.nvView);

        nvDrawer.getHeaderView(0).findViewById(R.id.nav_header_profile_expand).setOnClickListener((view) -> {
            if(nvDrawer.getMenu().findItem(R.id.nav_profile).isVisible()) {
                nvDrawer.getHeaderView(0).findViewById(R.id.nav_header_profile_expand).animate()
                        .rotation(0)
                        .setDuration(300)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
                nvDrawer.getMenu().findItem(R.id.nav_profile).setVisible(false);
            } else {
                nvDrawer.getHeaderView(0).findViewById(R.id.nav_header_profile_expand).animate()
                        .rotation(180f)
                        .setDuration(300)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
                nvDrawer.getMenu().findItem(R.id.nav_profile).setVisible(true);
            }
        });
        nvDrawer.getHeaderView(0).findViewById(R.id.nav_header_profile_image).setOnClickListener((view) -> {
            switchFragment(R.id.nav_profile);
            mDrawer.closeDrawers();
        });
        initSharedPrefs();
        loadProfiles();
        switchProfile(currentProfile);

        setupDrawerContent(nvDrawer);
        switchFragment(R.id.nav_devicecontrols);

        Intent intent = getIntent();
        if (Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null) {
            if ("application/json".equals(intent.getType())) {
                Uri jsonUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (jsonUri != null) {
                    importUri = jsonUri;
                    switchFragment(R.id.nav_backup_restore);
                }
            }
        }
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getType() != null) {
            if ("application/json".equals(intent.getType())) {
                Uri jsonUri = intent.getData();
                if (jsonUri != null) {
                    importUri = jsonUri;
                    switchFragment(R.id.nav_backup_restore);
                }
            }
        }
    }

    public void initSharedPrefs(){
        SharedPreferences sharedPrefs = getSharedPreferences("de.wladimircomputin.cryptohouse.main", Context.MODE_PRIVATE);
        int version_code = sharedPrefs.getInt("version_code", 0);
        if(version_code < BuildConfig.VERSION_CODE){
            if(version_code < 10) {
                oldVersionMigrations();
            }
            if(version_code < 430) {
                update0000ToUUID();
            }
        }
        sharedPrefs.edit().putInt("version_code", BuildConfig.VERSION_CODE).apply();
    }

    public void switchFragment(int id){
        nvDrawer.setCheckedItem(id);
        nvDrawer.getMenu().performIdentifierAction(id, 0);
    }

    public void switchProfile(ProfileItem profileItem){
        SharedPreferences sharedPrefs = getSharedPreferences("de.wladimircomputin.cryptohouse.profiles", Context.MODE_PRIVATE);
        sharedPrefs.edit().putString("current_profile", profileItem.id).apply();
        currentProfile = profileItem;
        nvDrawer.setCheckedItem(R.id.nav_devicecontrols);
        selectDrawerItem(nvDrawer.getMenu().findItem(R.id.nav_devicecontrols), true);
        SubMenu menu = nvDrawer.getMenu().findItem(R.id.nav_profile).getSubMenu();
        for(int i = 0; i < menu.size(); i++){
            MenuItem menuItem = menu.getItem(i);
            menuItem.setChecked(menuItem.getTitle().toString().equals(profileItem.name));
        }
        TextView textView = nvDrawer.getHeaderView(0).findViewById(R.id.nav_header_profile_text);
        textView.setText(currentProfile.name);
    }

    public void oldVersionMigrations(){
        SharedPreferences sharedPrefs = getSharedPreferences("de.wladimircomputin.cryptohouse.main", Context.MODE_PRIVATE);
        String devicesjson = sharedPrefs.getString("devices", "");
        if (!devicesjson.isEmpty()) {
            sharedPrefs.edit().remove("devices").apply();
            sharedPrefs = getSharedPreferences("de.wladimircomputin.cryptohouse.devices", Context.MODE_PRIVATE);
            sharedPrefs.edit().putString("0000", devicesjson).apply();
        }

        sharedPrefs = getSharedPreferences("de.wladimircomputin.cryptohouse.devices", Context.MODE_PRIVATE);
        Map<String, ?> allEntries = sharedPrefs.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            devicesjson = entry.getValue().toString();
            if (devicesjson.startsWith("[")) {
                try {
                    JSONArray devicesArr = new JSONArray(devicesjson);
                    JSONObject devicesObj = new JSONObject();
                    for (int i = 0; i < devicesArr.length(); i++) {
                        DeviceManagerDevice device = new DeviceManagerDevice(devicesArr.getJSONObject(i));
                        devicesObj.put(device.id, device.toJSON());
                    }
                    devicesjson = devicesObj.toString();
                    sharedPrefs.edit().putString(entry.getKey(), devicesjson).apply();
                } catch (Exception x) {
                }
            }
        }
    }

    public void update0000ToUUID(){
        try {
            SharedPreferences sharedPrefs = getSharedPreferences("de.wladimircomputin.cryptohouse.profiles", Context.MODE_PRIVATE);
            String profilejson = sharedPrefs.getString("profiles", "");
            String legacyUUID = "0000";
            String newUUID = UUID.randomUUID().toString();
            if(!profilejson.isEmpty()) {
                if(sharedPrefs.getString("current_profile", "").equals(legacyUUID)){
                    sharedPrefs.edit().putString("current_profile",newUUID).apply();
                }
                JSONArray arr;
                arr = new JSONArray(profilejson);
                for(int i = 0; i < arr.length(); i++) {
                    ProfileItem profileItem = new ProfileItem(arr.getJSONObject(i));
                    if (profileItem.id.equals(legacyUUID)) {
                        profileItem.id = newUUID;
                        arr.put(i, profileItem.toJSON());
                        sharedPrefs.edit().putString("profiles", arr.toString()).apply();
                        break;
                    }
                }
                sharedPrefs = getSharedPreferences("de.wladimircomputin.cryptohouse.devices", Context.MODE_PRIVATE);
                String devicesJson = sharedPrefs.getString(legacyUUID, "{}");
                sharedPrefs.edit().putString(newUUID, devicesJson).apply();
                sharedPrefs.edit().remove(legacyUUID).apply();

                sharedPrefs = getSharedPreferences("de.wladimircomputin.cryptohouse.actions", Context.MODE_PRIVATE);
                String actionsJson = sharedPrefs.getString(legacyUUID, "{}");
                sharedPrefs.edit().putString(newUUID, actionsJson).apply();
                sharedPrefs.edit().remove(legacyUUID).apply();
            }
        } catch (Exception x){

        }
    }

    public void loadProfiles(){
        try {
            SharedPreferences sharedPrefs = getSharedPreferences("de.wladimircomputin.cryptohouse.profiles", Context.MODE_PRIVATE);
            JSONArray arr;
            profiles = new ArrayList<>();
            String profilejson = sharedPrefs.getString("profiles", "");
            if(profilejson.isEmpty()){
                arr = new JSONArray();
                arr.put(new ProfileItem(UUID.randomUUID().toString(), "CryptoIoT", "").toJSON());
                sharedPrefs.edit().putString("profiles", arr.toString()).apply();
            } else {
                arr = new JSONArray(profilejson);
            }

            String current_profile = sharedPrefs.getString("current_profile", "");
            for(int i = 0; i < arr.length(); i++) {
                ProfileItem profileItem = new ProfileItem(arr.getJSONObject(i));
                if (profileItem.id.equals(current_profile)) {
                    currentProfile = profileItem;
                }
                profiles.add(profileItem);
            }
            if(currentProfile == null){
                currentProfile = profiles.get(0);
                sharedPrefs.edit().putString("current_profile", currentProfile.id).apply();
            }
            SubMenu menu = nvDrawer.getMenu().findItem(R.id.nav_profile).getSubMenu();
            menu.clear();
            int i = 0;
            for (ProfileItem profile : profiles) {
                menu.add(R.id.nav_profile, Menu.CATEGORY_ALTERNATIVE + i, i++, profile.name).setIcon(R.drawable.ic_baseline_account_circle_24).setOnMenuItemClickListener(menuItem -> {
                    switchProfile(profile);
                    menuItem.setChecked(true);
                    return true;
                });
            }
            menu.add(R.id.nav_profile, R.id.nav_add_profile, 100, R.string.add_profile).setIcon(R.drawable.ic_baseline_add_24);
        } catch (Exception x){
        }
    }

    public ProfileItem getCurrentProfile(){
        return currentProfile;
    }

    private ActionBarDrawerToggle setupDrawerToggle() {
        return new ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.drawer_open,  R.string.drawer_close);
    }

    public Uri getImportUri(){
        return importUri;
    }

    public void clearImportUri(){
        importUri = null;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(currentFragment == R.id.nav_devicecontrols) {
            finishAndRemoveTask();
        } else {
            switchFragment(R.id.nav_devicecontrols);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            // action with ID action_refresh was selected

            case android.R.id.home:
                mDrawer.openDrawer(GravityCompat.START);
                return true;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    private void setupDrawerContent(NavigationView navigationView) {
        nvDrawer.getMenu().findItem(R.id.nav_profile).setVisible(false);
        navigationView.setNavigationItemSelectedListener(
            menuItem -> {
                selectDrawerItem(menuItem);
                return true;
            });
    }

    public void selectDrawerItem(MenuItem menuItem) {
        selectDrawerItem(menuItem, false);
    }

    public void selectDrawerItem(MenuItem menuItem, boolean reload) {
        Fragment fragment;

        if (menuItem.getItemId() == R.id.nav_devicecontrols) {
            fragment = new DeviceControlsFragment();
        } else if (menuItem.getItemId() == R.id.nav_actions) {
            fragment = new ActionFragment();
        } else if (menuItem.getItemId() == R.id.nav_devicemanager) {
            fragment = new DeviceManagerFragment();
        } else if (menuItem.getItemId() == R.id.nav_profile) {
            fragment = new ProfileFragment(getCurrentProfile());
        } else if (menuItem.getItemId() == R.id.nav_add_profile) {
            fragment = new ProfileFragment();
        } else if (menuItem.getItemId() == R.id.nav_assistant) {
            Intent intent = new Intent(this, AssistantActivity.class);
            startActivity(intent);
            return;
        } else if (menuItem.getItemId() == R.id.nav_backup_restore) {
            fragment = new ImportExportFragment();
        } else if (menuItem.getItemId() == R.id.nav_about) {
            String version = BuildConfig.VERSION_NAME;
            String about = getString(R.string.about_author) + "\n\n" +
                    getString(R.string.about_source);
            AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
            SpannableString message = new SpannableString(about);
            Linkify.addLinks(message, Linkify.ALL);
            builder1.setMessage(message);
            builder1.setCancelable(true);
            builder1.setTitle("CryptoHouse " + version);
            AlertDialog aboutDialog = builder1.show();
            TextView messageView = aboutDialog.findViewById(android.R.id.message);
            if (messageView != null) {
                messageView.setMovementMethod(LinkMovementMethod.getInstance());
            }
            return;
        } else {
            fragment = new DeviceControlsFragment();
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment cf = fragmentManager.findFragmentById(R.id.flContent);
        if (cf == null || currentFragment != menuItem.getItemId() || reload){
            currentFragment = menuItem.getItemId();
            fragmentManager.beginTransaction().setCustomAnimations(R.anim.slide_in, R.anim.slide_out).replace(R.id.flContent, fragment).commit();
        }
        menuItem.setChecked(true);
        mDrawer.closeDrawers();
    }
}

