package com.github.robotics_in_concert.rocon_android_apps.beacon_awareness_ex;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.github.rosjava.android_remocons.common_tools.master.MasterId;

import org.ros.address.InetAddressFactory;
import org.ros.android.NodeMainExecutorService;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

import java.net.URI;
import java.net.URISyntaxException;


/**
 * Created by dwlee on 14. 12. 2.
 */
public class RoconConnector extends Service{
    private int masterPort = 11311;
    private String masterHost = "localhost";
    private String masterUri  = "http://" + masterHost + ":" + masterPort;

    private MasterId masterId;
    private NodeMainExecutorService nodeMainExecutorService;
    public BeaconAwarenessNode ba_bridge;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void setRoconConfig(String master_uri){
        masterUri = master_uri;
    }
    public void connectRocon(){
        masterId = new MasterId(masterUri,"", "", "");
        URI concertUri = null;
        try {
            concertUri = new URI(masterId.getMasterUri());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        ba_bridge = new BeaconAwarenessNode();
        nodeMainExecutorService = new NodeMainExecutorService();
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(
                InetAddressFactory.newNonLoopback().getHostAddress(), concertUri);
        nodeMainExecutorService.execute(
                ba_bridge,
                nodeConfiguration.setNodeName("beacon_awareness")
        );
    }

    public void disConnectRocon(){
        nodeMainExecutorService.shutdownNodeMain(ba_bridge);
    }

    public void publish(String data){
        ba_bridge.publish(data);
    }



    public class BeaconAwarenessNode implements NodeMain {
        public final java.lang.String NODE_NAME = "beacon_awareness";
        private Publisher<std_msgs.String> publisher;
        @Override
        public GraphName getDefaultNodeName() {
            return GraphName.of(NODE_NAME);
        }

        void publish (String data){
            if(publisher != null){
                std_msgs.String beacons = publisher.newMessage();
                beacons.setData(data);
                publisher.publish(beacons);
            }
        }
        @Override
        public void onStart(ConnectedNode connectedNode) {
            publisher = connectedNode.newPublisher("beacons", "std_msgs/String");
        }
        @Override
        public void onShutdown(Node node) {
        }

        @Override
        public void onShutdownComplete(Node node) {
        }
        @Override
        public void onError(Node node, Throwable throwable) {

        }
    }

}
