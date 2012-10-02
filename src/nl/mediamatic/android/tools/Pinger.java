package nl.mediamatic.android.tools;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.codec.net.URLCodec;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.AsyncTask;
import android.util.Log;

public class Pinger {
	private static final String TAG = "Pinger";
	private String mHost;
	private String mNote;
	private int mInterval;

	private static final String BASE_URL = "http://hwdeps.mediamatic.nl/ping.php";
	private static final int DEFAULT_INTERVAL = 30000;
	
	public Pinger(String host, String note) {
		this(host, note, DEFAULT_INTERVAL);
	}
	
	public Pinger(String readerId, String note, int interval) {
		mHost = readerId;
		mNote = note;
        mInterval = interval;
	}
	
	public void start() {
		new Timer().schedule(new TimerTask(){
			@Override
			public void run() {
				Pinger.this.ping();
			}}, 0, mInterval);
	}
	
	public void ping() {
		new PingerTask().execute();
	}
	
	private class PingerTask extends AsyncTask <Void, Void, Void>
	{
		@Override
		protected Void doInBackground(Void... params) {
			HttpClient httpclient = new DefaultHttpClient();
			URLCodec codec = new URLCodec();
			try {
				HttpGet req = new HttpGet(BASE_URL + "?host=" + codec.encode(mHost) + "&note=" + codec.encode(mNote));
				httpclient.execute(req);
			} catch (Throwable e) {
				Log.e(TAG, "Pinger error!", e);
			}
			return null;
		}
		
	}
}
