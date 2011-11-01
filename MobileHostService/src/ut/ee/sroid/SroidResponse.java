package ut.ee.sroid;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.RequestLine;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.message.BasicHttpResponse;

public class SroidResponse {
	DefaultHttpServerConnection serverConnection;
	HttpRequest request;
	RequestLine requestLine;
	Map<String, String> getParameters = new HashMap<String, String>();
	String message;

	public SroidResponse(DefaultHttpServerConnection serverConnection,
			HttpRequest request, RequestLine requestLine) {
		this.request = request;
		this.serverConnection = serverConnection;
		this.requestLine = requestLine;
		message = "";
	}

	public void write(String text) {
		message = message + text;
	}

	public void close() throws HttpException, IOException {
		HttpResponse response = new BasicHttpResponse(new HttpVersion(1, 1),
				200, "OK");
		response.setEntity(new StringEntity(message));
		serverConnection.sendResponseHeader(response);
		serverConnection.sendResponseEntity(response);
		// serverConnection.flush();
		// serverConnection.close();
	}

}
