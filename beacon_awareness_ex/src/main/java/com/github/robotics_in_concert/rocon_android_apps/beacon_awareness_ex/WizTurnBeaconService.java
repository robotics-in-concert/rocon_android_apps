package com.github.robotics_in_concert.rocon_android_apps.beacon_awareness_ex;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import com.wizturn.sdk.WizTurnDelegate;
import com.wizturn.sdk.WizTurnManager;
import com.wizturn.sdk.WizTurnProximityState;
import com.wizturn.sdk.baseclass.IWizTurnController;
import com.wizturn.sdk.entity.WizTurnBeacons;

import java.util.List;

/**
 * Created by dwlee on 14. 8. 12.
 */

public class WizTurnBeaconService extends Service {

    public WizTurnManager _wizturnMgr;
    private Vibrator _vibrator;
    public RoconConnector rocon_connector = new RoconConnector();

    protected enum Location{
        ENTANCE,DWLEE,INNO,UNKONW;
    }

    protected enum Annotation {
        entance("88:33:14:DE:D0:ED"),
        dwlee("D0:39:72:A3:DE:75"),
        inno("D0:FF:50:66:93:3F"),;
        private String span;
        Annotation(String strMacAddress) {
            span = strMacAddress;
        }
        public String getMacAddress(){
            return span;
        }
    }

    private Location preLocation;
    private double minimum_distance = 3;
    private ICallback mCallback;

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
    }

    public void registerCallback(ICallback cb) {
        mCallback = cb;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("[WTSrv]", "onBinder()");
        _vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        wizturnMgr_setup();

        Bundle intent_data = intent.getExtras();
        String master_uri;
        if (intent_data != null){
            master_uri = intent_data.getString("MasterURI");
        }else{
            master_uri = "http://localhost:11311";
        }
        rocon_connector_setup(master_uri);
        return mBinder;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }
    @Override
    public void onDestroy() {
        Log.d("[WTSrv]", "onDestroy()");
        super.onDestroy();
        mCallback.sendData("Scan Stop");
        rocon_connector.disConnectRocon();
        if (_wizturnMgr.isStarted()) {
            // WizTurn Scan Stop
            _wizturnMgr.stopController();
        }
        _wizturnMgr.destroy();
    }

    public void rocon_connector_setup(String master_uri){
        rocon_connector.setRoconConfig(master_uri);
        rocon_connector.connectRocon();
    }

    public void wizturnMgr_setup(){
        Log.d("[WTSrv]" ,"wizturnMgr_setup()");
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
            Log.d("[WTSrv]" ,"_wizturnMgr startController");
        }
    }

    private WizTurnDelegate _wtDelegate = new WizTurnDelegate() {
        //GetRssi Event
        @Override
        public void onGetRSSI(IWizTurnController iWizTurnController, List<String> Data, List<Integer> RSSI) {
            //Log.d("WTSrv" ,"GATT BLE onGetRSSI wtDelegate");

        }
        @Override
        public void onGetDeviceList(IWizTurnController iWizTurnController, final List<WizTurnBeacons> device) {
            String strMac = "";
            String strScanResult = "";
            for (int i = 0; i < device.size(); i++) {
                if (minimum_distance < device.get(i).getDistance()) {
                    continue;
                }
                strMac = device.get(i).getMacAddress();
                strScanResult = "discoveried device: ["+strMac+"] ["+device.get(i).getDistance() +" m]";
                Log.d("[WTSrv]" ,strScanResult);
                if (mCallback != null) {
                    mCallback.sendData(strScanResult);
                    rocon_connector.publish(strMac);
                }
            }
        }
        //Proximity Event
        @Override
        public void onGetProximity(IWizTurnController iWizTurnController, WizTurnProximityState proximity) {
            //Log.d("WTSrv" ,"GATT BLE onGetProximity wtDelegate");

        }
    };
}