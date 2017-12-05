package ch.ethz.inf.vs.fstreun.network;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by Kaan on 30.11.17.
 */

public class DataSyncSubscribeService extends Service {

    String TAG = "DataSyncSubscribeService";
    String SERVICE_TYPE = "_http._tcp.";
    String SERVICE_NAME = "DataSyncSubscriber";
    private NsdManager mNsdManager;
    private NsdServiceInfo mService;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager.ResolveListener mResolveListener;
    private InetAddress mHost;
    private int mPort;

    public DataSyncSubscribeService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
                } else if (service.getServiceName().contains("DataSyncPublisher")){
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
                String get_message = generateRequest(mHost.getHostAddress(), mPort, "/dataSync");

                OutputStream mOutputStream = mSocket.getOutputStream();

                PrintWriter wtr = new PrintWriter(mOutputStream);
                wtr.print(get_message);
                //mOutputStream.write(get_message.getBytes());
                wtr.flush();

                BufferedReader input = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
                // parse all header fields
                String result = "";
                String line;
                while ((line = input.readLine()) != null){
                    if (line.isEmpty()){
                        Log.i(TAG, result);
                        break;
                    }else {
                        result = result + line + "\r\n";
                    }
                }
                Log.i(TAG, "Result: " + result);
                mSocket.close();
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public String generateRequest(String host, int port, String path) {
            String accept = "text/plain";
            String connect = "Closed";


            String request = "GET " + path + " HTTP/1.1\r\n"
                    + "Host: " + host + ":" + port + "\r\n"
                    + "Accept: " + accept + "\r\n"
                    + "Connection: " + connect + "\r\n"
                    + "\r\n";

            return request;
        }
    }
}
