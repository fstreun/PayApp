package ch.ethz.inf.vs.fstreun.network;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Binder;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import ch.ethz.inf.vs.fstreun.payapp.DataService;

/**
 * Created by Kaan on 30.11.17.
 */

public class DataSyncSubscribeService extends Service {

    String TAG = "DataSyncSubscribeService";
    String SERVICE_TYPE = "_http._tcp.";
    String SERVICE_NAME = "DataSyncSubscriber";
    private NsdManager mNsdManager;

    private NsdManager.DiscoveryListener mDiscoveryListener;

    private Set<NsdServiceInfo> mServiceInfos = new HashSet<>();


    DataService.LocalBinder dataServiceBinder;
    boolean bound = false;

    @Override
    public void onCreate() {
        super.onCreate();
        // start listener
        initializeDiscoveryListener();
        mNsdManager = (NsdManager) this.getSystemService(Context.NSD_SERVICE);
        mNsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);


        // Bind DataService
        Intent intent = new Intent(this, DataService.class);
        bindService(intent, connection, BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //TODO: tear down listener

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
        return mBinder;
    }



    /*
    Binding of the service:
     */

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public DataSync getDataSync(){
            return mDataSync;
        }
    }


    DataSync mDataSync = new DataSync();

    public class DataSync{
        private DataSync(){
        };

        public synchronized void synchronizeSession(UUID sessionID){
            Log.d(TAG, "DataSync synchronizeSession called");
            // check if data service is already bounded
            if (!bound){
                return;
            }

            DataService.SessionNetworkAccess sessionNetworkAccess = dataServiceBinder.getSessionNetworkAccess(sessionID);
            if (sessionNetworkAccess == null){
                // not available
                return;
            }

            ClientThread clientThread = new ClientThread(sessionNetworkAccess);
            new Thread(clientThread).start();

        }
    }

    private void initializeDiscoveryListener() {
        // Instantiate a new DiscoveryListener
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onStartDiscoveryFailed(String s, int errorCode ) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            }

            @Override
            public void onStopDiscoveryFailed(String s, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            }

            @Override
            public void onDiscoveryStarted(String s) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found!  Do something with it.

                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals(SERVICE_NAME)) {
                    // The name of the service tells the user what they'd be
                    // connecting to. It could be "Bob's Chat App".
                    Log.d(TAG, "Same machine: " + SERVICE_NAME);
                } else if (service.getServiceName().contains("DataSyncPublisher")){
                    Log.d(TAG, "Resolve service: ");
                    mNsdManager.resolveService(service, new MyResolveListener());
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(TAG, "service lost" + service);
            }
        };
    }

    private class MyResolveListener implements NsdManager.ResolveListener {

            @Override
            public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int errorCode) {
                // Called when the resolve fails.  Use the error code to debug.
                Log.w(TAG, "Resolve failed" + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.w(TAG, "Resolve Succeeded. " + serviceInfo);

                //TODO: Same IP not detected!
                if (serviceInfo.getServiceName().equals(SERVICE_NAME)) {
                    Log.i(TAG, "Same IP.");
                    return;
                }

                // TODO: only add once
                boolean success = mServiceInfos.add(serviceInfo);
                Log.d(TAG, "onServiceResolved added new service info: " + success);

            }
    }


    private class ClientThread implements Runnable {

        private final  DataService.SessionNetworkAccess mSessionAccess;

        ClientThread(DataService.SessionNetworkAccess sessionAccess) {
            this.mSessionAccess = sessionAccess;
        }


        public void run() {
            Log.i("ClientThread", "run()");
            for (NsdServiceInfo serviceInfo : mServiceInfos){
                // iterate through all the discovered services

                InetAddress address = serviceInfo.getHost();
                int port = serviceInfo.getPort();
                Socket socket = null;
                try {
                    socket = new Socket(address, port);
                    socket.setSoTimeout(100);
                    handleSocket(socket);
                } catch (IOException e) {
                    Log.e(TAG, "new Socket Creation exception.", e);
                    // TODO: remove serviceInfo from mServiceInfos if not successful
                }
            }
        }

        private void handleSocket(Socket socket){
            try {

                JSONObject requestBody = generateRequestBody();

                String get_message = generateRequest(socket.getInetAddress().toString(), socket.getPort(), "/dataSync", requestBody.toString());

                OutputStream mOutputStream = socket.getOutputStream();

                PrintWriter wtr = new PrintWriter(mOutputStream);
                wtr.print(get_message);
                //mOutputStream.write(get_message.getBytes());
                wtr.flush();

                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String result = parseResponseForBody(input);

                JSONObject response = new JSONObject(result);

                String sessionId = response.getString("sessionID");
                JSONObject data = response.getJSONObject("data");

                // Todo: Was not sure if wee need to parse the length of the map array or the new data array

                Map<UUID, Integer> expected = new HashMap<UUID, Integer>();
                JSONArray jsonMap = response.getJSONArray("map");

                for(int i = 0; i < jsonMap.length(); i++) {
                    JSONObject item = (JSONObject) jsonMap.get(i);
                    UUID mUUid =  UUID.fromString((String) item.getString("deviceId"));
                    Integer mLength = Integer.valueOf(item.getString("length"));
                    expected.put(mUUid, mLength);
                }

                // save data
                mSessionAccess.putData(data, expected);

                socket.close();
                return;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        private JSONObject generateRequestBody() {

            JSONObject mJsonRequest = null;

            try {
                // get data for request (sessionId, Map<UUID, Int) -> start
                UUID sessionId = mSessionAccess.getSessionID();
                Map<UUID, Integer> mData = mSessionAccess.getLength();

                // create json request
                mJsonRequest = new JSONObject();
                mJsonRequest.put("sessionId", sessionId.toString());
                mJsonRequest.put("command", "start");
                JSONArray mJsonMap = new JSONArray();

                for (Map.Entry<UUID, Integer> item : mData.entrySet()) {
                    UUID deviceId = item.getKey();
                    Integer length = item.getValue();

                    JSONObject mJsonItem = new JSONObject();
                    mJsonItem.put("deviceId", deviceId.toString());
                    mJsonItem.put("length", length.toString());
                    mJsonMap.put(mJsonItem);
                }
                mJsonRequest.put("map", mJsonMap);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return mJsonRequest;
        }

        public String generateRequest(String host, int port, String path, String body) {
            String accept = "text/plain";
            String connect = "Closed";


            String request = "GET " + path + " HTTP/1.1\r\n"
                    + "Host: " + host + ":" + port + "\r\n"
                    + "Accept: " + accept + "\r\n"
                    + "Connection: " + connect + "\r\n\r\n"
                    + body + "\r\n"
                    + "\r\n";

            return request;
        }

        public String parseResponseForBody(BufferedReader input) {
            // parse all header fields

            try {
                String statusLine = input.readLine();;

                if (statusLine == null || statusLine.isEmpty()) {
                    return "";
                }

                String lengthLine = input.readLine();
                if (lengthLine == null || lengthLine.isEmpty()) {
                    return "";
                }

                String typeLine = input.readLine();
                if (typeLine == null || typeLine.isEmpty()) {
                    return "";
                }

                String connectionLine = input.readLine();
                if (connectionLine == null || connectionLine.isEmpty()) {
                    return "";
                }

                String result = "";
                String line;

                while ((line = input.readLine()) != null){
                    if (line.isEmpty()){
                        Log.w(TAG, result);
                        break;
                    }else {
                        result = result + line + "\r\n";
                    }
                }
                return result;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "";
        }
    }
}
