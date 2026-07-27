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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.wladimircomputin.cryptohouse.R;
import de.wladimircomputin.cryptohouse.actions.config.CommandAutoCompleteAdapter;
import de.wladimircomputin.cryptohouse.boilerplate.ItemTouchHelperAdapter;
import de.wladimircomputin.cryptohouse.boilerplate.OnStartDragListener;
import de.wladimircomputin.cryptohouse.boilerplate.SimpleItemTouchHelperCallback;
import de.wladimircomputin.libcryptoiot.v2.protocol.api.DeviceAPI;

public class DeviceEventRecyclerListAdapter extends RecyclerView.Adapter<DeviceEventHolder> implements ItemTouchHelperAdapter {

    public final List<DeviceEvent> list;
    private final OnStartDragListener mDragStartListener;
    private CommandAutoCompleteAdapter commandAutoCompleteAdapter;
    private ItemTouchHelper mItemTouchHelper;

    Context context;

    public DeviceEventRecyclerListAdapter(OnStartDragListener dragStartListener, List<DeviceEvent> deviceEvents, DeviceAPI deviceAPI, Context context) {
        mDragStartListener = dragStartListener;
        this.list = deviceEvents;
        this.context = context;
        commandAutoCompleteAdapter =  new CommandAutoCompleteAdapter(context, R.layout.support_simple_spinner_dropdown_item);
        commandAutoCompleteAdapter.update(deviceAPI);
    }

    @NonNull
    @Override
    public DeviceEventHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_events_item, parent, false);
        return new DeviceEventHolder(view);
    }

    @Override
    public void onBindViewHolder(final DeviceEventHolder holder, int position) {
        DeviceEventCommandRecyclerListAdapter deviceEventCommandRecyclerListAdapter = new DeviceEventCommandRecyclerListAdapter(viewHolder -> mItemTouchHelper.startDrag(viewHolder), new_commands -> {
            list.get(holder.getBindingAdapterPosition()).commands = new_commands;
            }, commandAutoCompleteAdapter, context);
        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(deviceEventCommandRecyclerListAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(holder.commandsRecycleview);
        holder.commandsRecycleview.setAdapter(deviceEventCommandRecyclerListAdapter);
        holder.commandsRecycleview.setLayoutManager(new LinearLayoutManager(context));
        deviceEventCommandRecyclerListAdapter.list.addAll(Arrays.asList(list.get(holder.getBindingAdapterPosition()).commands));
        deviceEventCommandRecyclerListAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                notifyItemChanged(-1);
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                notifyItemChanged(-1);
            }
        });
        deviceEventCommandRecyclerListAdapter.notifyDataSetChanged();

        holder.addButton.setOnClickListener(v -> {
            deviceEventCommandRecyclerListAdapter.list.add("");
            deviceEventCommandRecyclerListAdapter.notifyItemInserted(deviceEventCommandRecyclerListAdapter.getItemCount());
        });

        holder.deleteButton.setOnClickListener(v -> {
            onItemRemove(holder.getBindingAdapterPosition());
        });
        holder.cloneButton.setOnClickListener(v -> {
            onItemClone(holder.getBindingAdapterPosition());
        });

        holder.deviceEventsEventEdittext.setText(list.get(holder.getBindingAdapterPosition()).event);
        holder.deviceEventsEventEdittext.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                list.get(holder.getBindingAdapterPosition()).event = editable.toString();
            }
        });
    }

    @Override
    public void onItemRemove(int position) {
        if(position >= 0 && position < list.size()) {
            list.remove(position);
            notifyItemRemoved(position);
        }
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        Collections.swap(list, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    public void onItemClone(int position) {
        DeviceEvent item = list.get(position).clone();
        list.add(position+1, item);
        notifyItemInserted(position+1);
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

    public void updateDeviceApi(DeviceAPI deviceAPI){
        commandAutoCompleteAdapter.update(deviceAPI);
    }
}

