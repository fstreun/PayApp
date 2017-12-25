package ch.ethz.inf.vs.fstreun.network.SessionPublish.Server;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

import ch.ethz.inf.vs.fstreun.network.HttpParser;
import ch.ethz.inf.vs.fstreun.network.NetworkKeys;

/**
 * Created by fabio on 12/25/17.
 *
 */

public class GroupPublish implements Runnable {

    private final String TAG = "GroupPublish";

    private final Map<String, String> mGroups;
    private final Socket mSocket;

    GroupPublish(Map<String, String> groups, Socket socket){
        mGroups = groups;
        mSocket = socket;
    }

    @Override
    public void run() {
        try {
            handleCommunication();
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

        // get input buffer
        BufferedReader input = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));

        // parser http request
        HttpParser httpParser = new HttpParser(input);
        Log.d(TAG, "request: " + httpParser);

        if (httpParser.isError()){
            // error occurred during http parsing.
            Log.e(TAG, "HTTP Parsing error occurred");

            input.close();
            return;
        }

        // get http body
        String bodyString = httpParser.getBody();

        // get output buffer
        BufferedWriter output = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));
        PrintWriter wtr = new PrintWriter(output);

        // Create Here Response String
        String responseBody = generateResponseBody(bodyString);

        Log.d(TAG, "response: " + responseBody);

        wtr.print(generateResponse(responseBody));
        wtr.flush();
        wtr.close();

        input.close();
    }

    private String generateResponse(String body) {

        return "HTTP/1.1 200 OK\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "Content-Type: application/json\r\n" +
                "Connection: Closed\r\n\r\n" + body+ "\r\n\r\n";
    }

    private String generateResponseBody(String requestBody) {

        try {
            JSONObject json = new JSONObject(requestBody);

            return generateResponseBody(json).toString();

        } catch (JSONException e) {
            // requestBody is not a JSON
            return "{\"" + NetworkKeys.SUCCESS + "\": false}";
        }

    }

    private JSONObject generateResponseBody(JSONObject requestBody) throws JSONException {
        JSONObject jsonResponse = new JSONObject();

        String secret = requestBody.getString(NetworkKeys.SECRET);

        String groupString = mGroups.get(secret);

        if (groupString != null){
            jsonResponse.put(NetworkKeys.GROUP, groupString);
            jsonResponse.put(NetworkKeys.SECRET, secret);
            jsonResponse.put(NetworkKeys.SUCCESS, true);
        }else {
            jsonResponse.put(NetworkKeys.SUCCESS, false);
        }
        return jsonResponse;
    }


}
