# BackgroundAppUpdater

**This sample app will demonstrate you to how to update your app in foreground. This tutorial will also explain you how to update your app from external website using JSOUP library**

`We will use github to check app version update. Create update.md file somewhere in your git repo and put this code in it`

	<div class="content" itemprop="softwareVersion">4.0</div>


`Put this in your build.gradle file`

	// jsoup HTML parser library @ https://jsoup.org/
    implementation 'org.jsoup:jsoup:1.13.1'

`Now create a xml file named as provider_paths`

	<?xml version="1.0" encoding="utf-8"?>
	<paths>
    	<external-path name="external_files" path="."/>
	</paths>

`This will be your manifest file`

	<?xml version="1.0" encoding="utf-8"?>
	<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.myapplication">

    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
    <uses-permission android:name="android.permission.DOWNLOAD_WITHOUT_NOTIFICATION" />
    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/>

    <application
        android:label="@string/app_name"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/AppTheme"
        android:allowBackup="true"
        android:fullBackupContent="true">

        <activity android:name="com.example.myapplication.MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service android:name="com.example.myapplication.BackgroundService"
            android:exported="true"/>

        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.provider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/provider_paths"/>
        </provider>
    </application>
	</manifest>	

`Now make a java class file named as BackgroundService in your package. And please note that your apk file name should be main.apk because i used main.apk filename to check in device storage weather the app is present or not you can give any other name too but the name once given should not be changed.`

	public class BackgroundService extends Service {

    private final static int INTERVAL = 1000 * 60 * 2; //2 minutes
    Handler mHandler = new Handler();

    //This is path used for checking apk file is present or not in downloads directory
    private static File apkPath = new File(Environment.getExternalStoragePublicDirectory
            (Environment.DIRECTORY_DOWNLOADS), "main.apk"); //here i have gived app name as main.apk if your app fileName is different then //please update here also

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
                        //like i have used update.md file url. you should replace this url with your own.
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
            //Path where the apk will be downloaded. here i have gived app name as main.apk if your app fileName is different then change it here also
            //File path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "main.apk");

            //Replace with your file url where you uploaded your apk someWhere on the internet
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

`And this will be used MainActivity Java Class file`

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Request Storage Permissions On Android Version 6.0 And Above
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);
        } else {
            //Here we start the service
            startService(new Intent(this, BackgroundService.class));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {

        if (requestCode == 1) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // permission was granted, yay! Do the
                // contacts-related task you need to do.
                //Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                Log.d("PERMISSIONS","GRANTED");
                //Here we start the service
                startService(new Intent(this, BackgroundService.class));
            } else {

                // permission denied, boo! Disable the
                // functionality that depends on this permission.
                Toast.makeText(MainActivity.this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
	}		

## License
	Copyright 2020 TutorialsAndroid
	
	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at
	
	   http://www.apache.org/licenses/LICENSE-2.0
	
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.	    
