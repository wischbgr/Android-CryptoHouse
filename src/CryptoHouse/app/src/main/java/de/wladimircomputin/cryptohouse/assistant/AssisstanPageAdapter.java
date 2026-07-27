package de.wladimircomputin.cryptohouse.assistant;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;

import de.wladimircomputin.cryptohouse.R;
import de.wladimircomputin.cryptohouse.ui.PagerAdapterTitleProvider;
import de.wladimircomputin.cryptohouse.ui.PlaceholderFragment;

public class AssisstanPageAdapter extends FragmentStateAdapter implements PagerAdapterTitleProvider {
    public final String[] pages;
    final ArrayList<Class<? extends Fragment>> arr = new ArrayList<>();


    public AssisstanPageAdapter(FragmentActivity fa) {
        super(fa);
        pages = new String[]{fa.getString(R.string.connect), fa.getString(R.string.setup), fa.getString(R.string.finish)};
        arr.add(ConnectFragment.class);
        arr.add(SetupFragment.class);
        arr.add(FinishFragment.class);
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
        return f;
    }

    @Override
    public String getTitle(int position) {
        return pages[position];
    }
}