package monitor.movement;

import java.io.IOException;
import java.util.Random;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.RequestLine;
import org.apache.http.impl.DefaultHttpServerConnection;

import ut.ee.mds.SroidService;
import ut.ee.sroid.SroidRequest;
import ut.ee.sroid.SroidResponse;

import android.app.Activity;

import android.content.Context;
 
public class Accelerometer implements SroidService {

	private static String x, y, z;

	private static Context CONTEXT;
	private static Activity activity;

	protected void onResume() {

		if (AccelerometerManager.isSupported()) {
			AccelerometerManager.startListening(this);
		}
	}

	public static Context getContext() {
		return CONTEXT;
	}

	/**
	 * onShake callback
	 */
	public void onShake(float force) {
		// Toast.makeText(this, "Phone shaked : " + force, 1000).show();
	}

	public void onAccelerationChanged(float x, float y, float z) {
		// ((TextView) findViewById(R.id.x)).setText(String.valueOf(x));
		// ((TextView) findViewById(R.id.y)).setText(String.valueOf(y));
		// ((TextView) findViewById(R.id.z)).setText(String.valueOf(z));
		Accelerometer.x = String.valueOf(Math.abs(x));
		Accelerometer.y = String.valueOf(Math.abs(y));
		Accelerometer.z = String.valueOf(Math.abs(z));
		if (Math.abs(x) < 1 && Math.abs(y) < 1 && Math.abs(z) < 1) { // take in
																		// consideration
																		// values
																		// like
																		// 0.9999
																		// as 0
			// Here trigger the procedure when the phone is falling
			// ((TextView) findViewById(R.id.status)).setText("Falling...");
			Accelerometer.x = String.valueOf(Math.abs(x));
			Accelerometer.y = String.valueOf(Math.abs(y));
			Accelerometer.z = String.valueOf(Math.abs(z));
		}
	}

	public void onInit(int status) {

	}

	private static final Random RANDOM = new Random();

	@Override
	public void doCreate(Activity activity, Context context) {
		// TODO Auto-generated method stub
		Accelerometer.activity = activity;
		Accelerometer.CONTEXT = context;
		Accelerometer.x = "not read";
		Accelerometer.y = "not read";
		Accelerometer.z = "not read";
		if (AccelerometerManager.isSupported()) {
			AccelerometerManager.startListening(this);
		}
	}

	@Override
	public void doGEt(SroidRequest request, SroidResponse response)
			throws HttpException, IOException {
		// TODO Auto-generated method stub
		if (!AccelerometerManager.isSupported()) {
		}
		if (!AccelerometerManager.isListening()) {
		}
		if (AccelerometerManager.isListening()) {
			String returnMsg = "{\"x\": \"" + Accelerometer.x + "\", \"y\": \""
					+ Accelerometer.y + "\", \"z\": \"" + Accelerometer.z
					+ "\"}";
			response.write(returnMsg);
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

	}

	@Override
	public void doPost(SroidRequest request, SroidResponse response)
			throws HttpException, IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void doDelete(SroidRequest request, SroidResponse response)
			throws HttpException, IOException {
		// TODO Auto-generated method stub
		String message = getHTMLHeader();
		message = message + "<p> Not implemented yet </p>" + getHTMLFooter();
		response.write(message);
	}

	@Override
	public void doPut(SroidRequest request, SroidResponse response)
			throws HttpException, IOException {
		// TODO Auto-generated method stub
		String message = getHTMLHeader();
		message = message + "<p> Not implemented yet </p>" + getHTMLFooter();
		response.write(message);
	}

	public String getHTMLHeader() {
		return "<html><head><title>File Share</title></head><body>";
	}

	public String getHTMLFooter() {
		return "</body></html>";
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

		if (!AccelerometerManager.isSupported())
			return "not supported";
		if (!AccelerometerManager.isListening())
			return "not listening";
		if (AccelerometerManager.isListening())
			return "Is supported and listening,  current numbers are x: "
					+ Accelerometer.x + " y: " + Accelerometer.y + " z: "
					+ Accelerometer.z;
		return "Error raised";

	}

}
