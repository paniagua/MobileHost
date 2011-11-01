package ut.ee.sroid;

import java.io.IOException;

import org.apache.felix.framework.Felix;
import org.apache.http.HttpException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import ut.ee.mds.SroidService;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

public class SroidResolver {
	static Felix mFelix;
	static Activity activity;
	static String message = "";

	public SroidResolver(Felix felix, Activity main) {
		mFelix = felix;
		activity = main;
	}

	public String resolve(String serviceDescription, String method,
			final SroidRequest request, final SroidResponse response)
			throws InvalidSyntaxException {
		ServiceReference[] refs = mFelix.getBundleContext()
				.getServiceReferences(SroidService.class.getName(),
						"(Service="+serviceDescription+")");
		if (refs != null) {
			final SroidService sroidService = (SroidService) mFelix
					.getBundleContext().getService(refs[0]);
			Context context = activity.getApplicationContext();
			sroidService.doCreate(activity, context);
			Log.v("DEBUG-OWN", "before enter the while");
			// while (true)
			{
				message = sroidService.checkAvailability();
				try {
					sroidService.doGEt(request, response);
				} catch (HttpException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println(message);
				Log.v("DEBUG-OWN", message);
			}
			/*
			 * Thread s = new Thread(new Runnable() {
			 * 
			 * @Override public void run() { // TODO Auto-generated method stub
			 * 
			 * } }); s.start();
			 */
			// Log.v("DEBUG-OWN", message);
			// m_felix.getBundleContext().ungetService(refs[0]);
		}
		return "";
	}
}
