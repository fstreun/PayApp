package ch.ethz.inf.vs.fstreun.network;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import ch.ethz.inf.vs.fstreun.payapp.R;


public class SessionPublishService extends Service {

    String TAG = "## SessionPublishService";
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

    public SessionPublishService() {}

    @Override
    public IBinder onBind(Intent intent) {
        // Return the communication channel to the service.
        return mBinder;
    }

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public void setSecretGroup(String secret, String groupJSON){
            SessionPublishService.this.secret = secret;
            SessionPublishService.this.groupJsonString = groupJSON;
            Log.d(TAG, "setSecretGroup: " + secret + "\n" + groupJSON);
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        SessionPublishService.this.secret = "";
        SessionPublishService.this.groupJsonString = "";
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
        // get last used port
        SharedPreferences preferences = getSharedPreferences(getString(R.string.network_pref), MODE_PRIVATE);
        int oldPort = preferences.getInt(getString(R.string.sessionpublish_serverport), 0);

        // Initialize a server socket on last used port.
        if (mServerSocket == null) {
            try {
                mServerSocket = new ServerSocket(oldPort);
            } catch (IOException e) {
                Log.e(TAG, "FATAL ERROR: failed to initialized server socket.", e);
                preferences.edit().putInt(getString(R.string.sessionpublish_serverport), 0).apply();
                return;
            }
        }

        // Store the chosen port.
        mLocalPort = mServerSocket.getLocalPort();

        if (mLocalPort != oldPort){
            preferences.edit().putInt(getString(R.string.sessionpublish_serverport), mLocalPort).apply();
        }
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

                    ServerSlaveThread slaveThread = new ServerSlaveThread(socket);
                    new Thread(slaveThread).start();
                } catch (IOException e) {
                    Log.d(TAG, "ServerMainThread exception occured", e);
                }
            }
        }
    }

    class ServerSlaveThread implements Runnable {

        private final Socket socket;

        ServerSlaveThread(Socket socket){
            this.socket = socket;
        }

        @Override
        public void run() {

                try {

                    BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    String jsonBodyString = parseRequestForBody(input);

                    JSONObject mJson = null;
                    String key = "";
                    mJson = new JSONObject(jsonBodyString);
                    key = mJson.getString("secret");

                    JSONObject response = new JSONObject();
                    if (key.equals(secret)) {
                        response.put("result", true);
                        response.put("group", groupJsonString);
                    } else {
                        response.put("result", false);
                    }

                    BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                    PrintWriter wtr = new PrintWriter(output);
                    wtr.print(generateResponse(response.toString()));
                    wtr.flush();
                    wtr.close();

                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
        }

        public String generateResponse(String body) {
            String response =   "HTTP/1.1 200 OK\r\n" +
                                "Content-Length: " + body.length() + "\r\n" +
                                "Content-Type: application/json\r\n" +
                                "Connection: Closed\r\n" + body + "\r\n";
            return response;
        }

        public String parseRequestForBody(BufferedReader input) {
            // parse all header fields

            try {
                String requestLine = input.readLine();;

                if (requestLine == null || requestLine.isEmpty()) {
                    return "";
                }

                String hostLine = input.readLine();
                if (hostLine == null || hostLine.isEmpty()) {
                    return "";
                }

                String acceptLine = input.readLine();
                if (acceptLine == null || acceptLine.isEmpty()) {
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
    }
}
