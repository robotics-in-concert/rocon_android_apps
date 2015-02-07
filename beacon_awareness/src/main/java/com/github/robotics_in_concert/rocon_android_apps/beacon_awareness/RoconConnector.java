package com.github.robotics_in_concert.rocon_android_apps.beacon_awareness;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.github.rosjava.android_remocons.common_tools.master.ConcertChecker;
import com.github.rosjava.android_remocons.common_tools.master.MasterDescription;
import com.github.rosjava.android_remocons.common_tools.master.MasterId;
import com.github.rosjava.android_remocons.common_tools.master.RoconDescription;
import com.github.rosjava.android_remocons.common_tools.rocon.AppLauncher;
import com.github.rosjava.android_remocons.common_tools.rocon.AppsManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.ros.address.InetAddressFactory;
import org.ros.android.NodeMainExecutorService;
import org.ros.exception.RemoteException;
import org.ros.exception.RosRuntimeException;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMain;
import org.ros.node.service.ServiceResponseListener;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;
import org.yaml.snakeyaml.Yaml;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import rocon_interaction_msgs.GetInteractionResponse;
import rocon_interaction_msgs.Interaction;

/**
 * Created by dwlee on 14. 12. 2.
 */
public class RoconConnector{

    private int masterPort = 11311;
    private String masterHost = "localhost";
    private String masterUri  = "http://" + masterHost + ":" + masterPort;

    private RoconConnector rocon_connector = this;
    private JSONObject parameters = null;
    private JSONObject remappings = null;

    private Activity parent;
    private MasterId masterId;
    private NodeMainExecutorService nodeMainExecutorService;
    public BeaconAwarenessNode ba_bridge;

    private RoconDescription concert;
    private int appHash = 0;
    private Interaction app;
    private SendMassgeHandler mMainHandler = new SendMassgeHandler();
    private boolean waiting_flag = true;
    public boolean isConnectRocon = false;

    private ICallback mCallback;

    public interface ICallback {
        public void sendData(String data);
    }

    public void registerCallback(ICallback rocon_cb) {
        mCallback = rocon_cb;
    }

    public void setRoconConfig(Activity parent, String master_uri, String parameters, String remappings){
        this.parent = parent;
        this.masterUri  = master_uri;
        try {

            this.parameters = new JSONObject(parameters);
            this.remappings = new JSONObject(remappings.replace("\\/","\\\\/"));

            Log.i("RoconConnector", "parameters : " + this.parameters.toString());
            Log.i("RoconConnector", "remappings : " + this.remappings.toString());



        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public boolean connectRocon(){
        masterId = new MasterId(masterUri,"", "", "");
        URI concertUri = null;
        try {
            concertUri = new URI(masterId.getMasterUri());
            ba_bridge = new BeaconAwarenessNode();
            nodeMainExecutorService = new NodeMainExecutorService();
            NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(
                    InetAddressFactory.newNonLoopback().getHostAddress(), concertUri);
            nodeMainExecutorService.execute(
                    ba_bridge,
                    nodeConfiguration.setNodeName("beacon_awareness")
            );
            waitFor(10);
            Thread.sleep(1500); //for waiting register publisher and subscriber
        }
        catch (Exception e){
            Log.i("RoconConnector", "error: " + e);
            return false;
        }
        Log.i("RoconConnector", "Connect success");
        return true;
    }
    public void disConnectRocon(){
        if(isConnectRocon){
            nodeMainExecutorService.shutdownNodeMain(ba_bridge);
        }
    }
    public void publish(String data){
        ba_bridge.publish(data);
        Log.i("RoconConnector", "Publish data");
    }
    private void sendData2UI(String data){
        Message msg = mMainHandler.obtainMessage();
        msg.what = 1;
        Bundle bundle = new Bundle();
        bundle.putString("Data2UI", data);
        msg.setData(bundle);
        mMainHandler.sendMessage(msg);
    }
    private void checkConcert() throws Exception {
        concert = null;
        final ConcertChecker cc = new ConcertChecker(
                new ConcertChecker.ConcertDescriptionReceiver() {
                    @Override
                    public void receive(RoconDescription roconDescription) {
                        concert = roconDescription;
                        waiting_flag = false;
                        if ( concert.getConnectionStatus() == MasterDescription.UNAVAILABLE ) {
                            // Check that it's not busy
                            Log.e("RoconConnector", "Concert is unavailable: busy serving another remote controller");
                            sendData2UI("[1/3]Concert is unavailable: busy serving another remote controller");
                        }
                        else{
                            Log.e("RoconConnector", "Concert is available");
                            sendData2UI("[1/3]Concert is available");
                        }
                    }
                },
                new ConcertChecker.FailureHandler() {
                    public void handleFailure(String reason) {
                        Log.e("RoconConnector", "Cannot contact ROS master: " + reason);
                        sendData2UI("[1/3]Cannot contact ROS master: [" + reason + "]");
                        waiting_flag = false;
                    }
                }
        );
        try {
            cc.beginChecking(masterId);
            sendData2UI("[1/3]Waiting 10s for checkConcert result");
            if(waitFor(10) == false || concert == null) {
                cc.stopChecking();
                throw new Exception("Cannot connect to " + masterId.getMasterUri() + ": time out");
            }
        }
        catch(RosRuntimeException e){
            Log.e("RoconConnector", "RosRuntimeException"+ e);
            throw new Exception("Cannot connect to " + masterId.getMasterUri() + " : "+ e);
        }
        catch(Exception e){
            Log.e("RoconConnector", "Exception" + e);
            throw new Exception("Cannot connect to " + masterId.getMasterUri() + " : "+ e);
        }

        Log.e("RoconConnector", "checkConcert end");
    }
    private void getAppConfig() throws Exception {
        Log.e("RoconConnector", "getAppConfig start");
        app = null;
        AppsManager am = new AppsManager(new AppsManager.FailureHandler() {
            public void handleFailure(String reason) {
                //Log.e("NfcLaunch", "Cannot get app info: " + reason);
            }
        });
        am.setupAppInfoService(new ServiceResponseListener<GetInteractionResponse>() {
            @Override
            public void onSuccess(GetInteractionResponse getInteractionResponse) {
                if (getInteractionResponse.getResult() == true) {
                    app = getInteractionResponse.getInteraction();
                    waiting_flag = false;
                    Log.i("RoconConnector", "App with hash " + appHash + " found in concert-"+app.getDisplayName());
                    sendData2UI("[2/3]App with hash " + appHash + " found in concert-"+app.getDisplayName());

                } else {
                    Log.i("RoconConnector", "App with hash " + appHash + " not found in concert");
                    sendData2UI("[2/3]App with hash " + appHash + " not found in concert");
                }
            }
            @Override
            public void onFailure(RemoteException e) {
                Log.e("RoconConnector", "Get app info failed. " + e.getMessage());
            }
        });
        try {
            am.init(concert.getInteractionsNamespace());
            am.getAppInfo(masterId, appHash);
            sendData2UI("[2/3] Waiting 10s for getAppConfig result, ");
            if (waitFor(10) == false) {
                am.shutdown();
                throw new Exception("Cannot get app info for hash " + appHash + ". Aborting app launch");
            }

            Yaml param_yaml = new Yaml();
            Map<String, String> params = (Map<String, String>) param_yaml.load(app.getParameters());
            if (params != null && params.size() > 0){
                //todo
                // add extra data in param
                // ex.) location data in order app
                //      params.put("extra_data",String.valueOf(extraData));
                Yaml yaml = new Yaml();
                app.setParameters(yaml.dump(params));
            }
        }
        catch(Exception e){
            throw new Exception("Cannot connect to " + masterId.getMasterUri() + " : "+ e);
        }
        Log.e("RoconConnector", "getAppConfig end");
    }
    private void launchApp(Activity parent) throws Exception {
        Log.e("RoconConnector", "launchApp start");
        AppLauncher.Result result = AppLauncher.launch(parent, concert, app);
        if (result == AppLauncher.Result.SUCCESS) {
            Log.i("RoconConnector", "["+app.getDisplayName() + "] successfully launched");
            sendData2UI("[3/3]["+app.getDisplayName() + "] successfully launched");
        }
        else {
            // I could also show an "app not-installed" dialog and ask for going to play store to download the
            // missing app, but... this would stop to be a headless launcher! But maybe is a good idea, anyway
            throw new Exception("Launch [" + app.getDisplayName() + "] failed. " + result.message);
        }
        Log.e("RoconConnector", "launchApp end");

    }

    private boolean waitFor(final int timeout) {
            waiting_flag = true;
            AsyncTask<Void, Void, Boolean> asyncTask = new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... params) {
                    int count = 0;
                    int timeout_count = timeout * 1000 / 200;
                    while(waiting_flag){
                        try { Thread.sleep(200); }
                        catch (InterruptedException e) { return false; }
                        if(count < timeout_count){
                            count += 1;
                        }
                        else{
                            Log.e("RoconConnector", "time out waitting for app is not null");
                            return false;
                        }
                    }
                    return true;
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        try {
          return asyncTask.get(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e("RoconConnector", "Async task interrupted. " + e.getMessage());
            return false;
        } catch (ExecutionException e) {
            Log.e("RoconConnector", "Async task execution error. " + e.getMessage());
            return false;
        } catch (TimeoutException e) {
            Log.e("RoconConnector", "Async task timeout (" + timeout + " s). " + e.getMessage());
            return false;
        }
    }

    public class SendMassgeHandler extends Handler {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 0:
                        break;
                    case 1:
                        String data = msg.getData().getString("Data2UI");
                        mCallback.sendData(data);
                    default:
                        break;
                }
            }
     }
    private class BeaconAwarenessNode implements NodeMain {
        public final java.lang.String NODE_NAME = "beacon_awareness";
        private Publisher<std_msgs.String> pub_beacons;
        private String pub_beacons_topic_name = "beacons";
        private String sub_start_app_topic_name = "start_app";



        void publish (String data){
            if(pub_beacons != null){
                std_msgs.String beacons = pub_beacons.newMessage();
                beacons.setData(data);
                pub_beacons.publish(beacons);
            }
        }
        @Override
        public GraphName getDefaultNodeName() {
            return GraphName.of(NODE_NAME);
        }
        @Override
        public void onStart(ConnectedNode connectedNode) {
            sendData2UI("onConnect");

            //pub_beacons_topic_name = remappings.optString(pub_beacons_topic_name);
            //sub_start_app_topic_name = remappings.getString(sub_start_app_topic_name);

            pub_beacons_topic_name = pub_beacons_topic_name;
            sub_start_app_topic_name = sub_start_app_topic_name;

            pub_beacons = connectedNode.newPublisher(pub_beacons_topic_name , "std_msgs/String");
            pub_beacons.setLatchMode(true);
            Subscriber<std_msgs.String> sub_app_start =
                    connectedNode.newSubscriber(sub_start_app_topic_name, std_msgs.String._TYPE);

            sub_app_start.addMessageListener(new MessageListener<std_msgs.String>() {
                @Override
                public void onNewMessage(std_msgs.String message) {
                    sendData2UI("Recieve Data from Rocon: [" + message.getData() + "]");
                    appHash = Integer.parseInt(message.getData());
                    try {
                        checkConcert();
                        getAppConfig();
                        sendData2UI("[3/3]Launch App Start");
                        launchApp(parent);
                    }
                    catch (Exception e) {
                        sendData2UI("Start app Error: " + e);
                        e.printStackTrace();
                    }
                }
            });

            isConnectRocon = true;
            waiting_flag = false;
        }
        @Override
        public void onShutdown(Node node) {
            isConnectRocon = false;
            Log.i("RoconConnector", "onShutdown");
            sendData2UI("onShutdown");
        }

        @Override
        public void onShutdownComplete(Node node) {
            isConnectRocon = false;
            Log.i("RoconConnector", "onShutdownComplete");
            sendData2UI("onShutdownComplete");
        }
        @Override
        public void onError(Node node, Throwable throwable) {
            isConnectRocon = false;
            if (waiting_flag){
                waiting_flag = false;
            }
            Log.i("RoconConnector", "onError");
            sendData2UI("onError: ["+throwable.getMessage()+"]");
        }
    }

}
