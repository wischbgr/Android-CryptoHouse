package de.wladimircomputin.cryptohouse.devicesettings.TimeEvents;

import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import de.wladimircomputin.cryptohouse.R;
import de.wladimircomputin.cryptohouse.boilerplate.ItemTouchHelperViewHolder;

public class TimeEventHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder {

    public final Button deleteButton;
    public final Button cloneButton;
    public final Spinner timeEventsSpinner;
    public final TextView timeEventsLabel;
    public final ImageView timeEventsImage;
    public final RecyclerView commandsRecycleview;
    public final Button addButton;


    public TimeEventHolder(View itemView) {
        super(itemView);
        timeEventsSpinner = itemView.findViewById(R.id.time_events_spinner);
        deleteButton = itemView.findViewById(R.id.delete_button);
        cloneButton = itemView.findViewById(R.id.clone_button);
        timeEventsLabel = itemView.findViewById(R.id.time_events_label);
        timeEventsImage = itemView.findViewById(R.id.time_events_image);
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