package com.github.robotics_in_concert.rocon_demo.waiterbot_order_app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ArrivalActivity extends Activity {
    private static final int DRINK_TYPE_REQUEST_CODE = 0xD000;
    private static final int DRINK_TYPE_1 = 0xD001;
    private static final int DRINK_TYPE_2 = 0xD002;
    private static final int DRINK_TYPE_UNKNOWN = 0xDFFF;

    private static final int ARRIVAL_REQUEST_CODE = 0xA000;
    private static final int ARRIVAL_ORIGIN2VM = 0xA001;
    private static final int ARRIVAL_VM2ORIGIN = 0xA002;

    BroadcastReceiver mBroadcastReceiver_debug = null;
    BroadcastReceiver mBroadcastReceiver = null;
    BroadcastReceiver mBroadcastReceiver_battery_status = null;

    private boolean m_show_log = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arrival);

        IntentFilter pkgFilter = new IntentFilter();
        pkgFilter.addAction("com.github.robotics_in_concert.rocon_demo.waiterbot_order_app.CesDemoOrderApp.end_signal");
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("CES_DEMO", "[ArrivalActivity][BroadcastReceiver]: end signal ");
                finish();
            }
        };
        registerReceiver(mBroadcastReceiver,pkgFilter);

        IntentFilter pkgFilter_debug = new IntentFilter();
        pkgFilter_debug.addAction("com.github.robotics_in_concert.rocon_demo.waiterbot_order_app.CesDemoOrderApp.waiterbot_debug");
        mBroadcastReceiver_debug = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("CES_DEMO", "[ArrivalActivity][BroadcastReceiver_debug]: end signal ");
                if (intent.getStringExtra("data") != null){
                    TextView waiterbot_debug_tv = (TextView)findViewById(R.id.waiterbot_debug);
                    waiterbot_debug_tv.setText(intent.getStringExtra("data"));
                }
                if (intent.getStringExtra("log") != null){
                    TextView log_tv = (TextView)findViewById(R.id.log);
                    log_tv.setText(intent.getStringExtra("log"));
                }
            }
        };
        registerReceiver(mBroadcastReceiver_debug,pkgFilter_debug);

        IntentFilter pkgFilter_battery_status = new IntentFilter();
        pkgFilter_battery_status.addAction("com.github.robotics_in_concert.rocon_demo.waiterbot_order_app.CesDemoOrderApp.battery_status");
        mBroadcastReceiver_battery_status = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getStringExtra("battery_status") != null){
                    TextView  battery_status_tv= (TextView)findViewById(R.id.battery_status);
                    battery_status_tv.setText(intent.getStringExtra("battery_status"));
                }

            }
        };
        registerReceiver(mBroadcastReceiver_battery_status,pkgFilter_battery_status);

        findViewById(R.id.show_log).setOnClickListener(mClickListener);

        Intent intent = getIntent();

        String strStatus = intent.getStringExtra("status");
        String strStatusDescription = intent.getStringExtra("status_description");

        TextView status_tv = (TextView)findViewById(R.id.status);
        status_tv.setText(strStatus);

        TextView status_description_tv = (TextView)findViewById(R.id.status_description);
        status_description_tv.setText(strStatusDescription);

        if (CesDemoOrderApp.m_bIsLogging == true){
            TextView log_tv = (TextView)findViewById(R.id.log);
            log_tv.setVisibility(View.VISIBLE);

        }


    }

    Button.OnClickListener mClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            //Intent intent;
            //intent = new Intent(SelectDrinkActivity.this, ARMarkerActivity.class);
            switch (v.getId()) {
                case R.id.show_log:
                    if (CesDemoOrderApp.m_bIsLogging == true){
                        CesDemoOrderApp.m_bIsLogging = false;
                        TextView log_tv = (TextView)findViewById(R.id.log);
                        log_tv.setVisibility(View.INVISIBLE);

                    }
                    else{
                        CesDemoOrderApp.m_bIsLogging = true;
                        TextView log_tv = (TextView)findViewById(R.id.log);
                        log_tv.setVisibility(View.VISIBLE);
                    }
                    break;
            }
        }
    };




}
