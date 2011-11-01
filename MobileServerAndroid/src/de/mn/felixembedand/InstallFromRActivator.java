package de.mn.felixembedand;

import java.io.InputStream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import ut.ee.mh.R;

import de.mn.felixembedand.view.ViewFactory;

import android.content.res.Resources;
import android.util.Log;

public class InstallFromRActivator implements BundleActivator {

	private String fileRootPath = "";
	private Resources res;

	public InstallFromRActivator(Resources res, String fileRootPath) {
		this.res = res;
		this.fileRootPath = fileRootPath;
	}

	public void start(BundleContext arg0) throws Exception {

		// get from R and install to apps private files dir

		InputStream is = res.openRawResource(R.raw.bundlerepository);
		Bundle bundlebundlerepository = arg0.installBundle(fileRootPath
				+ "/felix/bundle/bundlerepository.jar", is);

		is = res.openRawResource(R.raw.shell);
		Bundle bundleshell = arg0.installBundle(fileRootPath
				+ "/felix/bundle/shell.jar", is);

		is = res.openRawResource(R.raw.ipojo);
		Bundle bundleipojo = arg0.installBundle(fileRootPath
				+ "/felix/bundle/ipojo.jar", is);

		is = res.openRawResource(R.raw.ipojoannotations);
		Bundle bundleipojoannotations = arg0.installBundle(fileRootPath
				+ "/felix/bundle/ipojoannotations.jar", is);

		is = res.openRawResource(R.raw.ipojoarch);
		Bundle bundleipojoarch = arg0.installBundle(fileRootPath
				+ "/felix/bundle/ipojoarch.jar", is);

		is = res.openRawResource(R.raw.fileinstall130);
		Bundle bundlefileinstall130 = arg0.installBundle(fileRootPath
				+ "/felix/bundle/fileinstall130.jar", is);

		// spell
		is = res.openRawResource(R.raw.spell);
		Bundle bundleChecker = arg0.installBundle(fileRootPath
				+ "/felix/bundle/spell.checker.jar", is);

		
		// service
		//is = res.openRawResource(R.raw.mobilesroidservice);
		//Bundle bundleMobileSroidService = arg0.installBundle(fileRootPath
			//	+ "/felix/bundle/mobilesroidservice.jar", is);

		
		// client
		//is = res.openRawResource(R.raw.mobilesroidinterface);
		//Bundle bundleMobileSroidClient = arg0.installBundle(fileRootPath
		//		+ "/felix/bundle/mobilesroidinterface.jar", is);

		
		bundleshell.start();
		bundlebundlerepository.start();
		bundleipojo.start();
		bundleipojoannotations.start();
		bundleipojoarch.start();
		bundlefileinstall130.start();
		//bundleMobileSroidService.start();
		
		
		
		ServiceReference[] s = bundleChecker.getRegisteredServices();
		
		Log.v("DEBUG-OWN", "Everything has been installed");

	}

	public void stop(BundleContext arg0) throws Exception {
		// TODO Auto-generated method stub

	}

}
