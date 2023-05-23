package ru.mtuci.trafficcap002.ui.attributes;

public abstract class Attribute {
    protected String name;
    protected String display_name;
    protected boolean enabled;

    protected Attribute(String name, String display_name) {
        this.name = name;
        this.display_name = display_name;
    }

    public final String getName() {
        return name;
    }

    public final String getDisplayName() {
        return display_name;
    }

    public final boolean getEnabled() {
        return enabled;
    }
}
