package de.mn.felixembedand;

import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
//import org.osgi.framework.ServiceRegistration;

// ZWECK: Verbindung HostApp zu Felix Framework

/**
 * felix.systembundle.activators, which is a list of bundle activator instances.
 * These bundle activator instances provide a convenient way for host
 * applications to interact with the Felix framework.
 * 
 * The ability offered by these activators can also be accomplished by invoking
 * init() on the framework instance and the using getBundleContext() to get the
 * System Bundle's context, but it can be more convenient to use an activator
 * instance.
 * 
 * Each activator instance passed into the constructor (von Felix) effectively
 * becomes part of the System Bundle. This means that the start()/stop() methods
 * of each activator instance in the list gets invoked when the System Bundle's
 * activator start()/stop() methods gets invoked, respectively. Each activator
 * instance will be given the System Bundle's BundleContext object so that they
 * can interact with the framework. Der Hostactivator erh�lt den Bundlecontecct.
 * Die Hostapplikation kennt diesen HostAtoivator und kann �ber ihn leicht auf
 * den Bundle Context zugreifen
 * 
 * 
 * => Bundle soll Service von HostApp nutzen ollowing host application bundle
 * activator, which will be used to register/unregister the property lookup
 * service when the embedded framework instance starts/stops:
 * 
 * @author matthiasneubert
 * 
 */
public class HostActivator implements BundleActivator

{
	// private Map<String,String> m_lookupMap = null;
	private BundleContext m_context = null;

	// private ServiceRegistration m_registration = null;

	public HostActivator(Map<String, String> lookupMap) {
		// Save a reference to the service's backing store.
		// m_lookupMap = lookupMap;
	}

	public HostActivator() {

	}

	public void start(BundleContext context) {
		// Save a reference to the bundle context.
		m_context = context;

		/*
		 * // Create a property lookup service implementation. (Implementiert
		 * das zu exportierende Service Interface) Lookup lookup = new Lookup()
		 * { public Object lookup(String name) { return m_lookupMap.get(name); }
		 * };
		 */

		// Register the property lookup service and save
		// the service registration.
		/*
		 * Rather than having the host application bundle activator register the
		 * service, it is also possible for the the host application to simply
		 * get the bundle context from the bundle activator and register the
		 * service directly, but the presented approach is perhaps a little
		 * cleaner since it allows the host application to register/unregister
		 * the service when the system bundle starts/stops.
		 */
		// => Vorteil: das registrieren/anbieten des Services das die HostApp
		// zur verf�gung stellt, wird gleich beim
		// start des system bundles gemacht, weil der HostActivator da ja auch
		// mit einem rutsch mit und als system bundle gestartet wird
		/*
		 * m_registration = m_context.registerService( Lookup.class.getName(),
		 * lookup, null);
		 */
	}

	public void stop(BundleContext context) {
		// Unregister the property lookup service.
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

}
