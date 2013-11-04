package com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list.annotations;

import org.ros.android.view.visualization.Color;

import javax.microedition.khronos.opengles.GL10;

public class Wall extends Annotation {
    private static final float MINIMUM_WIDTH  = 0.1f;
    private static final float MINIMUM_HEIGHT = 1.0f;
    private static final Color COLOR = Color.fromHexAndAlpha("841F27", 0.8f);
    private static final float VERTICES[] = rectangleVertices(0.1f, MINIMUM_WIDTH, 0.0f, 0.0f);

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

        width  = MINIMUM_WIDTH;
        height = MINIMUM_HEIGHT;
    }

    @Override
    protected void scale(GL10 gl) {
        // The scale is in metric space, so we can directly use shape's size.
        // Note that we scale only in y (width) dimension; wall's thickness remains constant
        gl.glScalef(1.0f, width / MINIMUM_WIDTH, 1.0f);
    }
}
