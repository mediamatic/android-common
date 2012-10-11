package nl.mediamatic.android.tools;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NfcAdapter;
import android.nfc.tech.NfcA;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class IkTagReader {

	private static final String TAG = "IkTagReader";
	private Activity mActivity;
	private Callback mCallback;
	private String mReaderId;
	private NfcAdapter mAdapter;
	private PendingIntent mPendingIntent;
	private IntentFilter[] mFilters;
	private String[][] mTechLists;
	
	public IkTagReader(Activity activity, Callback callback)
	{
		mActivity = activity;
		mCallback = callback;

		retrieveReaderId();
		initializeNFC();
	}
	
	private void retrieveReaderId()
	{
		// Get device id
		TelephonyManager tManager = (TelephonyManager) mActivity.getSystemService(Context.TELEPHONY_SERVICE);
		mReaderId = tManager.getDeviceId();
		if (mReaderId == null) {
			// not a phone; use ANDROID_ID.
			// note that ANDROID_ID is reset between factory-resets of the
			// device.
			mReaderId = Settings.Secure.getString(mActivity.getContentResolver(),
					Settings.Secure.ANDROID_ID);
		}
	}


	/**
	 * Initialize the NFC adapter, setting up a pending intent.
	 */
	private void initializeNFC() {
		mAdapter = NfcAdapter.getDefaultAdapter(mActivity);

		if (mAdapter == null) {
			Toast.makeText(mActivity, "Sorry, no NFC adapter found.", Toast.LENGTH_LONG)
					.show();
			mActivity.finish();
			return;
		}

		mPendingIntent = PendingIntent.getActivity(mActivity, 0, new Intent(mActivity,
				mActivity.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

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

	public String getReaderId() {
		return mReaderId;
	}
	
	public void onNewIntent(Intent intent) {
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
		
		mCallback.onTagAdded(tag);
	}

	public void onPause() {
		if (mAdapter != null)
			mAdapter.disableForegroundDispatch(mActivity);
	}
	
	public void onResume() {
		if (mAdapter != null)
			mAdapter.enableForegroundDispatch(mActivity, mPendingIntent, mFilters,
					mTechLists);
	}
	
	public interface Callback {
		public abstract void onTagAdded(String tag);
	}
}
