package com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list.annotations;

import org.ros.android.view.visualization.Color;

public class Column extends Annotation {
    private static final Color COLOR = Color.fromHexAndAlpha("E6E4D8", 0.8f);
    private static final float VERTICES[] = circleVertices(30, 0.1f, 0.0f, 0.0f);

    private static float[] circleVertices(int vertexCount, float radius,
                                          float center_x, float center_y) {
        //create a buffer for vertex data
        float buffer[] = new float[vertexCount*3]; // (x,y,z) for each vertex
        int idx = 0;

        // center vertex for triangle fan
        buffer[idx++] = center_x;
        buffer[idx++] = center_y;
        buffer[idx++] = 0.0f;  // z-axis

        // outer vertices of the circle
        int outerVertexCount = vertexCount-1;

        for (int i = 0; i < outerVertexCount; ++i) {
            float percent = (i / (float) (outerVertexCount-1));
            float rad = percent * 2f * (float)Math.PI;

            // vertex position
            float outer_x = center_x + radius * (float)Math.cos(rad);
            float outer_y = center_y + radius * (float)Math.sin(rad);

            buffer[idx++] = outer_x;
            buffer[idx++] = outer_y;
            buffer[idx++] = 0.0f;  // z-axis
        }

        return buffer;
    }

    public Column(String name) {
        super(name, VERTICES, COLOR);
        setGroup("Virtual Columns");
    }

    @Override
    public void setWidth(double width) {
        super.setWidth(width);

        setVertices(circleVertices(30, (float)width/2.0f, 0.0f, 0.0f));
    }
}
