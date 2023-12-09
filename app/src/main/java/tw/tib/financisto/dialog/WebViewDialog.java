/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package tw.tib.financisto.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.view.ContextThemeWrapper;
import android.webkit.WebView;

import tw.tib.financisto.R;
import tw.tib.financisto.utils.Utils;

public class WebViewDialog {

	public static String checkVersionAndShowWhatsNewIfNeeded(Activity activity) {
		try {
			PackageInfo info = Utils.getPackageInfo(activity);
			SharedPreferences preferences = activity.getPreferences(0); 
			int newVersionCode = info.versionCode;
			int oldVersionCode = preferences.getInt("versionCode", -1);
			if (newVersionCode > oldVersionCode) {
				preferences.edit().putInt("versionCode", newVersionCode).commit();
				showWhatsNew(activity);
			}
			return "v. "+info.versionName;
		} catch(Exception ex) { 
			return "Free";
		}
	}
	
	public static void showWhatsNew(Context context) {
		showHTMDialog(context, "whatsnew.htm", R.string.whats_new);
	}

	private static void showHTMDialog(Context context, String fileName, int dialogTitleResId) {
		Context theme = new ContextThemeWrapper(context, R.style.Theme_AppCompat_Dialog);
		WebView webView = new WebView(theme);
		webView.loadUrl("file:///android_asset/"+fileName);
		new AlertDialog.Builder(theme)
			.setView(webView)
			.setTitle(dialogTitleResId)
			.setPositiveButton(R.string.ok, null)
			.show();		
	}

}
