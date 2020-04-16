package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import org.jsoup.Jsoup;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    //This is path used for checking apk file is present or not in downloads directory
    private static File apkPath = new File(Environment.getExternalStoragePublicDirectory
            (Environment.DIRECTORY_DOWNLOADS), "main.apk" /* Replace with your apk name */);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Util.isConnectedToInternet(this);

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

        new DeleteOldApk(this).execute();
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

    //This method will delete the old apk when
    //user installs the update after update is installed
    //the old apk file will be deleted.
    public static class DeleteOldApk
            extends AsyncTask<String, Void, String> {

        private Context context;

        DeleteOldApk(Context context) {
            this.context = context;
        }

        protected String doInBackground(String... urls) {
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

        protected void onPostExecute(String string) {
            super.onPostExecute(string);
            try {
                String version = context.getPackageManager()
                        .getPackageInfo(context.getPackageName(), 0).versionName;

                //Now let's check if current app version is equals to uploaded
                //apk version. I mean it will check update.md file weather its
                //version is greater than your current app version
                if (version.equals(string)) {
                    File fdelete = new File(apkPath.getPath());
                    if (fdelete.exists()) {
                        if (fdelete.delete()) {
                            System.out.println("file Deleted :" + apkPath.getPath());
                        } else {
                            System.out.println("file not Deleted :" + apkPath.getPath());
                        }
                    }
                } else {
                    Log.d("new Version found", string);
                }
            } catch (PackageManager.NameNotFoundException e) {
                //No version do something
                Log.d("noVersion", "No version found");
            }

        }
    }
}