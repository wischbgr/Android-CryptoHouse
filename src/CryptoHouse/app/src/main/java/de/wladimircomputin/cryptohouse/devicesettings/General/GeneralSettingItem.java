package de.wladimircomputin.cryptohouse.devicesettings.General;

public class GeneralSettingItem {

    public enum Type {
        SHOW_STATUS,
        CHANGE_HOSTNAME,
        CHANGE_DEVICE_PASSWORD,
        SET_LOCATION,
        REBOOT,
        RESET_FACTORY_DEFAULTS
    }

    public final Type type;
    public final int titleRes;
    public final int descriptionRes;

    public GeneralSettingItem(Type type, int titleRes, int descriptionRes) {
        this.type = type;
        this.titleRes = titleRes;
        this.descriptionRes = descriptionRes;
    }
}
