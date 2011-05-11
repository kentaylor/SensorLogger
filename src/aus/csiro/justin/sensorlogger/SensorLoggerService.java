/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package aus.csiro.justin.sensorlogger;

import android.R.string;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import aus.csiro.justin.sensorlogger.activities.ResultsActivity;

import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

import aus.csiro.justin.sensorlogger.R;
import aus.csiro.justin.sensorlogger.rpc.SensorLoggerBinder;

/**
 *
 * @author chris
 */
public class SensorLoggerService extends Service {

	private int index=0;
	
	public String phoneLocation = "Pants front pocket";
	public String usrComment = "Empty";
	
	private final SensorLoggerBinder.Stub binder = new SensorLoggerBinder.Stub() {
    	
  
        public void setState(int newState) throws RemoteException {
            doSetState(newState);
        }
        public void setIndex(int index) throws RemoteException{
        	doSetIndex(index);
        }
        public int getCountdownTime() throws RemoteException {
            return countdown;
        }
        public int getIndex() throws RemoteException{
        	return index;
        }
        public int getState() throws RemoteException {
            return state;
        }

        public void submitClassification(String classification) throws RemoteException {
            if (!classifications.containsKey(classification)) {
                classifications.put(classification, 0);
            }

            classCount++;
            classifications.put(classification, classifications.get(classification) + 1);

            if (classCount == 15) {
//                showNotification();
//                setState(5);
            }
        }

        public String getClassification() throws RemoteException {
            int best = 0;
            String bestName = "CLASSIFIED/UNKNOWN";

            for (Map.Entry<String, Integer> entry : classifications.entrySet()) {
                if (entry.getValue() > best) {
                    best = entry.getValue();
                    bestName = entry.getKey();
                }
            }

            return bestName;
        }

        public void submitWithCorrection(String newCorrection,String location, String comment) throws RemoteException {
            correction = newCorrection;
            phoneLocation = location;
            usrComment = comment;
            submit();
        }

        public void submit() throws RemoteException {
            final Intent intent = new Intent(SensorLoggerService.this, UploaderService.class);
            intent.putExtra("x-correction", correction);
            intent.putExtra("x-activity", getClassification());
            intent.putExtra("x-version", getVersionName());
            intent.putExtra("x-application", "SensorLogger");
            intent.putExtra("x-imei", getIMEI());
            intent.putExtra("location", phoneLocation);
            intent.putExtra("comment", usrComment);
            setState(6);
            startService(intent);
        }
		@Override
		public void setClassfication(String strClss) throws RemoteException {
			correction = strClss;
		}
		@Override
		public void setLocation(String loc) throws RemoteException {
			phoneLocation = loc;
		}
		@Override
		public void setComment(String cmnt) throws RemoteException {
			usrComment = cmnt;
		}
        
    };

    final Handler handler = new Handler();

    Map<String, Integer> classifications = new HashMap<String, Integer>();
    String correction = "UNCLASSIFIED/NOTCORRECTED";
    int classCount = 0;
    int countdown;
    int state = 1;

    public String getIMEI() {
        return ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
    }

    public String getVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (NameNotFoundException ex) {
            return "Unknown";
        }
    }

    public void showNotification() {
        NotificationManager mNotificationManager
                = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        int icon = R.drawable.icon;
        CharSequence tickerText = "Classification complete";
        long when = System.currentTimeMillis();

        Notification notification = new Notification(icon, tickerText, when);
        notification.defaults |= Notification.DEFAULT_ALL;
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        CharSequence contentTitle = "Recording and analysis complete";
        CharSequence contentText = "Select to see results";
        Intent notificationIntent = new Intent(this, ResultsActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        notification.setLatestEventInfo(getApplicationContext(), contentTitle,
                contentText, contentIntent);

        mNotificationManager.notify(0, notification);
    }

    /** {@inheritDoc} */
    @Override
    public IBinder onBind(final Intent arg0) {
        return binder;
    }
    void doSetIndex(final int index) {
    	this.index=index;
    
    }
    void doSetState(final int newState) {
        switch (newState) {
            case 8:
                ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(0);
                stopSelf();
                break;
        }

        state = newState;
    }

}
