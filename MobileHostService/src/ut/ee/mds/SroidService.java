package ut.ee.mds;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.RequestLine;
import org.apache.http.impl.DefaultHttpServerConnection;

import android.app.Activity;
import android.content.Context;

import ut.ee.sroid.SroidRequest;
import ut.ee.sroid.SroidResponse;

public interface SroidService 
{
    /**
     * Check for the existence of a word.
     * @param word the word to be checked.
     * @return true if the word is in the dictionary,
     *         false otherwise.
    **/
	public void onAccelerationChanged(float x, float y, float z);
	public void onShake(float force);
	public void doCreate(Activity activity, Context context);
	public void doGEt(SroidRequest request, SroidResponse response) throws HttpException, IOException;
	public void doPost(SroidRequest request, SroidResponse response) throws HttpException, IOException;
	public void doDelete(SroidRequest request, SroidResponse response)throws HttpException, IOException;
	public void doPut(SroidRequest request, SroidResponse response) throws HttpException, IOException;
    public boolean processRequest(DefaultHttpServerConnection serverConnection, HttpRequest request,
			RequestLine requestLine);
    public String checkAvailability();
}
