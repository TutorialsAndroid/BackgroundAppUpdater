package com.example.myapplication;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.Notification;
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
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.jsoup.Jsoup;

import java.io.File;
import java.util.Objects;

public class BackgroundService extends Service {

    private final static int INTERVAL = 1000 * 60 * 2; //2 minutes
    Handler mHandler = new Handler();

    private static File apkPath = new File(Environment.getExternalStoragePublicDirectory
            (Environment.DIRECTORY_DOWNLOADS), "app-debug.apk");

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
                        Jsoup.connect("http://github.com/TutorialsAndroid/App/blob/master/README.md")
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
            //File path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "app-debug.apk");

            String URL = "https://github.com/TutorialsAndroid/App/raw/master/app-debug.apk";
            DownloadManager.Request request = new DownloadManager.Request(
                    Uri.parse(URL)
            );

            //Here we will guess fileName and fileExtension
            String url = URL;
            String fileExtenstion = MimeTypeMap.getFileExtensionFromUrl(url);
            String name = URLUtil.guessFileName(url, null, fileExtenstion);

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
            public void onReceive(Context ctxt, Intent intent) {
                

                intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setData(Uri.fromFile(apkPath));

                PendingIntent contentIntent = PendingIntent.getActivity(ctxt, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                NotificationCompat.Builder b = new NotificationCompat.Builder(ctxt);

                b.setAutoCancel(true)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setWhen(System.currentTimeMillis())
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Update Complete")
                        .setContentText("Install the app now to latest version")
                        .setDefaults(Notification.DEFAULT_LIGHTS| Notification.DEFAULT_SOUND)
                        .setContentIntent(contentIntent)
                        .setContentInfo("Info");

                NotificationManager notificationManager = (NotificationManager) ctxt.getSystemService(Context.NOTIFICATION_SERVICE);
                Objects.requireNonNull(notificationManager).notify(1, b.build());
            }
        };
    }
}