package org.altbeacon.beaconreference;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import android.app.Activity;
import android.os.Bundle;
import android.os.RemoteException;
import android.widget.EditText;

public class RangingActivity extends Activity implements BeaconConsumer {
    protected static final String TAG = "RangingActivity";
    private BeaconManager beaconManager = BeaconManager.getInstanceForApplication(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ranging);

        beaconManager.bind(this);
    }
    @Override 
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }
    @Override 
    protected void onPause() {
    	super.onPause();
    	if (beaconManager.isBound(this)) beaconManager.setBackgroundMode(true);
    }
    @Override 
    protected void onResume() {
    	super.onResume();
    	if (beaconManager.isBound(this)) beaconManager.setBackgroundMode(false);
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setRangeNotifier(new RangeNotifier() {
        @Override 
        public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
            if (beacons.size() > 0) {
            	//EditText editText = (EditText)RangingActivity.this.findViewById(R.id.rangingText);
                Beacon firstBeacon = beacons.iterator().next();
                //logToDisplay("The first beacon "+firstBeacon.toString()+" is about "+firstBeacon.getDistance()+" meters away.");            }
                logToDisplay("distance="+firstBeacon.getDistance());            }
        }

        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {   }
    }
    private void logToDisplay(final String line) {
    	runOnUiThread(new Runnable() {
    		Date date = new Date(System.currentTimeMillis());
    		SimpleDateFormat sfd = new SimpleDateFormat("HH:mm:ss.SSS",Locale.CHINA);
	    	String dateStr = sfd.format(date);
    	    public void run() {
    	    	EditText editText = (EditText)RangingActivity.this
    					.findViewById(R.id.rangingText);
    	    	editText.append(dateStr+"=="+line+"\n");            	
    	    }
    	});
    }
}
