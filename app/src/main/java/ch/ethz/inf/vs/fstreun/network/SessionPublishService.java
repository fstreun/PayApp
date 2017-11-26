package ch.ethz.inf.vs.fstreun.network;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;

public class SessionPublishService extends Service {

    String TAG = "SessionPublishService";
    String SERVICE_TYPE = "_http._tcp.";
    String SERVICE_NAME = "PayAppPublisher";
    private NsdManager mNsdManager;
    private NsdServiceInfo mServiceInfo;
    private String mServiceName;
    private ServerSocket mServerSocket;
    private int mLocalPort;
    private NsdManager.RegistrationListener mRegistrationListener;

    public SessionPublishService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        initializeServerSocket();
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

    public void initializeServerSocket() {
        // Initialize a server socket on the next available port.
        try {
            mServerSocket = new ServerSocket(0);

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Store the chosen port.
        mLocalPort =  mServerSocket.getLocalPort();
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
                mNsdManager.unregisterService(mRegistrationListener);
            }
        };
    }


}
