/*
 * Copyright (C) 2016 Alexandru Munteanu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.almunt.jgcaap.systemupdater;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.os.ResultReceiver;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener
{
    TextView currentRomTextView;
    int refreshDialog=1;
    RecyclerView rv;
    ArrayList<String> fileLinks=new ArrayList<>();
    ArrayList<RomFile> currentfiles=new ArrayList<>();
    ArrayList<RomFile> files=new ArrayList<>();
    ArrayList<RomFile> dablefiles=new ArrayList<>();
    ArrayList<RomFile> dedfiles=new ArrayList<>();
    SwipeRefreshLayout refreshLayout;
    ProgressDialog progressDialog;
    LinearLayoutManager llm;
    CardView currentRomcardView;
    Toolbar toolbar;
    Intent intent;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        rv=(RecyclerView)findViewById(R.id.rv);
        llm = new LinearLayoutManager(this);
        rv.setLayoutManager(llm);
        rv.setAdapter(new RVAdapter(currentfiles));
        currentRomcardView=(CardView)findViewById(R.id.cardview1);
        currentRomTextView=(TextView)findViewById(R.id.text);
        currentRomTextView.setText(getOS());
        refreshLayout=(SwipeRefreshLayout)findViewById(R.id.swiperefresh);
        refreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener()
                {
                    @Override
                    public void onRefresh()
                    {
                        RefreshLinks();
                    }
                }
        );
        SetupRecyclerViewClicks();
        RefreshLinks();
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.getMenu().getItem(0).setChecked(true);
    }
    public void SetupRecyclerViewClicks()
    {
        rv.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                try {
                    int firstPos = llm.findFirstCompletelyVisibleItemPosition();
                    if (firstPos > 0) {
                        refreshLayout.setEnabled(false);
                    } else {
                        refreshLayout.setEnabled(true);
                        if(rv.getScrollState() == 1)
                            if(refreshLayout.isRefreshing())
                                rv.stopScroll();
                    }
                }catch(Exception e) {e.printStackTrace();}
            }}
        );
        rv.addOnItemTouchListener(new RecyclerItemClickListener(this, rv, new RecyclerItemClickListener.OnItemClickListener()
        {
            @Override
            public void onItemClick(View view, final int position)
            {
                if(currentfiles.size()>0)
                {
                    if(currentfiles.get(position).status<2)
                    {
                        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                        alertDialog.setTitle("Download?");
                        alertDialog.setMessage("Are sure you want to download the "+currentfiles.get(position).filename+" update?");
                        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        InitiateDownloadDialog(position);
                                    }
                                });
                        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                });

                        alertDialog.show();
                    }
                    else
                    {
                        FlashFile(currentfiles.get(position).filename);
                    }
                }
            }
            @Override
            public void onItemLongClick(View view, int position)
            {
                if(currentfiles.get(position).status>1)
                    DeleteZip(currentfiles.get(position).filename);
            }
        }));
    }
    public void InitiateDownloadDialog(int position)
    {
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setTitle("Downloading " + files.get(position).filename);
        progressDialog.setMessage("Downloading...");
        progressDialog.setCancelable(true);
        progressDialog.setIndeterminate(false);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                File temp =new File(Environment.getExternalStorageDirectory()+"/JgcaapUpdates/temp/nd");
                try
                {
                    temp.createNewFile();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, "Download Canceled",
                        Toast.LENGTH_LONG).show();
            }
        });
        progressDialog.show();
        intent = new Intent(MainActivity.this, DownloadService.class);
        intent.putExtra("url", files.get(position).filename);
        intent.putExtra("receiver", new DownloadReceiver(new Handler()));
        intent.putExtra("stop",true);
        startService(intent);
    }
    public void RefreshLinks()
    {
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.M)
        {
            refreshLayout.setRefreshing(true);
            final GetLinks g = new GetLinks();
            RefreshLinks2(g);
            refreshLayout.setRefreshing(false);
        }
        else
        {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                refreshLayout.setRefreshing(true);
                final GetLinks g = new GetLinks();
                RefreshLinks2(g);
            }
            else
            {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
            refreshLayout.setRefreshing(true);
            final GetLinks g = new GetLinks();
            RefreshLinks2(g);
            refreshLayout.setRefreshing(false);
        }
        else
        {
            AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
            builder1.setTitle("This app needs the storage permission to download and view jgcaap rom updates");
            builder1.setMessage("Tap \"Request Permission\" to try requesting the permission again or close the app");
            builder1.setCancelable(false);
            builder1.setPositiveButton(
                    "Request Permission",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                        }
                    });
            builder1.setNegativeButton(
                    "Close App",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            finish();
                        }
                    });
            AlertDialog alert11 = builder1.create();
            alert11.show();
        }
    }
    public void RefreshLinks2(GetLinks g)
    {
        fileLinks = g.links;
        files=new ArrayList<>();
        File folder = new File(Environment.getExternalStorageDirectory()+File.separator+"JgcaapUpdates");
        File tempFolder=new File(Environment.getExternalStorageDirectory()+"/JgcaapUpdates/temp");
        if(tempFolder.exists())
        {
            File[] contents = tempFolder.listFiles();
            if (contents != null) {
                for (File f : contents) {
                    f.delete();
                }
            }
            tempFolder.delete();
        }
        File[] downloadedFiles =folder.listFiles();
        if(downloadedFiles!=null)
        for(int i=0;i<downloadedFiles.length;i++)
            if(downloadedFiles[i].getName().startsWith("cm-13")&&downloadedFiles[i].getName().endsWith(".zip"))
                files.add(new RomFile(downloadedFiles[i].getName(), 2));
        if(files.size()>0)
        {
            rv.setAdapter(new RVAdapter(files));
        }
        if (fileLinks.get(0).startsWith("No Internet"))
        {
            Toast.makeText(MainActivity.this, "No Internet Connection Detected",
                    Toast.LENGTH_LONG).show();
        }
        else
        {
            for (int i = 0; i < fileLinks.size(); i++)
            {
                if (RomExists(fileLinks.get(i))==false)
                    files.add(new RomFile(fileLinks.get(i), 1));
            }
        }
        Collections.sort(files, new Comparator<RomFile>() {
            @Override
            public int compare(RomFile rom2, RomFile rom1)
            {
                return  rom1.filename.compareTo(rom2.filename);
            }
        });
        currentfiles=files;
        rv.setAdapter(new RVAdapter(currentfiles));
        refreshLayout.setRefreshing(false);
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.getMenu().getItem(0).setChecked(true);
    }
    @Override
    public void onBackPressed()
    {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START))
        {
            drawer.closeDrawer(GravityCompat.START);
        } else
        {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if (id == R.id.howmenu)
        {
            About(false);
            return true;
        }
        else if(id==R.id.aboutmenu)
        {
            About(true);
            return true;
        }
        else if(id==R.id.rebootmenu)
        {
            RebootRecovery(false);
            return true;
        }
        else if(id==R.id.licenses)
        {
            LicencesDialog();
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item)
    {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_all)
        {
            rv.setAdapter(new RVAdapter(files));
            currentfiles=files;
            toolbar.setTitle("System Update");
        } else if (id == R.id.nav_download)
        {
            dablefiles=new ArrayList<>();
            for(int i=0;i<files.size();i++)
                if(files.get(i).status<2)
                    dablefiles.add(files.get(i));
            currentfiles=dablefiles;
            rv.setAdapter(new RVAdapter(dablefiles));
            toolbar.setTitle("Downloadable Updates");
        } else if (id == R.id.nav_cell)
        {
            dedfiles=new ArrayList<>();
            for(int i=0;i<files.size();i++)
                if(files.get(i).status>1)
                    dedfiles.add(files.get(i));
            currentfiles=dedfiles;
            rv.setAdapter(new RVAdapter(dedfiles));
            toolbar.setTitle("Downloaded Updates");
        }
        else if (id == R.id.nav_reboot)
        {
            RebootRecovery(false);
        } else if (id == R.id.nav_how)
        {
            About(false);
        } else if (id == R.id.nav_about)
        {
            About(true);
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    public String getOS()
    {
        String outputString="";
        try{
            Process su = Runtime.getRuntime().exec("sh");
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());
            InputStream inputStream = su.getInputStream();
            outputStream.writeBytes("getprop ro.cm.version"+"\nprint alemun@romania\n");
            outputStream.flush();
            outputString="";
            while (outputString.endsWith("alemun@romania\n")==false)
            {
                if(inputStream.available()>0)
                {
                    byte[] dstInput= new byte[inputStream.available()];
                    inputStream.read(dstInput);
                    String additionalString = new String(dstInput);
                    outputString +=additionalString;
                }
            }
            outputString="Current ROM: "+outputString.substring(0,outputString.length()-15);
            su.destroy();
        }
        catch(IOException e){
            currentRomcardView.setVisibility(CardView.GONE);
        }
        if(outputString.replaceAll("jgcaap","").length()<outputString.length())
            while(outputString.endsWith("bacon")==false)
                outputString=outputString.substring(0,outputString.length()-1);
        else
            currentRomcardView.setVisibility(CardView.GONE);
        return outputString;
    }
    public boolean RomExists(String link)
    {
        String dir = Environment.getExternalStorageDirectory()+File.separator+"JgcaapUpdates";
        File folder = new File(dir);
        folder.mkdirs();
        File file = new File(dir, link);
        if(file.exists())
            return true;
        else
            return false;
    }
    @SuppressLint("ParcelCreator")
    private class DownloadReceiver extends ResultReceiver{
        public DownloadReceiver(Handler handler) {
            super(handler);
        }
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);
            if (resultCode == DownloadService.UPDATE_PROGRESS) {
                double progress = resultData.getInt("progress");
                String filename=resultData.getString("filename");
                double total=resultData.getInt("total");
                double percent=(progress/total)*100;
                refreshDialog++;
                if(refreshDialog%100==0)
                {
                    progressDialog.setMessage(round(progress / 1000000, 2) + "MB of " + round(total / 1000000, 2) + "MB downloaded");
                    progressDialog.setProgress((int) percent);
                }
                if(resultData.getBoolean("error",false))
                {
                    progressDialog.dismiss();
                    String error=resultData.getString("errordetails");
                    File file=new File(Environment.getExternalStorageDirectory()+"/JgcaapUpdates/temp/"+filename.replaceAll("zip","temp"));
                    file.delete();
                    RetryDownload(filename, error);
                }
                if ((int)percent == 100) {
                    progressDialog.dismiss();
                    RefreshLinks();
                    FlashFile(filename);
                }
            }
        }
    }
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
    public void RetryDownload(final String filename, String errordetails)
    {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
        builder1.setTitle("There was an error downloading");
        builder1.setMessage("Would you like to retry the download?\n"+"Error Details:\n"+errordetails);
        builder1.setCancelable(true);
        builder1.setPositiveButton(
                "Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        progressDialog = new ProgressDialog(MainActivity.this);
                        progressDialog.setTitle("Downloading "+filename);
                        progressDialog.setMessage("Initializing Download...");
                        progressDialog.setIndeterminate(false);
                        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        progressDialog.show();
                        Intent intent = new Intent(MainActivity.this, DownloadService.class);
                        intent.putExtra("url", filename);
                        intent.putExtra("receiver", new DownloadReceiver(new Handler()));
                        startService(intent);
                    }
                });

        builder1.setNegativeButton(
                "No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        AlertDialog alert11 = builder1.create();
        alert11.show();
    }
    public void FlashFile(final String filename)
    {
        final boolean[] mayBeContinued = {true};
        AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
        builder1.setTitle("Flashing Confirmation");
        builder1.setMessage("Are You sure you want to flash this file?\nIt will be added to the OpenRecoveryScript file and TWRP will automatically flash it without a warning!");
        builder1.setCancelable(true);
        builder1.setPositiveButton(
                "Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        try{
                            Process su = Runtime.getRuntime().exec("su");
                            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());
                            outputStream.writeBytes("cd /cache/recovery/\n");
                            outputStream.flush();
                            outputStream.writeBytes("rm openrecoveryscript\n");
                            outputStream.flush();
                            outputStream.writeBytes("echo install "+Environment.getExternalStorageDirectory()+"/JgcaapUpdates/"+filename+">openrecoveryscript\n");
                            outputStream.flush();
                            outputStream.writeBytes("exit\n");
                            outputStream.flush();
                            su.waitFor();
                        }
                        catch(IOException e){
                            Toast.makeText(MainActivity.this, "Error No Root Access detected",
                                    Toast.LENGTH_LONG).show();
                            mayBeContinued[0] =false;
                        }
                        catch(InterruptedException e){
                            Toast.makeText(MainActivity.this, "Error No Root Access detected",
                                    Toast.LENGTH_LONG).show();
                            mayBeContinued[0] =false;
                        }
                        if(mayBeContinued[0])
                        {
                            RebootRecovery(true);
                        }
                    }
                });
        builder1.setNegativeButton(
                "No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        AlertDialog alert11 = builder1.create();
        alert11.show();
    }
    public void RebootRecovery(boolean update)
    {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
        builder1.setTitle("Recovery Reboot");
        if(update)
        builder1.setMessage("Would you like to reboot into recovery now to complete update?\nClear ORS will delete the OpenRecoveryScript to stop TWRP from automatically installing any files.");
        else
            builder1.setMessage("Would you like to reboot into recovery now.\n" +
            "Clear ORS will delete the current OpenRecoveryScript and stop any automatic update installations in TWRP");
        builder1.setCancelable(true);
        builder1.setPositiveButton(
                "Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        try{
                            Process su = Runtime.getRuntime().exec("su");
                            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());
                            outputStream.writeBytes("reboot recovery\n");
                            outputStream.flush();
                            outputStream.writeBytes("exit\n");
                            outputStream.flush();
                            su.waitFor();
                        }
                        catch(IOException e){
                            Toast.makeText(MainActivity.this, "Error No Root Access detected",
                                    Toast.LENGTH_LONG).show();
                        }
                        catch(InterruptedException e){
                            Toast.makeText(MainActivity.this, "Error No Root Access detected",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
        builder1.setNeutralButton(
                "Clear ORS",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        try{
                            Process su = Runtime.getRuntime().exec("su");
                            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());
                            outputStream.writeBytes("cd /cache/recovery/\n");
                            outputStream.flush();
                            outputStream.writeBytes("rm openrecoveryscript\n");
                            outputStream.flush();
                            outputStream.writeBytes("exit\n");
                            outputStream.flush();
                            su.waitFor();
                            Toast.makeText(MainActivity.this, "OpenRecoveryScript file was cleared",
                                    Toast.LENGTH_LONG).show();
                        }
                        catch(IOException e){
                            Toast.makeText(MainActivity.this, "Error No Root Access detected",
                                    Toast.LENGTH_LONG).show();
                        }
                        catch(InterruptedException e){
                            Toast.makeText(MainActivity.this, "Error No Root Access detected",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
        builder1.setNegativeButton(
                "Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        if(update)
        builder1.setNegativeButton(
                "Reboot Later",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });

        AlertDialog alert11 = builder1.create();
        alert11.show();
    }
    public void About(boolean about)
    {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
        if(about)
        {
            builder1.setMessage("The updater is designed and developed by AlMunt for the latest cm13 based jgcaap rom. I am NOT responsible for any damage caused to this device\nGo to Jgcaap's xda page for the cm13 based Oneplus One rom for support on the downloaded files.\nEmail me at munt.alexandru@gmail.com for feedback on this app.");
            builder1.setTitle("About");
        }
        else
        {
            builder1.setTitle("How to Use");
            builder1.setMessage(" Swipe down to get latest available updates" +
                    " Once a rom is selected it will immediately start downloading. You may delete a rom by long pressing it and tapping \"delete\"." +
                    " All downloads are stored in a Jgcaap folder in the sdcard. You will have an option to use automatic installation in TWRP with OpenRecoveryScript." +
                    " You may clear the OpenRecoveryScript file through the \"Reboot into recovery\" menu in the slide out navigation bar or the \"3 dots\"." +
                    "\nReboot into recovery to run the OpenRecoveryScript." +
                    "Note: The storage permission needs to be accepted in order to download updates to the sdcard and root access is required if you want automatic installation of the rom through TWRP.");
        }
        builder1.setCancelable(true);
        builder1.setPositiveButton(
                "Close",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        AlertDialog alert11 = builder1.create();
        alert11.show();
    }
    public void DeleteZip(final String filename)
    {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
        builder1.setTitle("Delete Zip?");
        builder1.setMessage("Are you sure you want to delete "+filename);
        builder1.setCancelable(true);
        builder1.setPositiveButton(
                "Delete",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    File file=new File(Environment.getExternalStorageDirectory()+"/JgcaapUpdates/"+filename);
                        file.delete();
                        RefreshLinks();
                    }
                });
        builder1.setNegativeButton(
                "Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        AlertDialog alert11 = builder1.create();
        alert11.show();
    }
    public void LicencesDialog()
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Open Source Licenses");
        WebView wv = new WebView(this);
        wv.loadUrl("file:///android_asset/open_source_licenses.html");
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
        alert.setView(wv);
        alert.setPositiveButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        alert.show();
    }
}