package com.github.robotics_in_concert.rocon_android_apps.beacon_awareness;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.github.rosjava.android_remocons.common_tools.apps.RosAppActivity;

import org.ros.android.view.RosTextView;
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
    public WizTurnBeaconService wt_service;

    public BeaconAwarenessActivity()
    {
        super("BeaconAwareness", "BeaconAwareness");
    }

    private boolean is_bound = false;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("BeaconAwareness", "Beacon Service connection!!!");
            wt_service = ((WizTurnBeaconService.WizTurnBeaconServiceBinder)service).getService();
            wt_service.registerCallback(mCallback);
            is_bound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("BeaconAwareness", "Beacon Service disconnection!!!");
            wt_service = null;
            is_bound = false;
        }
        private WizTurnBeaconService.ICallback mCallback = new WizTurnBeaconService.ICallback() {
            public void sendData(String data) {
            ba_bridge.publish(data);
            }
        };
    };
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        findViewById(R.id.srv_start).setOnClickListener(this);
        findViewById(R.id.srv_stop).setOnClickListener(this);
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
                //startService(new Intent(BeaconAwarenessActivity.this,WizTurnBeaconService.class));
                Intent intent = new Intent(this, WizTurnBeaconService.class);
                bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

                break;
            case R.id.srv_stop:
                Log.d("[BeaconAwareness]", "push srv_stop");
                if (is_bound) {
                    unbindService(mConnection);
                    is_bound= false;
                }
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
