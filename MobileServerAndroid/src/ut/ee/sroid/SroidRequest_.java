package ut.ee.sroid;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.RequestLine;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;

public class SroidRequest_ {
	DefaultHttpServerConnection serverConnection;
	HttpRequest request;
	RequestLine requestLine;
	Map<String, String> getParameters = new HashMap<String, String>();

	public SroidRequest_(DefaultHttpServerConnection serverConnection,
			HttpRequest request, RequestLine requestLine) {
		this.request = request;
		this.serverConnection = serverConnection;
		this.requestLine = requestLine;
		if (requestLine.getMethod().equals("POST")) {
			try {
				readPostParameters();
			} catch (HttpException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (requestLine.getMethod().equals("GET"))
			readGetParameters();

	}

	@SuppressWarnings("unchecked")
	private void readPostParameters() throws HttpException, IOException {

		BasicHttpEntityEnclosingRequest enclosingRequest = new BasicHttpEntityEnclosingRequest(
				request.getRequestLine());
		serverConnection.receiveRequestEntity(enclosingRequest);

		InputStream input = enclosingRequest.getEntity().getContent();
		InputStreamReader reader = new InputStreamReader(input);
		ArrayList<String> lines = new ArrayList<String>();
		StringBuffer form = new StringBuffer();
		String line = "";
		while (reader.ready()) {
			char car = (char) reader.read();
			if (car == '\r') {
				lines.add(line);
				line = "";
			}
			line = line + car;
			form.append((char) reader.read());
		}
		for (String paramKey : lines) {
			String[] param = paramKey.split("=");
			if (param.length > 1)
				getParameters.put(param[0], param[1]);
		}
		String password = form.substring(form.indexOf("=") + 1);
	}

	private void readGetParameters() {
		String url[] = requestLine.getUri().split("\\?");
		if (url.length > 1) {
			String[] params = url[1].split("&");
			for (String p : params) {
				String[] param = p.split("=");
				getParameters.put(param[0], param[1]);
			}
		}
	}

	public String readParameter(String parameterName) {
		if (getParameters.containsKey(parameterName)) {
			return (String) getParameters.get(parameterName);
		}
		return "";
	}
}
