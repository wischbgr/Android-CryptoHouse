package de.wladimircomputin.cryptohouse.devicesettings.TimeEvents;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.TimePickerDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Property;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.wladimircomputin.cryptohouse.R;
import de.wladimircomputin.cryptohouse.actions.config.CommandAutoCompleteAdapter;
import de.wladimircomputin.cryptohouse.boilerplate.ItemTouchHelperAdapter;
import de.wladimircomputin.cryptohouse.boilerplate.OnStartDragListener;
import de.wladimircomputin.cryptohouse.boilerplate.SimpleItemTouchHelperCallback;
import de.wladimircomputin.cryptohouse.devicesettings.DeviceEvents.DeviceEventCommandRecyclerListAdapter;
import de.wladimircomputin.libcryptoiot.v2.protocol.api.DeviceAPI;
import de.wladimircomputin.libcryptoiot.v2.protocol.api.TimeEventType;

public class TimeEventRecyclerListAdapter extends RecyclerView.Adapter<TimeEventHolder> implements ItemTouchHelperAdapter {

    public final List<TimeEvent> list;
    private final OnStartDragListener mDragStartListener;
    private CommandAutoCompleteAdapter commandAutoCompleteAdapter;
    private ItemTouchHelper mItemTouchHelper;

    Context context;

    public TimeEventRecyclerListAdapter(OnStartDragListener dragStartListener, List<TimeEvent> timeEvents, DeviceAPI deviceAPI, Context context) {
        mDragStartListener = dragStartListener;
        this.list = timeEvents;
        this.context = context;
        commandAutoCompleteAdapter =  new CommandAutoCompleteAdapter(context, R.layout.support_simple_spinner_dropdown_item);
        commandAutoCompleteAdapter.update(deviceAPI);
    }

    @NonNull
    @Override
    public TimeEventHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.time_events_item, parent, false);
        return new TimeEventHolder(view);
    }

    @Override
    public void onBindViewHolder(final TimeEventHolder holder, int position) {
        TimeEventCommandRecyclerListAdapter timeEventCommandRecyclerListAdapter = new TimeEventCommandRecyclerListAdapter(viewHolder -> mItemTouchHelper.startDrag(viewHolder), new_commands -> {
            list.get(holder.getBindingAdapterPosition()).commands = new_commands;
        }, commandAutoCompleteAdapter, context);
        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(timeEventCommandRecyclerListAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(holder.commandsRecycleview);
        holder.commandsRecycleview.setAdapter(timeEventCommandRecyclerListAdapter);
        holder.commandsRecycleview.setLayoutManager(new LinearLayoutManager(context));
        timeEventCommandRecyclerListAdapter.list.addAll(Arrays.asList(list.get(holder.getBindingAdapterPosition()).commands));
        timeEventCommandRecyclerListAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
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
        timeEventCommandRecyclerListAdapter.notifyDataSetChanged();

        holder.addButton.setOnClickListener(v -> {
            timeEventCommandRecyclerListAdapter.list.add("");
            timeEventCommandRecyclerListAdapter.notifyItemInserted(timeEventCommandRecyclerListAdapter.getItemCount());
        });

        holder.deleteButton.setOnClickListener(v -> {
            onItemRemove(holder.getBindingAdapterPosition());
        });
        holder.cloneButton.setOnClickListener(v -> {
            onItemClone(holder.getBindingAdapterPosition());
        });

        ArrayAdapter<CharSequence> timeEventsSpinnerAdapter = (ArrayAdapter<CharSequence>) (holder.timeEventsSpinner.getAdapter());
        int selection = 0;
        switch (list.get(holder.getBindingAdapterPosition()).timeEventType) {
            case DISABLED:
                selection = timeEventsSpinnerAdapter.getPosition("DISABLED");
                break;
            case SUNRISE:
                selection = timeEventsSpinnerAdapter.getPosition("SUNRISE");
                break;
            case SUNSET:
                selection = timeEventsSpinnerAdapter.getPosition("SUNSET");
                break;
            case TIME:
                selection = timeEventsSpinnerAdapter.getPosition("TIME");
                break;
            default:
                break;
        }
        holder.timeEventsSpinner.setSelection(selection);
        holder.timeEventsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                list.get(holder.getBindingAdapterPosition()).timeEventType = TimeEventType.fromString(timeEventsSpinnerAdapter.getItem(i).toString());
                if (list.get(holder.getBindingAdapterPosition()).timeEventType == TimeEventType.TIME) {
                    holder.timeEventsImage.setVisibility(View.INVISIBLE);
                    holder.timeEventsLabel.setAlpha(0f);
                    holder.timeEventsLabel.animate()
                            .alpha(1f)
                            .setDuration(500)
                            .setInterpolator(new DecelerateInterpolator());
                    holder.timeEventsLabel.setVisibility(View.VISIBLE);
                    String time = list.get(holder.getBindingAdapterPosition()).time;
                    if (!time.isEmpty()) {
                        LocalTime target = LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"));
                        ValueAnimator time_value_animator = ValueAnimator.ofInt(0, target.toSecondOfDay());
                        time_value_animator.addUpdateListener(valueAnimator -> {
                            LocalTime temp = LocalTime.ofSecondOfDay((int) valueAnimator.getAnimatedValue());
                            holder.timeEventsLabel.setText(temp.format(DateTimeFormatter.ofPattern("HH:mm")));
                        });
                        time_value_animator.setDuration(500);
                        time_value_animator.setInterpolator(new DecelerateInterpolator());
                        time_value_animator.start();
                    } else {
                        holder.timeEventsLabel.callOnClick();
                    }

                } else {
                    if (list.get(holder.getBindingAdapterPosition()).timeEventType == TimeEventType.SUNRISE) {
                        holder.timeEventsImage.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.baseline_wb_sunny_24));
                    } else if (list.get(holder.getBindingAdapterPosition()).timeEventType == TimeEventType.SUNSET) {
                        holder.timeEventsImage.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.baseline_nights_stay_24));
                    } else {
                        holder.timeEventsImage.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.baseline_cancel_24));
                    }
                    holder.timeEventsImage.setTranslationY(holder.timeEventsImage.getHeight());
                    holder.timeEventsImage.setAlpha(0f);
                    holder.timeEventsImage.setRotation(100f);

                    holder.timeEventsImage.animate()
                            .translationY(0)
                            .alpha(1.0f)
                            .setDuration(500)
                            .rotation(0f)
                            .setInterpolator(new OvershootInterpolator())
                    ;
                    holder.timeEventsImage.setVisibility(View.VISIBLE);
                    holder.timeEventsLabel.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        holder.timeEventsLabel.setText(list.get(holder.getBindingAdapterPosition()).time);

        holder.timeEventsLabel.setOnClickListener(view -> {
            if (list.get(holder.getBindingAdapterPosition()).timeEventType == TimeEventType.TIME) {
                LocalTime old = LocalTime.parse(list.get(holder.getBindingAdapterPosition()).time, DateTimeFormatter.ofPattern("HH:mm"));
                TimePickerDialog timePickerDialog = new TimePickerDialog(context,
                        (TimePickerDialog.OnTimeSetListener) (view1, hourOfDay, minute) -> {
                            LocalTime target = LocalTime.of(hourOfDay, minute);
                            list.get(holder.getBindingAdapterPosition()).time = target.format(DateTimeFormatter.ofPattern("HH:mm"));
                            ValueAnimator time_value_animator = ValueAnimator.ofInt(old.toSecondOfDay(), target.toSecondOfDay());
                            time_value_animator.addUpdateListener(valueAnimator -> {
                                LocalTime temp = LocalTime.ofSecondOfDay((int) valueAnimator.getAnimatedValue());
                                holder.timeEventsLabel.setText(temp.format(DateTimeFormatter.ofPattern("HH:mm")));
                            });
                            time_value_animator.setDuration(500);
                            time_value_animator.setInterpolator(new DecelerateInterpolator());
                            time_value_animator.start();
                        }, old.getHour(), old.getMinute(), true);
                timePickerDialog.show();
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
        TimeEvent item = list.get(position).clone();
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

