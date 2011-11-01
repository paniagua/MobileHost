package ut.ee.mh;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.apache.felix.framework.Felix;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import ut.ee.mds.SroidService;
import ut.ee.sroid.SroidHandler;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

public class SroidServer {
	static Felix mFelix;
	static Activity activity;
	private static final String TAG = "Sroid WebServer";
	private int mPort;
	private ServerSocketChannel mServerSocketChannel;
	private Context mContext;
	private SharedPreferences mSharedPreferences;
	private SQLiteDatabase mCookiesDatabase;
	SroidHandler sroidHandler;

	private TransferStartedListener mTransferStartedListener;

	public void setOnTransferStartedListener(TransferStartedListener listener) {
		mTransferStartedListener = listener;
	}

	/* How long we allow session cookies to last. */
	// private static final int COOKIE_EXPIRY_SECONDS = 3600;

	/* Start the webserver on specified port */
	public SroidServer(Context context, SharedPreferences sharedPreferences,
			SQLiteDatabase cookiesDatabase, int port, Felix felix, Activity main)
			throws IOException {
		mFelix = felix;
		activity = main;
		mPort = port;
		mServerSocketChannel = ServerSocketChannel.open();
		mServerSocketChannel.socket().setReuseAddress(true);
		mServerSocketChannel.socket().bind(new InetSocketAddress(mPort));
		mContext = context;
		mSharedPreferences = sharedPreferences;
		mCookiesDatabase = cookiesDatabase;
		// sroidHandler = new
		// SroidHandler(context,sharedPreferences,cookiesDatabase);
		deleteOldCookies();
	}

	/* Returns port we're using */
	public int getPort() {
		return mPort;
	}

	public void runWebServer() {
		while (true) {
			Log.i(TAG, "Running main webserver thread");
			try {
				SocketChannel channel = mServerSocketChannel.accept();
				final Socket socket = channel.socket();
				Log.d(TAG, "Socket accepted");
				Thread dispatcher = new Thread(new SroidHandler(mContext,
						mSharedPreferences, mCookiesDatabase, socket,
						mTransferStartedListener, mFelix, activity));
				dispatcher.start();
			} catch (ClosedByInterruptException e) {
				Log.i(TAG, "Received interrupt to shutdown.");
				return;
			} catch (IOException e) {
				Log.e(TAG, "Unexpected error, shutting down. " + e.toString());
				return;
			}
		}
	}

	private void deleteOldCookies() {
		mCookiesDatabase.delete("cookies", "expiry < ?", new String[] { ""
				+ (int) System.currentTimeMillis() / 1000 });
	}

}
