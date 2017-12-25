package ch.ethz.inf.vs.fstreun.network.SessionPublish.Client;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.net.Socket;

import ch.ethz.inf.vs.fstreun.payapp.JoinGroupActivity;

public class GroupSubscribeService extends Service {

    String TAG = "## GroupSubscribeService";
    String SERVICE_TYPE = "_http._tcp.";
    String SERVICE_NAME = "SessionSubscriber";

    private NsdManager mNsdManager;
    private NsdServiceInfo mService;
    private NsdManager.DiscoveryListener mDiscoveryListener;

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

            GroupSubscribe groupSubscribe = new GroupSubscribe(JoinGroupActivity.instance, socket);
            new Thread(groupSubscribe).start();
        }
    }

}
