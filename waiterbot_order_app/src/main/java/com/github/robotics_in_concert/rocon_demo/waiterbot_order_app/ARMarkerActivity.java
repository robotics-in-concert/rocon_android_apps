package com.github.robotics_in_concert.rocon_demo.waiterbot_order_app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class ARMarkerActivity extends Activity {
    private String m_drink_type = "";
    BroadcastReceiver mBroadcastReceiver = null;
    BroadcastReceiver mBroadcastReceiver_debug = null;
    BroadcastReceiver mBroadcastReceiver_battery_status = null;

    private boolean m_show_log = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_armarker);
        Intent intent = getIntent();
        m_drink_type = intent.getStringExtra("drink_type");
        Log.d("WAITERBOT_ORDER_APP", "[ARMarkerActivity][onCreate]: drink_type: "+m_drink_type);

        IntentFilter pkgFilter = new IntentFilter();
        pkgFilter.addAction("com.github.robotics_in_concert.rocon_demo.waiterbot_order_app.WaiterbotOrderApp.end_signal");
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("WAITERBOT_ORDER_APP", "[ARMarkerActivity][BroadcastReceiver]: end signal ");
                finish();
            }
        };
        registerReceiver(mBroadcastReceiver, pkgFilter);

        IntentFilter pkgFilter_debug = new IntentFilter();
        pkgFilter_debug.addAction("com.github.robotics_in_concert.rocon_demo.waiterbot_order_app.WaiterbotOrderApp.waiterbot_debug");
        mBroadcastReceiver_debug = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("WAITERBOT_ORDER_APP", "[ARMarkerActivity][BroadcastReceiver_debug]: end signal ");
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
        pkgFilter_battery_status.addAction("com.github.robotics_in_concert.rocon_demo.waiterbot_order_app.WaiterbotOrderApp.battery_status");
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


        if (WaiterbotOrderApp.m_bIsLogging == true){
            TextView log_tv = (TextView)findViewById(R.id.log);
            log_tv.setVisibility(View.VISIBLE);

        }
        showImage();

    }

    public void showImage(){
        Log.d("WAITERBOT_ORDER_APP", "[ARMarkerActivity][showImage]:");
        ImageView iv = (ImageView)findViewById(R.id.ar_marker);
        iv.setBackgroundColor(Color.rgb(255, 255, 255));
        if (m_drink_type.equals("drink1")){ // drink 1 ar marker
            Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.ar_marker_1);
            iv.setImageBitmap(bmp);
        }
        else if(m_drink_type.equals("drink2")){ // drink 2 ar marker
            Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.ar_marker_2);
            iv.setImageBitmap(bmp);
        }
        else{ // unknown
            Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.ar_marker_0);
            iv.setImageBitmap(bmp);
        }
        //setContentView(iv);
    }
    Button.OnClickListener mClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            //Intent intent;
            //intent = new Intent(SelectDrinkActivity.this, ARMarkerActivity.class);

            switch (v.getId()) {
                case R.id.show_log:
                    if (WaiterbotOrderApp.m_bIsLogging == true){
                        WaiterbotOrderApp.m_bIsLogging = false;
                        TextView log_tv = (TextView)findViewById(R.id.log);
                        log_tv.setVisibility(View.INVISIBLE);

                    }
                    else{
                        WaiterbotOrderApp.m_bIsLogging = true;
                        TextView log_tv = (TextView)findViewById(R.id.log);
                        log_tv.setVisibility(View.VISIBLE);

                    }
                    break;
            }
        }
    };



    public class Receiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("WAITERBOT_ORDER_APP", "[ARMarkerActivity][BroadcastReceiver]");
        }
    }
}
