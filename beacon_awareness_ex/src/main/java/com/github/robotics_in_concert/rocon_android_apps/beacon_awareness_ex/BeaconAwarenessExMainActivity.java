package com.github.robotics_in_concert.rocon_android_apps.beacon_awareness_ex;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.github.rosjava.android_remocons.common_tools.master.RoconDescription;


public class BeaconAwarenessExMainActivity extends Activity implements View.OnClickListener{

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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon_awareness_ex_main);
        initRoconConfig();
        Log.d("[BeaconAwareness]", "BeaconAwareness services running?-"+isServiceRunningCheck());
        setupUI(findViewById(R.id.main_layout));
        findViewById(R.id.srv_start).setOnClickListener(this);
        findViewById(R.id.srv_stop).setOnClickListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        saveRoconConfig();
        if (is_bound) {
            unbindService(mConnection);
            is_bound= false;
        }
    }

    private void saveRoconConfig(){
        Log.d("[BeaconAwareness]", "saveRoconConfig");
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

    private void initRoconConfig(){
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
            Log.d("[BeaconAwareness]", "getPreferences");
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


        Log.d("[BeaconAwareness]", "master uri-"+master_uri);
        Log.d("[BeaconAwareness]", "remappings-"+remappings);
        Log.d("[BeaconAwareness]", "parameters-"+parameters);
    }

    public boolean isServiceRunningCheck() {
        ActivityManager manager = (ActivityManager) this.getSystemService(Activity.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.github.robotics_in_concert.rocon_android_apps.beacon_awareness_ex.WizTurnBeaconService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(this, WizTurnBeaconService.class);

        switch(v.getId()){
            case R.id.srv_start:
                Log.d("[BeaconAwareness]", "push srv_start-"+is_bound);
                EditText edit_text;

                edit_text = (EditText)this.findViewById(R.id.master_uri_txt);
                master_uri = edit_text.getText().toString();

                edit_text = (EditText)this.findViewById(R.id.parameters_txt);
                parameters = edit_text.getText().toString();

                edit_text = (EditText)this.findViewById(R.id.remapping_txt);
                remappings = edit_text.getText().toString();

                intent.putExtra("Parameters",parameters);
                intent.putExtra("Remappings",remappings);
                intent.putExtra("MasterURI",master_uri);

                startService(intent);
                bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
                break;
            case R.id.srv_stop:
                Log.d("[BeaconAwareness]", "push srv_stop-"+is_bound);
                stopService(intent);
                if (is_bound) {
                    unbindService(mConnection);
                    is_bound= false;
                }
                break;
            default:
                break;
        }
    }

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
                Log.d("BeaconAwareness", "Beacon awareness!!!["+data+"]");
                TextView text_view;
                text_view = (TextView)ba_ex_activity.findViewById(R.id.bt_status_txt);
                text_view.setText(data);
            }
        };
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_beacon_awareness_ex_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager)  activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }

    public void setupUI(View view) {
        //Set up touch listener for non-text box views to hide keyboard.
        if(!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    Log.d("BeaconAwareness", "Touch UI");
                    hideSoftKeyboard(BeaconAwarenessExMainActivity.this);
                    return false;
                }
            });
        }
        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupUI(innerView);
            }
        }
    }

}
