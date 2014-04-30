/*
 * Copyright (C) 2013 OSRF.
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

import android.content.Context;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.github.rosjava.android_remocons.common_tools.apps.AppParameters;

import org.ros.android.view.visualization.Camera;
import org.ros.android.view.visualization.RotateGestureDetector;
import org.ros.android.view.visualization.VisualizationView;
import org.ros.android.view.visualization.layer.CameraControlLayer;
import org.ros.android.view.visualization.layer.CameraControlListener;
import org.ros.concurrent.ListenerGroup;
import org.ros.concurrent.SignalRunnable;
import org.ros.node.ConnectedNode;
import org.ros.rosjava_geometry.FrameTransformTree;

import java.util.concurrent.ExecutorService;

/**
 * @author murase@jsk.imi.i.u-tokyo.ac.jp (Kazuto Murase)
 */
public class ViewControlLayer extends CameraControlLayer {

    private final Context context;
    private final ListenerGroup<CameraControlListener> listeners;

    private GestureDetector translateGestureDetector;
    private RotateGestureDetector rotateGestureDetector;
    private ScaleGestureDetector zoomGestureDetector;

    private VisualizationView mapView;
    private boolean mapViewGestureAvailable;


    public ViewControlLayer(Context context, ExecutorService executorService, VisualizationView mapView,
                            final AppParameters params) {
        super(context, executorService);

        this.context = context;

        listeners = new ListenerGroup<CameraControlListener>(executorService);

        this.mapView = mapView;
        this.mapView.setClickable(false);
        this.mapView.getCamera().jumpToFrame((String)params.get("map_frame", context.getString(R.string.map_frame)));

        mapViewGestureAvailable = true;
    }


    @Override
    public boolean onTouchEvent(VisualizationView view, MotionEvent event) {

//        if (event.getAction() == MotionEvent.ACTION_UP) {
//            mapViewGestureAvaiable = true;
//        }

        if (translateGestureDetector == null ||
               rotateGestureDetector == null ||
                 zoomGestureDetector == null) {
            return false;
        }
        return translateGestureDetector.onTouchEvent(event) ||
                  rotateGestureDetector.onTouchEvent(event) ||
                    zoomGestureDetector.onTouchEvent(event);
    }

    @Override
    public void onStart(ConnectedNode connectedNode, Handler handler,
                        FrameTransformTree frameTransformTree, final Camera camera) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                translateGestureDetector =
                        new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                            @Override
                            public boolean onScroll(MotionEvent event1, MotionEvent event2,
                                                    final float distanceX, final float distanceY) {
                                if (mapViewGestureAvailable) {
                                    camera.translate(-distanceX, distanceY);
                                    listeners.signal(new SignalRunnable<CameraControlListener>() {
                                        @Override
                                        public void run(CameraControlListener listener) {
                                            listener.onTranslate(-distanceX, distanceY);
                                        }
                                    });
                                    return true;
                                }

                                return false;
                            }
                        });
                rotateGestureDetector =
                        new RotateGestureDetector(new RotateGestureDetector.OnRotateGestureListener() {
                            @Override
                            public boolean onRotate(MotionEvent event1, MotionEvent event2,
                                                    final double deltaAngle) {
                                if (mapViewGestureAvailable) {
                                    final double focusX = (event1.getX(0) + event1.getX(1)) / 2;
                                    final double focusY = (event1.getY(0) + event1.getY(1)) / 2;
                                    camera.rotate(focusX, focusY, deltaAngle);
                                    listeners.signal(new SignalRunnable<CameraControlListener>() {
                                        @Override
                                        public void run(CameraControlListener listener) {
                                            listener.onRotate(focusX, focusY, deltaAngle);
                                        }
                                    });
                                    // Don't consume this event in order to allow the zoom gesture
                                    // to also be detected.
                                    return false;
                                }

                                return true;
                            }
                        });
                zoomGestureDetector =
                        new ScaleGestureDetector(context,
                                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                                    @Override
                                    public boolean onScale(ScaleGestureDetector detector) {
                                        if (!detector.isInProgress()) {
                                            return false;
                                        }
                                        if (mapViewGestureAvailable) {
                                            final float focusX = detector.getFocusX();
                                            final float focusY = detector.getFocusY();
                                            final float factor = detector.getScaleFactor();
                                            camera.zoom(focusX, focusY, factor);
                                            listeners.signal(new SignalRunnable<CameraControlListener>() {
                                                @Override
                                                public void run(CameraControlListener listener) {
                                                    listener.onZoom(focusX, focusY, factor);
                                                }
                                            });
                                            return true;
                                        }

                                        return false;
                                    }
                                });
            }
        });
    }
}