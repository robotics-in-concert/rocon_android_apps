package com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list.annotations;

import org.ros.android.view.visualization.Color;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

public class Marker extends Annotation {
    public static final String GROUP_NAME = "AR Markers";

    private static final Color COLOR = Color.fromHexAndAlpha("0af0f0", 0.8f);
    private static final float VERTICES[] = {
            -0.03f, -0.4f, 0.0f,
             0.04f,  0.0f, 0.0f,
            -0.03f,  0.4f, 0.0f
    };

    private int id; // AR maker unique id

    public Marker(String name) {
        super(name, VERTICES, COLOR);
        setGroup(GROUP_NAME);
    }

    @Override
    public void setName(String name) {
        try {
            id = Integer.parseInt(name);
            super.setName(name);
        }
        catch (NumberFormatException e) {
            throw new RuntimeException("Marker name must be a positive integer");
        }
    }

    public int getId() { return id; }

    public void setHeight(float height)
    {
        // Markers have fixed size and are 3D-located, so height represents is z-coordinate
        this.setTransform(this.getTransform().multiply(Transform.translation(new Vector3(0.0, 0.0, height))));
    }
}
