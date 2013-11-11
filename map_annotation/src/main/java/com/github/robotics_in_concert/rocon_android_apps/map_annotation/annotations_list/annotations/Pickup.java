package com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list.annotations;

import org.ros.android.view.visualization.Color;

public class Pickup extends Annotation {
    public static final String GROUP_NAME = "Pickup Points";

    private static final Color COLOR = Color.fromHexAndAlpha("FDB813", 0.8f);
    private static final float VERTICES[] = starVertices(5, 0.224f, 0.5f, 0.0f, 0.0f);

    public Pickup(String name) {
        super(name, VERTICES, COLOR);
        setGroup(GROUP_NAME);
    }

    private static float[] starVertices(int arms, float radInner, float radOuter,
                                        float center_x, float center_y) {
        double angle = Math.PI / arms;

        // create a buffer for vertex data
        float buffer[] = new float[(2 * arms + 2) * 3]; // (x,y,z) for each vertex
        int idx = 0;

        // center vertex for triangle fan
        buffer[idx++] = center_x;
        buffer[idx++] = center_y;
        buffer[idx++] = 0.0f;  // z-axis

        // outer vertices of the circle
        int outerVertexCount = 2 * arms + 1;

        for (int i = 0; i < outerVertexCount; i++) {
            float percent = (i / (float) (outerVertexCount - 1));
            float radians = percent * 2f * (float)Math.PI;
            float radius  = (i & 1) == 0 ? radOuter : radInner;

            // vertex position
            float outer_x = center_x + radius * (float)Math.cos(radians);
            float outer_y = center_y + radius * (float)Math.sin(radians);

            buffer[idx++] = outer_x;
            buffer[idx++] = outer_y;
            buffer[idx++] = 0.0f;  // z-axis
        }

        return buffer;
    }

    // Dimensions are meaningless for a pickup point; just set an arbitrary (but visible) size
    public float getRadius() { return 0.04f; }
    public float getHeight() { return 0.4f;  }
}
