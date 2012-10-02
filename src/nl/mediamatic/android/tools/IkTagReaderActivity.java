package nl.mediamatic.android.tools;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NfcAdapter;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * A generic activity in which RFID tags ("IkTags") can be read while this
 * activity is in the foreground. While it is in the foreground, there is also a
 * wake lock in effect which prevents the phone from sleeping (and thus failing
 * to read tags)
 */
public abstract class IkTagReaderActivity extends Activity {

	private static final String TAG = "IkTagReaderActivity";

	private NfcAdapter mAdapter;
	private PendingIntent mPendingIntent;
	private IntentFilter[] mFilters;
	private String[][] mTechLists;
	private WakeLock mWakeLock;
	private String mReaderId;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Get device id
		TelephonyManager tManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		mReaderId = tManager.getDeviceId();
		if (mReaderId == null) {
			// not a phone; use ANDROID_ID.
			// note that ANDROID_ID is reset between factory-resets of the
			// device.
			mReaderId = Settings.Secure.getString(getContentResolver(),
					Settings.Secure.ANDROID_ID);
		}

		// Set up wake lock
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
		this.getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		initializeNFC();
	}

	/**
	 * Initialize the NFC adapter, setting up a pending intent.
	 */
	private void initializeNFC() {
		mAdapter = NfcAdapter.getDefaultAdapter(this);

		if (mAdapter == null) {
			Toast.makeText(this, "No NFC adapter found.", Toast.LENGTH_LONG)
					.show();
			this.finish();
			return;
		}

		mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

		// Setup an intent filter for all MIME based dispatches
		IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
		try {
			ndef.addDataType("*/*");
		} catch (MalformedMimeTypeException e) {
			throw new RuntimeException("fail", e);
		}
		mFilters = new IntentFilter[] { ndef };
		mTechLists = new String[][] { new String[] { NfcA.class.getName() } };
	}

	@Override
	public void onResume() {
		if (mAdapter != null)
			mAdapter.enableForegroundDispatch(this, mPendingIntent, mFilters,
					mTechLists);
		super.onResume();
		mWakeLock.acquire();
	}

	@Override
	public void onPause() {
		mWakeLock.release();
		if (mAdapter != null)
			mAdapter.disableForegroundDispatch(this);
		super.onPause();
	}

	@Override
	public void onNewIntent(Intent intent) {
		resolveIntent(intent);
	}

	private void resolveIntent(Intent intent) {
		byte[] byte_id = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
		if (byte_id == null) {
			Log.v(TAG, "This intent was not for us");
			return;
		}

		String hexStr = "";
		for (int i = 0; i < byte_id.length; i++) {
			hexStr += Integer.toString((byte_id[i] & 0xff) + 0x100, 16)
					.substring(1);
		}

		String tag = "urn:rfid:" + hexStr.toUpperCase();
		Log.d(TAG, "Tag: " + tag);
		onTagAdded(tag);
	}

	public String getReaderId() {
		return mReaderId;
	}

	public abstract void onTagAdded(String tag);

}
