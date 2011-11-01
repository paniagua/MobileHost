package ut.ee.mh;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.FelixConstants;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import dalvik.system.DexClassLoader;
import de.mn.felixembedand.FelixConfig;
import de.mn.felixembedand.HostActivator;
import de.mn.felixembedand.InstallFromRActivator;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import ut.ee.mds.Activator;
import ut.ee.mds.ListenerActivator;
import ut.ee.mds.SroidService;

import ut.ee.mh.IFileSharingService;

public class SroidServerService extends Service {

	/* Felix */
	public static Felix mFelix = null;
	public static Activity activity = null;

	// static final String PREFS_NAME = "FileSharerServicePrefs";
	static final String PREFS_NAME = "SroidServerPrefs";
	public static final String PREFS_ALLOW_UPLOADS = "ALLOW_UPLOADS";
	public static String PREFS_REQUIRE_LOGIN = "REQUIRE_LOGIN";
	public static String PREFS_PASSWORD = "PASSWORD";
	public static final String PREFS_SERVICE_ON_STARTUP = "SERVICE_ON_STARTUP";
	public static final String PREF_FELIX_SERVICE = "FELIX_SERVICE";

	private static final int DEFAULT_PORT = 9999;
	private static final String TAG = "SroidServerService";
	private SroidServer sroid;

	private Thread sroidThread;;
	private int mPort;

	private final IFileSharingService.Stub mBinder = new IFileSharingService.Stub() {
		public int getPort() {
			return mPort;
		}
	};

	public static void setFelix(Felix felix) {
		mFelix = felix;
	}

	public static void setActivity(Activity main) {
		activity = main;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		SharedPreferences settings = getSharedPreferences(PREFS_NAME,
				MODE_PRIVATE);
		mPort = settings.getInt("port", DEFAULT_PORT);
		try {

			sroid = new SroidServer(this, settings,
					new CookiesDatabaseOpenHelper(this).getWritableDatabase(),
					mPort, mFelix, activity);
			sroid.setOnTransferStartedListener(new TransferStartedListener() {
				public void started(Uri uri) {
					NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
					Notification notification = new Notification(
							R.drawable.folder, null, System.currentTimeMillis());
					PendingIntent pendingIntent = PendingIntent.getActivity(
							SroidServerService.this, 0, new Intent(
									SroidServerService.this, Sroid.class),
							0);
					notification.setLatestEventInfo(SroidServerService.this,
							"Service Provider", "Transfer Started",
							pendingIntent);
					nm.notify(1, notification);
				}
			});
		} catch (IOException e) {
			Log.e(TAG, "Problem creating server socket " + e.toString());
		}

		sroidThread = new Thread() {
			@Override
			public void run() {
				sroid.runWebServer();
			}
		};
		sroidThread.start();
		Log.i(TAG, "Started webserver");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (sroid != null) {
			sroidThread.interrupt();
			try {
				sroidThread.join();
			} catch (InterruptedException e) {

			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		if (IFileSharingService.class.getName().equals(intent.getAction())) {
			return mBinder;
		}
		return null;
	}

}
