package ch.ethz.inf.vs.fstreun.network;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.IBinder;
import android.util.Log;

import java.net.InetAddress;

public class SessionSubscribeService extends Service implements NsdManager.DiscoveryListener, NsdManager.ResolveListener{

    String TAG = "SessionSubscribeService";
    String SERVICE_TYPE = "payapp._tcp";
    String SERVICE_NAME = "PayAppSubscriber";
    private NsdManager mNsdManager;
    private NsdServiceInfo mService;

    public SessionSubscribeService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mNsdManager = (NsdManager) this.getSystemService(Context.NSD_SERVICE);
        mNsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, this);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onStartDiscoveryFailed(String s, int errorCode ) {
        Log.e(TAG, "Discovery failed: Error code:" + errorCode);
        mNsdManager.stopServiceDiscovery(this);
    }

    @Override
    public void onStopDiscoveryFailed(String s, int errorCode) {
        Log.e(TAG, "Discovery failed: Error code:" + errorCode);
        mNsdManager.stopServiceDiscovery(this);
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
        Log.d(TAG, "Service discovery success" + service);
        if (!service.getServiceType().equals(SERVICE_TYPE)) {
            // Service type is the string containing the protocol and
            // transport layer for this service.
            Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
        } else if (service.getServiceName().equals(SERVICE_NAME)) {
            // The name of the service tells the user what they'd be
            // connecting to. It could be "Bob's Chat App".
            Log.d(TAG, "Same machine: " + SERVICE_NAME);
        } else if (service.getServiceName().contains("PayApp")){
            mNsdManager.resolveService(service, this);
        }
    }

    @Override
    public void onServiceLost(NsdServiceInfo service) {
        // When the network service is no longer available.
        // Internal bookkeeping code goes here.
        Log.e(TAG, "service lost" + service);
    }

    @Override
    public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int errorCode) {
        // Called when the resolve fails.  Use the error code to debug.
        Log.e(TAG, "Resolve failed" + errorCode);
    }

    @Override
    public void onServiceResolved(NsdServiceInfo serviceInfo) {
        Log.e(TAG, "Resolve Succeeded. " + serviceInfo);

        if (serviceInfo.getServiceName().equals(SERVICE_NAME)) {
            Log.d(TAG, "Same IP.");
            return;
        }
        mService = serviceInfo;
        int port = mService.getPort();
        InetAddress host = mService.getHost();
    }
}
