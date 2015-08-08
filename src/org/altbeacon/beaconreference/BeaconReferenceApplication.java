package org.altbeacon.beaconreference;

import java.util.List;

import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.altbeacon.beacon.AltBeaconParser;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.TimedBeaconSimulator;
import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.logging.Loggers;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.RegionBootstrap;
import org.altbeacon.beacon.startup.BootstrapNotifier;

/**
 * Created by dyoung on 12/13/13.
 */

public class BeaconReferenceApplication extends Application implements BootstrapNotifier {
    private static final String TAG = "AndroidProximityReferenceApplication";
    private RegionBootstrap regionBootstrap;
    private BackgroundPowerSaver backgroundPowerSaver;
    private boolean haveDetectedBeaconsSinceBoot = false;
    private MonitoringActivity monitoringActivity = null;
    
    public void onCreate() {
        
        Log.d(TAG, "onCreate()");
        
        super.onCreate();
        
        // 设置Altbeacon library log messages                                                                            
         LogManager.setLogger(Loggers.verboseLogger());
               
        // 唯一实例 
        BeaconManager beaconManager = org.altbeacon.beacon.BeaconManager.getInstanceForApplication(this);
        
        /**********************************************************************
         *  BeaconManager beaconManager =BeaconManager.getInstanceForApplication(this);
         *  new BeaconManager(Context context) 
         *   this.beaconParser.add(new AltBeaconParser());
         *    AltBeaconParser()
         *        //The beacon device manufacturer's company identifier code.
         *        //https://www.bluetooth.org/en-us/specification/assigned-numbers/company-identifiers
         *        mHardwareAssistManufacturers = new int[]{0x0118};  // 0x0118 Radius networks,(280)
         *                                                           // ​0x015D	Estimote, Inc. (349)
         *                                                           // 0x004C Apple, Inc. (76) 在BeaconParser()中是默认的
         *        this.setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"); 
         * 因此，默认情况下，识别的是Radius networks的Beacon，beaconTypeCode=beac，其传输帧的字节序列按照以上顺序传输。
         * 也支持Apple的Beacon，在BeaconParser()中，mHardwareAssistManufacturers = new int[]{0x004c};
         * 经过测试，天津的Beacon应该是Apple的Beacon，beaconTypeCode=0215
         * 其传输帧的字节序列按照以下顺序传输，但是网络上查到2013年后的Estimote beacons也是下列的字节顺序
         * beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
         *********************************************************************/

        // By default the AndroidBeaconLibrary will only find AltBeacons.  If you wish to make it
        // find a different type of beacon, you must specify the byte layout for that beacon's
        // advertisement with a line like below.  The example shows how to find a beacon with the
        // same byte layout as AltBeacon but with a beaconTypeCode of 0xaabb.  To find the proper
        // layout expression for other beacon types, do a web search for "setBeaconLayout"
        // including the quotes.
        //
        // beaconManager.getBeaconParsers().add(new BeaconParser().
        //        setBeaconLayout("m:2-3=aabb,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
        //
        // "m:0-3=4c000215,i:4-19,i:20-21,i:22-23,p:24-24"
        // "m:8-9=0215,i:10-13,i:14-15,i:16-17,i:18-25"
        // "m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"
        // "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"
        // "m:0-3=ad7700c6"

        // 经过测试，天津的Beacon应该是Apple的Beacon，beaconTypeCode=0215
        // 其传输帧的字节序列按照以下顺序传输，但是网络上查到2013年后的Estimote beacons也是下列的字节顺序,ok
        //beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        
        //也可能是AltBeacon(即Radius)的Beacon,ok
        beaconManager.getBeaconParsers().add(new AltBeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        
        // 把制造商的写入前两个字节是Apple的0x004c，但是，在BeaconParser()也设为(0x004c),其余与上一句等同,ok
        // beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:0-3=4c000215,i:4-19,i:20-21,i:22-23,p:24-24"));
        // 也支持AltBeacon(即Radius),ok
        //beaconManager.getBeaconParsers().add(new AltBeaconParser().setBeaconLayout("m:0-3=4c000215,i:4-19,i:20-21,i:22-23,p:24-24"));
          
        Log.d(TAG, "setting up background monitoring for beacons and power saving");
        
        // wake up the app when a beacon is seen
        // Region region = new Region("backgroundRegion",null, null, null);
        Region region = new Region("backgroundRegion",null, null, null);
        
        // Constructor to bootstrap引导程序 your Application on an entry/exit from a single region.
        regionBootstrap = new RegionBootstrap(this, region);

        // simply constructing this class and holding a reference to it in your custom Application
        // class will automatically cause the BeaconLibrary to save battery whenever the application
        // is not visible.  This reduces bluetooth power usage by about 60%
        backgroundPowerSaver = new BackgroundPowerSaver(this);
        
        // If you wish to test beacon detection in the Android Emulator, you can use code like this:
        //BeaconManager.setBeaconSimulator(new TimedBeaconSimulator() );
        // ((TimedBeaconSimulator) BeaconManager.getBeaconSimulator()).createTimedSimulatedBeacons();
    }

	@Override
    public void didEnterRegion(Region arg0) {
    	 if (monitoringActivity != null) {
             monitoringActivity.logToDisplay("didEnterRegion");
         }
    	 
        // In this example, this class sends a notification to the user whenever a Beacon
        // matching a Region (defined above) are first seen.
        Log.d(TAG, "did enter region.");
        if (!haveDetectedBeaconsSinceBoot) {
            Log.d(TAG, "auto launching MainActivity");
            monitoringActivity.logToDisplay("didEnterRegion(),auto launching MainActivity");

            // The very first time since boot that we detect an beacon, we launch the
            // MainActivity
            Intent intent = new Intent(this, MonitoringActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // Important:  make sure to add android:launchMode="singleInstance" in the manifest
            // to keep multiple copies of this activity from getting created if the user has
            // already manually launched the app.
            this.startActivity(intent);
            haveDetectedBeaconsSinceBoot = true;
        } else {
            if (monitoringActivity != null) {
                // If the Monitoring Activity is visible, we log info about the beacons we have
                // seen on its display
                monitoringActivity.logToDisplay("I see a beacon again" );
            } else {
                // If we have already seen beacons before, but the monitoring activity is not in
                // the foreground, we send a notification to the user on subsequent detections.
                Log.d(TAG, "Sending notification.");
                sendNotification();
            }
        }


    }

    @Override
    public void didExitRegion(Region region) {
        if (monitoringActivity != null) {
            monitoringActivity.logToDisplay("I no longer see a beacon.");
        }
    }

    @Override
    public void didDetermineStateForRegion(int state, Region region) {
        if (monitoringActivity != null) {
            monitoringActivity.logToDisplay("I have just switched from seeing/not seeing beacons: " + state);
        }
    }

    private void sendNotification() {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setContentTitle("Beacon Reference Application")
                        .setContentText("An beacon is nearby.")
                        .setSmallIcon(R.drawable.ic_launcher);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntent(new Intent(this, MonitoringActivity.class));
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setContentIntent(resultPendingIntent);
        NotificationManager notificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    public void setMonitoringActivity(MonitoringActivity activity) {
        this.monitoringActivity = activity;
    }

}