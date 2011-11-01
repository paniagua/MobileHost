package ut.ee.sroid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.channels.ServerSocketChannel;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.fileupload.MultipartStream;
import org.apache.felix.framework.Felix;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.RequestLine;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.params.BasicHttpParams;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import ut.ee.mds.SroidService;
import ut.ee.mh.FileProvider;
import ut.ee.mh.FileSharingProvider;
import ut.ee.mh.SharedFileBrowser;
import ut.ee.mh.SroidServerService;
import ut.ee.mh.StreamingZipEntity;
import ut.ee.mh.TransferStartedListener;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

public class SroidHandler implements Runnable {
	static Felix mFelix;
	static Activity activity;
	private SQLiteDatabase mCookiesDatabase;
	private SharedPreferences mSharedPreferences;
	private static final int COOKIE_EXPIRY_SECONDS = 3600;
	private static final String TAG = "Sroid";
	private Context mContext;
	private Socket mSocket;
	private TransferStartedListener mTransferStartedListener;
	private String LIST_FILES_SERVICE = "ListFiles";

	public SroidHandler(Context context, SharedPreferences sharedPreferences,
			SQLiteDatabase cookiesDatabase, Socket socket,
			TransferStartedListener transferStartedListener, Felix felix,
			Activity main) {
		mFelix = felix;
		activity = main;
		mContext = context;
		mSharedPreferences = sharedPreferences;
		mCookiesDatabase = cookiesDatabase;
		mSocket = socket;
		mTransferStartedListener = transferStartedListener;
	}

	String getServiceName(RequestLine requestLine) {
		if (requestLine.getUri().contains("/service")) {
			return requestLine.getUri().substring(
					requestLine.getUri().lastIndexOf("/"),
					requestLine.getUri().length());
		}
		return LIST_FILES_SERVICE;
	}

	private void handleRequest() {
		try {
			DefaultHttpServerConnection serverConnection = new DefaultHttpServerConnection();
			serverConnection.bind(mSocket, new BasicHttpParams());
			HttpRequest request = serverConnection.receiveRequestHeader();
			RequestLine requestLine = request.getRequestLine();
			BasicHttpEntityEnclosingRequest enclosingRequest = new BasicHttpEntityEnclosingRequest(
					request.getRequestLine());
			serverConnection.receiveRequestEntity(enclosingRequest);

			if (requestLine.getUri().startsWith("/location")) {
				String serviceName = getServiceName(requestLine);
				String method = requestLine.getMethod();
				// SroidRequest_ srequest= new SroidRequest_(serverConnection,
				// enclosingRequest, requestLine);
				// SroidResponse_ sresponse = new
				// SroidResponse_(serverConnection, enclosingRequest,
				// requestLine);
				SroidRequest srequesta = new SroidRequest(serverConnection,
						enclosingRequest, requestLine);
				SroidResponse sresponsea = new SroidResponse(serverConnection,
						enclosingRequest, requestLine);
				SroidResolver resolver = new SroidResolver(mFelix, activity);
				try {
					resolver.resolve("AndroidGPS", requestLine.getMethod(),
							srequesta, sresponsea);

				} catch (InvalidSyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else if (requestLine.getUri().startsWith("/accelerometer")) {
				String serviceName = getServiceName(requestLine);
				String method = requestLine.getMethod();
				// SroidRequest_ srequest= new SroidRequest_(serverConnection,
				// enclosingRequest, requestLine);
				// SroidResponse_ sresponse = new
				// SroidResponse_(serverConnection, enclosingRequest,
				// requestLine);
				SroidRequest srequesta = new SroidRequest(serverConnection,
						enclosingRequest, requestLine);
				SroidResponse sresponsea = new SroidResponse(serverConnection,
						enclosingRequest, requestLine);
				SroidResolver resolver = new SroidResolver(mFelix, activity);
				try {
					resolver.resolve("AndroidAccelerometer", requestLine.getMethod(),
							srequesta, sresponsea);

				} catch (InvalidSyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			else if (requestLine.getUri().equals("/")) {
				Log.i(TAG, "Sending shared folder listing");
				sendSharedFolderListing(serverConnection);
			} else if (requestLine.getMethod().equals("GET")
					&& requestLine.getUri().startsWith("/folder")) {
				Log.i(TAG, "Sending list of shared files");
				sendSharedFilesList(serverConnection, requestLine);
			} else if (requestLine.getUri().startsWith("/zip")) {
				Log.i(TAG, "Sending zip file.");
				sendFolderContent(serverConnection, requestLine);
			} else if (requestLine.getUri().startsWith("/file")) {
				Log.i(TAG, "Sending file content");
				sendFileContent(serverConnection, requestLine);
			} else if (requestLine.getMethod().equals("POST")) {
				Log.i(TAG, "User is uploading file");
				handleUploadRequest(serverConnection, request, requestLine);
			} else if (requestLine.getUri().startsWith("/playlist")) {
				Log.i(TAG, "User is requesting playlist");
				sendPlaylist(serverConnection, requestLine);
			} else {
				Log.i(TAG, "No action for " + requestLine.getUri());
				sendNotFound(serverConnection);
			}
			serverConnection.flush();
			serverConnection.close();
		} catch (IOException e) {
			Log.e(TAG, "Problem with socket " + e.toString());
		} catch (HttpException e) {
			Log.e(TAG, "Problemw with HTTP server " + e.toString());
		}
	}

	private void sendPlaylist(DefaultHttpServerConnection serverConnection,
			RequestLine requestLine) throws IOException, HttpException {
		HttpResponse response = new BasicHttpResponse(new HttpVersion(1, 1),
				200, "OK");

		Cursor c = mContext.getContentResolver().query(
				FileSharingProvider.Files.CONTENT_URI,
				new String[] { FileSharingProvider.Files.Columns._ID,
						FileSharingProvider.Files.Columns.DISPLAY_NAME }, null,
				null, null);
		String playlist = "";
		while (c.moveToNext()) {
			long id = c.getLong(c
					.getColumnIndex(FileSharingProvider.Files.Columns._ID));
			String name = c
					.getString(c
							.getColumnIndex(FileSharingProvider.Files.Columns.DISPLAY_NAME));
			if (name.endsWith(".mp3")) {
				playlist += SharedFileBrowser.getShareURL(id, mContext) + "\n";
			}
		}
		c.close();
		response.addHeader("Content-Type", "audio/x-mpegurl");
		response.addHeader("Content-Length", "" + playlist.length());
		response.setEntity(new StringEntity(playlist));
		serverConnection.sendResponseHeader(response);
		serverConnection.sendResponseEntity(response);
	}

	private void sendSharedFilesList(
			DefaultHttpServerConnection serverConnection,
			RequestLine requestLine) throws UnsupportedEncodingException,
			HttpException, IOException {
		HttpResponse response = new BasicHttpResponse(new HttpVersion(1, 1),
				200, "OK");
		String folderId = getFolderId(requestLine.getUri());
		String header = getHTMLHeader();
		String form = getUploadForm(folderId);
		String footer = getHTMLFooter();
		String listing = getFileListing(Uri.withAppendedPath(
				FileSharingProvider.Folders.CONTENT_URI, folderId));
		response.setEntity(new StringEntity(header + listing + form + footer));
		serverConnection.sendResponseHeader(response);
		serverConnection.sendResponseEntity(response);
	}

	private String createCookie() {
		Random r = new Random();
		String value = Long.toString(Math.abs(r.nextLong()), 36);
		ContentValues values = new ContentValues();
		values.put("name", "id");
		values.put("value", value);
		values.put("expiry", (int) System.currentTimeMillis() / 1000
				+ COOKIE_EXPIRY_SECONDS);
		mCookiesDatabase.insert("cookies", "name", values);
		return value;
	}

	private boolean isValidCookie(String cookie) {
		Cursor cursor = mCookiesDatabase.query("cookies",
				new String[] { "value" },
				"name = ? and value = ? and expiry > ?", new String[] { "id",
						cookie, "" + (int) System.currentTimeMillis() / 1000 },
				null, null, null);
		boolean isValid = cursor.getCount() > 0;
		cursor.close();
		return isValid;
	}

	private void deleteOldCookies() {
		mCookiesDatabase.delete("cookies", "expiry < ?", new String[] { ""
				+ (int) System.currentTimeMillis() / 1000 });
	}

	private void sendNotFound(DefaultHttpServerConnection serverConnection)
			throws UnsupportedEncodingException, HttpException, IOException {
		HttpResponse response = new BasicHttpResponse(new HttpVersion(1, 1),
				404, "NOT FOUND");
		response.setEntity(new StringEntity("NOT FOUND"));
		serverConnection.sendResponseHeader(response);
		serverConnection.sendResponseEntity(response);
	}

	public String getHTMLHeader() {
		return "<html><head><title>File Share</title></head><body>";
	}

	public String getHTMLFooter() {
		return "</body></html>";
	}

	private void handleUploadRequest(
			DefaultHttpServerConnection serverConnection, HttpRequest request,
			RequestLine requestLine) throws IOException, HttpException,
			UnsupportedEncodingException {
		HttpResponse response = new BasicHttpResponse(new HttpVersion(1, 1),
				200, "OK");
		String folderId = getFolderId(requestLine.getUri());
		processUpload(folderId, request, serverConnection);
		String header = getHTMLHeader();
		String form = getUploadForm(folderId);
		String footer = getHTMLFooter();
		String listing = getFileListing(Uri.withAppendedPath(
				FileSharingProvider.Folders.CONTENT_URI, folderId));
		response.setEntity(new StringEntity(header + listing + form + footer));
		serverConnection.sendResponseHeader(response);
		serverConnection.sendResponseEntity(response);
	}

	private String getFolderId(String firstline) {
		Pattern p = Pattern.compile("/(?:folder|playlist|zip)/(\\d+)");
		Matcher m = p.matcher(firstline);
		boolean b = m.find(0);
		if (b) {
			return m.group(1);
		}
		return null;
	}

	private String getFileId(String firstline) {
		Pattern p = Pattern.compile("/file/(\\d+)");
		Matcher m = p.matcher(firstline);
		boolean b = m.find(0);
		if (b) {
			return m.group(1);
		}
		return null;
	}

	private void sendFileContent(DefaultHttpServerConnection serverConnection,
			RequestLine requestLine) throws IOException, HttpException {
		HttpResponse response = new BasicHttpResponse(new HttpVersion(1, 1),
				200, "OK");
		String fileId = getFileId(requestLine.getUri());
		addFileEntity(Uri.withAppendedPath(
				FileSharingProvider.Files.CONTENT_URI, fileId), response);
		serverConnection.sendResponseHeader(response);
		serverConnection.sendResponseEntity(response);
	}

	private void sendFolderContent(
			DefaultHttpServerConnection serverConnection,
			RequestLine requestLine) throws IOException, HttpException {
		HttpResponse response = new BasicHttpResponse(new HttpVersion(1, 1),
				200, "OK");
		String folderId = getFolderId(requestLine.getUri());
		addFolderZipEntity(folderId, response);
		serverConnection.sendResponseHeader(response);
		serverConnection.sendResponseEntity(response);
	}

	private void addFolderZipEntity(String folderId, HttpResponse response) {
		response.addHeader("Content-Type", "application/zip");
		response.setEntity(new StreamingZipEntity(
				mContext.getContentResolver(), folderId));
	}

	private String getZipLink(long folderId) {
		return "<a href=\"/zip/" + folderId + "/folder.zip\">"
				+ "Zip of Entire Folder</a>";
	}

	private void addFileEntity(final Uri uri, HttpResponse response)
			throws IOException {
		if (mTransferStartedListener != null) {
			mTransferStartedListener.started(uri);
		}

		Cursor c = mContext.getContentResolver().query(uri, null, null, null,
				null);
		c.moveToFirst();
		int nameIndex = c
				.getColumnIndexOrThrow(FileSharingProvider.Files.Columns.DISPLAY_NAME);
		String name = c.getString(nameIndex);
		int dataIndex = c
				.getColumnIndexOrThrow(FileSharingProvider.Files.Columns._DATA);
		Uri data = Uri.parse(c.getString(dataIndex));

		c = mContext.getContentResolver().query(data, null, null, null, null);
		c.moveToFirst();
		int sizeIndex = c.getColumnIndexOrThrow(OpenableColumns.SIZE);
		int sizeBytes = c.getInt(sizeIndex);
		c.close();

		InputStream input = mContext.getContentResolver().openInputStream(data);

		String contentType = "application/octet-stream";
		if (name.endsWith(".jpg")) {
			contentType = "image/jpg";
		}

		response.addHeader("Content-Type", contentType);
		response.addHeader("Content-Length", "" + sizeBytes);
		response.setEntity(new InputStreamEntity(input, sizeBytes));
	}

	private String folderToLink(String folderName, int folderId) {
		return "<a href=\"/folder/" + folderId + "\">" + folderName + "</a>";
	}

	private String fileToLink(String fileName, int fileId) {
		return "<a href=\"/file/" + fileId + "/" + fileName + "\">" + fileName
				+ "</a>";
	}

	private String getFolderListing() {
		/* Get list of folders */
		Cursor c = mContext.getContentResolver()
				.query(FileSharingProvider.Folders.CONTENT_URI, null, null,
						null, null);
		int nameIndex = c
				.getColumnIndexOrThrow(FileSharingProvider.Folders.Columns.DISPLAY_NAME);
		int idIndex = c
				.getColumnIndexOrThrow(FileSharingProvider.Folders.Columns._ID);
		String s = "";
		while (c.moveToNext()) {
			String name = c.getString(nameIndex);
			int id = c.getInt(idIndex);
			s += folderToLink(name, id) + "<br/>";
		}
		c.close();
		return s;
	}

	private void sendSharedFolderListing(
			DefaultHttpServerConnection serverConnection)
			throws UnsupportedEncodingException, HttpException, IOException {
		HttpResponse response = new BasicHttpResponse(new HttpVersion(1, 1),
				200, "OK");
		response.setEntity(new StringEntity(getHTMLHeader()
				+ getFolderListing() + getHTMLFooter()));
		serverConnection.sendResponseHeader(response);
		serverConnection.sendResponseEntity(response);
	}

	@SuppressWarnings("deprecation")
	public void processUpload(String folderId, HttpRequest request,
			DefaultHttpServerConnection serverConnection) throws IOException,
			HttpException {

		/* Find the boundary and the content length. */
		String contentType = request.getFirstHeader("Content-Type").getValue();
		String boundary = contentType.substring(contentType
				.indexOf("boundary=") + "boundary=".length());
		BasicHttpEntityEnclosingRequest enclosingRequest = new BasicHttpEntityEnclosingRequest(
				request.getRequestLine());
		serverConnection.receiveRequestEntity(enclosingRequest);

		InputStream input = enclosingRequest.getEntity().getContent();
		MultipartStream multipartStream = new MultipartStream(input,
				boundary.getBytes());
		String headers = multipartStream.readHeaders();

		/* Get the filename. */
		StringTokenizer tokens = new StringTokenizer(headers, ";", false);
		String filename = null;
		while (tokens.hasMoreTokens() && filename == null) {
			String token = tokens.nextToken().trim();
			if (token.startsWith("filename=")) {
				filename = URLDecoder.decode(
						token.substring("filename=\"".length(),
								token.lastIndexOf("\"")), "utf8");
			}
		}

		File uploadDirectory = new File("/sdcard/fileshare/uploads");
		if (!uploadDirectory.exists()) {
			uploadDirectory.mkdirs();
		}

		/* Write the file and add it to the shared folder. */
		File uploadFile = new File(uploadDirectory, filename);
		FileOutputStream output = new FileOutputStream(uploadFile);
		multipartStream.readBodyData(output);
		output.close();

		Uri fileUri = Uri.withAppendedPath(FileProvider.CONTENT_URI,
				uploadFile.getAbsolutePath());
		Uri folderUri = Uri.withAppendedPath(
				FileSharingProvider.Folders.CONTENT_URI, folderId);
		FileSharingProvider.addFileToFolder(mContext.getContentResolver(),
				fileUri, folderUri);
	}

	private String getFileListing(Uri uri) {
		int folderId = Integer.parseInt(uri.getPathSegments().get(1));
		Uri fileUri = FileSharingProvider.Files.CONTENT_URI;
		String where = FileSharingProvider.Files.Columns.FOLDER_ID + "="
				+ folderId;
		Cursor c = mContext.getContentResolver().query(fileUri, null, where,
				null, null);
		int nameIndex = c
				.getColumnIndexOrThrow(FileSharingProvider.Files.Columns.DISPLAY_NAME);
		int idIndex = c
				.getColumnIndexOrThrow(FileSharingProvider.Files.Columns._ID);
		String s = "";
		boolean hasMusic = false;
		while (c.moveToNext()) {
			String name = c.getString(nameIndex);
			int id = c.getInt(idIndex);
			s += fileToLink(name, id) + "<br/>";
			if (name.endsWith(".mp3")) {
				hasMusic = true;
			}
		}
		c.close();
		s += getZipLink(folderId) + "<br/>";
		return s;
	}

	private String getUploadForm(String folderId) {
		if (mSharedPreferences.getBoolean(
				SroidServerService.PREFS_ALLOW_UPLOADS, false)) {
			return "<form method=\"POST\" action=\"/folder/" + folderId + "\" "
					+ "enctype=\"multipart/form-data\"> "
					+ "<input type=\"file\" name=\"file\" size=\"40\"/> "
					+ "<input type=\"submit\" value=\"Upload\"/>";
		}
		return "";
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		handleRequest();

	}
}
