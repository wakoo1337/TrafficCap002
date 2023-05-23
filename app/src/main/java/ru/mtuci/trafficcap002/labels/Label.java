package ru.mtuci.trafficcap002.labels;

public class Label {
    private final String name, display_name;

    public Label(String name, String display_name) {
        this.name = name;
        this.display_name = display_name;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return display_name;
    }
}
