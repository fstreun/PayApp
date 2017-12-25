package ch.ethz.inf.vs.fstreun.network.DataSync.Server;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import ch.ethz.inf.vs.fstreun.network.HttpParser;
import ch.ethz.inf.vs.fstreun.network.NetworkKeys;
import ch.ethz.inf.vs.fstreun.payapp.filemanager.DataService;

/**
 * Created by fabio on 12/25/17.
 *
 */

public class DataSyncPublish implements Runnable{

    private final String TAG = "DataSyncPublish";

    private final DataService.DataServiceBinder mServiceBinder;
    private final Socket mSocket;

    DataSyncPublish(DataService.DataServiceBinder serviceBinder, Socket socket){
        mServiceBinder = serviceBinder;
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

        if (mServiceBinder == null) {
            // service not available
            return "{\"" + NetworkKeys.SUCCESS + "\": false}";
        }

        JSONObject json;
        try {
            json = new JSONObject(requestBody);

            return generateResponseBody(json).toString();

        } catch (JSONException e) {
            // requestBody is not a JSON
            return "{\"" + NetworkKeys.SUCCESS + "\": false}";
        }

    }

    private JSONObject generateResponseBody(JSONObject requestBody) throws JSONException {
        JSONObject jsonResponse = new JSONObject();

        // get requested sessionID
        UUID sessionID = UUID.fromString(requestBody.getString(NetworkKeys.SESSIONID));

        // get access to session
        DataService.SessionNetworkAccess sessionAccess = mServiceBinder.getSessionNetworkAccess(sessionID);
        if (sessionAccess == null){
            // no such session available
            Log.d(TAG, "Session not available on this device.");
            jsonResponse.put(NetworkKeys.SUCCESS, false);
            return jsonResponse;
        }

        // get start map
        JSONArray jsonMap = requestBody.getJSONArray(NetworkKeys.LENGTHMAP);
        Map<UUID, Integer> start = new HashMap<>(jsonMap.length());
        for (int i = 0; i < jsonMap.length(); i++){
            JSONObject jsonItem = jsonMap.getJSONObject(i);
            UUID deviceID = UUID.fromString(jsonItem.getString(NetworkKeys.DEVICEID));
            Integer length = Integer.valueOf(jsonItem.getString(NetworkKeys.LENGHT));
            start.put(deviceID, length);
        }

        // get data after start
        JSONObject data = sessionAccess.getData(start);

        // put data.
        jsonResponse.put(NetworkKeys.SESSIONID, sessionID.toString());
        jsonResponse.put(NetworkKeys.LENGTHMAP, jsonMap);
        jsonResponse.put(NetworkKeys.DATA, data);
        jsonResponse.put(NetworkKeys.SUCCESS, true);

        return jsonResponse;
    }
}
