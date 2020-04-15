package com.example.myapplication;

import android.annotation.SuppressLint;
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
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;

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
            (Environment.DIRECTORY_DOWNLOADS), "main.apk");

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
        String NOTIFICATION_CHANNEL_ID = "com.example.myapplication";
        String channelName = "BackgroundApp";

        NotificationChannel notificationChannel =
                new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName,
                        NotificationManager.IMPORTANCE_NONE);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Objects.requireNonNull(manager).createNotificationChannel(notificationChannel);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);

        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();

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

    public static class FetchAppVersion
            extends AsyncTask<String, Void, String> {

        @SuppressLint("StaticFieldLeak")
        private Context context;

        FetchAppVersion(Context context) {
            this.context = context;
        }

        protected String doInBackground(String... urls)
        {
            try {
                return
                        //Put your github url where you have located update.md file in github
                        //like i have used update.md file url you should replace this url with your own.
                        Jsoup.connect("https://github.com/TutorialsAndroid/BackgroundAppUpdater/blob/master/app/update.md")
                                .timeout(10000)
                                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                                .referrer("http://www.github.com")
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
            Log.d("new Version", string);

        try
        {
            String version = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;

            //Now let's check if current app version is equals to uploaded
            //apk version.        
            if(!version.equals(string))
            {
                //Toast.makeText(context, "New version available",
                        //Toast.LENGTH_SHORT).show();

                //This method will check if app exits in storage or not
                //if it exits then delete it first and download new one
                if (apkPath.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    apkPath.delete(); //old apk deleted now go for a download task
                }
                downloadManager();
            }

        }
        catch (PackageManager.NameNotFoundException e)
        {
            //No version do something
        }

        }

        private void downloadManager()
        {
            //Path where the apk will be downloaded
            //File path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "main.apk");

            String URL = "https://github.com/TutorialsAndroid/BackgroundAppUpdater/raw/master/app/main.apk";
            DownloadManager.Request request = new DownloadManager.Request(
                    Uri.parse(URL)
            );

            //Here we will guess fileName and fileExtension
            String fileExtenstion = MimeTypeMap.getFileExtensionFromUrl(URL);
            String name = URLUtil.guessFileName(URL, null, fileExtenstion);

            // Title of the Download Notification
            request.setTitle(name);

            //TODO this method is deprecated now.
            request.setVisibleInDownloadsUi(false);

            // Description of the Download Notification
            request.setDescription("Downloading Update");

            // Uri of the destination file
            //request.setDestinationUri(Uri.fromFile(path));

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

                String channelId = "noti";
                Log.d("onDownloadComplete","Download Completed");

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    String channelName = context.getString(R.string.app_name);
                    NotificationChannel notificationChannel = new NotificationChannel(channelId,
                            channelName, NotificationManager.IMPORTANCE_LOW);
                    notificationChannel.setLightColor(Color.BLUE);
                    notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
                    NotificationManager notificationManager = (NotificationManager)
                            context.getSystemService(Context.NOTIFICATION_SERVICE);
                    if (notificationManager != null) {
                        notificationManager.createNotificationChannel(notificationChannel);
                    }

                    Uri uri = FileProvider.getUriForFile(context,
                            BuildConfig.APPLICATION_ID + ".provider",apkPath);

                    NotificationManagerCompat notificationManagerCompat =
                            NotificationManagerCompat.from(context);

                    String contentTitle = "New Update Ready To Install";
                    Intent notifyIntent = new Intent();
                    notifyIntent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                    notifyIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    notifyIntent.setData(uri);

                    PendingIntent notifyPendingIntent =
                            PendingIntent.getActivity(context, 3, notifyIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT |
                                            PendingIntent.FLAG_ONE_SHOT);

                    NotificationCompat.Builder notificationBuilder =
                            new NotificationCompat.Builder(context,channelId);
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

                    NotificationCompat.Builder b = new NotificationCompat.Builder(context,channelId);

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