package de.wladimircomputin.cryptohouse.devicesettings.General;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import de.wladimircomputin.cryptohouse.R;

public class GeneralSettingRecyclerListAdapter extends RecyclerView.Adapter<GeneralSettingHolder> {

    public final List<GeneralSettingItem> list;
    Context context;
    GeneralSettingClickListener callback;

    public GeneralSettingRecyclerListAdapter(List<GeneralSettingItem> list, GeneralSettingClickListener callback, Context context) {
        this.list = list;
        this.callback = callback;
        this.context = context;
    }

    @NonNull
    @Override
    public GeneralSettingHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.general_setting_item, parent, false);
        return new GeneralSettingHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GeneralSettingHolder holder, int position) {
        GeneralSettingItem item = list.get(holder.getBindingAdapterPosition());
        holder.titleText.setText(item.titleRes);
        holder.descriptionText.setText(item.descriptionRes);
        holder.itemView.setOnClickListener(v -> callback.onGeneralSettingClicked(item));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
