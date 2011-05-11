/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package aus.csiro.justin.sensorlogger.activities;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.TimerTask;

import aus.csiro.justin.sensorlogger.R;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.flurry.android.FlurryAgent;

/**
 *
 * @author chris
 * modified by Justin
 */
public class RecordingActivity extends BoundActivity implements OnClickListener, SensorEventListener{

	private SensorManager sensors = null;

	private SensorManager mSensorManager;
	private float[] mags = new float[3];
	private float[] accels = new float[3];
	private float[] mGData = new float[3];
	private float[] mMData = new float[3];
	private float[] mOData = new float[3];
	private float[] mR = new float[16];
	private float[] mI = new float[16];
	private long startTime;
	private static final int DELAY = 30000;
	int defTimeOut =0;
	private final Handler handler = new Handler();
	int state = 3;
	int phase = 3;
	static int sec = 0;
	static int min = 0;
	static int hour = 0;
	static int check=1;
	private final TimerTask task = new TimerTask() {
		@Override
		public void run() {
			handler.postDelayed(task, 1000);
			checkSamples();
		}
	};

	public String ElapsedTime()
	{
		long elapsedTime = System.currentTimeMillis() - startTime;
	      elapsedTime = elapsedTime / 1000;

	      String seconds = Integer.toString((int) (elapsedTime % 60));
	      String minutes = Integer.toString((int) ((elapsedTime % 3600) / 60));
	      String hours = Integer.toString((int) (elapsedTime / 3600));

	      if (seconds.length() < 2) {
	        seconds = "0" + seconds;
	      }

	      if (minutes.length() < 2) {
	        minutes = "0" + minutes;
	      }

	      if (hours.length() < 2) {
	        hours = "0" + hours;
	      }
	      return hours + ":" + minutes + ":" + seconds;

	}
	//When "Stop" button is clicked, 
	public void onClick(View arg0) {
		FlurryAgent.onEvent("process_stop_click");
		try {
			service.setState(20);
			//Start ResultsActicity
			startActivity(new Intent(this, ResultsActivity.class));
			finish(); //function to finish the application
		} catch (RemoteException ex) {
			Log.e(getClass().getName(), "Unable to submit correction", ex);
		}
	}
	protected void onResume() {
		// Ideally a game should implement onResume() and onPause()
		// to take appropriate action when the activity looses focus
		super.onResume();
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		Sensor gsensor = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		Sensor msensor = sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		sensors.registerListener(this, gsensor, SensorManager.SENSOR_DELAY_GAME);
		sensors.registerListener(this, msensor, SensorManager.SENSOR_DELAY_GAME);
	}
	/** {@inheritDoc} */
	@Override
	public void onCreate(final Bundle icicle) {
		super.onCreate(icicle);
		//Keep screen on during collecting data
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.recording);
		//add stop button on the screen
		((Button) findViewById(R.id.stop)).setOnClickListener(this);;

		sensors = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		startTime = System.currentTimeMillis();
		
		handler.removeCallbacks(task);
		handler.postDelayed(task, 500);

	}
	
	public void onPause() {
		if (sensors != null) {
			sensors.unregisterListener(this);
		}
		super.onPause();
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		sec = 0;
		min = 0;
		hour = 0;
		check=1;
		handler.removeCallbacks(task);
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		//        Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, defTimeOut);
	}

	public void checkSamples() {
		try {
			
			String time = ElapsedTime();

			((TextView) findViewById(R.id.recordingcount)).setText(time);

			final int serviceState = service.getState();

			if (serviceState > state) {
				state = serviceState;

				if (state == 4) {
					setTitle("Sensor Logger > Analysing");
					sec = 0;
					min = 0;
					hour = 0;
					check=1;
					((TextView) findViewById(R.id.recordingheader)).setTag(R.string.analysingheader);
				}
			}

			if (serviceState > 4) {
				sec = 0;
				min = 0;
				hour = 0;
				check=1;
				FlurryAgent.onEvent("countdown_to_results");
				startActivity(new Intent(this, ResultsActivity.class));
				finish();
			} 
		} catch (RemoteException ex) {
			Log.e(getClass().getName(), "Error getting countdown", ex);
		}
	}

	/** {@inheritDoc} */
	@Override
	protected void onStart() {
		super.onStart();

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		FlurryAgent.onStartSession(this, "TFBJJPQUQX3S1Q6IUHA6");
	}

	/** {@inheritDoc} */
	@Override
	protected void onStop() {
		super.onStop();

		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		FlurryAgent.onEndSession(this);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}
	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub

	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK
				&& event.getRepeatCount() == 0) {
			event.startTracking();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.isTracking()
				&& !event.isCanceled()) {
			try {
				service.setState(20);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			startActivity(new Intent(this,IntroActivity.class));
			finish();
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}



}
