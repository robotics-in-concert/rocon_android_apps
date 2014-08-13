package com.github.robotics_in_concert.rocon_android_apps.beacon_awareness;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.github.rosjava.android_remocons.common_tools.apps.RosAppActivity;

import org.ros.android.view.RosTextView;
import org.ros.internal.message.RawMessage;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMain;
import org.ros.node.NodeMainExecutor;
import org.ros.node.topic.Publisher;

import java.io.IOException;

public class BeaconAwarenessActivity extends RosAppActivity implements View.OnClickListener
{
    private Toast lastToast;
    private RosTextView<std_msgs.String> rosTextView;
    BeaconAwarenessNode ba_bridge;
    public BeaconAwarenessActivity()
    {
        super("BeaconAwareness", "BeaconAwareness");
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        findViewById(R.id.srv_start).setOnClickListener(this);
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        //String topic = remaps.get(getString(R.string.chatter_topic));
//        super.init(nodeMainExecutor);
        try {
            java.net.Socket socket = new java.net.Socket(getMasterUri().getHost(), getMasterUri().getPort());
            java.net.InetAddress local_network_address = socket.getLocalAddress();
            socket.close();
            NodeConfiguration nodeConfiguration =
                    NodeConfiguration.newPublic(local_network_address.getHostAddress(), getMasterUri());
            Log.e("BeaconAwareness", "master uri [" + getMasterUri() + "]");

            ba_bridge = new BeaconAwarenessNode();
            nodeMainExecutor.execute(ba_bridge, nodeConfiguration);
            //nodeMainExecutor.execute(rosTextView, nodeConfiguration);
        }
        catch (IOException e)
        {
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.srv_start:
                Log.d("[BeaconAwareness]", "push srv_start");
                startService(new Intent(BeaconAwarenessActivity.this,WizTurnBeacon.class));
                break;
            case R.id.srv_stop:
                Log.d("[BeaconAwareness]", "push srv_start");
                stopService(new Intent(BeaconAwarenessActivity.this,WizTurnBeacon.class));
                break;
            default:
                break;
        }
    }

    public class BeaconAwarenessNode implements NodeMain{
        public final java.lang.String NODE_NAME = "beacon_awareness";
        private Publisher<std_msgs.String> publisher;
        @Override
        public GraphName getDefaultNodeName() {
            return GraphName.of(NODE_NAME);
        }

        void publish (String data){
            if(publisher != null){
                std_msgs.String beacons = new std_msgs.String() {
                    @Override
                    public String getData() {
                        return null;
                    }
                    @Override
                    public void setData(String s) {

                    }
                    @Override
                    public RawMessage toRawMessage() {
                        return null;
                    }
                };
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
