package com.github.robotics_in_concert.rocon_android_apps.beacon_awareness;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
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
        if (_wizturnMgr.isStarted()) {
            // WizTurn Scan Stop
            _wizturnMgr.stopController();
        }
        _wizturnMgr.destroy();
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
            for (int i = 0; i < device.size(); i++) {
                if (minimum_distance < device.get(i).getDistance()) {
                    continue;
                }
                strMac = device.get(i).getMacAddress();
                Log.d("[WTSrv]" ,"discoveried device: ["+strMac+"] ["+device.get(i).getDistance() +" m]");

                if (strMac.equals(Annotation.dwlee.getMacAddress())){
                    if(preLocation != Location.DWLEE) {
                        Toast.makeText(getApplicationContext(),"Here is dwlee",Toast.LENGTH_SHORT).show();
                        _vibrator.vibrate(500);
                        if (mCallback != null) {
                            mCallback.sendData("Here is dwlee");
                        }

                    }
                    if(preLocation == Location.DWLEE) {
                        Toast.makeText(getApplicationContext(),"Here is dwlee, still",Toast.LENGTH_SHORT).show();
                        if (mCallback != null) {
                            mCallback.sendData("Here is dwlee, still");
                        }
                    }
                    preLocation = Location.DWLEE;
                }
                else if(strMac.equals(Annotation.entance.getMacAddress())){
                    Log.d("[WTSrv]" ,"discoveried device: entance");
                    if(preLocation != Location.ENTANCE) {
                        Toast.makeText(getApplicationContext(),"Here is entance",Toast.LENGTH_SHORT).show();
                        _vibrator.vibrate(500);
                        if (mCallback != null) {
                            mCallback.sendData("Here is entance");
                        }
                    }
                    preLocation = Location.ENTANCE;
                }
                else if(strMac.equals(Annotation.inno.getMacAddress())){
                    Log.d("[WTSrv]" ,"discoveried device: inno");
                    if(preLocation != Location.INNO) {
                        Toast.makeText(getApplicationContext(),"Here is inno",Toast.LENGTH_SHORT).show();
                        _vibrator.vibrate(500);
                        if (mCallback != null) {
                            mCallback.sendData("Here is entance, inno");
                        }
                    }
                    preLocation = Location.INNO;
                }
                else{
                    Log.d("[WTSrv]" ,"discoveried device: unknown");
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
