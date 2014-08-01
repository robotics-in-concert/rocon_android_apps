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

package com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list;

import android.content.Context;
import android.database.DataSetObserver;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.github.robotics_in_concert.rocon_android_apps.map_annotation.R;
import com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list.annotations.Annotation;
import com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list.annotations.Column;
import com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list.annotations.Location;
import com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list.annotations.Marker;
import com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list.annotations.Table;
import com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list.annotations.Wall;
import com.github.rosjava.android_remocons.common_tools.apps.AppParameters;
import com.github.rosjava.android_remocons.common_tools.apps.AppRemappings;

import org.ros.exception.RemoteException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.internal.message.DefaultMessageFactory;
import org.ros.internal.message.definition.MessageDefinitionReflectionProvider;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;
import org.ros.node.topic.Subscriber;
import org.ros.rosjava_geometry.Quaternion;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

import ar_track_alvar_msgs.AlvarMarker;
import ar_track_alvar_msgs.AlvarMarkers;
import simple_annotation_msgs.SaveARMarkers;
import simple_annotation_msgs.SaveARMarkersRequest;
import simple_annotation_msgs.SaveARMarkersResponse;
import simple_annotation_msgs.SaveColumns;
import simple_annotation_msgs.SaveColumnsRequest;
import simple_annotation_msgs.SaveColumnsResponse;
import simple_annotation_msgs.SaveTables;
import simple_annotation_msgs.SaveTablesRequest;
import simple_annotation_msgs.SaveTablesResponse;
import simple_annotation_msgs.SaveWalls;
import simple_annotation_msgs.SaveWallsRequest;
import simple_annotation_msgs.SaveWallsResponse;
import yocs_msgs.ColumnList;
import yocs_msgs.TableList;
import yocs_msgs.WallList;

/**
 * @author jorge@yujinrobot.com (Jorge Santos Simon)
 *
 * Publishes current annotations every time the information on the annotations list changes.
 */
public class AnnotationsPublisher extends DataSetObserver implements NodeMain {
    public  static final String NODE_NAME = "remocon_status_pub_node";

    private AnnotationsList annotationsList;
    private String mapFrame;  // TODO take from map description instead of being a parameter

    private String markersTopic = "ar_markers";
    private String tablesTopic  = "tables";
    private String columnsTopic = "columns";
    private String wallsTopic   = "walls";

    private String markers_serviceName = "save_ar_markers";
    private String columns_serviceName = "save_columns";
    private String tables_serviceName = "save_tables";
    private String walls_serviceName = "save_walls";

    private Subscriber<AlvarMarkers> markersSub;
    private Subscriber<yocs_msgs.TableList> tablesSub;
    private Subscriber<yocs_msgs.ColumnList> columnsSub;
    private Subscriber<yocs_msgs.WallList> wallsSub;

    private ServiceClient<SaveARMarkersRequest, SaveARMarkersResponse> save_ar_markers_srvClient;
    private ServiceClient<SaveColumnsRequest, SaveColumnsResponse> save_columns_srvClient;
    private ServiceClient<SaveTablesRequest, SaveTablesResponse> save_tables_srvClient;
    private ServiceClient<SaveWallsRequest, SaveWallsResponse> save_wall_srvClient;

    private int marker_num = 0;
    private int table_num = 0;
    private int wall_num = 0;
    private int column_num = 0;


    // Required to create new entries in the annotation messages lists
    MessageDefinitionReflectionProvider messageDefinitionProvider = new MessageDefinitionReflectionProvider();
    DefaultMessageFactory messageFactory = new DefaultMessageFactory(messageDefinitionProvider);

    private boolean initialized = false;
    private boolean markers_initialized = false;
    private boolean tables_initialized = false;
    private boolean columns_initialized = false;
    private boolean walls_initialized = false;

    private Context context;
    private NameResolver masterNameSpace;

    private AppRemappings remaps;

    public AnnotationsPublisher(final Context context, final AnnotationsList list,
                                final AppParameters params, final AppRemappings remaps) {
        mapFrame = (String)params.get("map_frame", context.getString(R.string.map_frame));
        this.context = context;
        this.remaps = remaps;
        this.annotationsList = list;
        this.annotationsList.registerDataSetObserver(this);
    }

    public void setMasterNameSpace(NameResolver nameResolver){
        this.masterNameSpace = nameResolver;
    }

    public void onChanged() {
        if (! initialized) {
            return;  // Still not ready!
        }

        if (annotationsList.getGroupCount() < 5) {
            return;  // Still initializing the list  // TODO replace with a timer; only publish after 1/3 sec w/o changes
        }

        if (! markers_initialized || !tables_initialized || !columns_initialized || !walls_initialized){
            Log.i("MapAnn", "markers_initialized: "+markers_initialized);
            Log.i("MapAnn", "tables_initialized: "+tables_initialized);
            Log.i("MapAnn", "columns_initialized: "+columns_initialized);
            Log.i("MapAnn", "walls_initialized: "+walls_initialized);

            Log.i("MapAnn", "Annotations list initializing");
            return;
        }
        Log.i("MapAnn", "Annotations list changed; republishing...");

        AlvarMarkers         markersMsg = messageFactory.newFromType(AlvarMarkers._TYPE);
        yocs_msgs.TableList  tablesMsg  = messageFactory.newFromType(TableList._TYPE);
        yocs_msgs.ColumnList  columnsMsg  = messageFactory.newFromType(ColumnList._TYPE);
        yocs_msgs.WallList  wallsMsg  = messageFactory.newFromType(WallList._TYPE);

        Transform makeVertical = new Transform(new Vector3(0.0, 0.0, 0.0), new Quaternion(0.5, 0.5, 0.5, 0.5));

        for (Annotation ann: annotationsList.listFullContent()) {
            if (ann.getGroup().equals(Marker.GROUP_NAME)) {
                AlvarMarker annMsg = messageFactory.newFromType(AlvarMarker._TYPE);
                annMsg.setId(((Marker) ann).getId());
                annMsg.getPose().getHeader().setFrameId(mapFrame);
                Transform tf = ann.getTransform().multiply(makeVertical);
                tf.toPoseMessage(annMsg.getPose().getPose());
                markersMsg.getMarkers().add(annMsg);
            }
            else if (ann.getGroup().equals(Column.GROUP_NAME)) {
                Column column = (Column)ann;
                yocs_msgs.Column annMsg = messageFactory.newFromType(yocs_msgs.Column._TYPE);
                annMsg.setName(column.getName());
                annMsg.setHeight(column.getHeight());
                annMsg.setRadius(column.getRadius());
                annMsg.getPose().getHeader().setFrameId(mapFrame);
                column.getTransform().toPoseMessage(annMsg.getPose().getPose().getPose());
                columnsMsg.getObstacles().add(annMsg);
            }
            else if (ann.getGroup().equals(Wall.GROUP_NAME)) {
                Wall wall = (Wall)ann;
                yocs_msgs.Wall annMsg = messageFactory.newFromType(yocs_msgs.Wall._TYPE);
                annMsg.setName(wall.getName());
                annMsg.setHeight(wall.getHeight());
                annMsg.setLength(wall.getLength());
                annMsg.setWidth(wall.getWidth());
                annMsg.getPose().getHeader().setFrameId(mapFrame);
                wall.getTransform().toPoseMessage(annMsg.getPose().getPose().getPose());
                wallsMsg.getObstacles().add(annMsg);
            }
            else if (ann.getGroup().equals(Table.GROUP_NAME)) {
                Table table = (Table)ann;
                yocs_msgs.Table annMsg = messageFactory.newFromType(yocs_msgs.Table._TYPE);
                annMsg.setName(table.getName());
                annMsg.setHeight(table.getHeight());
                annMsg.setRadius(table.getRadius());
                annMsg.getPose().getHeader().setFrameId(mapFrame);
                table.getTransform().toPoseMessage(annMsg.getPose().getPose().getPose());
                tablesMsg.getTables().add(annMsg);
            }
            else if (ann.getGroup().equals(Location.GROUP_NAME)) {

                Location location = (Location)ann;
                yocs_msgs.Table annMsg = messageFactory.newFromType(yocs_msgs.Table._TYPE);
                annMsg.setName(location.getName());
                annMsg.setHeight(location.getHeight());
                annMsg.getPose().getHeader().setFrameId(mapFrame);
                location.getTransform().toPoseMessage(annMsg.getPose().getPose().getPose());
                tablesMsg.getTables().add(annMsg);
            }
            else {
                Log.w("MapAnn", "Unrecognized group name: " + ann.getGroup());
            }
        }

        if(markersMsg.getMarkers().size() != this.marker_num){
            final SaveARMarkersRequest markers_request = save_ar_markers_srvClient.newMessage();
            markers_request.setData(markersMsg.getMarkers());
            save_ar_markers_srvClient.call(markers_request, new ServiceResponseListener<SaveARMarkersResponse>() {
                @Override
                public void onSuccess(SaveARMarkersResponse saveARMarkersResponse) {
                    if (saveARMarkersResponse.getSuccess()){
                        Log.i("[MA]", "save_ar_markers Srv Call success/ size: " + marker_num);
                    }
                    else{
                        Log.i("[MA]", "save_ar_markers Srv failure");
                    }
                }
                @Override
                public void onFailure(RemoteException e) {
                }
            });
            this.marker_num = markersMsg.getMarkers().size();
        }

        if(columnsMsg.getObstacles().size() != this.column_num) {
            final SaveColumnsRequest columns_request = save_columns_srvClient.newMessage();
            columns_request.setData(columnsMsg.getObstacles());
            save_columns_srvClient.call(columns_request, new ServiceResponseListener<SaveColumnsResponse>() {
                @Override
                public void onSuccess(SaveColumnsResponse saveColumnsResponse) {
                    if (saveColumnsResponse.getSuccess()){
                        Log.i("[MA]", "save_columns Srv Call success / size: " + column_num);
                    }
                    else{
                        Log.i("[MA]", "save_columns Srv failure");
                    }
                }

                @Override
                public void onFailure(RemoteException e) {
                }
            });
            this.column_num = columnsMsg.getObstacles().size();
        }

        if(tablesMsg.getTables().size() != this.table_num) {
            final SaveTablesRequest tables_request = save_tables_srvClient.newMessage();
            tables_request.setData(tablesMsg.getTables());
            save_tables_srvClient.call(tables_request, new ServiceResponseListener<SaveTablesResponse>() {
                @Override
                public void onSuccess(SaveTablesResponse saveTablesResponse) {
                    if (saveTablesResponse.getSuccess()){
                        Log.i("[MA]", "save_tables Srv Call success/ size: " + table_num);
                    }
                    else{
                        Log.i("[MA]", "save_tables Srv failure");
                    }
                }
                @Override
                public void onFailure(RemoteException e) {
                }
            });
            this.table_num = tablesMsg.getTables().size();
        }

        if(wallsMsg.getObstacles().size() != this.wall_num) {
            final SaveWallsRequest wall_request = save_wall_srvClient.newMessage();
            wall_request .setData(wallsMsg.getObstacles());
            save_wall_srvClient.call(wall_request , new ServiceResponseListener<SaveWallsResponse>() {
                @Override
                public void onSuccess(SaveWallsResponse saveWallsResponse) {
                    if (saveWallsResponse.getSuccess()){
                        Log.i("[MA]", "save_wall Srv Call success / size: " + wall_num);
                    }
                    else{
                        Log.i("[MA]", "save_wall Srv failure");
                    }
                }

                @Override
                public void onFailure(RemoteException e) {
                }
            });
            this.wall_num = wallsMsg.getObstacles().size();
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(NODE_NAME);
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        //set the services

        markersTopic = this.masterNameSpace.resolve(this.remaps.get(markersTopic)).toString();
        tablesTopic = this.masterNameSpace.resolve(this.remaps.get(tablesTopic)).toString();
        columnsTopic = this.masterNameSpace.resolve(this.remaps.get(columnsTopic)).toString();
        wallsTopic = this.masterNameSpace.resolve(this.remaps.get(wallsTopic)).toString();

        markers_serviceName = this.masterNameSpace.resolve(this.remaps.get(markers_serviceName)).toString();
        columns_serviceName = this.masterNameSpace.resolve(this.remaps.get(columns_serviceName)).toString();
        tables_serviceName = this.masterNameSpace.resolve(this.remaps.get(tables_serviceName)).toString();
        walls_serviceName = this.masterNameSpace.resolve(this.remaps.get(walls_serviceName)).toString();

        try{
            Log.i("MapAnn", "Request service client created [" + markers_serviceName + "]");
            save_ar_markers_srvClient = connectedNode.newServiceClient(markers_serviceName, SaveARMarkers._TYPE);
            save_columns_srvClient = connectedNode.newServiceClient(columns_serviceName, SaveColumns._TYPE);
            save_tables_srvClient = connectedNode.newServiceClient(tables_serviceName, SaveTables._TYPE);
            save_wall_srvClient = connectedNode.newServiceClient(walls_serviceName, SaveWalls._TYPE);
        } catch (ServiceNotFoundException e) {
            Log.w("MapAnn", "Request service not found");
            Toast.makeText(this.context, "Request service not found", Toast.LENGTH_LONG).show();
            return;
            //throw new RosRuntimeException(e); // TODO we should recover from this calling onFailure on listener
        }
        //set the subscribers
        markers_initialized = false;
        markersSub = connectedNode.newSubscriber(markersTopic, AlvarMarkers._TYPE);
        tablesSub = connectedNode.newSubscriber(tablesTopic, TableList._TYPE);
        columnsSub = connectedNode.newSubscriber(columnsTopic, ColumnList._TYPE);
        wallsSub = connectedNode.newSubscriber(wallsTopic, WallList._TYPE);

        markersSub.addMessageListener(new MessageListener<AlvarMarkers>() {
            @Override
            public void onNewMessage(AlvarMarkers alvarMarkers) {
                Log.i("[MA]","[Call subscriber] new AlvarMarkers : "+alvarMarkers.getMarkers().size());
                Transform inv_makeVertical = new Transform(new Vector3(0.0, 0.0, 0.0), new Quaternion(0.5, 0.5, 0.5, 0.5));
                inv_makeVertical = inv_makeVertical.invert();
                marker_num = alvarMarkers.getMarkers().size();
                for(AlvarMarker ar_marker:alvarMarkers.getMarkers()){
                    Marker annotation = new Marker(""+ar_marker.getId());
                    annotation.setName(""+ar_marker.getId());
                    float pose_x =  (float)ar_marker.getPose().getPose().getPosition().getX();
                    float pose_y =  (float)ar_marker.getPose().getPose().getPosition().getY();
                    float pose_z =  (float)ar_marker.getPose().getPose().getPosition().getZ();

                    float ori_x =  (float)ar_marker.getPose().getPose().getOrientation().getX();
                    float ori_y =  (float)ar_marker.getPose().getPose().getOrientation().getY();
                    float ori_z =  (float)ar_marker.getPose().getPose().getOrientation().getZ();
                    float ori_w =  (float)ar_marker.getPose().getPose().getOrientation().getW();
                    Transform T = new Transform(new Vector3(pose_x, pose_y, pose_z), new Quaternion(ori_x, ori_y, ori_z, ori_w));
                    annotation.setTransform(T.multiply(inv_makeVertical));
                    Message ann_msg = Message.obtain();
                    ann_msg.what = 1;
                    ann_msg.obj = annotation;
                    handler.sendMessage(ann_msg);
                }
                markersSub.shutdown();
                Message init_msg = Message.obtain();
                init_msg.what = 2;
                init_msg.obj = "markers_initialized";
                handler.sendMessage(init_msg);

            }
        });

        tablesSub.addMessageListener(new MessageListener<TableList>() {
            @Override
            public void onNewMessage(TableList tables) {
                Log.i("[MA]","[Call subscriber] new TableList : "+tables.getTables().size());
                table_num = tables.getTables().size();
                for (yocs_msgs.Table table:tables.getTables()){

                    Table annotation = new Table(table.getName());
                    annotation.setHeight(table.getHeight());
                    annotation.setSizeXY(table.getRadius());
                    float pose_x =  (float)table.getPose().getPose().getPose().getPosition().getX();
                    float pose_y =  (float)table.getPose().getPose().getPose().getPosition().getY();
                    float pose_z =  (float)table.getPose().getPose().getPose().getPosition().getZ();

                    float ori_x =  (float)table.getPose().getPose().getPose().getOrientation().getX();
                    float ori_y =  (float)table.getPose().getPose().getPose().getOrientation().getY();
                    float ori_z =  (float)table.getPose().getPose().getPose().getOrientation().getZ();
                    float ori_w =  (float)table.getPose().getPose().getPose().getOrientation().getW();
                    Transform T = new Transform(new Vector3(pose_x, pose_y, pose_z), new Quaternion(ori_x, ori_y, ori_z, ori_w));
                    annotation.setTransform(T);
                    Message ann_msg = Message.obtain();
                    ann_msg.what = 1;
                    ann_msg.obj = annotation;
                    handler.sendMessage(ann_msg);
                }
                tablesSub.shutdown();
                Message init_msg = Message.obtain();
                init_msg.what = 2;
                init_msg.obj = "tables_initialized";
                handler.sendMessage(init_msg);

            }
        });

        columnsSub.addMessageListener(new MessageListener<ColumnList>() {
            @Override
            public void onNewMessage(ColumnList columns) {
                Log.i("[MA]","[Call subscriber] new ColumnList : "+columns.getObstacles().size());
                column_num = columns.getObstacles().size();
                for (yocs_msgs.Column column:columns.getObstacles()){

                    Column annotation = new Column(column.getName());
                    annotation.setHeight(column.getHeight());
                    annotation.setSizeXY(column.getRadius());
                    float pose_x =  (float)column.getPose().getPose().getPose().getPosition().getX();
                    float pose_y =  (float)column.getPose().getPose().getPose().getPosition().getY();
                    float pose_z =  (float)column.getPose().getPose().getPose().getPosition().getZ();

                    float ori_x =  (float)column.getPose().getPose().getPose().getOrientation().getX();
                    float ori_y =  (float)column.getPose().getPose().getPose().getOrientation().getY();
                    float ori_z =  (float)column.getPose().getPose().getPose().getOrientation().getZ();
                    float ori_w =  (float)column.getPose().getPose().getPose().getOrientation().getW();
                    Transform T = new Transform(new Vector3(pose_x, pose_y, pose_z), new Quaternion(ori_x, ori_y, ori_z, ori_w));
                    annotation.setTransform(T);
                    Message ann_msg = Message.obtain();
                    ann_msg.what = 1;
                    ann_msg.obj = annotation;
                    handler.sendMessage(ann_msg);
                }
                columnsSub.shutdown();
                Message init_msg = Message.obtain();
                init_msg.what = 2;
                init_msg.obj = "columns_initialized";
                handler.sendMessage(init_msg);
            }
        });

        wallsSub.addMessageListener(new MessageListener<WallList>() {
            @Override
            public void onNewMessage(WallList walls) {
                Log.i("[MA]","[Call subscriber] new WallList : "+walls.getObstacles().size());
                wall_num = walls.getObstacles().size();
                for (yocs_msgs.Wall wall:walls.getObstacles()){
                    Wall annotation = new Wall(wall.getName());
                    annotation.setHeight(wall.getHeight());
                    annotation.setSizeXY(wall.getLength());

                    float pose_x =  (float)wall.getPose().getPose().getPose().getPosition().getX();
                    float pose_y =  (float)wall.getPose().getPose().getPose().getPosition().getY();
                    float pose_z =  (float)wall.getPose().getPose().getPose().getPosition().getZ();

                    float ori_x =  (float)wall.getPose().getPose().getPose().getOrientation().getX();
                    float ori_y =  (float)wall.getPose().getPose().getPose().getOrientation().getY();
                    float ori_z =  (float)wall.getPose().getPose().getPose().getOrientation().getZ();
                    float ori_w =  (float)wall.getPose().getPose().getPose().getOrientation().getW();
                    Transform T = new Transform(new Vector3(pose_x, pose_y, pose_z), new Quaternion(ori_x, ori_y, ori_z, ori_w));
                    annotation.setTransform(T);
                    Message ann_msg = Message.obtain();
                    ann_msg.what = 1;
                    ann_msg.obj = annotation;
                    handler.sendMessage(ann_msg);
                }
                wallsSub.shutdown();
                Message init_msg = Message.obtain();
                init_msg.what = 2;
                init_msg.obj = "walls_initialized";
                handler.sendMessage(init_msg);
            }
        });
        initialized = true;
    }

    @Override
    public void onShutdown(Node node) {
        //markersPub.shutdown();
        //tablesPub.shutdown();
        //columnsPub.shutdown();
        //wallsPub.shutdown();
        initialized = false;
        Log.i("MapAnn", "Annotations publisher shutdown");
    }

    @Override
    public void onShutdownComplete(Node node) {
    }

    @Override
    public void onError(Node node, Throwable throwable) {
        Log.e("MapAnn", "Annotations publisher error: " + throwable.getMessage());
    }

    final Handler handler = new Handler(){
        public void handleMessage(Message msg){
            if (msg.what == 1 ){
                Annotation anno = (Annotation) msg.obj;
                annotationsList.addItem(anno);
            }
            else if(msg.what == 2){
                String init_annotation = (String) msg.obj;
                if (init_annotation.equals("walls_initialized")){
                    walls_initialized = true;
                }
                else if (init_annotation.equals("tables_initialized")){
                    tables_initialized = true;
                }
                else if (init_annotation.equals("markers_initialized")){
                    markers_initialized= true;
                }
                else if (init_annotation.equals("columns_initialized")){
                    columns_initialized = true;
                }

            }
        }
    };
}
