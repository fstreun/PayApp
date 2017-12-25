package ch.ethz.inf.vs.fstreun.network.DataSync.Server;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import ch.ethz.inf.vs.fstreun.payapp.filemanager.DataService;

/**
 * Created by fstreun on 25.12.17.
 *
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

    DataService.DataServiceBinder dataServiceBinder;
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
        if (mNsdManager != null) {
            mNsdManager.unregisterService(mRegistrationListener);
        }
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
            while (mServerSocket != null && !mServerSocket.isClosed()) {

                try {
                    Log.i(TAG, "run() - open RequestThread");
                    Socket socket = mServerSocket.accept();
                    socket.setSoTimeout(1000);

                    // start slave thread
                    DataSyncPublish slaveThread = new DataSyncPublish(dataServiceBinder, socket);

                    // run not concurrent
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

}
