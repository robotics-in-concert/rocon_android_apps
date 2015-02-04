package com.github.robotics_in_concert.rocon_android_apps.motion_retargeting;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ToggleButton;

import com.github.rosjava.android_apps.application_management.RosAppActivity;

import org.ros.address.InetAddressFactory;
import org.ros.android.MessageCallable;
import org.ros.android.view.RosTextView;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.node.topic.Publisher;

public class MotionRetargeting extends RosAppActivity
{
    private RosTextView<std_msgs.UInt16MultiArray> availableUsersView;
    private RosTextView<std_msgs.UInt16> trackedUserView;
    private EditText newUser;
    private Button setUserButton;
    private ToggleButton motionControlToggleButton;
    private ToggleButton motionRecordingToggleButton;
    private Publisher<std_msgs.UInt16> newUserPublisher;
    private Publisher<std_msgs.Empty> motionControlPublisher;
    private Publisher<std_msgs.Empty> motionRecordingPublisher;
    boolean publisher_initialised = false;

    private static final java.lang.String TAG = "MotionRetargeting";

    public MotionRetargeting()
    {
        super("MotionRetargeting", "MotionRetargeting");
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        setDefaultRobotName(getString(R.string.default_robot_name));
        setDefaultAppName(getString(R.string.paired_app_name));
        setDashboardResource(R.id.dashboard);
        setMainWindowResource(R.layout.main);

        super.onCreate(savedInstanceState);

        newUser = (EditText) findViewById(R.id.editTextNewUser);
        setUserButton = (Button) findViewById(R.id.buttonSetUser);
        motionControlToggleButton= (ToggleButton) findViewById(R.id.toggleButtonMotionControl);
        motionRecordingToggleButton= (ToggleButton) findViewById(R.id.toggleButtonMotionRecording);
        Log.e(TAG, "motionControlToggleButton users: " + motionControlToggleButton);
        Log.e(TAG, "motionRecordingToggleButton users: " + motionRecordingToggleButton);
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor)
    {
        super.init(nodeMainExecutor);
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(), getMasterUri());

        availableUsersView = (RosTextView<std_msgs.UInt16MultiArray>) findViewById(R.id.textViewAvailableUsers);
        availableUsersView.setTopicName(getRobotNameSpace().resolve(getString(R.string.available_users_topic_name)).toString());
        availableUsersView.setMessageType(std_msgs.UInt16MultiArray._TYPE);
        availableUsersView.setMessageToStringCallable(new MessageCallable<java.lang.String, std_msgs.UInt16MultiArray>()
        {
            @Override
            public java.lang.String call(std_msgs.UInt16MultiArray message)
            {
                java.lang.String users;
                users = "";
                for (int user = 0; user < message.getData().length; ++user)
                {
                    users += message.getData()[user];
                    if (user < (message.getData().length - 1))
                    {
                        users += " ";
                    }
                }
                Log.e(TAG, "Available users: " + users);
                return users;
            }
        });
        nodeConfiguration.setNodeName(getString(R.string.node_name) + "/available_users_viewer");
        nodeMainExecutor.execute(availableUsersView, nodeConfiguration);

        trackedUserView = (RosTextView<std_msgs.UInt16>) findViewById(R.id.textViewTrackedUser);
        trackedUserView.setTopicName(getRobotNameSpace().resolve(getString(R.string.tracked_user_topic_name)).toString());
        Log.i(TAG, "trackedUserView: Will listen to: " + getRobotNameSpace().resolve(getString(R.string.tracked_user_topic_name)).toString());
        trackedUserView.setMessageType(std_msgs.UInt16._TYPE);
        trackedUserView.setMessageToStringCallable(new MessageCallable<java.lang.String, std_msgs.UInt16>() {
            @Override
            public java.lang.String call(std_msgs.UInt16 message) {
                Log.i(TAG, "Tracked user: " + java.lang.String.valueOf(message.getData()));
                return java.lang.String.valueOf(message.getData());
            }
        });
        nodeConfiguration.setNodeName(getString(R.string.node_name) + "/tracked_user_viewer");
        nodeMainExecutor.execute(trackedUserView, nodeConfiguration);

        nodeConfiguration.setNodeName(getString(R.string.node_name) + "/user_chooser_publisher");
        nodeMainExecutor.execute(new AbstractNodeMain() {
            @Override
            public GraphName getDefaultNodeName() {
                return GraphName.of("default_name");
            }

            @Override
            public void onStart(ConnectedNode connectedNode) {
                newUserPublisher = connectedNode.newPublisher(getRobotNameSpace().resolve(getString(R.string.user_chooser_topic_name)).toString(), std_msgs.UInt16._TYPE);
                Log.i(TAG, "trackedUserView: Will publish to: " + getRobotNameSpace().resolve(getString(R.string.user_chooser_topic_name)).toString());
                newUserPublisher.setLatchMode(true);
                publisher_initialised = true;
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
        }, nodeConfiguration);

        setUserButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view)
            {
                if (publisher_initialised)
                {
                    Log.i(TAG, "onClick: New user will be: " + newUser.getText().toString());
                    std_msgs.UInt16 new_user = newUserPublisher.newMessage();
                    new_user.setData(java.lang.Short.parseShort(newUser.getText().toString()));
                    newUserPublisher.publish(new_user);
                }
                else
                {
                    Log.w(TAG, "onClick: Publisher not ready yet.");
                }
            }
        });

        nodeConfiguration.setNodeName(getString(R.string.node_name) + "/motion_control_publisher");
        nodeMainExecutor.execute(new AbstractNodeMain() {
            @Override
            public GraphName getDefaultNodeName() {
                return GraphName.of("default_name");
            }

            @Override
            public void onStart(ConnectedNode connectedNode) {
                motionControlPublisher = connectedNode.newPublisher(getRobotNameSpace().resolve(getString(R.string.motion_control_topic_name)).toString(), std_msgs.Empty._TYPE);
                motionControlPublisher.setLatchMode(true);
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
        }, nodeConfiguration);
//        motionControlToggleButton.setChecked(true);
        motionControlToggleButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view)
            {
                Log.i(TAG, "motionControlToggleButton: Changing motion control state.");
                std_msgs.Empty msg = motionControlPublisher.newMessage();
                motionControlPublisher.publish(msg);
            }
        });

        nodeConfiguration.setNodeName(getString(R.string.node_name) + "/motion_recording_publisher");
        nodeMainExecutor.execute(new AbstractNodeMain() {
            @Override
            public GraphName getDefaultNodeName() {
                return GraphName.of("default_name");
            }

            @Override
            public void onStart(ConnectedNode connectedNode) {
                motionRecordingPublisher = connectedNode.newPublisher(getRobotNameSpace().resolve(getString(R.string.motion_recording_topic_name)).toString(), std_msgs.Empty._TYPE);
                motionRecordingPublisher.setLatchMode(true);
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
        }, nodeConfiguration);
//        motionRecordingToggleButton.setChecked(false);
        motionRecordingToggleButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view)
            {
                Log.i(TAG, "motionRecordingToggleButton: Changing motion recording state.");
                std_msgs.Empty msg = motionRecordingPublisher.newMessage();
                motionRecordingPublisher.publish(msg);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add(0,0,0,R.string.stop_app);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()){
            case 0:
                finish();
                break;
        }
        return true;
    }
}
