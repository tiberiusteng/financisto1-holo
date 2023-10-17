/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package tw.tib.financisto.activity;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TabHost;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import tw.tib.financisto.R;
import tw.tib.financisto.utils.Utils;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 3/24/11 10:20 PM
 */
public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        setTitle("Financisto ("+getAppVersion(this)+")");

        setContentView(R.layout.main2);

        TabLayout tabLayout = findViewById(R.id.tabs);
        ViewPager2 viewPager = findViewById(R.id.viewpager);
        viewPager.setUserInputEnabled(false);

        viewPager.setAdapter(new RecyclerView.Adapter() {
            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                WebView webView = new WebView(parent.getContext());
                webView.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                return new ViewHolder(webView);
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                ViewHolder vh = (ViewHolder) holder;
                switch (position) {
                    case 0:
                        vh.webView.loadUrl("file:///android_asset/about.htm");
                        break;
                    case 1:
                        vh.webView.loadUrl("file:///android_asset/whatsnew.htm");
                        break;
                    case 2:
                        vh.webView.loadUrl("file:///android_asset/gpl-2.0-standalone.htm");
                        break;
                }
            }

            @Override
            public int getItemCount() {
                return 3;
            }
        });

        new TabLayoutMediator(tabLayout, viewPager, true, false,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText(R.string.about);
                            break;
                        case 1:
                            tab.setText(R.string.whats_new);
                            break;
                        case 2:
                            tab.setText(R.string.license);
                            break;
                    }
                }).attach();
    }

    public static String getAppVersion(Context context) {
        try {
            PackageInfo info = Utils.getPackageInfo(context);
            return "v. "+info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final WebView webView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            webView = (WebView) itemView;
        }
    }

}
