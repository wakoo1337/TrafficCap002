package com.wakoo.trafficcap002.labels;

import java.util.Set;

public final class Label implements Comparable<Label> {
    private final String label;
    private final Set<String> active;
    private boolean mark;

    public Label(String label, Set<String> active, boolean mark) {
        this.label = label;
        this.active = active;
        this.mark = mark;
    }

    public String getLabel() {
        return label;
    }

    public boolean getChecked() {
        return mark;
    }

    public void setChecked(boolean mark) {
        this.mark = mark;
        if (mark) {
            active.add(label);
        } else {
            active.remove(label);
        }
    }

    @Override
    public int compareTo(Label label) {
        return getLabel().compareTo(label.getLabel());
    }
}
