package ch.ethz.inf.vs.fstreun.network;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

import ch.ethz.inf.vs.fstreun.payapp.JoinGroupActivity;

public class SessionSubscribeService extends Service {

    String TAG = "SessionSubscribeService";
    String SERVICE_TYPE = "_http._tcp.";
    String SERVICE_NAME = "SessionSubscriber";
    private Context mContext;
    private NsdManager mNsdManager;
    private NsdServiceInfo mService;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager.ResolveListener mResolveListener;
    private InetAddress mHost;
    private int mPort;
    private String secret;
    private JoinGroupActivity activity;

    public SessionSubscribeService(){}

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mContext = this;
        secret = intent.getStringExtra("SECRET");

        activity = JoinGroupActivity.instance;

        initializeResolveListener();
        initializeDiscoveryListener();
        mNsdManager = (NsdManager) this.getSystemService(Context.NSD_SERVICE);
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
                } else if (service.getServiceName().contains("SessionPublisher")){
                    Log.d(TAG, "Resolve service: ");
                    mNsdManager.resolveService(service, mResolveListener);
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

    public void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {

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
                mPort = mService.getPort();
                mHost = mService.getHost();

                new Thread(new ClientThread()).start();
            }
        };
    }

    class ClientThread implements Runnable {

        private Socket mSocket;

        public void run() {
            Log.i("ClientThread", "run()");
            try {
                mSocket = new Socket(mHost, mPort);
                String get_message = generateRequest(mHost.getHostAddress(), mPort, "/joinGroup", secret);

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
                        JSONObject group = new JSONObject(response.getString("group"));
                        activity.addGroupToList(group);
                    }
                } else {
                    // do nothing
                    Log.w(TAG, "Failure response: " + result);
                    return;
                }
                mSocket.close();
                return;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
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
