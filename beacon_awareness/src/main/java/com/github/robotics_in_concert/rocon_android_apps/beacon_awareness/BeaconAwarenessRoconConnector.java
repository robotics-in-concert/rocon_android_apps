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
import com.github.rosjava.android_remocons.common_tools.rocon.InteractionsManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ros.address.InetAddressFactory;
import org.ros.android.NodeMainExecutorService;
import org.ros.exception.RemoteException;
import org.ros.exception.RosRuntimeException;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMain;
import org.ros.node.service.ServiceResponseListener;
import org.ros.node.topic.Publisher;
import org.yaml.snakeyaml.Yaml;

import java.lang.String;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import rocon_interaction_msgs.GetInteractionResponse;
import rocon_interaction_msgs.GetInteractionsResponse;
import rocon_interaction_msgs.Interaction;

import static com.github.rosjava.android_remocons.common_tools.rocon.Constants.ANDROID_PLATFORM_INFO;
/**
 * Created by dwlee on 14. 12. 2.
 */
public class BeaconAwarenessRoconConnector {

    private int masterPort = 11311;
    private String masterHost = "localhost";
    private String masterUri  = "http://" + masterHost + ":" + masterPort;
    //todo
    //parameterization
    private String role_name = "Customer";
    private int role_index = -1;


    private BeaconAwarenessRoconConnector rocon_connector = this;
    private JSONObject beacon_profile= null;
    private JSONObject parameters = null;
    private JSONObject remappings = null;

    private Activity parent;
    private MasterId masterId;
    private NodeMainExecutorService nodeMainExecutorService;
    public BeaconAwarenessNode ba_bridge;
    private InteractionsManager interactionsManager;
    private ArrayList<Interaction> availableAppsCache = null;
    private ArrayList<String> launchableApps = null;

    private RoconDescription rocon_description;
    private Interaction app;
    private SendMessageHandler mMainHandler = new SendMessageHandler();
    private boolean waiting_flag = true;
    public boolean isConnectRocon = false;

    private ICallback mCallback;

    public interface ICallback {
        public void sendUIData(String data);
    }
    public void registerCallback(ICallback rocon_cb) {
        mCallback = rocon_cb;
    }
    public void setRoconConfig(Activity parent, String master_uri, String parameters, String remappings){
        this.parent = parent;
        this.masterUri  = master_uri;
//        try {
//            if (remappings.length() != 0){
//                this.remappings = new JSONObject();
//                String[] remappingList = remappings.replaceAll("\\{","").replaceAll("\\}","").replaceAll(" ","").split(",");
//                Log.i("BARoconConnector", "remappingList-"+remappingList.toString());
//                for(int i =0 ; i < remappingList.length; i++){
//                    String[] remapping = remappingList[i].split("\\:");
//                    Log.i("BARoconConnector", "remapping-"+remapping.toString());
//                    this.remappings.put(remapping[0],remapping[1]);
//                }
//                Log.i("BARoconConnector", "remappings : " + this.remappings.toString());
//            }
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
    }
    public boolean connectRocon(){
        if(isConnectRocon == false){
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
                Thread.sleep(1000); //for waiting register publisher and subscriber
            }
            catch (Exception e){
                Log.i("BARoconConnector", "error: " + e);
                return false;
            }
            Log.i("BARoconConnector", "ROS Connect success");

            try {
                checkConcert();
                getInteractions();
                isConnectRocon = true;
                sendData2UI("[ROCON] Enjoy ROCON services.");
            } catch (Exception e) {
                e.printStackTrace();
                isConnectRocon = false;
                sendData2UI("[ROCON] Cannot connect to ROCON.\nPlease reconnect by tap SERVICE START button");
            }

        }
        return true;
    }
    public void disConnectRocon(){
        if(isConnectRocon){
            nodeMainExecutorService.shutdownNodeMain(ba_bridge);
        }
    }
    public void publish_beacons_topic(String data){
        ba_bridge.publish_beacons_topic(data);
        Log.i("BARoconConnector", "Publish data");
    }
    public ArrayList<String> getLaunchableApps(String awared_beacon){
        ArrayList<String> Apps = new ArrayList<String>();
        try {
            for(Iterator<String> iter = this.beacon_profile.keys();iter.hasNext();) {
                String key = iter.next();
                if(this.beacon_profile.getJSONObject(key).getString("mac").equals(awared_beacon)){
                    JSONArray app_list = this.beacon_profile.getJSONObject(key).getJSONArray("app_list");
                    for (int app_idx = 0 ; app_idx < launchableApps.size(); app_idx++){
                        for (int i = 0; i < app_list.length(); i++){
                            String app_name = (String) app_list.get(i);
                            if (launchableApps.get(app_idx).contains(app_name.replaceAll(" ", ""))){
                                Apps.add(launchableApps.get(app_idx));
                            }
                        }
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (NullPointerException e){
            e.printStackTrace();
        }
        return Apps;
    }

    public boolean appLauncher(int app_hash){
        try {
            getAppConfig(app_hash);
            launchApp(parent);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
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
        rocon_description = null;
        final ConcertChecker cc = new ConcertChecker(
                new ConcertChecker.ConcertDescriptionReceiver() {
                    @Override
                    public void receive(RoconDescription roconDescription) {
                        rocon_description = roconDescription;
                        waiting_flag = false;
                        if ( rocon_description.getConnectionStatus() == MasterDescription.UNAVAILABLE ) {
                            // Check that it's not busy
                            Log.e("BARoconConnector", "Concert is unavailable: busy serving another remote controller");
                            sendData2UI("[Check Concert] Concert is unavailable: busy serving another remote controller");
                        }
                        else{
                            Log.e("BARoconConnector", "Concert is available");
                            sendData2UI("[Check Concert] Concert is available");
                        }
                    }
                },
                new ConcertChecker.FailureHandler() {
                    public void handleFailure(String reason) {
                        Log.e("BARoconConnector", "Cannot contact ROS master: " + reason);
                        sendData2UI("[Check Concert] Is Rocon Running? .\nPlease check Rocon server IP and reconnect by tap SERVICE START button");
                        try {
                            throw new Exception("[Check Concert] Cannot connect to " + masterId.getMasterUri() + " : "+ reason);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        waiting_flag = false;
                    }
                }
        );
        try {
            sendData2UI("[Check Concert] Concert Checking.....");
            cc.beginChecking(masterId);
            if(waitFor(30) == false || rocon_description == null) {
                cc.stopChecking();
                throw new Exception("[Check Concert] Cannot connect to " + masterId.getMasterUri() + ": time out.\n" +
                        "Please reconnect by tap SERVICE START button");
            }
            setRoconConnectorRole();
        }
        catch(RosRuntimeException e){
            Log.e("BARoconConnector", "RosRuntimeException"+ e);
            throw new Exception("[Check Concert] Cannot connect to " + masterId.getMasterUri() + " : "+ e);
        }
        catch(Exception e){
            Log.e("BARoconConnector", "Exception" + e);
            throw new Exception("[Check Concert] Cannot connect to " + masterId.getMasterUri() + " : "+ e);
        }
        sendData2UI("[Check Concert] Concert Connected");
        Log.e("BARoconConnector", "checkConcert end");
    }
    private void setBeaconInfomation(String data){
        if (data.length() != 0 ){
            try {
                this.beacon_profile = new JSONObject(data.replaceAll("\n","").replaceAll(" ",""));
                for(Iterator<String> iter = this.beacon_profile.keys();iter.hasNext();) {
                    String key = iter.next();
                    String beacon_info = this.beacon_profile.getString(key);
                    this.beacon_profile.put(key,new JSONObject(beacon_info));
                }
                Log.i("BARoconConnector", "Beacon information setting finish");
            } catch (JSONException e) {
                e.printStackTrace();
                Log.i("BARoconConnector", "Beacon information setting fail: " + e);
            }
        }

    }
    private void setRoconConnectorRole(){
        if(rocon_description != null){
            String[] user_roles = rocon_description.getUserRoles();
            for(int i = 0 ; i < user_roles.length ;i ++ ){
                if(user_roles[i].equals(role_name)){
                    role_index = i;
                }
            }
            rocon_description.setCurrentRole(role_index);
        }
        else{
            Log.e("BARoconConnector", "Beacon awareness rocon connector role setting fail: rocon_description null");
        }
        Log.e("BARoconConnector", "Beacon awareness rocon connector role name: " + role_name);
        Log.e("BARoconConnector", "Beacon awareness rocon connector role index: " + role_index);
    }
    private void getInteractions() throws Exception {
        interactionsManager = new InteractionsManager(
                new InteractionsManager.FailureHandler() {
                    public void handleFailure(String reason) {
                        Log.e("BARoconConnector", "Failure on interactions manager: " + reason);
                    }
                }
        );
        Log.w("BARoconConnector", "ANDROID_PLATFORM_INFO.getUri() " + ANDROID_PLATFORM_INFO.getUri());
        interactionsManager.setupGetInteractionsService(new ServiceResponseListener<GetInteractionsResponse>() {
            @Override
            public void onSuccess(rocon_interaction_msgs.GetInteractionsResponse response) {
                List<Interaction> apps = response.getInteractions();
                if (apps.size() > 0) {
                    availableAppsCache = (ArrayList<Interaction>) apps;
                    String beaconInformation = "";
                    launchableApps = new ArrayList<String>();
                    Log.i("BARoconConnector", "Interaction Publication: " + availableAppsCache.size() + " apps");
                    for (int i = 0 ; i < availableAppsCache.size(); i ++){
                        String user_friendly_name = availableAppsCache.get(i).getDisplayName().replaceAll(" ","") + "/" + availableAppsCache.get(i).getHash();
                        launchableApps.add(user_friendly_name);
                        if(availableAppsCache.get(i).getName().contains(parent.getPackageName())){
                            beaconInformation = availableAppsCache.get(i).getParameters();
                        };
                    }
                    setBeaconInfomation(beaconInformation);
                    waiting_flag = false;

                }
            }
            @Override
            public void onFailure(RemoteException e) {

            }
        });
        try {
            sendData2UI("[Interactions Checker] Checking interactions....");
            interactionsManager.init(rocon_description.getInteractionsNamespace());
            interactionsManager.getAppsForRole(rocon_description.getMasterId(), rocon_description.getCurrentRole());

            if(waitFor(10) == false || this.availableAppsCache == null) {

                throw new Exception("Cannot get interactions: [name space: " + rocon_description.getInteractionsNamespace() +
                        "][master uri: " + rocon_description.getMasterId() +
                        "][role: " + rocon_description.getCurrentRole()+ "]") ;

            }
        }
        catch(Exception e){
            Log.e("BARoconConnector", "Exception" + e);
            throw new Exception("[Interactions]Cannot connect to " + masterId.getMasterUri() + " : "+ e);
        }
    }

    private void getAppConfig(final int app_hash) throws Exception {
        Log.e("BARoconConnector", "getAppConfig start");
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
                    Log.i("BARoconConnector", "App with hash " + app_hash + " found in concert-" + app.getDisplayName());
                    sendData2UI("App with hash " + app_hash + " found in concert-" + app.getDisplayName());

                } else {
                    Log.i("BARoconConnector", "App with hash " + app_hash + " not found in concert");
                    sendData2UI("App with hash " + app_hash + " not found in concert");
                }
            }

            @Override
            public void onFailure(RemoteException e) {
                Log.e("BARoconConnector", "Get app info failed. " + e.getMessage());
            }
        });
        try {
            am.init(rocon_description.getInteractionsNamespace());
            am.getAppInfo(masterId, app_hash);
            sendData2UI(" Waiting 10s for getAppConfig result, ");
            if (waitFor(10) == false) {
                am.shutdown();
                throw new Exception("Cannot get app info for hash " + app_hash + ". Aborting app launch");
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
        Log.e("BARoconConnector", "getAppConfig end");
    }
    private void launchApp(Activity parent) throws Exception {
        sendData2UI("Launch App Start");
        Log.e("BARoconConnector", "launchApp start");
        AppLauncher.Result result = AppLauncher.launch(parent, rocon_description, app);
        if (result == AppLauncher.Result.SUCCESS) {
            Log.i("BARoconConnector", "["+app.getDisplayName() + "] successfully launched");
            sendData2UI("["+app.getDisplayName() + "] successfully launched");
        }
        else {
            // I could also show an "app not-installed" dialog and ask for going to play store to download the
            // missing app, but... this would stop to be a headless launcher! But maybe is a good idea, anyway
            throw new Exception("Launch [" + app.getDisplayName() + "] failed. " + result.message);
        }
        Log.e("BARoconConnector", "launchApp end");
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
                            Log.e("BARoconConnector", "time out waitting for app is not null");
                            return false;
                        }
                    }
                    return true;
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        try {
          return asyncTask.get(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e("BARoconConnector", "Async task interrupted. " + e.getMessage());
            return false;
        } catch (ExecutionException e) {
            Log.e("BARoconConnector", "Async task execution error. " + e.getMessage());
            return false;
        } catch (TimeoutException e) {
            Log.e("BARoconConnector", "Async task timeout (" + timeout + " s). " + e.getMessage());
            return false;
        }
    }
    public class SendMessageHandler extends Handler {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 0:
                        break;
                    case 1:
                        String data2UIData = msg.getData().getString("Data2UI");
                        mCallback.sendUIData(data2UIData);
                        break;
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
        private String sub_list_launchable_apps_topic_name = "list_launchable_apps";
        void publish_beacons_topic (String data){
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
            try {
                if (rocon_connector.remappings!= null && rocon_connector.remappings.getString(pub_beacons_topic_name) != null){
                    pub_beacons_topic_name = rocon_connector.remappings.getString(pub_beacons_topic_name);
                }
                if (rocon_connector.remappings != null && rocon_connector.remappings.getString(sub_start_app_topic_name) != null){
                    sub_start_app_topic_name = rocon_connector.remappings.getString(sub_start_app_topic_name);
                }
                Log.i("BARoconConnector", "pub_beacons_topic_name: " + pub_beacons_topic_name);
                Log.i("BARoconConnector", "sub_strat_app_topic_name: "+ sub_start_app_topic_name);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            pub_beacons = connectedNode.newPublisher(pub_beacons_topic_name , std_msgs.String._TYPE);
            pub_beacons.setLatchMode(true);
            waiting_flag = false;

//            Subscriber<std_msgs.String> sub_app_start =
//                    connectedNode.newSubscriber(sub_start_app_topic_name, std_msgs.String._TYPE);
//
//            sub_app_start.addMessageListener(new MessageListener<std_msgs.String>() {
//                @Override
//                public void onNewMessage(std_msgs.String message) {
//                    sendData2UI("Recieve Data from Rocon: [" + message.getData() + "]");
//                    appHash = Integer.parseInt(message.getData());
//                    try {
//                        //checkConcert();
//                        //getAppConfig();
//                        //sendData2UI("Launch App Start");
//                        //launchApp(parent);
//                    }
//                    catch (Exception e) {
//                        sendData2UI("Start app Error: " + e);
//                        e.printStackTrace();
//                    }
//                }
//            });
        }
        @Override
        public void onShutdown(Node node) {
            isConnectRocon = false;
            Log.i("BARoconConnector", "onShutdown");
            sendData2UI("onShutdown");
        }

        @Override
        public void onShutdownComplete(Node node) {
            isConnectRocon = false;
            Log.i("BARoconConnector", "onShutdownComplete");
            sendData2UI("onShutdownComplete");
        }
        @Override
        public void onError(Node node, Throwable throwable) {
            isConnectRocon = false;
            if (waiting_flag){
                waiting_flag = false;
            }
            Log.i("BARoconConnector", "onError");
            sendData2UI("onError: ["+throwable.getMessage()+"]");
        }
    }

}
