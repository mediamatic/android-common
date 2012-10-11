package nl.mediamatic.android.tools;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.WindowManager;

/**
 * A generic activity in which RFID tags ("IkTags") can be read while this
 * activity is in the foreground. While it is in the foreground, there is also a
 * wake lock in effect which prevents the phone from sleeping (and thus failing
 * to read tags)
 */
public abstract class IkTagReaderActivity extends Activity implements IkTagReader.Callback {

	private static final String TAG = "IkTagReaderActivity";

	private WakeLock mWakeLock;

	private IkTagReader mReader;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set up wake lock
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
		this.getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		mReader = new IkTagReader(this, this);
	}

	@Override
	public void onResume() {
		mReader.onResume();
		super.onResume();
		mWakeLock.acquire();
	}

	@Override
	public void onPause() {
		mWakeLock.release();
		mReader.onPause();
		super.onPause();
	}

	@Override
	public void onNewIntent(Intent intent) {
		mReader.onNewIntent(intent);
	}

	public String getReaderId() {
		return mReader.getReaderId();
	}

}
