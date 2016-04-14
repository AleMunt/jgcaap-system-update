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

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;
public class RVAdapter extends RecyclerView.Adapter<RVAdapter.RomVH>{
    List<RomFile> details;
    RVAdapter(List<RomFile> detailslist)
    {
        this.details = detailslist;
    }
    @Override
    public RomVH onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.cardview, viewGroup, false);
        RomVH pvh = new RomVH(v);
        return pvh;
    }
    @Override
    public void onBindViewHolder(RomVH personViewHolder, int i) {
        RomFile Detail=details.get(i);
        personViewHolder.name.setText(Detail.filename);
        if(Detail.status>1)
            personViewHolder.download.setText("Tap to Install");
        else
            personViewHolder.download.setText("Tap to Download");
    }
    @Override
    public int getItemCount()
    {
        return details.size();
    }
    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }
    public static class RomVH extends RecyclerView.ViewHolder {
        TextView name;
        TextView download;
        RomVH(View itemView) {
            super(itemView);
            name = (TextView)itemView.findViewById(R.id.text1);
            download = (TextView)itemView.findViewById(R.id.text2);
        }
    }
}
