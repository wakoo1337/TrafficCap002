package com.wakoo.trafficcap002;

public final class Label implements Comparable<Label> {
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

    @Override
    public int compareTo(Label label) {
        return getLabel().compareTo(label.getLabel());
    }
}
