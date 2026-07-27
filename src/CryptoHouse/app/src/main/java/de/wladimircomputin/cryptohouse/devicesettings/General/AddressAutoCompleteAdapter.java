package de.wladimircomputin.cryptohouse.devicesettings.General;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Backs an AutoCompleteTextView with suggestions fetched from {@link NominatimGeocoder}.
 * The filter is a passthrough: relevance filtering already happened server-side via the
 * search query itself, so it just republishes whatever {@link #updateSuggestions} set last.
 */
public class AddressAutoCompleteAdapter extends ArrayAdapter<String> {

    private List<String> suggestions = new ArrayList<>();

    public AddressAutoCompleteAdapter(Context context) {
        super(context, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
    }

    public void updateSuggestions(List<String> newSuggestions) {
        suggestions = newSuggestions;
        clear();
        addAll(suggestions);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return suggestions.size();
    }

    @Override
    public String getItem(int position) {
        return suggestions.get(position);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                results.values = suggestions;
                results.count = suggestions.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                notifyDataSetChanged();
            }
        };
    }
}
