/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package aus.csiro.justin.sensorlogger.activities;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.Vibrator;
import android.util.Log;
import android.widget.TextView;
import com.flurry.android.FlurryAgent;
import java.util.TimerTask;

import aus.csiro.justin.sensorlogger.R;

/**
 *
 * @author chris
 */
public class CountdownActivity extends BoundActivity {

	private int iCountDown;
	private final Handler handler = new Handler();

	private final TimerTask task = new TimerTask() {
		@Override
		public void run() {
			handler.postDelayed(task, 1000);

			updateCountdown();
		}
	};
	private MediaPlayer mMediaPlayer;

	/** {@inheritDoc} */
	@Override
	public void onCreate(final Bundle icicle) {
		super.onCreate(icicle);

		setContentView(R.layout.countdown);
		//Initializing the counddown task:
		iCountDown = 10;
		task.run();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	public void updateCountdown() {
		try {

			if (iCountDown > 0) {
				iCountDown--;
				((TextView) findViewById(R.id.countdowntimer)).setText("" + iCountDown);
				
			}
			//Only the first time this part runs, otherwise there will be few beeps and vibrations!
			else if (iCountDown-- == 0){
				FlurryAgent.onEvent("countdown_to_recording");
				playAudio();
				Vibrate();
				startActivity(new Intent(this, RecordingActivity.class));
				service.setState(3);
				//Stopping the countdown:
				task.cancel();
				finish();
			}
		} catch (RemoteException ex) {
			Log.e(getClass().getName(), "Error updating countdown", ex);
		}
	}

	/** {@inheritDoc} */
	@Override
	protected void onStart() {
		super.onStart();

		FlurryAgent.onStartSession(this, "TFBJJPQUQX3S1Q6IUHA6");
	}

	/** {@inheritDoc} */
	@Override
	protected void onStop() {
		super.onStop();

		FlurryAgent.onEndSession(this);
	}

	private void playAudio () {
		try {
			// http://www.soundjay.com/beep-sounds-1.html lots of free beeps here
			mMediaPlayer = MediaPlayer.create(this, R.raw.beep_mp3);
			mMediaPlayer.setLooping(false);
			Log.e("beep","started0");
			mMediaPlayer.start();
			mMediaPlayer.release();
			//     Log.e("beep","started1");
		} catch (Exception e) {
			Log.e("beep", "error: " + e.getMessage(), e);
		}
	}
	
	private void Vibrate()
	{
		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		v.vibrate(300);
	}

}
