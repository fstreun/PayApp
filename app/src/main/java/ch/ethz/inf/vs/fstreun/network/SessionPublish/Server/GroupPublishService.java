package ch.ethz.inf.vs.fstreun.network.SessionPublish.Server;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;


public class GroupPublishService extends Service {

    String TAG = "## GroupPublishService";
    String SERVICE_TYPE = "_http._tcp.";
    String SERVICE_NAME = "SessionPublisher";

    private boolean registered;
    private NsdManager mNsdManager;
    private NsdServiceInfo mServiceInfo;
    private NsdManager.RegistrationListener mRegistrationListener;

    private ServerSocket mServerSocket;
    private int mLocalPort;
    private boolean initialized;
    Thread mMainServerThread = null;

    private String secret;
    private String groupJsonString;

    public GroupPublishService() {}

    @Override
    public IBinder onBind(Intent intent) {
        // Return the communication channel to the service.
        return mBinder;
    }

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public void setSecretGroup(String secret, String groupJSON){
            GroupPublishService.this.secret = secret;
            GroupPublishService.this.groupJsonString = groupJSON;
            Log.d(TAG, "setSecretGroup: " + secret + "\n" + groupJSON);
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        GroupPublishService.this.secret = "";
        GroupPublishService.this.groupJsonString = "";
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            initializeServerSocket();
        } catch (IOException e) {
            Log.e(TAG, "FATAL ERRO: Server Socket initialization failed!");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // start service
        Log.d(TAG, "StartCommand");

        if (!initialized){
            Log.e(TAG, "FATAL ERROR: Socket has not been properly initialized");
            stopSelf();
            return START_STICKY;
        }


        // start server
        if (mMainServerThread == null) {
            mMainServerThread = new Thread(new ServerMainThread());
            mMainServerThread.start();
        }

        // register service
        try {
            if (!registered) {
                registerService();
                registered = true;
            }
        } catch (IOException e) {
            Log.e(TAG, "FATAL ERROR: Service has not been properly initialized");
            stopSelf();
            return START_STICKY;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public void initializeServerSocket() throws IOException {
        // Initialize a server socket on the next available port.
        mServerSocket = new ServerSocket(0);
        mLocalPort =  mServerSocket.getLocalPort();
        initialized = true;
    }


    public void registerService() throws IOException {
        initializeRegistrationListener();
        registerService(mLocalPort);
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
                Log.d(TAG, "onServiceRegistered: " + nsdServiceInfo.getServiceName());
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo nsdServiceInfo) {
                Log.d(TAG, "onServiceUnregistered: " + nsdServiceInfo.getServiceName());
            }
        };
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
        if (mNsdManager != null) {
            mNsdManager.registerService(mServiceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
            Log.d(TAG, "NSD Manager registerService() called");
        }else {
            Log.e(TAG, "Failed to instantiate NSDManager");
        }
    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (mNsdManager != null && registered) {
            Log.d(TAG, "Unregister NSD");
            mNsdManager.unregisterService(mRegistrationListener);
            registered = false;
        }

        try {
            closeServerSocket();
            Log.d(TAG, "Server Socket closed");
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }


    public void closeServerSocket() throws IOException {
        if (mServerSocket != null && !mServerSocket.isClosed()){
            mServerSocket.close();
        }
    }


    class ServerMainThread implements Runnable {

        @Override
        public void run() {
            Log.i(TAG, "ServerMainThread run()");
            while (mServerSocket != null && !mServerSocket.isClosed()) {
                try {
                    Socket socket = mServerSocket.accept();
                    Log.i(TAG, "ServerMainThread accepted socket");
                    socket.setSoTimeout(1000);

                    Map<String, String> groups = new HashMap<>(1);
                    groups.put(secret, groupJsonString);
                    GroupPublish slaveThread = new GroupPublish(groups, socket);
                    new Thread(slaveThread).start();

                } catch (IOException e) {
                    Log.d(TAG, "ServerMainThread exception occured", e);
                }
            }
        }
    }

}
