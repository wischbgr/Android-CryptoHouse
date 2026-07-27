package de.wladimircomputin.cryptohouse.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;

import java.util.UUID;

import de.wladimircomputin.cryptohouse.MainActivity;
import de.wladimircomputin.cryptohouse.R;
import de.wladimircomputin.cryptohouse.databinding.FragmentProfileBinding;

public class ProfileFragment extends Fragment {
    SharedPreferences sharedPrefs;

    ProfileItem profileItem;

    private FragmentProfileBinding binding;

    public ProfileFragment(){
        this.profileItem = new ProfileItem(UUID.randomUUID().toString(), "", "");
    }

    public ProfileFragment(ProfileItem profileItem){
        this.profileItem = profileItem;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        sharedPrefs = getContext().getSharedPreferences("de.wladimircomputin.cryptohouse.profiles", Context.MODE_PRIVATE);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(R.string.edit_profile);
        binding.profileFragmentName.setText(profileItem.name);
        binding.profileFragmentDelete.setVisibility(View.VISIBLE);
        binding.profileFragmentDelete.setOnClickListener((v) -> {
            new AlertDialog.Builder(getContext())
                .setTitle(R.string.delete_profile)
                .setMessage(R.string.delete_profile_question)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.yes, (dialog, whichButton) -> {
                    MainActivity activity = (MainActivity) getActivity();
                    deleteProfile();
                    activity.loadProfiles();
                    activity.switchProfile(activity.getCurrentProfile());
                    activity.switchFragment(R.id.nav_devicecontrols);
                })
                .setNegativeButton(R.string.no, null).show();
        });


        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear();
                menuInflater.inflate(R.menu.options_profile, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.menu_apply) {
                    apply();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner());
    }

    public void deleteProfile(){
        try {
            if(profileItem != null) {
                String profilejson = sharedPrefs.getString("profiles", "");
                JSONArray arr;
                arr = new JSONArray(profilejson);
                for (int i = 0; i < arr.length(); i++) {
                    ProfileItem p = new ProfileItem(arr.getJSONObject(i));
                    if (p.equals(profileItem)) {
                        arr.remove(i);
                        sharedPrefs.edit().putString("profiles", arr.toString()).apply();
                        break;
                    }
                }
            }
            SharedPreferences sharedPref2 = getContext().getSharedPreferences("devices", Context.MODE_PRIVATE);
            sharedPref2.edit().remove(profileItem.id).apply();

            sharedPref2 = getContext().getSharedPreferences("actions", Context.MODE_PRIVATE);
            sharedPref2.edit().remove(profileItem.id).apply();
        } catch (Exception x){}
    }

    public void saveProfile(){
        profileItem.name = binding.profileFragmentName.getText().toString();
        try {
            String profilejson = sharedPrefs.getString("profiles", "");
            JSONArray arr;
            arr = new JSONArray(profilejson);
            boolean updated = false;
            for (int i = 0; i < arr.length(); i++) {
                ProfileItem p = new ProfileItem(arr.getJSONObject(i));
                if (p.equals(profileItem)) {
                    arr.getJSONObject(i).put("name", profileItem.name);
                    //more
                    updated = true;
                    break;
                }
            }
            if(!updated){
                arr.put(profileItem.toJSON());
            }
            sharedPrefs.edit()
                    .putString("profiles", arr.toString())
                    .putString("current_profile", profileItem.id)
                    .apply();
        } catch (Exception x){}
    }

    public void apply(){
        hideSoftKeyboard();
        MainActivity activity = (MainActivity) getActivity();
        saveProfile();
        activity.loadProfiles();
        activity.switchProfile(profileItem);
        activity.switchFragment(R.id.nav_devicecontrols);
    }

    private void hideSoftKeyboard(){
        View view = getView().getRootView();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
