package ch.ethz.inf.vs.fstreun.network;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import ch.ethz.inf.vs.fstreun.payapp.JoinGroupActivity;

public class SessionSubscribeService extends Service {

    String TAG = "## SessionSubscribeService";
    String SERVICE_TYPE = "_http._tcp.";
    String SERVICE_NAME = "SessionSubscriber";

    private NsdManager mNsdManager;
    private NsdServiceInfo mService;
    private NsdManager.DiscoveryListener mDiscoveryListener;

    private String secret;

    private JoinGroupActivity activity;

    public SessionSubscribeService(){}


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    @Override
    public void onCreate() {
        super.onCreate();
        // initialize listener
        initializeDiscoveryListener();
        mNsdManager = (NsdManager) this.getSystemService(Context.NSD_SERVICE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mNsdManager.stopServiceDiscovery(mDiscoveryListener);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (mNsdManager == null){
            Log.e(TAG, "FATAL ERRO: NSD Manager has not been properly initialized.");
            stopSelf();
            return START_STICKY;
        }

        secret = intent.getStringExtra("SECRET");
        activity = JoinGroupActivity.instance;


        mNsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);

        return super.onStartCommand(intent, flags, startId);
    }


    public void initializeDiscoveryListener() {
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
                Log.d(TAG, "Service discovery started: " + s);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(TAG, "service lost" + service);
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found!  Do something with it.

                Log.d(TAG, "onServiceFound: " + service);

                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals(SERVICE_NAME)) {
                    // The name of the service tells the user what they'd be
                    // connecting to. It could be "Bob's Chat App".
                    Log.d(TAG, "Same machine: " + SERVICE_NAME);
                } else if (service.getServiceName().contains("SessionPublisher")){
                    Log.d(TAG, "Resolve service: ");
                    mNsdManager.resolveService(service, new MyResolveListener());
                }
            }
        };
    }

    class MyResolveListener implements NsdManager.ResolveListener {


        @Override
        public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int errorCode) {
            // Called when the resolve fails.  Use the error code to debug.
            Log.w(TAG, "Resolve failed" + errorCode);
        }

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            Log.w(TAG, "Resolve Succeeded. " + serviceInfo);

            if (serviceInfo.getServiceName().equals(SERVICE_NAME)) {
                Log.i(TAG, "Same IP.");
                return;
            }
            mService = serviceInfo;
            Socket socket = null;
            try {
                socket = new Socket(mService.getHost(), mService.getPort());
            } catch (IOException e) {
                Log.e(TAG, "onServiceResolved failed due to socket creation failure.", e);
                return;
            }

            NetworkTask networkTask = new NetworkTask();
            networkTask.execute(socket);
        }
    }

    /**
     * Have to use AsyncTask since UI Thread is needed to access the group list!
     */
    class NetworkTask extends AsyncTask<Socket, Void, JSONObject>{

        private Socket mSocket;


        @Override
        protected JSONObject doInBackground(Socket... sockets) {
            mSocket = sockets[0];
            if (mSocket == null){
                return null;
            }
            Log.i("ClientThread", "run()");
            try {
                //String get_message = generateRequest(mHost.getHostAddress(), mPort, "/joinGroup", secret);
                String get_message = generateRequest(mSocket.getInetAddress().toString(), mSocket.getPort(), "/joinGroup", secret);

                OutputStream mOutputStream = mSocket.getOutputStream();

                PrintWriter wtr = new PrintWriter(mOutputStream);
                wtr.print(get_message);
                wtr.flush();

                BufferedReader input = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));

                String result = parseResponseForBody(input);

                JSONObject response = new JSONObject(result);
                boolean success = response.getBoolean("result");
                if (success) {
                    Log.w(TAG, "Success response: " + result);
                    if (activity != null) {
                        // we are calling here activity's method
                        return new JSONObject(response.getString("group"));
                    }
                } else {
                    // do nothing
                    Log.w(TAG, "Failure response: " + result);
                    return null;
                }
                return null;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject simpleGroup) {
            if (mSocket != null && !mSocket.isClosed()){
                try {
                    mSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to close socket");
                }
            }

            if (simpleGroup != null) {
                activity.addGroupToList(simpleGroup);
            }
            super.onPostExecute(simpleGroup);
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

        public String generateRequest(String host, int port, String path, String secret) {
            String accept = "application/json";
            String connect = "Closed";

            JSONObject requestBody = new JSONObject();
            try {
                requestBody.put("secret", secret);
            } catch (JSONException e) {
                e.printStackTrace();
            }


            String request = "GET " + path + " HTTP/1.1\r\n"
                    + "Host: " + host + ":" + port + "\r\n"
                    + "Accept: " + accept + "\r\n"
                    + "Connection: " + connect + "\r\n"
                    + requestBody.toString() + "\r\n"
                    + "\r\n";

            return request;
        }
    }

}
