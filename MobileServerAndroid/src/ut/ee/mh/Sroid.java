//   Copyright 2009 Google Inc.
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.

package ut.ee.mh;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.impl.DNSIncoming;
import javax.jmdns.impl.DNSRecord;
import javax.jmdns.impl.HostInfo;

import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.FelixConstants;
import org.osgi.framework.BundleActivator;

import ut.ee.mds.Activator;
import ut.ee.mds.ListenerActivator;
import dalvik.system.DexClassLoader;
import de.mn.felixembedand.FelixConfig;
import de.mn.felixembedand.HostActivator;
import de.mn.felixembedand.InstallFromRActivator;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

public class Sroid extends Activity {

	/* Advertasing */
	android.net.wifi.WifiManager.MulticastLock lock;
	android.os.Handler handler = new android.os.Handler();
	private String type = "_workstation._tcp.local.";
	private String http_type = "_http._tcp.local.";
	private JmDNS jmdns = null;
	private ServiceListener listener = null;
	private ServiceInfo serviceInfo;

	/* Felix */
	private HostActivator m_hostActivator = null;
	private InstallFromRActivator instFromR = null;
	public Felix m_felix = null;
	private Properties m_felixProperties;
	private File bundlesDir;
	private File newBundlesDir;
	private File cacheDir;
	static String message = "not read";
	/* End Felix */

	private static final String TAG = "FileSharer";

	/* startActivity request codes */
	private static final int PICK_FILE_REQUEST = 1;
	private static final int PICK_FOLDER_REQUEST = 2;

	/* Used to keep track of the currently selected file */
	private Uri mFileToShare;

	private static final int DIALOG_PASSWORD = 0;

	void setUpFelix() {
		m_felixProperties = new FelixConfig(this.getFilesDir()
				.getAbsolutePath()).getConfigProps();
		m_hostActivator = new HostActivator();
		String path = this.getFilesDir().getAbsolutePath();
		bundlesDir = new File(path + "/felix/bundle");
		if (!bundlesDir.exists()) {
			if (!bundlesDir.mkdirs()) {
				throw new IllegalStateException("Unable to create bundles dir");
			}
		}
		newBundlesDir = new File(path + "/felix/newbundle");
		if (!newBundlesDir.exists()) {
			if (!newBundlesDir.mkdirs()) {
				throw new IllegalStateException(
						"Unable to create newBundlesDir dir");
			}
		}
		cacheDir = new File(path + "/felix/cache");
		if (!cacheDir.exists()) {
			if (!cacheDir.mkdirs()) {
				throw new IllegalStateException(
						"Unable to create felixcache dir");
			}
		}
		instFromR = new InstallFromRActivator(this.getResources(), this
				.getFilesDir().getAbsolutePath());
		List<BundleActivator> activatorList = new ArrayList<BundleActivator>();
		activatorList.add(m_hostActivator);
		activatorList.add(instFromR);
		activatorList.add((BundleActivator) getActivator());
		activatorList.add((BundleActivator) getAccelerometerActivator());
		m_felixProperties.put(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP,
				activatorList);
		try {
			m_felix = new Felix(m_felixProperties);
			m_felix.start();
		} catch (Exception ex) {
			System.out.println("Could not create framework: " + ex);
			ex.printStackTrace();
		}
	}

	/* jmDNS */

	@Override
	protected void onStart() {
		super.onStart();
		// new Thread(){public void run() {setUp();}}.start();
	}

	@Override
	protected void onStop() {
		if (jmdns != null) {
			if (listener != null) {
				jmdns.removeServiceListener(type, listener);
				listener = null;
			}
			jmdns.unregisterAllServices();
			try {
				jmdns.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			jmdns = null;
		}
		// repo.stop();
		// s.stop();
		lock.release();
		super.onStop();
	}

	private void notifyUser(final String msg) {
		handler.postDelayed(new Runnable() {
			public void run() {

				TextView t = (TextView) findViewById(R.id.services);
				t.setText(msg + "\n===========SERVICE==========\n"
						+ t.getText());

			}
		}, 1);

	}

	private void setUp() {

		android.net.wifi.WifiManager wifi = (android.net.wifi.WifiManager) getSystemService(android.content.Context.WIFI_SERVICE);
		lock = wifi.createMulticastLock("mylockthereturn");
		lock.setReferenceCounted(true);
		lock.acquire();
		try {

			jmdns = JmDNS.create();

			jmdns.addServiceListener(http_type,
					listener = new ServiceListener() {

						@Override
						public void serviceResolved(ServiceEvent ev) {
							notifyUser("Service resolved: " + "Info: "
									+ ev.getInfo() + "Qualified Name: "
									+ ev.getInfo().getQualifiedName()
									+ " DNS: " + ev.getDNS() + " port:"
									+ ev.getInfo().getPort());

						}

						@Override
						public void serviceRemoved(ServiceEvent ev) {
							// notifyUser("Service removed: " + ev.getName());
						}

						@Override
						public void serviceAdded(ServiceEvent event) {
							// Required to force serviceResolved to be called
							// again
							// (after the first search)
							jmdns.requestServiceInfo(event.getType(),
									event.getName(), 1);
						}
					});

			/*
			 * jmdns.addServiceListener(type, listener = new ServiceListener() {
			 * 
			 * @Override public void serviceResolved(ServiceEvent ev) {
			 * notifyUser("Service resolved: " + "Info: " + ev.getInfo() +
			 * "Qualified Name: " + ev.getInfo().getQualifiedName() + " DNS: " +
			 * ev.getDNS() + " port:" + ev.getInfo().getPort());
			 * 
			 * }
			 * 
			 * @Override public void serviceRemoved(ServiceEvent ev) { //
			 * notifyUser("Service removed: " + ev.getName()); }
			 * 
			 * @Override public void serviceAdded(ServiceEvent event) { //
			 * Required to force serviceResolved to be called again // (afterthe
			 * first search) jmdns.requestServiceInfo(event.getType(),
			 * event.getName(), 1); } });
			 */
			/*
			 * serviceInfo = ServiceInfo.create("_test._tcp.local.",
			 * "AndroidTest", 0, "plain test service from android");
			 * jmdns.registerService(serviceInfo);
			 */
			serviceInfo = ServiceInfo.create(
					http_type,
					"Mobile cloud REST service SroidServer-"
							+ String.valueOf(this.hashCode()), 9999,
					"SroidServer");

			jmdns.registerService(serviceInfo);

		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

	public void setUpjmDNS() {
		handler.postDelayed(new Runnable() {
			public void run() {
				setUp();
			}
		}, 1000);
	}

	public Felix getFelix() {
		return m_felix;
	}

	private Object getAccelerometerActivator() {
		String path = this.getFilesDir().getAbsolutePath();
		File dexOutputDir = this.getDir("dex", 0);
		InputStream is = this.getResources().openRawResource(
				R.raw.mobilesroidservice);
		try {
			FileOutputStream out = openFileOutput("sroid.jar",
					Context.MODE_PRIVATE);

			byte buf[] = new byte[1024];
			int len;
			while ((len = is.read(buf)) > 0)
				out.write(buf, 0, len);
			out.close();
			is.close();
			String jarPath = getFilesDir() + "/sroid.jar";
			if (new File(jarPath).exists()) {

				DexClassLoader dex = new DexClassLoader(jarPath,
						dexOutputDir.getAbsolutePath(), null, getClassLoader());

				Class calledClass = dex.loadClass(ListenerActivator.class
						.getName());

				Object obj = calledClass.newInstance();
				return obj;
			}
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private Object getActivator() {
		String path = this.getFilesDir().getAbsolutePath();
		File dexOutputDir = this.getDir("dex", 0);
		InputStream is = this.getResources().openRawResource(
				R.raw.mobilesroidservice);
		Resources r = this.getResources();
		try {
			FileOutputStream out = openFileOutput("sroid.jar",
					Context.MODE_PRIVATE);

			byte buf[] = new byte[1024];
			int len;
			while ((len = is.read(buf)) > 0)
				out.write(buf, 0, len);
			out.close();
			is.close();
			String jarPath = getFilesDir() + "/sroid.jar";
			if (new File(jarPath).exists()) {

				DexClassLoader dex = new DexClassLoader(jarPath,
						dexOutputDir.getAbsolutePath(), null, getClassLoader());

				Class calledClass = dex.loadClass(Activator.class.getName());

				Object obj = calledClass.newInstance();
				return obj;
			}
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;

	}

	private final View.OnClickListener mAddFileListener = new View.OnClickListener() {
		public void onClick(View v) {
			Intent pickFileIntent = new Intent();
			pickFileIntent.setAction(Intent.ACTION_GET_CONTENT);
			pickFileIntent.addCategory(Intent.CATEGORY_OPENABLE);
			pickFileIntent.setType("*/*");
			Intent chooserIntent = Intent.createChooser(pickFileIntent,
					getText(R.string.choosefile_title));
			startActivityForResult(chooserIntent, PICK_FILE_REQUEST);
		}
	};

	private final View.OnClickListener mManageContentListener = new View.OnClickListener() {
		public void onClick(View v) {
			Intent manageIntent = new Intent();
			manageIntent.setAction(Intent.ACTION_MAIN);
			manageIntent.setType(FileSharingProvider.Folders.CONTENT_TYPE);
			startActivity(manageIntent);
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		// jm = new jmDNSConfigurator(this);
		// jm.setUpjmDNS();

		setUpjmDNS();
		setUpFelix();

		SroidServerService.setActivity(this);
		SroidServerService.setFelix(m_felix);

		final SharedPreferences sharedPreferences = getSharedPreferences(
				SroidServerService.PREFS_NAME, MODE_PRIVATE);
		ToggleButton serviceButton = (ToggleButton) findViewById(R.id.service);
		if (sharedPreferences.getBoolean(
				SroidServerService.PREFS_SERVICE_ON_STARTUP, true)) {

			Intent serviceIntent = new Intent();
			serviceIntent.setAction("ut.ee.mh.IFileSharingService");
			startService(serviceIntent);
			serviceButton.setChecked(true);
		} else {
			serviceButton.setChecked(false);
		}
		/* Setup toggling the service. */
		serviceButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean newValue) {
				Intent serviceIntent = new Intent();
				serviceIntent.setAction("ut.ee.mh.IFileSharingService");
				if (newValue == true) {
					startService(serviceIntent);
				} else {
					stopService(serviceIntent);
				}
				SharedPreferences.Editor editor = sharedPreferences.edit();
				editor.putBoolean(SroidServerService.PREFS_SERVICE_ON_STARTUP,
						newValue);
				editor.commit();
			}
		});

		/* Add the add file to shared folder */
		// Button addFileButton = (Button) findViewById(R.id.addfile);
		// addFileButton.setOnClickListener(mAddFileListener);

		/* Manage content button */
		// Button manageButton = (Button) findViewById(R.id.manage);
		// manageButton.setOnClickListener(mManageContentListener);

		/* Setup the status text */
		TextView ipTextView = (TextView) findViewById(R.id.url);
		ipTextView.setText("http://" + getIPAddress(this) + ":9999");

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK)
			return;

		switch (requestCode) {
		case PICK_FILE_REQUEST:
			/* Store this file somewhere */
			mFileToShare = data.getData();

			/* Now pick a folder */
			Intent pickFolder = new Intent();
			pickFolder.setAction(Intent.ACTION_PICK);
			pickFolder.setType(FileSharingProvider.Folders.CONTENT_ITEM_TYPE);
			startActivityForResult(pickFolder, PICK_FOLDER_REQUEST);
			break;
		case PICK_FOLDER_REQUEST:
			addFileToFolder(mFileToShare, data.getData());
			break;

		}
	}

	/**
	 * Adds a file to a shared folder. If the provided file is actually a
	 * folder, all files under that folder will be added to the shared folder.
	 * This includes files from sub-directories as well.
	 * 
	 * @param file
	 *            Uri for file.
	 * @param folder
	 *            Uri for shared folder.
	 */
	private void addFileToFolder(Uri file, Uri folder) {
		/*
		 * The URI could be a folder. If it is, assume we want all files under
		 * that folder.
		 */
		if (getContentResolver().getType(file)
				.equals(FileProvider.CONTENT_TYPE)) {
			Cursor c = managedQuery(file,
					new String[] { OpenableColumns.DISPLAY_NAME }, null, null,
					null);
			while (c.moveToNext()) {
				String filename = c.getString(c
						.getColumnIndex(OpenableColumns.DISPLAY_NAME));
				Uri uri = Uri.withAppendedPath(file, filename);
				addFileToFolder(uri, folder);
			}
		} else {
			try {
				FileSharingProvider.addFileToFolder(getContentResolver(), file,
						folder);
			} catch (SQLException exception) {
				Log.w(TAG, "Error adding file " + file + " to folder " + folder);
			}
		}
	}

	public static String getIPAddress(Context context) {
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		android.net.wifi.WifiInfo info = wifiManager.getConnectionInfo();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);

		byte[] intByte;
		try {
			dos.writeInt(info.getIpAddress());
			dos.flush();
			intByte = bos.toByteArray();
		} catch (IOException e) {
			Log.e(TAG, "Problem converting IP address");
			return "unknown";
		}

		// Reverse int bytes.. damn, this is a hack.
		byte[] addressBytes = new byte[intByte.length];
		for (int i = 0; i < intByte.length; i++) {
			addressBytes[i] = intByte[(intByte.length - 1) - i];
		}

		InetAddress address = null;
		try {
			address = InetAddress.getByAddress(addressBytes);
		} catch (UnknownHostException e) {
			Log.e(TAG, "Problem determing IP address");
			return "unknown";
		}
		return address.getHostAddress();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch (id) {
		case DIALOG_PASSWORD:
			dialog = new Dialog(Sroid.this);
			dialog.setContentView(R.layout.password_dialog);
			dialog.setTitle(R.string.set_password_title);
			final EditText passwordText = (EditText) dialog
					.findViewById(R.id.password);
			passwordText.setText(getSharedPreferences(
					FileSharingService.PREFS_NAME, MODE_PRIVATE).getString(
					FileSharingService.PREFS_PASSWORD, ""));
			Button okButton = (Button) dialog.findViewById(R.id.ok_button);
			final Dialog passwordDialog = dialog;
			okButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					SharedPreferences preferences = getSharedPreferences(
							FileSharingService.PREFS_NAME, MODE_PRIVATE);
					String newPassword = passwordText.getText().toString();
					if (!newPassword.equals(preferences.getString(
							FileSharingService.PREFS_PASSWORD, ""))) {
						SharedPreferences.Editor editor = preferences.edit();
						editor.putString(FileSharingService.PREFS_PASSWORD,
								passwordText.getText().toString());
						editor.commit();
						/* The password has changed, delete all cookies. */
						new CookiesDatabaseOpenHelper(Sroid.this)
								.getWritableDatabase().delete("cookies", null,
										null);
					}
					passwordDialog.dismiss();
				}
			});
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

}