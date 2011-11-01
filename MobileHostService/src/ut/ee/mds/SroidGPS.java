package ut.ee.mds;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.RequestLine;
import org.apache.http.impl.DefaultHttpServerConnection;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import ut.ee.sroid.SroidRequest;
import ut.ee.sroid.SroidResponse;

public class SroidGPS implements SroidService {

	private static final long MINIMUM_DISTANCE_CHANGE_FOR_UPDATES = 1; // in
																		// Meters
	private static final long MINIMUM_TIME_BETWEEN_UPDATES = 1000; // in
																	// Milliseconds

	protected LocationManager locationManager;
	protected Activity activity;
	protected Context context;

	public String getHTMLHeader() {
		return "<html><head><title>File Share</title></head><body>";
	}

	public String getHTMLFooter() {
		return "</body></html>";
	}

	@Override
	public void doCreate(Activity activity, Context context) {
		this.activity = activity;
		this.context = context;
		locationManager = (LocationManager) activity
				.getSystemService(Context.LOCATION_SERVICE);

		/*
		 * locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
		 * MINIMUM_TIME_BETWEEN_UPDATES, MINIMUM_DISTANCE_CHANGE_FOR_UPDATES,
		 * new SroidLocationListener());
		 */
	}

	private String getLocation() {
		Location location = locationManager
				.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (location == null)
			location = locationManager
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if (location != null) {
			String message = String.format(
					"\"Longitude\": \"%1$s\", \"Latitude\": \"%2$s\", \"timestamp\": \"%3$s\"",
					location.getLongitude(), location.getLatitude(),System.currentTimeMillis());
			return message;
		}

		return "Location services not available";
	}

	@Override
	public void doGEt(SroidRequest request, SroidResponse response) {
		// TODO Auto-generated method stub
		/*String message = getHTMLHeader();
		message = message + "<p> " + getLocation() + "</p>";
		message = message + "<br><br>";
		message = message + "<p> Works! </p>" + getHTMLFooter();
		response.write(message);
		try {
			response.close();
		} catch (HttpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		String message = "{"+getLocation()+"}";
		response.write(message);
		
		try {
			response.close();
		} catch (HttpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void doPost(SroidRequest request, SroidResponse response)
			throws HttpException, IOException {
		// TODO Auto-generated method stub
		String message = getHTMLHeader();
		message = message + "<p> Not implemented yet </p>" + getHTMLFooter();
		response.write(message);
		response.close();
	}

	@Override
	public void doDelete(SroidRequest request, SroidResponse response) {
		// TODO Auto-generated method stub
		String message = getHTMLHeader();
		message = message + "<p> Not implemented yet </p>" + getHTMLFooter();
		response.write(message);
	}

	@Override
	public void doPut(SroidRequest request, SroidResponse response) {
		// TODO Auto-generated method stub
		String message = getHTMLHeader();
		message = message + "<p> Not implemented yet </p>" + getHTMLFooter();
		response.write(message);

	}

	@Override
	public boolean processRequest(DefaultHttpServerConnection serverConnection,
			HttpRequest request, RequestLine requestLine) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String checkAvailability() {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		String message = getHTMLHeader();
		message = message + "<p> " + getLocation() + "</p>";
		message = message + "<br><br>";
		message = message + "<p> Works! </p>" + getHTMLFooter();
		return message;
	}

	private class SroidLocationListener implements LocationListener {
		@Override
		public void onLocationChanged(Location location) {
			// TODO Auto-generated method stub
			String message = String.format(
					"New Location \n Longitude: %1$s \n Latitude: %2$s",
					location.getLongitude(), location.getLatitude());

		}

		@Override
		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onProviderDisabled(String arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onProviderEnabled(String arg0) {
			// TODO Auto-generated method stub

		}

	}

	@Override
	public void onAccelerationChanged(float x, float y, float z) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onShake(float force) {
		// TODO Auto-generated method stub

	}
}
