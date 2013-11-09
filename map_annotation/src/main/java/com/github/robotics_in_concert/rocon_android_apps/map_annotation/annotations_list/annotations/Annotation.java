
package com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list.annotations;

import org.ros.android.view.visualization.Color;
import org.ros.android.view.visualization.shape.TriangleFanShape;

/**
 * Base class for all annotations to manage properties common to all annotations.
 * We store just one value for 2D (top view) size; subclasses must provide meaningfull planar dimensions.
 * However the height is treated similarly in all annotations (though it's meaningless in some cases)
 */
public abstract class Annotation extends TriangleFanShape {
    protected String name;
    protected String group;
    protected float  sizeXY;
    protected float  height;

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

    public float getSizeXY() { return sizeXY; }

    public void setSizeXY(float sizeXY) {
        this.sizeXY = sizeXY;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }
}
