package com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list.annotations;

import org.ros.android.view.visualization.Color;

public class Wall extends Annotation {

    private static final Color COLOR = Color.fromHexAndAlpha("841F27", 0.8f);
    private static final float VERTICES[] = rectangleVertices(0.1f, 0.4f, 0.0f, 0.0f);

    private static float[] rectangleVertices(float length, float width,
                                             float center_x, float center_y) {
        // create a buffer for vertex data
        float buffer[] = new float[4 * 3]; // (x,y,z) for each vertex

        int idx = 0;

        // center vertex for triangle fan
        buffer[idx++] = center_x - length/2;
        buffer[idx++] = center_y - width/2;
        buffer[idx++] = 0.0f;  // z-axis
        buffer[idx++] = center_x - length/2;
        buffer[idx++] = center_y + width/2;
        buffer[idx++] = 0.0f;  // z-axis
        buffer[idx++] = center_x + length/2;
        buffer[idx++] = center_y + width/2;
        buffer[idx++] = 0.0f;  // z-axis
        buffer[idx++] = center_x + length/2;
        buffer[idx++] = center_y - width/2;
        buffer[idx++] = 0.0f;  // z-axis

        return buffer;
    }

    public Wall(String name) {
        super(name, VERTICES, COLOR);
        setGroup("Virtual Walls");
    }

    @Override
    public void setWidth(double width) {
        super.setWidth(width);

        setVertices(rectangleVertices(0.1f, (float)width*2.0f, 0.0f, 0.0f));
    }
}
