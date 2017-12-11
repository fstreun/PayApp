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
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import ch.ethz.inf.vs.fstreun.payapp.DataService;

/**
 * Created by Kaan on 30.11.17.
 */

public class DataSyncPublishService extends Service {

    String TAG = "DataSyncPublishService";
    String SERVICE_TYPE = "_http._tcp.";
    String SERVICE_NAME = "DataSyncPublisher";
    private NsdManager mNsdManager;
    private NsdServiceInfo mServiceInfo;
    private String mServiceName;
    private ServerSocket mServerSocket;
    private int mLocalPort;
    private NsdManager.RegistrationListener mRegistrationListener;

    DataService.LocalBinder dataServiceBinder;
    boolean bound = false;

    @Override
    public void onCreate() {
        super.onCreate();
        // Bind DataService
        Intent intent = new Intent(this, DataService.class);
        bindService(intent, connection, BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

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
        if (mServerSocket != null) {
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

    class ServerMasterThread implements Runnable {

        private final static String TAG = "ServerMasterThread";

        public void run() {
            Log.i("ServerMasterThread", "run()");
            while (mServerSocket != null) {

                try {
                    Log.i(TAG, "run() - open RequestThread");
                    Socket socket = mServerSocket.accept();
                    // start slave thread
                    ServerSlaveThread slaveThread = new ServerSlaveThread(socket);
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

    class ServerSlaveThread implements Runnable{

        private final static String TAG = "ServerSlaveThread";

        private final Socket mSocket;

        ServerSlaveThread(Socket socket) {
            this.mSocket = socket;
        }

        @Override
        public void run() {

            try {

                if (!bound){
                    // session access service not available
                    mSocket.close();
                    return;
                }

                BufferedReader input = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));

                // parse all header fields
                String result = "";
                String line;
                while ((line = input.readLine()) != null) {
                    if (line.isEmpty()) {
                        Log.i(TAG, result);
                        break;
                    } else {
                        result = result + line + "\r\n";
                    }
                }

                BufferedWriter output = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));

                PrintWriter wtr = new PrintWriter(output);

                // Create Here Response String (favourably JSON)
                wtr.print(generateResponse("Success"));
                wtr.flush();
                wtr.close();
            }catch (IOException e){

            }
        }

        public String generateResponse(String body) {
            String response =    "HTTP/1.1 200 OK\r\n" +
                    "Content-Length: " + body.length() + "\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "Connection: Closed\r\n\r\n" + body;
            return response;
        }

    }
}
