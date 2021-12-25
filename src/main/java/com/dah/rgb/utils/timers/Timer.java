package com.dah.rgb.utils.timers;

public interface Timer {
    double getTime();

    default double getProgress(double fromTime, double toTime) {
        return (getTime() - fromTime) / (toTime - fromTime);
    }
}
