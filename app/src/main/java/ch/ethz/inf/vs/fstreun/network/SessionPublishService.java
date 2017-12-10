package ch.ethz.inf.vs.fstreun.network;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
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

public class SessionPublishService extends Service {

    String TAG = "SessionPublishService";
    String SERVICE_TYPE = "_http._tcp.";
    String SERVICE_NAME = "SessionPublisher";
    private NsdManager mNsdManager;
    private NsdServiceInfo mServiceInfo;
    private String mServiceName;
    private ServerSocket mServerSocket;
    private int mLocalPort;
    private NsdManager.RegistrationListener mRegistrationListener;
    private String secret;
    private String groupJsonString;

    public SessionPublishService() {}

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        secret = intent.getStringExtra("SECRET");
        groupJsonString = intent.getStringExtra("SIMPLEGROUP");

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
            new Thread(new ServerThread()).start();
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

    class ServerThread implements Runnable {

        Thread mThread;

        public void run() {
            Log.i("ServerMasterThread", "run()");
            while (mServerSocket != null) {
                try {
                    Log.i("ServerMasterThread", "run() - open RequestThread");
                    Socket mSocket = mServerSocket.accept();

                    BufferedReader input = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));

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

                    BufferedWriter output = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));

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
