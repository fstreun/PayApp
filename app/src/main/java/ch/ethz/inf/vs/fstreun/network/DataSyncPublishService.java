package ch.ethz.inf.vs.fstreun.network;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.IBinder;
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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import ch.ethz.inf.vs.fstreun.payapp.R;
import ch.ethz.inf.vs.fstreun.payapp.filemanager.DataService;

/**
 * Created by Kaan on 30.11.17.
 */

public class DataSyncPublishService extends Service {

    String TAG = "DataSyncPublishService";
    String SERVICE_TYPE = "_http._tcp.";
    String SERVICE_NAME = "DataSyncPublisher";
    private NsdManager mNsdManager;
    private NsdServiceInfo mServiceInfo;
    private String mServiceName;
    private ServerSocket mServerSocket = null;
    private int mLocalPort;
    private NsdManager.RegistrationListener mRegistrationListener;

    DataService.LocalBinder dataServiceBinder;
    boolean bound = false;

    @Override
    public void onCreate() {
        super.onCreate();
        // start server Socket
        initializeServerSocket();
        // Bind DataService
        Intent intent = new Intent(this, DataService.class);
        bindService(intent, connection, BIND_AUTO_CREATE);
    }

    public void initializeServerSocket() {
        // Initialize a server socket on the next available port.
        if (mServerSocket == null) {
            try {
                mServerSocket = new ServerSocket(0);
                // start master network thread
                new Thread(new ServerMasterThread()).start();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Store the chosen port.
            mLocalPort = mServerSocket.getLocalPort();
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        // unregister service
        mNsdManager.unregisterService(mRegistrationListener);
        mNsdManager = null;

        // stop ServerSocket
        if (mServerSocket != null){
            try {
                mServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "failed to close ServerSocket");
            }
        }

        // Unbind from service
        if (bound){
            unbindService(connection);
            bound = false;
        }
    }

    ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (name.getClassName().equals(DataService.class.getName())){
                dataServiceBinder = (DataService.LocalBinder) service;
                bound = true;
                Log.d(TAG, "onServiceConnected: " + name.getClassName());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG, "onServiceDisconnected");
            bound = false;
        }
    };



    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        initializeRegistrationListener();
        registerService(mLocalPort);
        return super.onStartCommand(intent, flags, startId);
    }



    public void registerService(int port) {
        // Create the NsdServiceInfo object, and populate it.
        mServiceInfo  = new NsdServiceInfo();

        // The name is subject to change based on conflicts
        // with other services advertised on the same network.
        mServiceInfo.setServiceName(SERVICE_NAME);
        mServiceInfo.setServiceType(SERVICE_TYPE);
        mServiceInfo.setPort(port);

        mNsdManager = (NsdManager) this.getSystemService(Context.NSD_SERVICE);

        mNsdManager.registerService(mServiceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
    }


    public void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onRegistrationFailed(NsdServiceInfo nsdServiceInfo, int errorCode) {
                Log.d(TAG, "Registration Failed: " + errorCode);
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo nsdServiceInfo, int errorCode) {
                Log.d(TAG, "Unregistration Failed: " + errorCode);
            }

            @Override
            public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
                mServiceName = mServiceInfo.getServiceName();
                Log.d(TAG, "onServiceRegistered: " + mServiceName);
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo nsdServiceInfo) {
                Log.d(TAG, "onServiceUnregistered: " + nsdServiceInfo.getServiceName());
            }
        };
    }

    class ServerMasterThread implements Runnable {

        private final static String TAG = "ServerMasterThread";

        public void run() {
            Log.i("ServerMasterThread", "run()");
            while (mServerSocket != null) {

                try {
                    Log.i(TAG, "run() - open RequestThread");
                    Socket socket = mServerSocket.accept();
                    socket.setSoTimeout(1000);
                    // start slave thread
                    ServerSlaveThread slaveThread = new ServerSlaveThread(socket);
                    slaveThread.run();


                } catch (SocketException e) {
                    // socket closed
                    Log.e(TAG, "socket exception, probably closed.", e);
                } catch (IOException e) {
                    Log.e(TAG, "io excepted.", e);
                }
            }
        }
    }

    class ServerSlaveThread implements Runnable{

        private final static String TAG = "ServerSlaveThread";

        private final Socket mSocket;

        ServerSlaveThread(Socket socket) {
            this.mSocket = socket;
        }

        @Override
        public void run() {

            try {
                Log.d(TAG, "run");
                if (!bound){
                    // session access service not available
                    mSocket.close();
                    return;
                }

                BufferedReader input = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));

                String jsonBodyString = parseRequestForBody(input);

                Log.d(TAG, "request: " + jsonBodyString);

                JSONObject jsonBody = new JSONObject(jsonBodyString);


                BufferedWriter output = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));

                PrintWriter wtr = new PrintWriter(output);

                // Create Here Response String (favourably JSON)
                JSONObject responseBody = generateResponseBody(jsonBody);
                String stringBody = "";
                if (jsonBodyString != null){
                    stringBody = jsonBody.toString();
                }
                Log.d(TAG, "response: " + stringBody);

                wtr.print(generateResponse(stringBody));
                wtr.flush();
                wtr.close();

                Log.d(TAG, "run finished without exception");
            }catch (IOException e){
                e.printStackTrace();
            }catch (JSONException e){
                e.printStackTrace();
            }
        }

        public String generateResponse(String body) {
            String response =    "HTTP/1.1 200 OK\r\n" +
                    "Content-Length: " + body.length() + "\r\n" +
                    "Content-Type: application/json\r\n" +
                    "Connection: Closed\r\n\r\n" + body+ "\r\n\r\n";
            return response;
        }

        public JSONObject generateResponseBody(JSONObject requestBody){
            JSONObject jsonResponse = null;
            try {
                UUID sessionID = UUID.fromString(requestBody.getString(NetworkKeys.SESSIONID));

                DataService.SessionNetworkAccess sessionAccess = dataServiceBinder.getSessionNetworkAccess(sessionID);
                if (sessionAccess == null){
                    // no such session available
                    Log.d(TAG, "Session not available on this device.");
                    return null;
                }

                String command = requestBody.getString(NetworkKeys.COMMAND);


                JSONArray jsonMap = requestBody.getJSONArray(NetworkKeys.LENGTHMAP);

                Map<UUID, Integer> start = new HashMap<>(jsonMap.length());

                for (int i = 0; i < jsonMap.length(); i++){
                    JSONObject jsonItem = jsonMap.getJSONObject(i);
                    UUID deviceID = UUID.fromString(jsonItem.getString(NetworkKeys.DEVICEID));
                    Integer length = Integer.valueOf(jsonItem.getString(NetworkKeys.LENGHT));
                    start.put(deviceID, length);
                }

                JSONObject data = sessionAccess.getData(start);

                jsonResponse = new JSONObject();
                jsonResponse.put(NetworkKeys.SESSIONID, sessionID.toString());
                jsonResponse.put(NetworkKeys.LENGTHMAP, jsonMap);
                jsonResponse.put(NetworkKeys.DATA, data);

            }catch (JSONException e){
                Log.e(TAG, "JSONException in generateResponseBody.", e);
                return null;
            }
            return jsonResponse;
        }

        public String parseRequestForBody(BufferedReader input) {
            // parse all header fields

            try {
                String requestLine = input.readLine();;

                if (requestLine == null || requestLine.isEmpty()) {
                    return "";
                }

                String hostLine = input.readLine();
                if (hostLine == null || hostLine.isEmpty()) {
                    return "";
                }

                String acceptLine = input.readLine();
                if (acceptLine == null || acceptLine.isEmpty()) {
                    return "";
                }

                String connectionLine = input.readLine();
                if (connectionLine == null || connectionLine.isEmpty()) {
                    return "";
                }

                String emptyLine = input.readLine();
                if (emptyLine == null || !emptyLine.isEmpty()){
                    return "";
                }

                String result = "";
                String line;

                while ((line = input.readLine()) != null){
                    if (line.isEmpty()){
                        Log.d(TAG, result);
                        break;
                    }else {
                        result = result + line + "\r\n";
                    }
                }
                return result;
            } catch (IOException e) {
                Log.e(TAG, "IOException in parseRequestForBody");
            }
            return "";
        }


    }
}
