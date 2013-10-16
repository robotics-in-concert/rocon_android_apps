package com.github.robotics_in_concert.rocon_android_apps.map_annotation;

import android.content.Context;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list.ExpandableListAdapter;
import com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list.annotations.*;
import com.google.common.base.Preconditions;

import org.ros.android.view.visualization.Camera;
import org.ros.android.view.visualization.VisualizationView;
import org.ros.android.view.visualization.layer.DefaultLayer;
import org.ros.android.view.visualization.shape.GoalShape;
import org.ros.android.view.visualization.shape.PoseShape;
import org.ros.namespace.NameResolver;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.rosjava_geometry.FrameTransformTree;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

import javax.microedition.khronos.opengles.GL10;

import geometry_msgs.PoseStamped;

public class MapAnnotationLayer extends DefaultLayer {

    private static final String MAP_FRAME = "map";

    private final Context context;
    private Annotation annotation;
    private NameResolver nameResolver;
    private GestureDetector gestureDetector;
    private Transform pose;
    private Transform fixedPose;
    private Camera camera;
    private ConnectedNode connectedNode;
    private ExpandableListAdapter annotationsList;
    private Mode mode;
    private PoseShape origin_shape;
    private GoalShape camera_shape;
    private double distance = 1.0;

    public enum Mode {
        ADD_MARKER, ADD_TABLE, ADD_COLUMN, ADD_WALL, ADD_PICKUP
    }

    public MapAnnotationLayer(NameResolver newNameResolver, Context context,
                              ExpandableListAdapter annotationsList) {
        this.nameResolver = newNameResolver;
        this.context = context;
        this.annotationsList = annotationsList;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    @Override
    public void draw(GL10 gl) {
        if (annotation != null) {
            annotation.draw(gl);
        }
//        if (origin_shape != null) {  TODO  create tf ref shape   maybe add a layer
//            origin_shape.setTransform(Transform.identity());
//            origin_shape.draw(gl);
//        }
//        if (camera_shape != null) {
//            camera_shape.setTransform(camera.transform);
//            camera_shape.draw(gl);
//        }
        for (Annotation annotation : annotationsList.listFullContent()) {

            //((ColumnShape)ann.getShape()).scale(gl, (float)(camera.getZoom()*distance));
            annotation.draw(gl);

 //           Log.e("@@@@@@@@@@", ann.getShape().getTransform().toString());
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
                annotation.setWidth(dist);
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
        this.connectedNode = connectedNode;
        this.camera = camera;
        this.camera.setFrame(MAP_FRAME);
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
                                pose = Transform.translation(camera.toMetricCoordinates(
                                        (int) e.getX(), (int) e.getY()));

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
                                    case ADD_PICKUP:
                                        annotation = new Pickup("Pickup 1");
                                        break;
                                    default:
                                        Log.e("MapAnn", "Unimplemented annotation mode: " + mode);
                                }
                                annotation.setTransform(Transform.translation(
                                        camera.toMetricCoordinates((int) e.getX(), (int) e.getY())));
                       //         Log.e("@@@@@@@@@@", camera.transform.toString());

//                                camera.setFrame(MAP_FRAME);
                                fixedPose = Transform.translation(camera.toMetricCoordinates(
                                        (int) e.getX(), (int) e.getY()));
//                                camera.setFrame(ROBOT_FRAME);

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
        final EditText name = (EditText) promptView.findViewById(R.id.name);
        final EditText height = (EditText) promptView.findViewById(R.id.height);

        // setup a dialog window
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // get user input and set it to result
                try {
                    if (name.getText().length() == 0)
                        throw new Exception("Annotation name cannot be empty");
                    if (height.getText().length() > 0)
                        annotation.setHeight(Double.parseDouble(height.getText().toString()));

                    annotation.setName(name.getText().toString());
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

        // create and show an alert dialog to get annotation info
        final AlertDialog alertDlg = alertDialogBuilder.create();
        alertDlg.show();

        // ensure that both fields contain something before enabling OK
        alertDlg.getButton(AlertDialog.BUTTON_POSITIVE).

                setEnabled(false);

        name.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                alertDlg.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setEnabled(name.getText().length() > 0);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
    }
}
