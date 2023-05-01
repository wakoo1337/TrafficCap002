package com.wakoo.trafficcap002;

public class Label {
    private final String label;
    private boolean mark;

    public Label(String label, boolean mark) {
        this.label = label;
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
    }
}
