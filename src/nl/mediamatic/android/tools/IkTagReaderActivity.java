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
        mReader = new IkTagReader(this, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mReader.onResume();
        if (mWakeLock != null) {
            mWakeLock.acquire();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mReader.onPause();
        if (mWakeLock != null) {
            mWakeLock.release();
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        mReader.onNewIntent(intent);
    }

    public void enableWakeLock() {
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            mWakeLock.acquire();
        }
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void disableWakeLock() {
        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }
        this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }


    public String getReaderId() {
        return mReader.getReaderId();
    }

}
