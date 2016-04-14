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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

public class GetLinks {
    ArrayList<String> links =new ArrayList<>();
    public GetLinks()
    {
        LinkGenerator(GetHTMLSource("http://download.jgcaap.xyz/files/oneplusone/cm-13.0/").replace("><",">\n<"));
    }
    public String GetHTMLSource(String u) {
        URL url;
        InputStream is = null;
        BufferedReader br;
        String line;
        String htmlString="";
        try {
            url = new URL(u);
            is = url.openStream();
            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                htmlString=htmlString+line;
            }
        } catch (MalformedURLException mue) {
        } catch (IOException ioe) {
            links.add("No Internet Connection Detected");
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException ioe) {
            }
        }
        return htmlString;
    }
    public void LinkGenerator(String html)
    {
        if(html.contains("cm")&&html.contains("zip")) {
            html = html.substring(html.indexOf("<a href=\"/files/oneplusone/cm-13.0/cm"),html.lastIndexOf("</td>"));
            String[] files = html.split("\n<a href");
            for (int i = 0; i < files.length; i++) {
                String tempFileLink = files[i].substring(files[i].indexOf("\">"));
                tempFileLink = tempFileLink.substring(tempFileLink.indexOf(">")+1,tempFileLink.indexOf("<"));
                if (tempFileLink.contains("cm"))
                    links.add(tempFileLink);
            }
        }
        Collections.reverse(links);
        while(links.size()>5)
            links.remove(links.size()-1);
    }
}