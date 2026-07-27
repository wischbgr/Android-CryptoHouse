package de.wladimircomputin.cryptohouse.devicesettings.DeviceEvents;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Property;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.wladimircomputin.cryptohouse.R;
import de.wladimircomputin.cryptohouse.actions.config.CommandAutoCompleteAdapter;
import de.wladimircomputin.cryptohouse.actions.config.CommandsCallback;
import de.wladimircomputin.cryptohouse.boilerplate.ItemTouchHelperAdapter;
import de.wladimircomputin.cryptohouse.boilerplate.OnStartDragListener;

public class DeviceEventCommandRecyclerListAdapter extends RecyclerView.Adapter<DeviceEventCommandItemHolder> implements ItemTouchHelperAdapter {

    public final List<String> list = new ArrayList<>();
    private final OnStartDragListener mDragStartListener;
    Context context;
    CommandsCallback callback;
    CommandAutoCompleteAdapter autoCompleteAdapter;

    public DeviceEventCommandRecyclerListAdapter(OnStartDragListener dragStartListener, CommandsCallback callback, CommandAutoCompleteAdapter autoCompleteAdapter, Context context) {
        mDragStartListener = dragStartListener;
        this.context = context;
        this.callback = callback;
        this.autoCompleteAdapter = autoCompleteAdapter;
    }

    @NonNull
    @Override
    public DeviceEventCommandItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.action_device_command_item, parent, false);
        return new DeviceEventCommandItemHolder(view);
    }

    @Override
    public void onBindViewHolder(final DeviceEventCommandItemHolder holder, int position) {
        holder.deleteButton.setOnClickListener(v -> {
            onItemRemove(holder.getBindingAdapterPosition());
        });

        holder.commandEdittext.setText(list.get(holder.getBindingAdapterPosition()));

        holder.commandEdittext.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                list.set(holder.getBindingAdapterPosition(), editable.toString());
                callback.onCommandsChange(list.toArray(new String[0]));
            }
        });
        holder.commandEdittext.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AutoCompleteTextView v = holder.commandEdittext;
                v.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        v.showDropDown();
                    }
                },100);
                v.setText(v.getText().toString());
                v.setSelection(v.getText().length());
            }
        });
        holder.commandEdittext.setOnClickListener((v) -> {
            holder.commandEdittext.showDropDown();
        });
        holder.commandEdittext.setThreshold(0);
        holder.commandEdittext.setAdapter(autoCompleteAdapter);
    }

    @Override
    public void onItemRemove(int position) {
        if(position >= 0 && position < list.size()) {
            list.remove(position);
            notifyItemRemoved(position);
            callback.onCommandsChange(list.toArray(new String[0]));
        }
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        Collections.swap(list, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        callback.onCommandsChange(list.toArray(new String[0]));
        return true;
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private void animateHeightTo(@NonNull View view, int height) {
        final int currentHeight = view.getHeight();
        ObjectAnimator animator = ObjectAnimator.ofInt(view, new HeightProperty(), currentHeight, height);
        animator.setDuration(300);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
    }

    static class HeightProperty extends Property<View, Integer> {

        public HeightProperty() {
            super(Integer.class, "height");
        }

        @Override public Integer get(View view) {
            return view.getHeight();
        }

        @Override public void set(View view, Integer value) {
            view.getLayoutParams().height = value;
            view.setLayoutParams(view.getLayoutParams());
        }
    }


}

