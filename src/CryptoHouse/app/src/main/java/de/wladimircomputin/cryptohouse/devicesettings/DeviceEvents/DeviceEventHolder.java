package de.wladimircomputin.cryptohouse.devicesettings.DeviceEvents;

import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

import androidx.recyclerview.widget.RecyclerView;

import de.wladimircomputin.cryptohouse.R;
import de.wladimircomputin.cryptohouse.boilerplate.ItemTouchHelperViewHolder;

public class DeviceEventHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder {

    public final Button deleteButton;
    public final Button cloneButton;
    public final AutoCompleteTextView deviceEventsEventEdittext;
    public final RecyclerView commandsRecycleview;
    public final Button addButton;


    public DeviceEventHolder(View itemView) {
        super(itemView);
        deleteButton = itemView.findViewById(R.id.delete_button);
        cloneButton = itemView.findViewById(R.id.clone_button);
        deviceEventsEventEdittext = itemView.findViewById(R.id.device_events_event_edittext);
        commandsRecycleview = itemView.findViewById(R.id.commands_recycleview);
        addButton = itemView.findViewById(R.id.add_button);
    }

    @Override
    public void onItemSelected() {
        itemView.animate().scaleX(1.05f);
        itemView.animate().scaleY(1.05f);
        itemView.animate().alpha(0.9f);
    }

    @Override
    public void onItemClear() {
        itemView.animate().alpha(1);
        itemView.animate().scaleX(1);
        itemView.animate().scaleY(1);
    }
    
}