package ru.mtuci.trafficcap002.labels;

import java.util.Collections;
import java.util.List;

public final class Category {
    private final String name;
    private final String display_name;
    private final List<Label> labels;
    private boolean enabled;
    private int index;

    public Category(String name, String display_name, List<Label> labels) {
        this.name = name;
        this.display_name = display_name;
        this.labels = labels;
        this.enabled = false;
        this.index = 0;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return display_name;
    }

    public List<Label> getLabels() {
        return Collections.unmodifiableList(labels);
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        enabled = value;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
