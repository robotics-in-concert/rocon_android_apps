
package com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list.annotations;

import org.ros.android.view.visualization.Color;
import org.ros.android.view.visualization.shape.TriangleFanShape;

public abstract class Annotation extends TriangleFanShape {
    protected String name;
    protected String group;
    protected double width;
    protected double height;

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Annotation(String name, float[] vertices, Color color) {
        super(vertices, color);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }
}
