package ru.mtuci.trafficcap002.ui.attributes;

import android.view.View;

public interface Scaler {
    double[] scale(double[] x);
    int getConfigLayout();
}
