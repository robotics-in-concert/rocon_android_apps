/*
 * Copyright (C) 2013 Yujin Robot.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.github.robotics_in_concert.rocon_android_apps.map_annotation;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list.AnnotationsList;
import com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list.annotations.Annotation;
import com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list.annotations.Column;
import com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list.annotations.Location;
import com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list.annotations.Marker;
import com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list.annotations.Table;
import com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list.annotations.Wall;
import com.github.rosjava.android_remocons.common_tools.apps.AppParameters;
import com.google.common.base.Preconditions;

import org.ros.android.view.visualization.Camera;
import org.ros.android.view.visualization.VisualizationView;
import org.ros.android.view.visualization.layer.DefaultLayer;
import org.ros.android.view.visualization.shape.GoalShape;
import org.ros.android.view.visualization.shape.PoseShape;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.rosjava_geometry.FrameTransformTree;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * Shows a map retrieved from map_store and allows add annotations (i.e. semantic information)
 *
 * @author jorge@yujinrobot.com (Jorge Santos Simon)
 */
public class MapAnnotationLayer extends DefaultLayer {

    private final Context context;
    private Annotation annotation;
    private GestureDetector gestureDetector;
    private Transform pose;
    private Transform fixedPose;
    private Camera camera;
    private ConnectedNode connectedNode;
    private AnnotationsList annotationsList;
    private Mode mode;
    private PoseShape origin_shape;
    private GoalShape camera_shape;
    private double distance = 1.0;
    private AppParameters params;

    public enum Mode {
        ADD_MARKER, ADD_TABLE, ADD_COLUMN, ADD_WALL, ADD_LOCATION
    }

    public MapAnnotationLayer(final Context context, final AnnotationsList annotationsList,
                              final AppParameters params) {
        this.context = context;
        this.annotationsList = annotationsList;
        this.params = params;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

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

    @Override
    public void draw(GL10 gl) {
        // Draw currently creating annotation
        if (annotation != null) {
            annotation.draw(gl);
        }
        //Draw already created annotations
        for (Annotation annotation: annotationsList.listFullContent()) {
            annotation.draw(gl);
        }
    }

    private double angle(double x1, double y1, double x2, double y2) {
        double deltaX = x1 - x2;
        double deltaY = y1 - y2;
        return Math.atan2(deltaY, deltaX);
    }

    private double dist(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    @Override
    public boolean onTouchEvent(VisualizationView view, MotionEvent event) {
        if (annotation != null) {
            Preconditions.checkNotNull(pose);

            Vector3 poseVector;
            Vector3 pointerVector;

            if (event.getAction() == MotionEvent.ACTION_MOVE) {

                poseVector = pose.apply(Vector3.zero());
                pointerVector = camera.toMetricCoordinates((int) event.getX(), (int) event.getY());

                double dist  = dist(pointerVector.getX(), pointerVector.getY(),
                                    poseVector.getX(), poseVector.getY());
                double angle = angle(pointerVector.getX(), pointerVector.getY(),
                                     poseVector.getX(), poseVector.getY());
                pose = Transform.translation(poseVector).multiply(Transform.zRotation(angle));

                annotation.setTransform(pose);
                annotation.setSizeXY((float) dist);
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                confirmAnnotation();
                return true;
           }
        }
        gestureDetector.onTouchEvent(event);
        return false;
    }

    @Override
    public void onStart(ConnectedNode connectedNode, Handler handler,
                        FrameTransformTree frameTransformTree, final Camera camera) {
        this.camera = camera;
        this.camera.setFrame((String) params.get("map_frame", context.getString(R.string.map_frame)));
        this.origin_shape = new PoseShape(camera);
        this.camera_shape = new GoalShape();
        this.mode = Mode.ADD_MARKER;

        handler.post(new Runnable() {
            @Override
            public void run() {
                gestureDetector = new GestureDetector(context,
                        new GestureDetector.SimpleOnGestureListener() {
                            @Override
                            public void onLongPress(MotionEvent e) {
                                pose = Transform.translation(camera.toMetricCoordinates((int) e.getX(), (int) e.getY()));
                                switch (mode) {
                                    case ADD_MARKER:
                                        annotation = new Marker("marker 1");
                                        break;
                                    case ADD_TABLE:
                                        annotation = new Table("Table 1");
                                        break;
                                    case ADD_COLUMN:
                                        annotation = new Column("Column 1");
                                        break;
                                    case ADD_WALL:
                                        annotation = new Wall("Wall 1");
                                        break;
                                    case ADD_LOCATION:
                                        annotation = new Location("Location 1");
                                        break;
                                    default:
                                        Log.e("MapAnn", "Unimplemented annotation mode: " + mode);
                                }

                                annotation.setTransform(pose);
                                //fixedPose = Transform.translation(camera.toMetricCoordinates((int) e.getX(), (int) e.getY()));
                            }
                        });
            }
        });
    }

    @Override
    public void onShutdown(VisualizationView view, Node node) {
    }

    private void confirmAnnotation() {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View promptView = layoutInflater.inflate(R.layout.annotation_cfg, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setView(promptView);

        final boolean annotationAccepted = false;
        final EditText name_edit    = (EditText) promptView.findViewById(R.id.name_edit);
        final EditText height_edit  = (EditText) promptView.findViewById(R.id.height_edit);
        final TextView name_label   = (TextView) promptView.findViewById(R.id.name_label);
        final TextView height_label = (TextView) promptView.findViewById(R.id.height_label);

        // Customize for some slightly exotic annotations
        if (mode == Mode.ADD_MARKER)                // Marker name must be its id, a positive integer
            name_edit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        // Setup a dialog windowAnnotation
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // get user input and set it to result
                try {
                    if (name_edit.getText().length() == 0)
                        throw new Exception("Annotation name cannot be empty");
                    if (height_edit.getText().length() > 0)
                        annotation.setHeight(Float.parseFloat(height_edit.getText().toString()));

                    annotation.setName(name_edit.getText().toString());
                    annotationsList.addItem(annotation);
                } catch (NumberFormatException e) {
                    Toast.makeText(context, "Height must be a number; discarding...", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
                } finally {
                    annotation = null;
                }
            }
        }
        ).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                annotation = null;
            }
        });

        // Create and show an alert dialog to get annotation info
        final AlertDialog alertDlg = alertDialogBuilder.create();
        alertDlg.show();

        name_edit.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                alertDlg.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setEnabled(name_edit.getText().length() > 0);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
    }
}
