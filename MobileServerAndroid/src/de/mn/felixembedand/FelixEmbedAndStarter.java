package de.mn.felixembedand;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.FelixConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import ut.ee.mds.Activator;
import ut.ee.mds.ListenerActivator;
import ut.ee.mds.SroidService;
import ut.ee.mh.R;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import dalvik.system.DexClassLoader;
import de.mn.felixembedand.view.ViewFactory;

public class FelixEmbedAndStarter extends Activity {

	private HostActivator m_hostActivator = null;
	private InstallFromRActivator instFromR = null;
	public Felix m_felix = null;
	private Properties m_felixProperties;
	private File bundlesDir;
	private File newBundlesDir;
	private File cacheDir;
	static String message = "not read";
	// �berwacht, ob neue Services (deren Service Interfaces dieser HostApp
	// bekannt sind)
	// durch Bundles implementiert werden, welche wiederum in der eingebetteen
	// Felix Instanz isntalliert sind und laufen
	private ServiceTracker m_servicetracker = null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		setContentView(R.layout.main);

		// load Properties (from class, not from config.properties here)
		m_felixProperties = new FelixConfig(this.getFilesDir()
				.getAbsolutePath()).getConfigProps();

		// hostactivator for connection hostapp to framework
		m_hostActivator = new HostActivator();

		String path = this.getFilesDir().getAbsolutePath();

		// create empty bundle dir (InstallFromRActivator will use it)
		bundlesDir = new File(path + "/felix/bundle");
		if (!bundlesDir.exists()) {
			if (!bundlesDir.mkdirs()) {
				throw new IllegalStateException("Unable to create bundles dir");
			}
		}

		// the fileinstall watched dir fr new bundles
		newBundlesDir = new File(path + "/felix/newbundle");
		if (!newBundlesDir.exists()) {
			if (!newBundlesDir.mkdirs()) {
				throw new IllegalStateException(
						"Unable to create newBundlesDir dir");
			}
		}

		// create felix cache dir
		cacheDir = new File(path + "/felix/cache");

		// if it still exists, because delte on destroy has failed, we delete it
		// here
		// if (cacheDir.exists()) delete(cacheDir);

		if (!cacheDir.exists()) {
			if (!cacheDir.mkdirs()) {
				throw new IllegalStateException(
						"Unable to create felixcache dir");
			}
		}

		// activator which loads from Res and isntalls to files dir and starts
		// bundles
		instFromR = new InstallFromRActivator(this.getResources(), this
				.getFilesDir().getAbsolutePath());

		List<BundleActivator> activatorList = new ArrayList<BundleActivator>();
		activatorList.add(m_hostActivator);
		activatorList.add(instFromR);
		// activatorList.add(new Activator());
		activatorList.add((BundleActivator) getActivator());
		activatorList.add((BundleActivator) getAccelerometerActivator());
		// add list of activators which shall be started with system bundle to
		// config

		m_felixProperties.put(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP,
				activatorList);

		// start felix with configProps
		try {
			// Now create an instance of the framework with our configuration
			// properties.
			m_felix = new Felix(m_felixProperties);
			// Now start Felix instance.
			m_felix.start();
		} catch (Exception ex) {
			System.out.println("Could not create framework: " + ex);
			ex.printStackTrace();
		}

		initServiceTracker();
		try {
			ServiceReference[] refs = m_felix.getBundleContext()
					.getServiceReferences(SroidService.class.getName(),
							"(Service=Android Mobile Accelerometer)");
			if (refs != null) {
				final SroidService sroidService = (SroidService) m_felix
						.getBundleContext().getService(refs[0]);
				sroidService.doCreate(this, this.getApplicationContext());

				Thread s = new Thread(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						Log.v("DEBUG-OWN", "before enter the while");
						while (true) {
							message = sroidService.checkAvailability();
							System.out.println(message);
							Log.v("DEBUG-OWN", message);
						}
					}
				});
				//s.start();
				// Log.v("DEBUG-OWN", message);
				// m_felix.getBundleContext().ungetService(refs[0]);
			} else {
				System.out.println("Couldn't find any dictionary service...");
				Log.v("DEBUG-OWN", "Couldn't find any dictionary service...");
			}

			refs = m_felix.getBundleContext().getServiceReferences(
					SroidService.class.getName(),
					"(Service=Android Mobile Host Service)");
			if (refs != null) {
				final SroidService sroidService = (SroidService) m_felix
						.getBundleContext().getService(refs[0]);
				sroidService.doCreate(this, this.getApplicationContext());
				Thread s = new Thread(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						while (true) {
							String result = sroidService.checkAvailability();
							System.out.println(result);
							Log.v("DEBUG-OWN", result);
						}
					}
				});
				s.start();
				// m_felix.getBundleContext().ungetService(refs[0]);
			} else {
				System.out.println("Couldn't find any dictionary service...");
				Log.v("DEBUG-OWN", "Couldn't find any dictionary service...");
			}

		} catch (InvalidSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// TEST
		testInstalledBundleState();
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

	private void initServiceTracker() {

		try {

			m_servicetracker = new ServiceTracker(m_felix.getBundleContext(),
					m_felix.getBundleContext().createFilter(
							"(" + Constants.OBJECTCLASS + "="
									+ ViewFactory.class.getName() + ")"),
					new ServiceTrackerCustomizer() {

						@Override
						public Object addingService(ServiceReference ref) {
							System.out
									.println("=============== Service found ! =============");
							final ViewFactory fac = (ViewFactory) m_felix
									.getBundleContext().getService(ref);
							if (fac != null) {
								runOnUiThread(new Runnable() {
									public void run() {
										setContentView(fac
												.create(FelixEmbedAndStarter.this));
									}
								});
							}
							return fac;
						}

						@Override
						public void modifiedService(ServiceReference ref,
								Object service) {
							// TODO Auto-generated method stub
							removedService(ref, service);
							addingService(ref);
						}

						@Override
						public void removedService(ServiceReference ref,
								Object service) {
							m_felix.getBundleContext().ungetService(ref);
							// TODO Auto-generated method stub
							runOnUiThread(new Runnable() {
								public void run() {
									setContentView(new View(
											FelixEmbedAndStarter.this));
								}
							});
						}
					});

			m_servicetracker.open();

		} catch (InvalidSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public synchronized void onStop() {
		System.out.println("============= ON STOP ==========");
		super.onStop();
		/*
		 * m_tracker.close(); m_tracker = null;
		 */

		shutdownApplication();

		m_felix = null;
	}

	@Override
	public synchronized void onDestroy() {
		super.onDestroy();
		System.out.println("============= ON DESTROY ==========");

		// without this we get errors about illegal bundle states!
		// delete(cacheDir);
		/*
		 * 
		 * m_configMap = null; m_cache = null;
		 */
	}

	public Bundle[] getInstalledBundles() {
		// Use the system bundle activator to gain external
		// access to the set of installed bundles.
		return m_hostActivator.getBundles();
	}

	public void shutdownApplication() {
		// Shut down the felix framework when stopping the
		// host application.
		try {
			m_felix.stop();
		} catch (BundleException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Cannot stop HostApplication");
		}

		// timeout time? in msec?
		long waittime = 10000;

		try {
			m_felix.waitForStop(waittime);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			System.out.println("Thread has waited and was then interrupted");
			e.printStackTrace();
		}
	}

	private void testInstalledBundleState() {

		Bundle[] bundles = getInstalledBundles();
		String teststr = "TEST: ";
		for (Bundle b : bundles) {

			String stateStr = "";
			int state = b.getState();
			switch (state) {

			case Bundle.ACTIVE:
				stateStr = "ACTIVE";
				break;
			case Bundle.INSTALLED:
				stateStr = "INSTALLED";
				break;
			case Bundle.RESOLVED:
				stateStr = "RESOLVED";
				break;
			case Bundle.STARTING:
				stateStr = "STARTING";
				break;
			case Bundle.STOPPING:
				stateStr = "STOPPING";
				break;

			}
			if (stateStr.length() == 0)
				stateStr = "UNKNOWN STATE";

			teststr = teststr + "\n " + b.getSymbolicName() + "\nSTATE: "
					+ stateStr + " " + " ID " + b.getBundleId();
			System.out.println("TESTBUNDLE: " + b.getSymbolicName());
		}

		TextView tv = new TextView(this);

		tv.setText("Hello: " + teststr);

		setContentView(tv);

	}

	/**
	 * recursive file deleting
	 * 
	 * @param target
	 */
	private void delete(File target) {

		if (target.isDirectory()) {
			for (File file : target.listFiles()) {
				delete(file);
			}
		}
		target.delete();

	}

}