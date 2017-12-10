package ch.ethz.inf.vs.fstreun.network;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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
    private NsdManager.ResolveListener mResolveListener;

    private Set<NsdServiceInfo> mServiceInfos = new HashSet<>();


    DataService.LocalBinder dataServiceBinder;
    boolean bound = false;

    @Override
    public void onCreate() {
        super.onCreate();
        // start listener
        initializeResolveListener();
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
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
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

                // TODO: hope this is no capture (and only added once. I guess it should)
                boolean success = mServiceInfos.add(serviceInfo);
                Log.d(TAG, "onServiceResolved added new service info: " + success);

            }
        };
    }

    public synchronized void synchronizeSession(UUID sessionID){
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
        new Thread(clientThread).run();

    }


    class ClientThread implements Runnable {

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
                }
            }
        }

        public void handleSocket(Socket socket){
            try {
                String get_message = generateRequest(socket.getInetAddress().toString(), socket.getPort(), "/dataSync");

                OutputStream mOutputStream = socket.getOutputStream();

                PrintWriter wtr = new PrintWriter(mOutputStream);
                wtr.print(get_message);
                //mOutputStream.write(get_message.getBytes());
                wtr.flush();

                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
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


                socket.close();
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
