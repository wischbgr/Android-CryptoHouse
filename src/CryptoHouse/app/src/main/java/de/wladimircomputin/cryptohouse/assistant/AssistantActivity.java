package de.wladimircomputin.cryptohouse.assistant;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;

import de.wladimircomputin.cryptohouse.databinding.ActivityAssistantBinding;
import de.wladimircomputin.cryptohouse.ui.PagerAdapterTitleProvider;

public class AssistantActivity extends AppCompatActivity {
    FragmentStateAdapter pagerAdapter;
    public DeviceSetupPack pack;

    ActivityAssistantBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAssistantBinding.inflate(getLayoutInflater());
        pagerAdapter = new AssisstanPageAdapter(this);
        binding.pager.setAdapter(pagerAdapter);
        binding.pager.setUserInputEnabled(false);
        binding.pager.setOffscreenPageLimit(2);
        binding.pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
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

        new TabLayoutMediator(binding.tabLayout, binding.pager, (tab, position) -> {
            tab.setText(((PagerAdapterTitleProvider)pagerAdapter).getTitle(position));
        }
        ).attach();
        setContentView(binding.getRoot());
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }

    public void nextPage() {
        binding.pager.setCurrentItem(binding.pager.getCurrentItem() + 1, true);
    }

    public void setPage(int page){
        binding.pager.setCurrentItem(page, true);
    }

    public void setProgessAnimateDelayed(ProgressBar pb, int progress, int delay){
        runOnUiThread(() -> {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                setProgressAnimate(pb,progress);
            }, delay);
        });
    }

    private void setProgressAnimate(ProgressBar pb, int progressTo) {
        if(progressTo > 300)
            progressTo = 300;
        ObjectAnimator animation = ObjectAnimator.ofInt(pb, "progress", pb.getProgress(), progressTo);
        animation.setDuration(300);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();
    }
}
