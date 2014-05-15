package com.github.robotics_in_concert.rocon_demo.waiterbot_order_app;

import android.app.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.widget.Toast;

// Android Log
import android.util.Log;


// RosJava Imports
import org.ros.message.MessageListener;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeListener;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;
import org.ros.node.NodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.AbstractNodeMain;
import org.ros.namespace.GraphName;
import org.ros.address.InetAddressFactory;

// Android Core Imports
import org.ros.android.MessageCallable;
import org.ros.android.view.RosTextView;

import java.lang.String;
import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;

import std_msgs.*;


public class SelectDrinkActivity extends Activity
{

    private static final int DRINK_TYPE_REQUEST_CODE = 0xD000;
    private static final int DRINK_LIST     = 0xD001;
    private static final int DRINK_TYPE_UNKNOWN = 0xDFFF;

    private static final int ARRIVAL_REQUEST_CODE = 0xA000;
    private static final int ARRIVAL_ORIGIN2VM = 0xA001;
    private static final int ARRIVAL_VM2ORIGIN = 0xA002;

    private int m_count_drink_1 = 0;
    private int m_count_drink_2 = 0;

    private Toast    lastToast;
    private ConnectedNode node;

    BroadcastReceiver mBroadcastReceiver = null;
    BroadcastReceiver mBroadcastReceiver_debug = null;
    BroadcastReceiver mBroadcastReceiver_battery_status = null;
    BroadcastReceiver mBroadcastReceiver_auto_order = null;

    private boolean m_show_log = false;
    private Activity m_parent = this;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {

        setContentView(R.layout.activity_select_drink);

        findViewById(R.id.drink_1).setOnClickListener(mClickListener);
        findViewById(R.id.drink_2).setOnClickListener(mClickListener);
        findViewById(R.id.order).setOnClickListener(mClickListener);
        findViewById(R.id.show_log).setOnClickListener(mClickListener);

        super.onCreate(savedInstanceState);

        Log.d("CES_DEMO","[SelectDrinkActivity][onCreate]: end");

        IntentFilter pkgFilter = new IntentFilter();
        pkgFilter.addAction("com.github.robotics_in_concert.rocon_demo.waiterbot_order_app.CesDemoOrderApp.end_signal");
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("CES_DEMO", "[SelectDrinkActivity][BroadcastReceiver]: end signal ");

                Intent result_intent = new Intent ();
                m_parent.setResult(DRINK_TYPE_UNKNOWN,result_intent);
                finish();
            }
        };
        registerReceiver(mBroadcastReceiver,pkgFilter);

        IntentFilter pkgFilter_auto_order = new IntentFilter();
        pkgFilter_auto_order.addAction("com.github.robotics_in_concert.rocon_demo.waiterbot_order_app.CesDemoOrderApp.auto_order");

        mBroadcastReceiver_auto_order = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                m_count_drink_1 = (int)(Math.random() * 10)%2 +1;
                m_count_drink_2 = (int)(Math.random() * 10)%2 +1;
                short [] order_list = new short[m_count_drink_1+m_count_drink_2 ];
                for(int i = 0 ; i <order_list.length ;i ++){
                    if (i < m_count_drink_1){
                        order_list[i] = 1;
                    }
                    else{
                        order_list[i] = 2;
                    }
                }
                Log.d("CES_DEMO", "[SelectDrinkActivity][BroadcastReceiver_auto_order]:"+m_count_drink_1+" "+m_count_drink_2);
                Intent result_intent = new Intent ();
                result_intent.putExtra("drink_list",order_list);
                m_parent.setResult(DRINK_LIST, result_intent);
                finish();
            }
        };
        registerReceiver(mBroadcastReceiver_auto_order,pkgFilter_auto_order);


        IntentFilter pkgFilter_debug = new IntentFilter();
        pkgFilter_debug.addAction("com.github.robotics_in_concert.rocon_demo.waiterbot_order_app.CesDemoOrderApp.waiterbot_debug");
        mBroadcastReceiver_debug = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Log.d("CES_DEMO", "[SelectDrinkActivity][BroadcastReceiver_debug]: "+ intent.getStringExtra("log"));
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

        m_count_drink_1 = 0;
        m_count_drink_2 = 0;

        if (CesDemoOrderApp.m_bIsLogging == true){
            TextView log_tv = (TextView)findViewById(R.id.log);
            log_tv.setVisibility(View.VISIBLE);
        }
    }

    Button.OnClickListener mClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.drink_1:
                    Log.d("CES_DEMO","[SelectDrinkActivity][setOnClickListener]: drink 1");
                    m_count_drink_1 +=1;
                    if(m_count_drink_1 == 3){
                        m_count_drink_1 = 0;
                    }
                    if(m_count_drink_1+m_count_drink_2>2){
                        m_count_drink_1 = 0;
                    }

                    TextView drink_1_quantity_tv = (TextView)findViewById(R.id.drink_1_quantity);
                    String count_drink_1 = ""+m_count_drink_1;
                    drink_1_quantity_tv.setText(count_drink_1);

                    break;

                case R.id.drink_2:
                    Log.d("CES_DEMO","[SelectDrinkActivity][setOnClickListener]: drink 2");
                    m_count_drink_2+=1 ;
                    if(m_count_drink_2 == 3){
                        m_count_drink_2 = 0;
                    }
                    if(m_count_drink_1+m_count_drink_2>2){
                        m_count_drink_2 = 0;
                    }

                    TextView drink_2_quantity_tv = (TextView)findViewById(R.id.drink_2_quantity);
                    String count_drink_2 = ""+m_count_drink_2;
                    drink_2_quantity_tv.setText(count_drink_2);

                    break;

                case R.id.order:

                    int total_order_count = m_count_drink_1+m_count_drink_2;
                    if (total_order_count == 0){
                        Log.d("CES_DEMO","[SelectDrinkActivity][setOnClickListener]: order fail "+ (m_count_drink_1+m_count_drink_2));
                        Toast err_toast = Toast.makeText(SelectDrinkActivity.this, "No order drink.\nPlease, press drink button", Toast.LENGTH_SHORT);
                        err_toast.setGravity(Gravity.CENTER,0,0);
                        err_toast.show();
                    }
                    else if (total_order_count >2){
//                        Log.d("CES_DEMO","[SelectDrinkActivity][setOnClickListener]: order fail "+ (m_count_drink_1+m_count_drink_2));
//                        Toast err_toast = Toast.makeText(SelectDrinkActivity.this, "Too many drink\nPlease, order under 2", Toast.LENGTH_SHORT);
//                        err_toast.setGravity(Gravity.CENTER,0,0);
//                        err_toast.show();
                    }
                    else{
                        Log.d("CES_DEMO","[SelectDrinkActivity][setOnClickListener]: order "+ (m_count_drink_1+m_count_drink_2));
                        short[] order_list = new short [m_count_drink_1+m_count_drink_2];
                        for(int i = 0 ; i <order_list.length ;i ++){
                            if (i < m_count_drink_1){
                                order_list[i] = 1;
                            }
                            else{
                                order_list[i] = 2;
                            }
                        }

                        Intent intent = new Intent ();
                        intent.putExtra("drink_list",order_list);
                        setResult(DRINK_LIST,intent);
                        finish();
                    }
                    break;

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
                default:
                    Log.d("CES_DEMO","[SelectDrinkActivity][setOnClickListener]: drink 2");
                    setResult(DRINK_TYPE_UNKNOWN);
                    break;
            }
        }
    };

}
