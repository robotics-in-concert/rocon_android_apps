package com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list.annotations;

import org.ros.android.view.visualization.Color;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

public class Location extends Annotation {
    public static final String GROUP_NAME = "Location Points";
    private static final float DEFAULT_HEIGHT = 1.0f;
    private static final float DEFAULT_RADIUS = 0.4f;
    private static final Color COLOR = Color.fromHexAndAlpha("FDB813", 0.8f);
    private static final float VERTICES[] = {
            -0.2f, -0.2f, 0.0f,
            0.4f,  0.0f, 0.0f,
            -0.2f,  0.2f, 0.0f
    };

    public Location(String name) {
        super(name, VERTICES, COLOR);
        setGroup(GROUP_NAME);
        height = DEFAULT_HEIGHT;
        sizeXY = DEFAULT_RADIUS;
    }

}
