package com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list;

import android.database.DataSetObserver;
import android.util.Log;

import com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list.annotations.*;
import com.google.common.base.Preconditions;

import org.ros.internal.message.DefaultMessageFactory;
import org.ros.internal.message.definition.MessageDefinitionReflectionProvider;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

import ar_track_alvar.AlvarMarker;
import ar_track_alvar.AlvarMarkers;

import java.util.Hashtable;

/**
 * Created by jorge on 11/6/13.
 *
 * Publishes current annotations every time the information on the annotations list changes.
 */
public class AnnotationsPublisher extends DataSetObserver implements NodeMain {
    public  static final String NODE_NAME = "remocon_status_pub_node";

    private AnnotationsList annotationsList;

    private String mapFrame = "/map";  // TODO this should come from the map description, or parameters list, or at least from a common place in the app

    private String markersTopic = "markers";
    private String tablesTopic  = "tables";
    private String columnsTopic = "columns";
    private String wallsTopic   = "walls";

    private Publisher <AlvarMarkers>         markersPub;
    private Publisher <yocs_msgs.TableList>  tablesPub;
    private Publisher <yocs_msgs.ColumnList> columnsPub;
    private Publisher <yocs_msgs.WallList>   wallsPub;

    // Required to create new entries in the annotation messages lists
    MessageDefinitionReflectionProvider messageDefinitionProvider = new MessageDefinitionReflectionProvider();
    DefaultMessageFactory messageFactory = new DefaultMessageFactory(messageDefinitionProvider);

    private boolean initialized = false;

    public AnnotationsPublisher(final Hashtable<String, String> remaps, AnnotationsList list) {
        if (remaps.containsKey(markersTopic)) markersTopic = remaps.get(markersTopic);
        if (remaps.containsKey(tablesTopic))  tablesTopic  = remaps.get(tablesTopic);
        if (remaps.containsKey(columnsTopic)) columnsTopic = remaps.get(columnsTopic);
        if (remaps.containsKey(wallsTopic))   wallsTopic   = remaps.get(wallsTopic);

        this.annotationsList = list;
        this.annotationsList.registerDataSetObserver(this);
    }

    public void onChanged() {
        if (! initialized)
            return;  // Still not ready!

        if (annotationsList.getGroupCount() < 5)
            return;  // Still initializing the list  // TODO replace with a timer; only publish after 1/3 sec w/o changes

        Log.d("MapAnn", "Annotations list changed; republishing...");

        AlvarMarkers         markersMsg = markersPub.newMessage();
        yocs_msgs.TableList  tablesMsg  = tablesPub.newMessage();
        yocs_msgs.ColumnList columnsMsg = columnsPub.newMessage();
        yocs_msgs.WallList   wallsMsg   = wallsPub.newMessage();

        for (Annotation ann: annotationsList.listFullContent()) {
            if (ann.getGroup().equals(Marker.GROUP_NAME)) {
                AlvarMarker annMsg = messageFactory.newFromType(AlvarMarker._TYPE);
                annMsg.setId(((Marker)ann).getId());
                annMsg.getPose().getHeader().setFrameId(mapFrame);
                ann.getTransform().toPoseMessage(annMsg.getPose().getPose());  // TODO  needs rotate!!!
                markersMsg.getMarkers().add(annMsg);
            }
            if (ann.getGroup().equals(Column.GROUP_NAME)) {
                Column column = (Column)ann;
                yocs_msgs.Column annMsg = messageFactory.newFromType(yocs_msgs.Column._TYPE);
                annMsg.setName(column.getName());
                annMsg.setHeight(column.getHeight());
                annMsg.setRadius(column.getRadius());
                annMsg.getPose().getHeader().setFrameId(mapFrame);
                column.getTransform().toPoseMessage(annMsg.getPose().getPose().getPose());
                columnsMsg.getObstacles().add(annMsg);
            }
            if (ann.getGroup().equals(Wall.GROUP_NAME)) {
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
            if (ann.getGroup().equals(Table.GROUP_NAME)) {
                Table table = (Table)ann;
                yocs_msgs.Table annMsg = messageFactory.newFromType(yocs_msgs.Table._TYPE);
                annMsg.setName(table.getName());
                annMsg.setHeight(table.getHeight());
                annMsg.setRadius(table.getRadius());
                annMsg.getPose().getHeader().setFrameId(mapFrame);
                table.getTransform().toPoseMessage(annMsg.getPose().getPose().getPose());
                tablesMsg.getTables().add(annMsg);
            }
            if (ann.getGroup().equals(Pickup.GROUP_NAME)) {
                // Pickup; as legacy from our first demo, pickup points are tables; mmm.... sucks! TODO
                Pickup pickup = (Pickup)ann;
                yocs_msgs.Table annMsg = messageFactory.newFromType(yocs_msgs.Table._TYPE);
                annMsg.setName(pickup.getName());
                annMsg.setHeight(pickup.getHeight());
                annMsg.setRadius(pickup.getRadius());
                annMsg.getPose().getHeader().setFrameId(mapFrame);
                pickup.getTransform().toPoseMessage(annMsg.getPose().getPose().getPose());
                tablesMsg.getTables().add(annMsg);
            }
        }

        markersPub.publish(markersMsg);
        tablesPub.publish(tablesMsg);
        columnsPub.publish(columnsMsg);
        wallsPub.publish(wallsMsg);
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

        // Prepare a latched publisher for every type of annotation
        markersPub = connectedNode.newPublisher(markersTopic, AlvarMarkers._TYPE);
        tablesPub  = connectedNode.newPublisher(tablesTopic,  yocs_msgs.TableList._TYPE);
        columnsPub = connectedNode.newPublisher(columnsTopic, yocs_msgs.ColumnList._TYPE);
        wallsPub   = connectedNode.newPublisher(wallsTopic,   yocs_msgs.WallList._TYPE);

        markersPub.setLatchMode(true);
        tablesPub.setLatchMode(true);
        columnsPub.setLatchMode(true);
        wallsPub.setLatchMode(true);

        initialized = true;
        Log.i("MapAnn", "Annotations publisher initialized");
    }

    @Override
    public void onShutdown(Node node) {
        markersPub.shutdown();
        tablesPub.shutdown();
        columnsPub.shutdown();
        wallsPub.shutdown();
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
}
