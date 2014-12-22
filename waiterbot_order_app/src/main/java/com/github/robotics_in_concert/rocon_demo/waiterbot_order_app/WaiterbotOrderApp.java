package com.github.robotics_in_concert.rocon_demo.waiterbot_order_app;

import android.app.*;
import android.content.Intent;
import android.os.*;
import android.view.*;
import android.widget.*;

// Android Log
import android.util.Log;


// RosJava Imports
import org.ros.internal.message.RawMessage;
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

// Android App Imports
import com.github.rosjava.android_remocons.common_tools.apps.RosAppActivity;

import java.lang.String;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import std_msgs.*;
import diagnostic_msgs.*;

public class WaiterbotOrderApp extends RosAppActivity
{
    static boolean m_bIsLogging = false;
    private static final int ROBOT_MASTER_CHOOSER_REQUEST_CODE = 1;

    private static final int DRINK_TYPE_REQUEST_CODE = 0xD000;
    private static final int DRINK_LIST     = 0xD001;
    private static final int DRINK_TYPE_UNKNOWN = 0xDFFF;

    private static final int ARRIVAL_REQUEST_CODE = 0xA000;
    private static final int ARRIVAL_ORIGIN2VM = 0xA001;
    private static final int ARRIVAL_VM2ORIGIN = 0xA002;

    private Toast    lastToast;
    private ConnectedNode node;
    private RosTextView<std_msgs.String> rosTextView;
    private OrderApp m_order_app= new OrderApp();


    public WaiterbotOrderApp()
    {
        super("WaiterbotOrderApp", "WaiterbotOrderApp");
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        setDefaultMasterName(getString(R.string.default_robot));
        setDefaultAppName(getString(R.string.paired_app_name));
        setDashboardResource(R.id.top_bar);
        setMainWindowResource(R.layout.main);

        //setContentView(R.layout.main);
        super.onCreate(savedInstanceState);
        Log.d("WAITERBOT_ORDER_APP","[WaiterbotOrderApp][onCreate]: end");
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor)
    {
        Log.d("WAITERBOT_ORDER_APP","[WaiterbotOrderApp][init]: start");
        super.init(nodeMainExecutor);
        NodeConfiguration nodeConfiguration =
                NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(), getMasterUri());

        nodeMainExecutor.execute(m_order_app, nodeConfiguration);
        Log.d("WAITERBOT_ORDER_APP","[WaiterbotOrderApp][init]: end");
        startActivityForResult(new Intent(this, SelectDrinkActivity.class),DRINK_TYPE_REQUEST_CODE);
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

    public void printLog(String str){
        Log.d("WAITERBOT_ORDER_APP","[WaiterbotOrderApp][printLog]"+str);
    }
    public void setContent(int resource){
        //setMainWindowResource()
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent){
        super.onActivityResult(requestCode,resultCode,intent);
        Log.d("WAITERBOT_ORDER_APP","[WaiterbotOrderApp][onActivityResult] "+ requestCode+" "+resultCode);
        if (requestCode == DRINK_TYPE_REQUEST_CODE){

            if(resultCode ==DRINK_LIST ){
                std_msgs.UInt16MultiArray drink_order = m_order_app.publisher_drink_order.newMessage();
                short[] drink_order_data = null;
                Intent sub_intent = null;
                drink_order_data  = intent.getShortArrayExtra("drink_list");
                    drink_order.setData(drink_order_data);
                    m_order_app.publisher_drink_order.publish(drink_order);

                    sub_intent = new Intent(this, StatusActivity.class);

                    if(((int) (Math.random() * 10))%2 == 0){
                        sub_intent.putExtra("order_status", "I'll be right back.");
                    }
                    else{
                        sub_intent.putExtra("order_status","One moment, please.");
                    }
                    startActivity(sub_intent);
            }
            else if(requestCode == DRINK_TYPE_UNKNOWN){
                Log.d("WAITERBOT_ORDER_APP","[WaiterbotOrderApp][onActivityResult] DRINK_TYPE_REQUEST_CODE is NULL");
            }
        }
        Log.d("WAITERBOT_ORDER_APP","[WaiterbotOrderApp][onActivityResult] end process");
    }

    public class OrderApp extends AbstractNodeMain {
        private ArrayList<String> m_debug_log_list;
        private int m_maximum_log = 30;
        private int m_debug_count = 0;
        private float m_laptop_batt = 0;
        private float m_robot_batt = 0;

        private Publisher<std_msgs.UInt16MultiArray> publisher_drink_order = null;



        private HashMap<String, String> keyValueArrayToMap(List<KeyValue> list) {
            HashMap<String, String> map = new HashMap<String, String>();
            for(KeyValue kv : list) {
                map.put(kv.getKey(), kv.getValue());
            }
            return map;
        }

        private float populateBatteryFromStatus(DiagnosticStatus status) {
            HashMap<String, String> values = keyValueArrayToMap(status.getValues());
            try {
                float percent = 100 * Float.parseFloat(values.get("Charge (Ah)")) / Float.parseFloat(values.get("Capacity (Ah)"));
                Log.d("WAITERBOT_ORDER_APP","[WaiterbotOrderApp][diagnosticSubscriber][populateBatteryFromStatus]:"+ percent);

                // TODO: set color red/yellow/green based on level (maybe with
                // level-set
                // in XML)
                return percent;
            } catch(NumberFormatException ex) {
                // TODO: make battery level gray
            } catch(ArithmeticException ex) {
                // TODO: make battery level gray
            } catch(NullPointerException ex) {
                // Do nothing: data wasn't there.
            }
            try {
                return 0;
            } catch(NumberFormatException ex) {
            } catch(ArithmeticException ex) {
            } catch(NullPointerException ex) {
            }
            return 0;
        }

        private void handleDiagnosticArray(DiagnosticArray msg) {
            String mode = null;
            for(DiagnosticStatus status : msg.getStatus()) {
                if(status.getName().equals("/Power System/Battery")) {
                    Log.d("WAITERBOT_ORDER_APP","[WaiterbotOrderApp][diagnosticSubscriber][handleDiagnosticArray]: /Power System/Battery");
                    m_robot_batt = (int)populateBatteryFromStatus(status);
                }
                if(status.getName().equals("/Power System/Laptop Battery")) {
                    Log.d("WAITERBOT_ORDER_APP","[WaiterbotOrderApp][diagnosticSubscriber][handleDiagnosticArray]: /Power System/Laptop Battery");
                    m_laptop_batt = (int)populateBatteryFromStatus(status);
                }
                if(status.getName().equals("/Mode/Operating Mode")) {
                    Log.d("WAITERBOT_ORDER_APP","[WaiterbotOrderApp][diagnosticSubscriber][handleDiagnosticArray]: /Mode/Operating Mode");
                    mode = status.getMessage();
                }
            }
            String battery_status ="[Robot: "+m_robot_batt+"][laptop: "+m_laptop_batt+"]";
            sendBatteryStatus(battery_status);

        }

        public void sendBatteryStatus(String battery_status){
            Intent indent  = new Intent("com.github.robotics_in_concert.rocon_demo.waiterbot_order_app.WaiterbotOrderApp.battery_status");
            indent.putExtra("battery_status",battery_status);
            sendBroadcast(indent);
        }
        public void sendLog(String log){
            m_debug_count++;
            String strLog = m_debug_count+ ": "+log+ "\n";
            m_debug_log_list.add(0,strLog);
            if(m_debug_count >= m_maximum_log){
                m_debug_log_list.remove(m_debug_log_list.size()-1);
            }

            Log.d("WAITERBOT_ORDER_APP","[WaiterbotOrderApp][sendLog]"+log);
            Intent indent  = new Intent("com.github.robotics_in_concert.rocon_demo.waiterbot_order_app.WaiterbotOrderApp.waiterbot_debug");
            indent.putExtra("data",log);
            indent.putExtra("log",m_debug_log_list.toString());
            sendBroadcast(indent);
        }
        @Override
        public GraphName getDefaultNodeName() {
            return GraphName.of("waiterbot_order_app");
        }

        @Override
        public void onStart(ConnectedNode connectedNode) {
            m_debug_log_list = new ArrayList<String>();
            //sub
            String DrinkArTopic = remaps.get(getString(R.string.drink_ar_topic));
            String DrinkDispensedTopic = remaps.get(getString(R.string.drinks_dispensed_topic));
            String WaiterbotDebugTopic = remaps.get(getString(R.string.waiterbot_debug_topic));
            String TrayEmptyTopic = remaps.get(getString(R.string.tray_empty_topic));
            String OrderCancelledTopic = remaps.get(getString(R.string.order_cancelled_topic));

            //pub
            String DrinkOrderTopic = remaps.get(getString(R.string.drink_order_topic));

            //set the publisher
            publisher_drink_order =
                    connectedNode.newPublisher(getMasterNameSpace().resolve(DrinkOrderTopic).toString(), std_msgs.UInt16MultiArray._TYPE);

            //set the subscriber
            ////////////////////////////////////////////////////////////////////////////////////////////////
            Subscriber<diagnostic_msgs.DiagnosticArray> diagnosticSubscriber =
                    connectedNode.newSubscriber("diagnostics_agg", diagnostic_msgs.DiagnosticArray._TYPE);
            diagnosticSubscriber.addMessageListener(new MessageListener<diagnostic_msgs.DiagnosticArray>() {
                @Override
                public void onNewMessage(final diagnostic_msgs.DiagnosticArray message) {
                    Log.d("WAITERBOT_ORDER_APP","[WaiterbotOrderApp][onNewMessage][diagnosticSubscriber]");
                    handleDiagnosticArray(message);
                }
            });
            ////////////////////////////////////////////////////////////////////////////////////////////////
            Subscriber<std_msgs.UInt16> subscriber_drink_ar =
                    connectedNode.newSubscriber(getMasterNameSpace().resolve(DrinkArTopic).toString(), std_msgs.UInt16._TYPE);

            subscriber_drink_ar.addMessageListener(new MessageListener<std_msgs.UInt16>() {
                @Override
                public void onNewMessage(std_msgs.UInt16 message) {
                    Log.d("WAITERBOT_ORDER_APP","[WaiterbotOrderApp][onNewMessage][subscriber_drink_ar]"+message.getData());
                    Intent indentBroadcast  = new Intent("com.github.robotics_in_concert.rocon_demo.waiterbot_order_app.WaiterbotOrderApp.end_signal");
                    sendBroadcast(indentBroadcast);

                    Intent intent = new Intent(WaiterbotOrderApp.this, ARMarkerActivity.class);
                    if (message.getData() == 1){
                        intent.putExtra("drink_type","drink1");
                    }
                    else if (message.getData() == 2){
                        intent.putExtra("drink_type","drink2");
                    }
                    startActivity(intent);
                }
            });

            ////////////////////////////////////////////////////////////////////////////////////////////////
            Subscriber<std_msgs.Empty> subscriber_order_cancelled=
                    connectedNode.newSubscriber(getMasterNameSpace().resolve(OrderCancelledTopic).toString(), std_msgs.Empty._TYPE);

            subscriber_order_cancelled.addMessageListener(new MessageListener<std_msgs.Empty>() {
                @Override
                public void onNewMessage(std_msgs.Empty message) {
                    Log.d("WAITERBOT_ORDER_APP","[WaiterbotOrderApp][onNewMessage][subscriber_order_cancelled]");
                    Intent indentBroadcast  = new Intent("com.github.robotics_in_concert.rocon_demo.waiterbot_order_app.WaiterbotOrderApp.end_signal");
                    sendBroadcast(indentBroadcast);

                    startActivityForResult(new Intent(WaiterbotOrderApp.this, SelectDrinkActivity.class),DRINK_TYPE_REQUEST_CODE);

                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) { }

                    //Intent indentAutoOrder  = new Intent("com.github.robotics_in_concert.rocon_demo.waiterbot_order_app.WaiterbotOrderApp.auto_order");
                    // sendBroadcast(indentAutoOrder);

                }
            });

            ////////////////////////////////////////////////////////////////////////////////////////////////
            Subscriber<std_msgs.Empty> subscriber_tray_empty =
                    connectedNode.newSubscriber(getMasterNameSpace().resolve(TrayEmptyTopic).toString(), std_msgs.Empty._TYPE);

            subscriber_tray_empty.addMessageListener(new MessageListener<std_msgs.Empty>() {
                @Override
                public void onNewMessage(std_msgs.Empty message) {
                    Log.d("WAITERBOT_ORDER_APP","[WaiterbotOrderApp][onNewMessage][subscriber_tray_empty]");
                    Intent indentBroadcast  = new Intent("com.github.robotics_in_concert.rocon_demo.waiterbot_order_app.WaiterbotOrderApp.end_signal");
                    sendBroadcast(indentBroadcast);

                    Intent indent  = new Intent(WaiterbotOrderApp.this, ArrivalActivity.class);
                    indent.putExtra("status","Thank you and enjoy the show!!");
                    indent.putExtra("status_description","");
                    startActivityForResult(indent, ARRIVAL_REQUEST_CODE);
                    //sleep

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) { }

                    sendBroadcast(indentBroadcast);
                    startActivityForResult(new Intent(WaiterbotOrderApp.this, SelectDrinkActivity.class),DRINK_TYPE_REQUEST_CODE);

                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) { }

                    //Intent indentAutoOrder  = new Intent("com.github.robotics_in_concert.rocon_demo.waiterbot_order_app.WaiterbotOrderApp.auto_order");
                    //sendBroadcast(indentAutoOrder);

                }
            });
            ////////////////////////////////////////////////////////////////////////////////////////////////
            Subscriber<std_msgs.Empty> subscriber_drink_dispensed =
                    connectedNode.newSubscriber(getMasterNameSpace().resolve(DrinkDispensedTopic).toString(), std_msgs.Empty._TYPE);

            subscriber_drink_dispensed.addMessageListener(new MessageListener<std_msgs.Empty>() {
                @Override
                public void onNewMessage(std_msgs.Empty message) {
                    Log.d("WAITERBOT_ORDER_APP", "[WaiterbotOrderApp][onNewMessage][subscriber_drink_dispensed]");
                    Intent indentBroadcast  = new Intent("com.github.robotics_in_concert.rocon_demo.waiterbot_order_app.WaiterbotOrderApp.end_signal");
                    sendBroadcast(indentBroadcast);

                    Intent indent  = new Intent(WaiterbotOrderApp.this, ArrivalActivity.class);
                    indent.putExtra("status","Enjoy your drink.");
                    indent.putExtra("status_description","Please, press the green button.");
                    startActivityForResult(indent, ARRIVAL_REQUEST_CODE);

                }
            });

            ////////////////////////////////////////////////////////////////////////////////////////////////
            Subscriber<std_msgs.String> subscriber_waiterbot_debug =
                    connectedNode.newSubscriber(getMasterNameSpace().resolve(WaiterbotDebugTopic).toString(), std_msgs.String._TYPE);

            subscriber_waiterbot_debug.addMessageListener(new MessageListener<std_msgs.String>() {
                @Override
                public void onNewMessage(std_msgs.String message) {
                    sendLog(message.getData());
                }
            });
            ////////////////////////////////////////////////////////////////////////////////////////////////
        }

    }

}
