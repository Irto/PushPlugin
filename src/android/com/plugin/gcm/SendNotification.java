package com.plugin.gcm;

import java.io.InputStream;
import java.io.IOException;

import java.net.URL;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;

import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.os.Build;
 
import com.google.android.gcm.GCMBaseIntentService;

public class SendNotification extends AsyncTask<String, Void, Bitmap> {

    private static final String TAG = "SendNotification";

    int notId;
    NotificationCompat.Builder mBuilder;
    GCMBaseIntentService service;
    NotificationManager mNotificationManager;

    public SendNotification(Context context, Bundle extras, GCMBaseIntentService service) {
        super();
        this.service = service;
        this.createNotification(context, extras, service);
    }

    public void createNotification(Context context, Bundle extras, GCMBaseIntentService service)
    {
        notId = 0;
        
        try {
            notId = Integer.parseInt(extras.getString("notId", Integer.toString(new Random().nextInt(1000))));
        } catch(NumberFormatException e) {
            Log.e(TAG, "Number format exception - Error parsing Notification ID: " + e.getMessage());
        } catch(Exception e) {
            Log.e(TAG, "Number format exception - Error parsing Notification ID" + e.getMessage());
        }

        mNotificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent notificationIntent = new Intent(service, PushHandlerActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        notificationIntent.putExtra("pushBundle", extras);

        PendingIntent contentIntent = PendingIntent.getActivity(service, notId, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        
        int defaults = Notification.DEFAULT_ALL;

        if (extras.getString("defaults") != null) {
            try {
                defaults = Integer.parseInt(extras.getString("defaults"));
            } catch (NumberFormatException e) {}
        }
        
        mBuilder =
            new NotificationCompat.Builder(context)
                .setDefaults(defaults)
                .setSmallIcon(context.getApplicationInfo().icon)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(extras.getString("title"))
                .setTicker(extras.getString("title"))
                .setContentIntent(contentIntent)
                .setAutoCancel(true);

        String message = extras.getString("message");
        if (message != null) {
            mBuilder.setContentText(message);

            if (Build.VERSION.SDK_INT > 16) {
                mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
            }
        } else {
            mBuilder.setContentText("<missing message content>");
        }

        String msgcnt = extras.getString("msgcnt");
        if (msgcnt != null) {
            mBuilder.setNumber(Integer.parseInt(msgcnt));
        }

    }
    
    private static String getAppName(Context context) {
        CharSequence appName = 
                context
                    .getPackageManager()
                    .getApplicationLabel(context.getApplicationInfo());
        
        return (String)appName;
    }

    @Override
    protected Bitmap doInBackground(String... params) {
        InputStream in;

        String largeIcon = params[0];
        if (largeIcon != null) {
            try {
                URL url = new URL(largeIcon);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                in = connection.getInputStream();
                Bitmap myBitmap = BitmapFactory.decodeStream(in);
                return myBitmap;

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        String appName = getAppName(this.service);

        mBuilder.setLargeIcon(result);

        mNotificationManager.notify((String) appName, notId, mBuilder.build());
    }

}