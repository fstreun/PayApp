package ch.ethz.inf.vs.fstreun.network.SessionPublish.Client;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import ch.ethz.inf.vs.fstreun.network.HttpParser;
import ch.ethz.inf.vs.fstreun.network.NetworkKeys;

/**
 * Created by fabio on 12/25/17.
 *
 */

public class GroupSubscribe implements Runnable{

    private final String TAG = "GroupSubscribe";

    private final Callback mCallback;
    private final Socket mSocket;

    GroupSubscribe(Callback callback, Socket socket){
        mCallback = callback;
        mSocket = socket;
    }

    @Override
    public void run() {
        try {
            if (mCallback != null) {
                handleCommunication();
            }
        } catch (IOException e) {
            Log.e(TAG, "handleCommunication throws IOException", e);
        }

        if (mSocket != null){
            try {
                mSocket.close();
                Log.d(TAG, "closed socket");
            } catch (IOException e) {
                Log.e(TAG, "failed closing socket");
            }
        }
    }

    private void handleCommunication() throws IOException {

        String requestBody;
        try {
            requestBody = generateRequestBody();
        } catch (JSONException e) {
            // generation of Request failed
            Log.e(TAG, "Request generation failed!");
            return;
        }

        String request = generateRequest(mSocket.getInetAddress().toString(), mSocket.getPort(), requestBody);
        Log.d(TAG, "Request: " + request);

        // get output stream and print writer
        OutputStream outputStream = mSocket.getOutputStream();
        PrintWriter printWriter = new PrintWriter(outputStream);

        printWriter.print(request);
        printWriter.flush();

        // get input buffer
        BufferedReader input = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));

        // parse http response
        HttpParser httpParser = new HttpParser(input);
        Log.d(TAG, "request: " + httpParser);

        if (httpParser.isError()){
            // error occurred druing http parsing.
            Log.e(TAG, "HTTP Parsing error occurred");

            input.close();
            printWriter.close();
            return;
        }

        // get http body
        String bodyString = httpParser.getBody();

        handleResponse(bodyString);

        // clean up streams and buffers
        input.close();
        printWriter.close();

    }

    private void handleResponse(String responseBody) {

        try {
            JSONObject object = new JSONObject(responseBody);

            Boolean success = object.getBoolean(NetworkKeys.SUCCESS);
            if (!success){
                // not successfull or wrong format
                return;
            }

            String secret = object.getString(NetworkKeys.SECRET);
            String group = object.getString(NetworkKeys.GROUP);

            mCallback.groupFound(group, secret);

        } catch (JSONException e) {
            // response body is not json
            Log.e(TAG, "Response body handling failed, due to JSONException", e);
        }
    }

    private String generateRequest(String host, int port, String body) {
        String path = "/joinGroup";
        String accept = "text/plain";
        String connect = "Closed";


        return "GET " + path + " HTTP/1.1\r\n"
                + "Host: " + host + ":" + port + "\r\n"
                + "Accept: " + accept + "\r\n"
                + "Connection: " + connect + "\r\n\r\n"
                + body + "\r\n"
                + "\r\n";
    }


    private String generateRequestBody() throws JSONException {

        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put(NetworkKeys.SECRET, mCallback.getSecret());

        return jsonRequest.toString();
    }

    public interface Callback{
        String getSecret();
        void groupFound(String simpleGroup, String secret);
    }
}
