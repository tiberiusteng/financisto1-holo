/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package tw.tib.financisto.activity;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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

    protected WebView webView;
    protected OnBackPressedCallback onBackPressedCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        setTitle("Financisto Holo ("+getAppVersion(this)+")");

        setContentView(R.layout.main2);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.tabs), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.statusBars()
                    | WindowInsetsCompat.Type.captionBar());
            v.setPadding(0, insets.top, 0, 0);
            return WindowInsetsCompat.CONSUMED;
        });

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
                        webView = vh.webView;
                        webView.setWebViewClient(new WebViewClient() {
                            @Override
                            public void onPageFinished(WebView view, String url) {
                                onBackPressedCallback.setEnabled(view.canGoBack());
                            }
                        });
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

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == 1 && webView != null) {
                    onBackPressedCallback.setEnabled(webView.canGoBack());
                }
                else {
                    onBackPressedCallback.setEnabled(false);
                }
            }
        });

        onBackPressedCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                webView.goBack();
            }
        };

        getOnBackPressedDispatcher().addCallback(onBackPressedCallback);

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
