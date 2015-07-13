package com.github.robotics_in_concert.rocon_android_apps.beacon_awareness;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import com.wizturn.sdk.WizTurnDelegate;
import com.wizturn.sdk.WizTurnManager;
import com.wizturn.sdk.WizTurnProximityState;
import com.wizturn.sdk.baseclass.IWizTurnController;
import com.wizturn.sdk.entity.WizTurnBeacons;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by dwlee on 14. 8. 12.
 */

public class WizTurnBeaconService extends Service {

    public WizTurnManager _wizturnMgr;

    private double minimum_distance = 1.5;
    private String pre_awareness_beacon = "";
    private boolean isbinding = false;
    private int noti_id = 0;

    private ICallback mWizturnCallback;
    private ICallback mRoconCallback;
    NotificationManager notification_mgr = null;
    Notification noti = null;
    private SendMassgeHandler mMainHandler = new SendMassgeHandler();

    private static ArrayList<String> recog_beacons = new ArrayList<String>();
    private int max_recong_beacons_num = 1;

    private TimerTask mTask;
    private Timer mTimer;

    Calendar calendar = Calendar.getInstance();

    // Binder given to clients
    private final IBinder mBinder = new WizTurnBeaconServiceBinder();
    public class WizTurnBeaconServiceBinder extends Binder {
        WizTurnBeaconService getService() {
            // Return this instance of LocalService so clients can call public methods
            return WizTurnBeaconService.this;
        }
    }
    public interface ICallback {
        public void sendData(String data);
        public void sendDbgData(String data);
    }

    public void registerCallback(ICallback wizturn_cb, ICallback rocon_cb) {
        mWizturnCallback = wizturn_cb;
        mRoconCallback = rocon_cb;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("[WTSrv]", "onStartCommand()");
        //_vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        wizturnMgr_setup();
        noti_setup();
        return START_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i("[WTSrv]", "onUnbind()");
        uninitWizturnSrv();
        return true;
    }
    @Override
    public void onRebind(Intent intent) {
        Log.i("[WTSrv]", "onRebind()");
        initWizturnSrv();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i("[WTSrv]", "onBind()");
        initWizturnSrv();
        return mBinder;
    }
    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public void onDestroy() {
        Log.i("[WTSrv]", "onDestroy()");
        super.onDestroy();
        if (_wizturnMgr.isStarted()) {
            // WizTurn Scan Stop
            _wizturnMgr.stopController();
        }
        _wizturnMgr.destroy();
        if(mTimer != null){
            mTimer.cancel();
            mTimer.purge();
            mTimer = null;
        }
    }

    private void initWizturnSrv(){
        isbinding = true;
        pre_awareness_beacon = "";
        deleteNoit();
    }

    private void uninitWizturnSrv(){
        isbinding = false;
        pre_awareness_beacon = "";
    }

    public void deleteNoit(){
        if (notification_mgr != null){
            notification_mgr.cancel(noti_id);
        }
    }

    public void noti_setup(){
        notification_mgr = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        noti = new Notification(R.drawable.ic_launcher, "BeaconAwareness", System.currentTimeMillis());
        //noti.defaults = Notification.DEFAULT_SOUND;
        noti.flags = Notification.FLAG_ONLY_ALERT_ONCE;
        noti.flags = Notification.FLAG_AUTO_CANCEL;
        Intent intent = new Intent(WizTurnBeaconService.this, BeaconAwarenessMainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingl = PendingIntent.getActivity(WizTurnBeaconService.this, 0,intent, 0);
        noti.setLatestEventInfo(WizTurnBeaconService.this, "New Message", "You can use Rocon services", pendingl);
    }

    public void wizturnMgr_setup(){
        Log.i("[WTSrv]" ,"wizturnMgr_setup()");
        _wizturnMgr = WizTurnManager.sharedInstance(this);
        // Check if device supports BLE.
        if (!_wizturnMgr.hasBluetooth()) {
            Toast.makeText(this, "Device does not have Bluetooth Low Energy", Toast.LENGTH_LONG).show();
            return;
        }
        else {
            //Wizturn Scan Start
            _wizturnMgr.setInitController();
            _wizturnMgr.setWizTurnDelegate(_wtDelegate);
            _wizturnMgr.startController();

            Log.i("[WTSrv]" ,"_wizturnMgr startController");
        }
    }
    private void sendData2UI (String Data){
        Message msg = mMainHandler.obtainMessage();
        msg.what = 2;
        Bundle bundle = new Bundle();
        bundle.putString("sendData2UI", Data);
        msg.setData(bundle);
        mMainHandler.sendMessage(msg);
    }

    private void sendDbgData2UI (String Data){
        Message msg = mMainHandler.obtainMessage();
        msg.what = 3;
        Bundle bundle = new Bundle();
        bundle.putString("sendDbgData2UI", Data);
        msg.setData(bundle);
        mMainHandler.sendMessage(msg);
    }

    private void sendData2Rocon (String Data){
        Message msg = mMainHandler.obtainMessage();
        msg.what = 1;
        Bundle bundle = new Bundle();
        bundle.putString("sendData2Rocon", Data);
        msg.setData(bundle);
        mMainHandler.sendMessage(msg);
    }

    private void notifyData(){
        if (noti != null && notification_mgr != null && isbinding == false){
            deleteNoit();
            notification_mgr.notify(noti_id,noti);
        }
    }

    private void set_recog_beacon(String beacon_data){
        if (recog_beacons.size() >= max_recong_beacons_num){
            recog_beacons.remove(0);
        }
        recog_beacons.add(beacon_data);
        Log.i("[WTSrv]", "set_recog_beacon: " + recog_beacons.toString());
    }
    private String get_recog_beacon(){
        HashMap<String, Integer> recog_beacon_map = new HashMap<String, Integer>();
        for(int i = 0 ; i < recog_beacons.size() ; i ++ ){
            if (recog_beacon_map.containsKey(recog_beacons.get(i)) == false){
                recog_beacon_map.put(recog_beacons.get(i),1);
            }else{
                int count = recog_beacon_map.get(recog_beacons.get(i));
                recog_beacon_map.put(recog_beacons.get(i),count+1);
            }
        }

        HashSet hs = new HashSet(recog_beacons);
        ArrayList<String> recog_beacons_key = new ArrayList<String>(hs);

        int max_count = -1;
        String return_beacon = "";
        for(int i = 0 ; i< recog_beacons_key.size(); i ++){
            int count = recog_beacon_map.get(recog_beacons_key.get(i));
            if (count > max_count){
                return_beacon = recog_beacons_key.get(i);
                max_count = count;
            }
        }
        if (max_count < (int)((float)max_recong_beacons_num/2+0.5)){
            return_beacon = "";
        }
        Log.i("[WTSrv]", "get_recog_beacon: " + max_count + " / " + return_beacon);
        return return_beacon;
    }

    private WizTurnDelegate _wtDelegate = new WizTurnDelegate() {
        //GetRssi Event
        @Override
        public void onGetRSSI(IWizTurnController iWizTurnController, List<String> Data, List<Integer> RSSI) {
        }

        @Override
        public void onGetDeviceList(IWizTurnController iWizTurnController, final List<WizTurnBeacons> device) {
            String strMac = "";
            String strScanResult = "";

            String dbgData = "";
            for (int i = 0; i < device.size(); i++) {
                dbgData +=  "["+device.get(i).getMacAddress() + ": " + device.get(i).getDistance() + "m]";
            }
            sendDbgData2UI(dbgData);

            for (int i = 0; i < device.size(); i++) {
                if (minimum_distance < device.get(i).getDistance()) {
                    continue;
                }else{
                    set_recog_beacon(device.get(i).getMacAddress());
                }
                strMac = get_recog_beacon();
                if(strMac.length() == 0){
                    continue;
                }

                strScanResult = "discoveried device: ["+strMac+"] ["+device.get(i).getDistance() +" m]";
                if (mTimer != null){
                    mTimer.cancel();
                    mTimer.purge();
                    mTimer = null;
                }
                mTimer = new Timer();
                mTimer.schedule(new BeaconTimer(), 10000);

                Log.i("[WTSrv]" ,strScanResult);
                sendData2UI(strScanResult);
                if (!pre_awareness_beacon.equals(device.get(i).getMacAddress())){
                    notifyData();
                    sendData2Rocon(strMac);
                }
                pre_awareness_beacon = device.get(i).getMacAddress();
                break;
            }
        }
        //Proximity Event
        @Override
        public void onGetProximity(IWizTurnController iWizTurnController, WizTurnProximityState proximity) {
        }
    };

    class BeaconTimer extends TimerTask {
        public void run() {
            Log.i("[WTSrv]" ,"Alram Timer!!!!");
            pre_awareness_beacon = "";
            recog_beacons.clear();
            deleteNoit();
            sendData2Rocon("");
            sendData2UI("Scanning...");
        }
    }


    ArrayList<String> dbgDataList = new ArrayList<String>();
    int nMaxDbgDataLength = 15;
    public class SendMassgeHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String data = "";

            switch (msg.what) {
                case 1:
                    data = msg.getData().getString("sendData2Rocon");
                    if (mRoconCallback != null && isbinding) {
                        mRoconCallback.sendData(data);
                    }
                    break;
                case 2:
                    data = msg.getData().getString("sendData2UI");
                    if (mWizturnCallback != null && isbinding) {
                        mWizturnCallback.sendData(data);
                    }
                    break;
                case 3:
                    Calendar calendar = Calendar.getInstance();
                    data = calendar.getTime().toString() + "-" + msg.getData().getString("sendDbgData2UI") + "\n";
                    if(dbgDataList.size() > nMaxDbgDataLength){
                        dbgDataList.remove(0);
                    }
                    dbgDataList.add(data);
                    if (mWizturnCallback != null && isbinding) {
                        mWizturnCallback.sendDbgData(dbgDataList.toString());
                    }
                    break;
                default:
                    break;
            }
        }
    }

}