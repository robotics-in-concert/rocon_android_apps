package com.github.robotics_in_concert.rocon_android_apps.beacon_awareness;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.rosjava.android_remocons.common_tools.master.RoconDescription;

import java.util.ArrayList;


public class BeaconAwarenessMainActivity extends Activity implements View.OnClickListener{

    private static final String PREFS_KEY_MASTER_URI = "MASTER_URI_KEY";
    private static final String PREFS_KEY_PARAMETERS = "PARAMETERS_KEY";
    private static final String PREFS_KEY_REMAPPINGS = "REMAPPINGS_KEY";

    public WizTurnBeaconService wt_service;
    private boolean is_bound = false;
    private String parameters = "{}";
    private String remappings = "{}";
    private String master_uri = "http://localhost:11311";
    private RoconDescription rocon_desc = null;
    private Activity ba_ex_activity= this;
    private String last_detected_beacon = "";
    private CharSequence[] launchable_app_list = null;
    final BeaconAwarenessRoconConnector rocon_connector = new BeaconAwarenessRoconConnector();
    private AlertDialog ConnectConfirmDlg = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon_awareness_main);

        findViewById(R.id.srv_start).setOnClickListener(this);
        findViewById(R.id.srv_stop).setOnClickListener(this);
        findViewById(R.id.rocon_srv_start).setOnClickListener(this);
        findViewById(R.id.logo_show).setOnClickListener(this);
        initUI();
        initRoconConfig(false);
        connectRocon();

        if (isWizturnServiceRunning()){
            Log.i("[BeaconAwareness]", "BeaconAwareness services running?-true");
            startWizturnService(true);
        }
        else{
            Log.i("[BeaconAwareness]", "BeaconAwareness services running?-false");
        }

        String version;
        try {
            PackageInfo i = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
            Log.i("[BeaconAwareness]", "versionName: " + i.versionName);
            Log.i("[BeaconAwareness]", "versionCode: " + i.versionCode);

        } catch(PackageManager.NameNotFoundException e) {
        }
    }

    @Override
    public void onDestroy() {
        Log.i("[BeaconAwareness]", "onDestroy");
        super.onDestroy();
        saveRoconConfig();
        if (is_bound) {
            unbindService(mConnection);
            is_bound= false;
        }
        disconnectRocon();
        destroyProgressDialog();
        destroyLaunchableAppSelectorDialog();
    }

    @Override
    public void onResume() {
        Log.i("[BeaconAwareness]", "onResume");
        super.onResume();
        /*
        initRoconConfig(true);
        if (isWizturnServiceRunning()){
            Log.i("[BeaconAwareness]", "On Resume BeaconAwareness services running?-true");
            startWizturnService(true);
        }
        else{
            Log.i("[BeaconAwareness]", "On Resume BeaconAwareness services running?-false");
        }
        */
    }
    @Override
    public void onPause() {
        Log.i("[BeaconAwareness]", "OnPause");
        super.onPause();
        finish();
    }

    private void initUI(){
        last_detected_beacon = "";
        LinearLayout linear_layout = (LinearLayout)BeaconAwarenessMainActivity.this.findViewById(R.id.linearLayout);
        linear_layout.setVisibility(View.INVISIBLE);
    }

    private void saveRoconConfig(){
        Log.i("[BeaconAwareness]", "saveRoconConfig");
        EditText edit_text;
        edit_text = (EditText)this.findViewById(R.id.master_uri_txt);
        master_uri = edit_text.getText().toString();

        edit_text = (EditText)this.findViewById(R.id.parameters_txt);
        parameters = edit_text.getText().toString();

        edit_text = (EditText)this.findViewById(R.id.remapping_txt);
        remappings = edit_text.getText().toString();

        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
        editor.putString(PREFS_KEY_REMAPPINGS, remappings);
        editor.putString(PREFS_KEY_PARAMETERS, parameters);
        editor.putString(PREFS_KEY_MASTER_URI, master_uri);
        editor.commit();
    }

    private void initRoconConfig(boolean isRestart){
        TextView text_view;
        text_view = (TextView) com.github.robotics_in_concert.rocon_android_apps.beacon_awareness.BeaconAwarenessMainActivity.this.findViewById(R.id.rocon_status_txt);
        if (rocon_connector.isConnectRocon){
            text_view.setText("Connected");
        }
        else{
            text_view.setText("Disconnected");
        }

        if(isRestart == false){
            Bundle intent_data = getIntent().getExtras();
            if (intent_data != null){
                parameters = intent_data.getString("Parameters");
                if(parameters == null){
                    parameters = getPreferences(MODE_PRIVATE).getString(PREFS_KEY_PARAMETERS,"{}");
                }

                remappings = intent_data.getString("Remappings");
                if(remappings == null){
                    remappings = getPreferences(MODE_PRIVATE).getString(PREFS_KEY_REMAPPINGS,"{}");
                }

                rocon_desc = (RoconDescription) intent_data.get(RoconDescription.UNIQUE_KEY);
                if(rocon_desc == null){
                    master_uri = getPreferences(MODE_PRIVATE).getString(PREFS_KEY_MASTER_URI,"http://localhost:11311");
                }
                else{
                    master_uri = rocon_desc.getMasterUri().toString();
                }
            }
            else{
                Log.i("[BeaconAwareness]", "getPreferences");
                parameters = getPreferences(MODE_PRIVATE).getString(PREFS_KEY_PARAMETERS,"{}");
                remappings = getPreferences(MODE_PRIVATE).getString(PREFS_KEY_REMAPPINGS,"{}");
                master_uri = getPreferences(MODE_PRIVATE).getString(PREFS_KEY_MASTER_URI,"http://localhost:11311");
            }

            EditText edit_text;
            edit_text = (EditText)this.findViewById(R.id.master_uri_txt);
            edit_text.setText(master_uri);

            edit_text = (EditText)this.findViewById(R.id.parameters_txt);
            edit_text.setText(parameters);

            edit_text = (EditText)this.findViewById(R.id.remapping_txt);
            edit_text.setText(remappings);

            Log.i("[BeaconAwareness]", "master uri-"+master_uri);
            Log.i("[BeaconAwareness]", "remappings-"+remappings);
            Log.i("[BeaconAwareness]", "parameters-"+parameters);
        }

    }

    public boolean isWizturnServiceRunning() {
        ActivityManager manager = (ActivityManager) this.getSystemService(Activity.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.github.robotics_in_concert.rocon_android_apps.beacon_awareness.WizTurnBeaconService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void startWizturnService(boolean isRestart){
        Intent intent = new Intent(this, WizTurnBeaconService.class);
        if(isRestart == false){
            startService(intent);
        }
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        TextView text_view;
        text_view = (TextView) com.github.robotics_in_concert.rocon_android_apps.beacon_awareness.BeaconAwarenessMainActivity.this.findViewById(R.id.bt_status_txt);
        text_view.setText("Scanning...");
    }

    public void stopWizturnService() {
        Intent intent = new Intent(this, WizTurnBeaconService.class);
        stopService(intent);
        if (is_bound) {
            unbindService(mConnection);
            is_bound= false;
            TextView text_view;
            text_view = (TextView) com.github.robotics_in_concert.rocon_android_apps.beacon_awareness.BeaconAwarenessMainActivity.this.findViewById(R.id.bt_status_txt);
            text_view.setText("Scan Stop");
        }
        if(rocon_connector.isConnectRocon) {
            disconnectRocon();
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.srv_start:
                Log.i("[BeaconAwareness]", "push srv_start-"+is_bound);
                connectRocon();
                startWizturnService(false);
                break;
            case R.id.srv_stop:
                Log.i("[BeaconAwareness]", "push srv_stop-"+is_bound);
                stopWizturnService();
                disconnectRocon();
                break;
            case R.id.rocon_srv_start:
                Log.i("[BeaconAwareness]", "push rocon_srv_start");
                startAppLaunchService();
                break;
            case R.id.logo_show:
                LinearLayout linear_layout = (LinearLayout)BeaconAwarenessMainActivity.this.findViewById(R.id.linearLayout);
                if (linear_layout.getVisibility() == View.INVISIBLE){
                    linear_layout.setVisibility(View.VISIBLE);
                }
                else{
                    linear_layout.setVisibility(View.INVISIBLE);
                }
                break;
            default:
                break;
        }
    }

    private AlertDialog.Builder dialogConnectConfirm = null;
    private void destroyLaunchableAppSelectorDialog(){
        if (ConnectConfirmDlg != null ){
            Log.i("[BeaconAwareness]", "ConnectConfirmDlg dismiss: beacon info is null");
            ConnectConfirmDlg.dismiss();
            ConnectConfirmDlg = null;
        }
        else{
            Log.i("[BeaconAwareness]", "ConnectConfirmDlg dismiss: beacon info is already null");
        }
    }

    private void createLaunchableAppSelectorDialog(){
        //creatSelection Popup
        if(!((Activity)BeaconAwarenessMainActivity.this).isFinishing()){
            if(ConnectConfirmDlg != null){
                Log.i("[BeaconAwareness]", "ConnectConfirmDlg dismiss: already pupop");
                ConnectConfirmDlg.dismiss();
            }
            dialogConnectConfirm = new AlertDialog.Builder(BeaconAwarenessMainActivity.this);
            dialogConnectConfirm.setTitle("You can start rocon service. Please choose!");
            dialogConnectConfirm.setSingleChoiceItems(launchable_app_list, -1, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String user_friendly_name = (String) launchable_app_list[which];
                    Log.i("[BeaconAwareness]", "Selected service: " + user_friendly_name);
                    int app_hash = Integer.parseInt(user_friendly_name.split("\\/")[1]);
                    appLauncher(app_hash);
                }
            });
            dialogConnectConfirm.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dlog, int i) {
                    dlog.dismiss();
                    dialogConnectConfirm = null;
                }
            });
            ConnectConfirmDlg = dialogConnectConfirm.create();
            ConnectConfirmDlg.show();
        }
        else{
            Log.i("[BeaconAwareness]", "Activity is not running!!");
        }

    }

    private ProgressDialog progressDialog = null;
    private void createProgressDialog(){
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        progressDialog = ProgressDialog.show(this,"", "Waiting Rocon...", true, true);
        progressDialog.setCancelable(false);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    }
    private void destroyProgressDialog(){
        if (progressDialog != null){
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    private void startAppLaunchService(){
        if (last_detected_beacon.length() != 0 ){
            updateLaunchableAppList(last_detected_beacon);
            Button btn = (Button)ba_ex_activity.findViewById(R.id.rocon_srv_start);
            Log.i("[BeaconAwareness]", "launchable_app_list: "+ launchable_app_list);
            Log.i("[BeaconAwareness]", "rocon_connector.isConnectRocon: "+ rocon_connector.isConnectRocon);
            if (launchable_app_list != null && launchable_app_list.length != 0 ){
                createLaunchableAppSelectorDialog();
            }

            if (rocon_connector.isConnectRocon){
                btn.setVisibility(Button.VISIBLE);
            }
            else{
                btn.setVisibility(Button.INVISIBLE);
            }

            publishData2Rocon(last_detected_beacon);
        }
        else{
            destroyLaunchableAppSelectorDialog();
            last_detected_beacon = "";
            Button btn = (Button)ba_ex_activity.findViewById(R.id.rocon_srv_start);
            btn.setVisibility(Button.INVISIBLE);
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i("[BeaconAwareness]", "Beacon Service connection!!!");
            wt_service = ((WizTurnBeaconService.WizTurnBeaconServiceBinder)service).getService();
            wt_service.registerCallback(mWizTurnCallback, mRoconCallback);

            is_bound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i("[BeaconAwareness]", "Beacon Service disconnection!!!");
            wt_service = null;
            is_bound = false;
        }

        private WizTurnBeaconService.ICallback mRoconCallback = new WizTurnBeaconService.ICallback() {
            @Override
            public void sendData(String data) {
                last_detected_beacon = data;
                startAppLaunchService();
            }
        };

        private WizTurnBeaconService.ICallback mWizTurnCallback = new WizTurnBeaconService.ICallback() {
            public void sendData(String data) {
                TextView text_view;
                text_view = (TextView)ba_ex_activity.findViewById(R.id.bt_status_txt);
                text_view.setText(data);
            }
        };
    };
    private void publishData2Rocon(String data){
        if (rocon_connector.isConnectRocon){
            rocon_connector.publish_beacons_topic(data);
        }
    }
    private boolean isCheckingRocon = false;

    private void connectRocon(){
        if (isCheckingRocon == true){
            Log.i("[BeaconAwareness]", "Now Checking rocon.......... wait");
            return;
        }
        createProgressDialog();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i("[BeaconAwareness]", "Connect rocon");
                if (rocon_connector.isConnectRocon == false){
                    isCheckingRocon = true;
                    EditText edit_text;
                    edit_text = (EditText)ba_ex_activity.findViewById(R.id.master_uri_txt);
                    master_uri = edit_text.getText().toString();
                    edit_text = (EditText)ba_ex_activity.findViewById(R.id.parameters_txt);
                    parameters = edit_text.getText().toString();
                    edit_text = (EditText)ba_ex_activity.findViewById(R.id.remapping_txt);
                    remappings = edit_text.getText().toString();
                    rocon_connector.setRoconConfig(ba_ex_activity,master_uri, parameters, remappings);
                    rocon_connector.registerCallback(new BeaconAwarenessRoconConnector.ICallback() {
                        @Override
                        public void sendUIData(String data) {
                            TextView text_view;
                            text_view = (TextView)ba_ex_activity.findViewById(R.id.rocon_status_txt);
                            text_view.setText(data);
                        }
                    });
                    rocon_connector.connectRocon();
                }
                destroyProgressDialog();
                isCheckingRocon = false;
                if(rocon_connector.isConnectRocon){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startAppLaunchService();
                        }
                    });
                    Log.i("[BeaconAwareness]", "Connected rocon");
                }
                else{
                    Log.i("[BeaconAwareness]", "Not connected rocon");
                }
            }
        });
        t.start();
    }

    private void disconnectRocon(){
        if(rocon_connector.isConnectRocon) {
            rocon_connector.disConnectRocon();
        }
    }

    private void updateLaunchableAppList(String detected_beacon){
        if(rocon_connector.isConnectRocon) {
            ArrayList<String> launchableApps = rocon_connector.getLaunchableApps(detected_beacon);
            launchable_app_list = launchableApps.toArray(new CharSequence[launchableApps.size()]);
        }
    }

    private void appLauncher(int app_hash){
        rocon_connector.appLauncher(app_hash);
    }



}
