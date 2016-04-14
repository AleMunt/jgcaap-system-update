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

public class RomFile {
    public String filename;
    int status;
    //1 status means not downloaded
    //2 status means downloaded
    public RomFile(String f, int s)
    {
        filename=f;
        status=s;
    }
}