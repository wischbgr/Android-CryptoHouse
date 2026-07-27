package de.wladimircomputin.cryptohouse.devicesettings;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.HashMap;

import de.wladimircomputin.cryptohouse.R;
import de.wladimircomputin.cryptohouse.devicesettings.DeviceEvents.DeviceEventsFragment;
import de.wladimircomputin.cryptohouse.devicesettings.General.GeneralFragment;
import de.wladimircomputin.cryptohouse.devicesettings.PersistentMemory.PersistentMemoryFragment;
import de.wladimircomputin.cryptohouse.devicesettings.Terminal.TerminalFragment;
import de.wladimircomputin.cryptohouse.devicesettings.TimeEvents.TimeEventsFragment;
import de.wladimircomputin.cryptohouse.devicesettings.Update.UpdateFragment;
import de.wladimircomputin.cryptohouse.ui.PagerAdapterTitleProvider;
import de.wladimircomputin.cryptohouse.ui.PlaceholderFragment;

public class DeviceSettingsPagerAdapter extends FragmentStateAdapter implements PagerAdapterTitleProvider {
    public final String[] pages;
    final ArrayList<Class<? extends Fragment>> arr = new ArrayList<>();
    public HashMap<Integer, Fragment> hashMap = new HashMap<>();


    public DeviceSettingsPagerAdapter(FragmentActivity fa) {
        super(fa);
        pages = new String[]{fa.getString(R.string.general), fa.getString(R.string.time_events), fa.getString(R.string.device_events), fa.getString(R.string.pmem), fa.getString(R.string.terminal), fa.getString(R.string.update)};
        arr.add(GeneralFragment.class);
        arr.add(TimeEventsFragment.class);
        arr.add(DeviceEventsFragment.class);
        arr.add(PersistentMemoryFragment.class);
        arr.add(TerminalFragment.class);
        arr.add(UpdateFragment.class);
    }

    // Returns total number of pages
    @Override
    public int getItemCount() {
        return pages.length;
    }

    // Returns the fragment to display for that page
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment f;
        try {
            f = arr.get(position).getDeclaredConstructor().newInstance();
        } catch (Exception x){
            f = new PlaceholderFragment();
        }
        hashMap.put(position, f);
        return f;
    }

    @Override
    public String getTitle(int position) {
        return pages[position];
    }
}