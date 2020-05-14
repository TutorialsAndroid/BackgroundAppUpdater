package com.example.myapplication;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;

import org.jsoup.Jsoup;

import java.io.File;
import java.util.Objects;

public class BackgroundService extends Service {

    private final static int INTERVAL = 1000 * 60 * 2; //2 minutes
    Handler mHandler = new Handler();

    //This is path used for checking apk file is present or not in downloads directory
    private static File apkPath = new File(Environment.getExternalStoragePublicDirectory
            (Environment.DIRECTORY_DOWNLOADS), "main.apk" /* Replace with your apk name */);

    @Override
    public void onCreate(){
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground(2, new Notification());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startMyOwnForeground(){
        String NOTIFICATION_CHANNEL_ID = "channel1"; //CHANNEL ID
        String channelName = "BackgroundApp";  //CHANNEL NAME

        //Create a notification channel
        NotificationChannel notificationChannel =
                new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName,
                        NotificationManager.IMPORTANCE_NONE);

        NotificationManager manager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        Objects.requireNonNull(manager).createNotificationChannel(notificationChannel);

        //Create Notification To Display
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();

        //RUN NOTIFICATION IN FOREGROUND
        startForeground(2, notification);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        // do your jobs here
        startTask();
        return super.onStartCommand(intent, flags, startId);

    }

    Runnable mHandlerTask = new Runnable()
    {
        @Override
        public void run()
        {
            new FetchAppVersion(BackgroundService.this).execute();
            mHandler.postDelayed(mHandlerTask, INTERVAL);
        }
    };

    void startTask()
    {
        mHandlerTask.run();
    }

    /**
     * This Class Help To To Fetch The App Update If It Is
     * Available Or Not And If New Version Is Available Go
     * For A Download Task.
     */
    public static class FetchAppVersion
            extends AsyncTask<String, Void, String> {

        private Context context;

        FetchAppVersion(Context context) {
            this.context = context;
        }

        protected String doInBackground(String... urls)
        {
            try {
                return
                        //Put your github url where you have located update.md file in github
                        //like i have used update.md file url you should replace this url with
                        // your own url.
                        Jsoup.connect(Util.UPDATE_MD_FILE_URL)
                                .timeout(10000)
                                .userAgent(Util.USER_AGENT)
                                .referrer(Util.REFERRER)
                                .get()
                                .select("div[itemprop=softwareVersion]")
                                .first()
                                .ownText();

            } catch (Exception e) {
                return "";
            }
        }

        protected void onPostExecute(String string)
        {
            super.onPostExecute(string);
        try
        {
            String version = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;

            //Now let's check if current app version is equals to uploaded
            //apk version. I mean it will check update.md file weather its
            //version is greater than your current app version
            if(!version.equals(string))
            {
                Log.d("new Version", string);

                File file = new File(apkPath.getPath());
                if (file.exists()) {
                    System.out.println("file path :" + apkPath.getPath());

                    //Now here files exits then show the notification to user
                    String NOTIFICATION_CHANNEL_ID = "channel2";
                    String CHANNEL_NAME = "BackgroundApp";  //CHANNEL NAME

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        NotificationChannel notificationChannel =
                                new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                                        CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);

                        NotificationManager notificationManager = (NotificationManager)
                                context.getSystemService(Context.NOTIFICATION_SERVICE);
                        if (notificationManager != null) {
                            notificationManager.createNotificationChannel(notificationChannel);
                        }

                        //This method is for launching app from specified path this method
                        //is only used android newer version if you delete this then you
                        //will face app crashing issue when download complete notification
                        //is shown.
                        Uri uri = FileProvider.getUriForFile(context,
                                BuildConfig.APPLICATION_ID + ".provider",apkPath);

                        NotificationManagerCompat notificationManagerCompat =
                                NotificationManagerCompat.from(context);

                        String contentTitle = "New Update Ready To Install";
                        Intent notifyIntent;
                        notifyIntent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                        notifyIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        notifyIntent.setData(uri);

                        PendingIntent notifyPendingIntent =
                                PendingIntent.getActivity(context, 3, notifyIntent,
                                        PendingIntent.FLAG_UPDATE_CURRENT |
                                                PendingIntent.FLAG_ONE_SHOT);

                        NotificationCompat.Builder notificationBuilder =
                                new NotificationCompat.Builder(context,NOTIFICATION_CHANNEL_ID);
                        notificationBuilder.setContentIntent(notifyPendingIntent);
                        notificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
                        notificationBuilder.setContentTitle(contentTitle);
                        notificationManagerCompat.notify(4, notificationBuilder.build());
                    } else {
                        Intent intent;
                        intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.setData(Uri.fromFile(apkPath));

                        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                                intent, PendingIntent.FLAG_UPDATE_CURRENT);

                        NotificationCompat.Builder b = new NotificationCompat.Builder(context,NOTIFICATION_CHANNEL_ID);

                        b.setAutoCancel(true)
                                .setDefaults(Notification.DEFAULT_ALL)
                                .setWhen(System.currentTimeMillis())
                                .setSmallIcon(R.mipmap.ic_launcher)
                                .setContentTitle("Update Complete")
                                .setContentText("Install the app now to latest version")
                                .setDefaults(Notification.DEFAULT_LIGHTS| Notification.DEFAULT_SOUND)
                                .setContentIntent(contentIntent)
                                .setContentInfo("Info");

                        NotificationManager notificationManager = (NotificationManager)
                                context.getSystemService(Context.NOTIFICATION_SERVICE);
                        Objects.requireNonNull(notificationManager).notify(4, b.build());
                    }
                } else {
                    downloadManager();
                }
            }
        }
        catch (PackageManager.NameNotFoundException e)
        {
            //No version do something
            Log.d("noVersion","No version found");
        }

        }

        private void downloadManager()
        {
            DownloadManager.Request request = new DownloadManager.Request(
                    Uri.parse(Util.DOWNLOAD_APK_URL)
            );

            //Here we will guess fileName and fileExtension
            String fileExtensionFromUrl =
                    MimeTypeMap.getFileExtensionFromUrl(Util.DOWNLOAD_APK_URL);
            String name = URLUtil.guessFileName(Util.DOWNLOAD_APK_URL,
                    null, fileExtensionFromUrl);

            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,name);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);

            DownloadManager downloadManager= (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
            // enqueue puts the download request in the queue.
            Objects.requireNonNull(downloadManager).enqueue(request);

            //This will check that apk download has completed.
            context.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }

        //This method will start a notification service when apk has been downloaded.
        BroadcastReceiver onComplete = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                Log.d("onDownloadComplete","Download Completed");

                String NOTIFICATION_CHANNEL_ID = "channel2";
                String CHANNEL_NAME = "BackgroundApp";  //CHANNEL NAME

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    NotificationChannel notificationChannel =
                            new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                            CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);

                    NotificationManager notificationManager = (NotificationManager)
                            context.getSystemService(Context.NOTIFICATION_SERVICE);
                    if (notificationManager != null) {
                        notificationManager.createNotificationChannel(notificationChannel);
                    }

                    //This method is for launching app from specified path this method
                    //is only used android newer version if you delete this then you
                    //will face app crashing issue when download complete notification
                    //is shown.
                    Uri uri = FileProvider.getUriForFile(context,
                            BuildConfig.APPLICATION_ID + ".provider",apkPath);

                    NotificationManagerCompat notificationManagerCompat =
                            NotificationManagerCompat.from(context);

                    String contentTitle = "New Update Ready To Install";
                    Intent notifyIntent;
                    notifyIntent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                    notifyIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    notifyIntent.setData(uri);

                    PendingIntent notifyPendingIntent =
                            PendingIntent.getActivity(context, 3, notifyIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT |
                                            PendingIntent.FLAG_ONE_SHOT);

                    NotificationCompat.Builder notificationBuilder =
                            new NotificationCompat.Builder(context,NOTIFICATION_CHANNEL_ID);
                    notificationBuilder.setContentIntent(notifyPendingIntent);
                    notificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
                    notificationBuilder.setContentTitle(contentTitle);
                    notificationManagerCompat.notify(4, notificationBuilder.build());
                } else {
                    intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setData(Uri.fromFile(apkPath));

                    PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                            intent, PendingIntent.FLAG_UPDATE_CURRENT);

                    NotificationCompat.Builder b = new NotificationCompat.Builder(context,NOTIFICATION_CHANNEL_ID);

                    b.setAutoCancel(true)
                            .setDefaults(Notification.DEFAULT_ALL)
                            .setWhen(System.currentTimeMillis())
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle("Update Complete")
                            .setContentText("Install the app now to latest version")
                            .setDefaults(Notification.DEFAULT_LIGHTS| Notification.DEFAULT_SOUND)
                            .setContentIntent(contentIntent)
                            .setContentInfo("Info");

                    NotificationManager notificationManager = (NotificationManager)
                            context.getSystemService(Context.NOTIFICATION_SERVICE);
                    Objects.requireNonNull(notificationManager).notify(4, b.build());
                }
            }
        };
    }
}