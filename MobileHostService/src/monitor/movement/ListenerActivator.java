package monitor.movement;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Properties;


import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.RequestLine;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.message.BasicHttpResponse;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import ut.ee.mds.SroidService;

public class ListenerActivator implements BundleActivator {
	private BundleContext m_context = null;
	private ServiceRegistration m_registration = null;

	/**
	 * Implements BundleActivator.start(). Registers an instance of a dictionary
	 * service using the bundle context; attaches properties to the service that
	 * can be queried when performing a service look-up.
	 * 
	 * @param context
	 *            the framework context for the bundle.
	 **/
	public void start(BundleContext context) {
		this.m_context = context;
		Properties props = new Properties();
		props.put("Service", "AndroidAccelerometer");
		m_registration = context.registerService(SroidService.class.getName(),
				new Accelerometer(), props);
	}

	/**
	 * Implements BundleActivator.stop(). Does nothing since the framework will
	 * automatically unregister any registered services.
	 * 
	 * @param context
	 *            the framework context for the bundle.
	 **/
	public void stop(BundleContext context) {
		// NOTE: The service is automatically unregistered.
		// m_registration.unregister();
		m_context = null;
	}

	public BundleContext getContext() {
		return m_context;
	}

	public Bundle[] getBundles() {
		if (m_context != null) {
			return m_context.getBundles();
		}
		return null;
	}

	/**
	 * A private inner class that implements a dictionary service; see
	 * DictionaryService for details of the service.
	 **/
}
