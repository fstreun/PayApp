package ch.ethz.inf.vs.fstreun.network.DataSync.Client;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
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

public class DataSyncSubscribe implements Runnable{

    private final String TAG = "DataSyncSubscribe";

    private final DataService.SessionNetworkAccess mSessionAccess;
    private final Socket mSocket;

    DataSyncSubscribe(DataService.SessionNetworkAccess sessionAccess, Socket socket){
        mSessionAccess = sessionAccess;
        mSocket = socket;
    }

    @Override
    public void run() {
        try {
            if (mSessionAccess != null) {
                handleCommunication();
            }
        } catch (IOException e) {
            Log.e(TAG, "handleSocket throws IOException", e);
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

            UUID sessionId = UUID.fromString(object.getString(NetworkKeys.SESSIONID));
            if (!sessionId.equals(mSessionAccess.getSessionID())){
                // data not supposed to be for this session
                return;
            }

            JSONObject data = object.getJSONObject(NetworkKeys.DATA);

            Map<UUID, Integer> expected = new HashMap<>();
            JSONArray jsonMap = object.getJSONArray(NetworkKeys.LENGTHMAP);

            for (int i = 0; i < jsonMap.length(); i++) {
                JSONObject item = (JSONObject) jsonMap.get(i);
                UUID mUUid = UUID.fromString( item.getString(NetworkKeys.DEVICEID));
                Integer mLength = Integer.valueOf(item.getString(NetworkKeys.LENGHT));
                expected.put(mUUid, mLength);
            }

            // save data
            mSessionAccess.putData(data, expected);

        } catch (JSONException e) {
            // response body is not json
            Log.e(TAG, "Response body handling failed, due to JSONException", e);
        }


    }


    private String generateRequest(String host, int port, String body) {
        String path = "/dataSync";
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

            // get data for request (UUID sessionId, Map<UUID, Int> start)
            UUID sessionId = mSessionAccess.getSessionID();
            Map<UUID, Integer> mData = mSessionAccess.getLength();

            // create json request
            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put(NetworkKeys.SESSIONID, sessionId.toString());
            jsonRequest.put(NetworkKeys.COMMAND, "start");
            JSONArray mJsonMap = new JSONArray();

            for (Map.Entry<UUID, Integer> item : mData.entrySet()) {
                UUID deviceId = item.getKey();
                Integer length = item.getValue();

                JSONObject mJsonItem = new JSONObject();
                mJsonItem.put(NetworkKeys.DEVICEID, deviceId.toString());
                mJsonItem.put(NetworkKeys.LENGHT, length.toString());
                mJsonMap.put(mJsonItem);
            }

            jsonRequest.put(NetworkKeys.LENGTHMAP, mJsonMap);

            return jsonRequest.toString();


    }
}
