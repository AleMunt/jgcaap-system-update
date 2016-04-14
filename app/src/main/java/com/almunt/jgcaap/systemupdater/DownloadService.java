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

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.os.ResultReceiver;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;

public class DownloadService extends IntentService
{
    public static final int UPDATE_PROGRESS = 8344;
    public String filename;
    public boolean continuedownload=true;
    public DownloadService() {
        super("DownloadService");
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        String urlToDownload = "http://download.jgcaap.xyz/files/oneplusone/cm-13.0/"+intent.getStringExtra("url");
        filename=intent.getStringExtra("url");
        String newfilename=filename.replace("zip","temp");
        ResultReceiver receiver = (ResultReceiver) intent.getParcelableExtra("receiver");
        try {
            URL url = new URL(urlToDownload);
            URLConnection connection = url.openConnection();
            connection.connect();
            // this will be useful so that you can show a typical 0-100% progress bar
            int fileLength = connection.getContentLength();
            // download the file
            InputStream input = new BufferedInputStream(connection.getInputStream());
            File tempDir=new File(Environment.getExternalStorageDirectory()+"/JgcaapUpdates/temp/");
            tempDir.mkdir();
            //make a folder for downloading files
            OutputStream output = new FileOutputStream(Environment.getExternalStorageDirectory()+"/JgcaapUpdates/temp/"+newfilename);
            byte data[] = new byte[1024];
            long total = 0;
            int count;
            File temp =new File(Environment.getExternalStorageDirectory()+"/JgcaapUpdates/temp/nd");
            //temp file is created in case of an error
            while ((count = input.read(data)) != -1&&continuedownload) {
                total += count;
                // publishing the progress....
                Bundle resultData = new Bundle();
                resultData.putInt("progress" , (int) total);
                resultData.putInt("total" , fileLength);
                resultData.putString("filename" , filename);
                resultData.putBoolean("error",false);
                receiver.send(UPDATE_PROGRESS, resultData);
                output.write(data, 0, count);
                if(temp.exists())
                {
                    continuedownload=false;
                    temp.delete();
                }
            }
            output.flush();
            output.close();
            input.close();
        }
        catch (IOException e)
        {
            System.out.println(e.getMessage());
            Bundle resultData = new Bundle();
            resultData.putInt("progress" , 0);
            resultData.putInt("total" , 1);
            resultData.putString("filename" , filename);
            resultData.putBoolean("error", true);
            resultData.putString("errordetails",e.getMessage());
            receiver.send(UPDATE_PROGRESS, resultData);
        }
        String dir = Environment.getExternalStorageDirectory() + File.separator + "JgcaapUpdates";
        // copy downloaded file if download is successful
        if(continuedownload)
        {
            try
            {
                if(new File(Environment.getExternalStorageDirectory() + "/JgcaapUpdates/temp/" + newfilename).length()>1000)
                    copy(new File(Environment.getExternalStorageDirectory() + "/JgcaapUpdates/temp/" + newfilename), new File(Environment.getExternalStorageDirectory() + "/JgcaapUpdates/" + filename));

            } catch (IOException e)
            {
                e.printStackTrace();
            }
            Bundle resultData = new Bundle();
            resultData.putInt("progress", 100);
            receiver.send(UPDATE_PROGRESS, resultData);
        }
        //clear the temporary folder
        File deletefolder = new File(dir + "/temp");
        if (deletefolder.exists())
        {
            File[] contents = deletefolder.listFiles();
            if (contents != null)
            {
                for (File f : contents)
                {
                    f.delete();
                }
            }
        }
        deletefolder.delete();
    }
    public void copy(File src, File dst) throws IOException {
        FileInputStream inStream = new FileInputStream(src);
        FileOutputStream outStream = new FileOutputStream(dst);
        FileChannel inChannel = inStream.getChannel();
        FileChannel outChannel = outStream.getChannel();
        inChannel.transferTo(0, inChannel.size(), outChannel);
        inStream.close();
        outStream.close();
    }
}