
package com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list.annotations;

import org.ros.android.view.visualization.Color;
import org.ros.android.view.visualization.Vertices;
import org.ros.android.view.visualization.shape.TriangleFanShape;

import java.nio.FloatBuffer;

/**
 * Base class for all annotations to manage properties common to all annotations.
 * We store just one value for 2D (top view) size; subclasses must provide meaningful planar dimensions.
 * However the height is treated similarly in all annotations (though it's meaningless in some cases)
 */
public abstract class Annotation extends TriangleFanShape {
    protected String name;
    protected String group;
    protected float  sizeXY;
    protected float  height;
    protected float[] vertices;
    protected Color color;

    private static final ThreadLocal<FloatBuffer> buffer = new ThreadLocal<FloatBuffer>() {
        @Override
        protected FloatBuffer initialValue() {
            return FloatBuffer.allocate(16);
        };

        @Override
        public FloatBuffer get() {
            FloatBuffer buffer = super.get();
            buffer.clear();
            return buffer;
        };
    };

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Annotation(String name, float[] vertices, Color color) {
        super(vertices, color);
        this.name = name;
        this.vertices = vertices;
        this.color = color;
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

    @Override
    public void setColor(Color color) {
        this.color = color;
    }

    protected void invertScale(javax.microedition.khronos.opengles.GL10 gl) {

    }

    @Override
    public void draw(javax.microedition.khronos.opengles.GL10 gl) {
        FloatBuffer matrix = buffer.get();
        for (double value : getTransform().toMatrix()) {
            matrix.put((float) value);
        }
        matrix.position(0);
        gl.glMultMatrixf(matrix);
        scale(gl);
        Vertices.drawTriangleFan(gl, Vertices.toFloatBuffer(this.vertices),this.color);
        invertScale(gl);
        FloatBuffer inv_matrix = buffer.get();
        for (double value : getTransform().invert().toMatrix()) {
            inv_matrix.put((float) value);
        }
        inv_matrix.position(0);
        gl.glMultMatrixf(inv_matrix);

    }
}
