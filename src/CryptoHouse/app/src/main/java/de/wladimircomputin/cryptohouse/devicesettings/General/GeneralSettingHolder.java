package de.wladimircomputin.cryptohouse.devicesettings.General;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import de.wladimircomputin.cryptohouse.R;

public class GeneralSettingHolder extends RecyclerView.ViewHolder {

    public final TextView titleText;
    public final TextView descriptionText;

    public GeneralSettingHolder(View itemView) {
        super(itemView);
        titleText = itemView.findViewById(R.id.general_setting_title);
        descriptionText = itemView.findViewById(R.id.general_setting_description);
    }
}
