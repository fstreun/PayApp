package ch.ethz.inf.vs.fstreun.network.DataSync.Client;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import ch.ethz.inf.vs.fstreun.payapp.filemanager.DataService;

/**
 * Created by fstreun on 25.12.17.
 *
 */

public class DataSyncSubscribeService extends Service {

    String TAG = "DataSyncSubscribeService";
    String SERVICE_TYPE = "_http._tcp.";
    String SERVICE_NAME = "DataSyncSubscriber";
    private NsdManager mNsdManager;

    private NsdManager.DiscoveryListener mDiscoveryListener;

    private Set<NsdServiceInfo> mServiceInfos = new HashSet<>();


    DataService.DataServiceBinder dataServiceBinder;
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
        //tear down listener
        if (mNsdManager != null) {
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        }
        mNsdManager = null;

        // Unbind from service
        if (bound){
            unbindService(connection);
            bound = false;
        }
        super.onDestroy();
    }

    ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (name.getClassName().equals(DataService.class.getName())){
                dataServiceBinder = (DataService.DataServiceBinder) service;
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

        public synchronized void synchronizeSession(UUID sessionID, DataSyncCallback callback){
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

            ClientThread clientThread = new ClientThread(sessionNetworkAccess, callback);
            new Thread(clientThread).start();
        }
    }

    public interface DataSyncCallback{
        void dataUpdated();
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

                // only add once
                boolean success = addOnce(serviceInfo);
                Log.d(TAG, "onServiceResolved added new service info: " + success);

            }


    }

    private synchronized boolean addOnce (NsdServiceInfo serviceInfo){
        for (NsdServiceInfo info : mServiceInfos) {
            if (equal(info, serviceInfo)) {
                return false;
            }
        }
        return mServiceInfos.add(serviceInfo);
    }
    private boolean equal (NsdServiceInfo x, NsdServiceInfo y){
        return x.getServiceName().equals(y.getServiceName()) && x.getHost().equals(y.getHost()) && x.getPort() == y.getPort();
    }


    private class ClientThread implements Runnable {

        final static String TAG = "ClientThread";

        private final  DataService.SessionNetworkAccess mSessionAccess;
        private final DataSyncCallback mCallback;

        ClientThread(DataService.SessionNetworkAccess sessionAccess, DataSyncCallback callback) {
            this.mSessionAccess = sessionAccess;
            this.mCallback = callback;
        }


        public void run() {
            Log.d(TAG, "run()");

            for (Iterator<NsdServiceInfo> iterator = mServiceInfos.iterator(); iterator.hasNext();){
                NsdServiceInfo serviceInfo = iterator.next();
                InetAddress address = serviceInfo.getHost();
                int port = serviceInfo.getPort();
                try {
                    Socket socket = new Socket(address, port);
                    socket.setSoTimeout(1000);

                    DataSyncSubscribe dataSyncSubscribe = new DataSyncSubscribe(mSessionAccess, socket);
                    // execute not concurrent
                    dataSyncSubscribe.run();

                } catch (IOException e) {
                    Log.e(TAG, "new Socket Creation exception.", e);
                    iterator.remove();
                }
            }

            // call that data is updated
            mCallback.dataUpdated();

            Log.d(TAG, "run finished");
        }


    }
}
