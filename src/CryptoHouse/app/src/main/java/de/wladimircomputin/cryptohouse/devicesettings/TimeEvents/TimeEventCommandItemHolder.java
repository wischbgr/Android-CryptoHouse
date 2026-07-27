package de.wladimircomputin.cryptohouse.devicesettings.TimeEvents;

import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

import androidx.recyclerview.widget.RecyclerView;

import de.wladimircomputin.cryptohouse.R;
import de.wladimircomputin.cryptohouse.boilerplate.ItemTouchHelperViewHolder;

public class TimeEventCommandItemHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder {

    public final AutoCompleteTextView commandEdittext;
    public final Button deleteButton;


    public TimeEventCommandItemHolder(View itemView) {
        super(itemView);
        commandEdittext = itemView.findViewById(R.id.command_edittext);
        deleteButton = itemView.findViewById(R.id.delete_button);
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