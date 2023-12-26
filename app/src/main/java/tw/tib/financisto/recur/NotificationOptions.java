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
package tw.tib.financisto.recur;

import static androidx.core.app.NotificationCompat.DEFAULT_ALL;
import static androidx.core.app.NotificationCompat.DEFAULT_LIGHTS;
import static androidx.core.app.NotificationCompat.DEFAULT_VIBRATE;

import tw.tib.financisto.R;
import tw.tib.financisto.utils.LocalizableEnum;
import tw.tib.financisto.utils.Utils;

import android.app.Notification;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.Settings;

import androidx.core.app.NotificationCompat;

public class NotificationOptions {
	
	private static final String DEFAULT_SOUND =  Settings.System.DEFAULT_NOTIFICATION_URI.toString();
	
	public static enum VibrationPattern implements LocalizableEnum {
		OFF(R.string.notification_options_off, null),
		DEFAULT(R.string.notification_options_default, null),
		SHORT(R.string.notification_options_short, new long[]{0,200}),
		SHORT_SHORT(R.string.notification_options_2_short, new long[]{0,200,200,200}),
		THREE_SHORTS(R.string.notification_options_3_short, new long[]{0,200,200,200,200,200}),
		LONG(R.string.notification_options_long, new long[]{0,500}),
		LONG_LONG(R.string.notification_options_2_long, new long[]{0,500,300,500}),
		THREE_LONG(R.string.notification_options_3_long, new long[]{0,500,300,500,300,500});
		
		public final int titleId;
		public final long[] pattern;
		
		private VibrationPattern(int titleId, long[] pattern) {
			this.titleId = titleId;
			this.pattern = pattern;
		}

		@Override
		public int getTitleId() {
			return titleId;
		}
	}
	
	public static enum LedColor implements LocalizableEnum  {
		OFF(R.string.notification_options_off, Color.BLACK),
		DEFAULT(R.string.notification_options_default, Color.BLACK),
		GREEN(R.string.notification_options_led_green, Color.GREEN),
		BLUE(R.string.notification_options_led_blue, Color.BLUE),
		YELLOW(R.string.notification_options_led_yellow, Color.YELLOW),
		RED(R.string.notification_options_led_red, Color.RED),
		PINK(R.string.notification_options_led_pink, Color.parseColor("#FF00FF"));

		public final int titleId;
		public final int color;
		
		private LedColor(int titleId, int color) {
			this.titleId = titleId;
			this.color = color;
		}

		@Override
		public int getTitleId() {
			return titleId;
		}

	}
	
	public String sound;
	public VibrationPattern vibration;
	public LedColor ledColor; 
	
	private NotificationOptions(String sound, VibrationPattern vibration, LedColor ledColor) {
		this.sound = sound;
		this.vibration = vibration;
		this.ledColor = ledColor;		
	}
	
	public static NotificationOptions createDefault() {
		return new NotificationOptions(DEFAULT_SOUND, VibrationPattern.DEFAULT, LedColor.DEFAULT);
	}
	
	public boolean isDefault() {
		return DEFAULT_SOUND.equals(sound) && vibration == VibrationPattern.DEFAULT && ledColor == LedColor.DEFAULT;
	}
	
	public static NotificationOptions createOff() {
		return new NotificationOptions(null, VibrationPattern.OFF, LedColor.OFF);
	}
	
	public boolean isOff() {
		return sound == null && vibration == VibrationPattern.OFF && ledColor == LedColor.OFF;
	}

	public static NotificationOptions parse(String options) {
		String[] a = options.split(";");
		return new NotificationOptions(!Utils.isEmpty(a[0]) ? a[0] : null, VibrationPattern.valueOf(a[1]), LedColor.valueOf(a[2]));
	}
	
	public String stateToString() {
		StringBuilder sb = new StringBuilder();
		sb.append(sound != null ? sound : "").append(";").append(vibration).append(";").append(ledColor).append(";");
		return sb.toString();
	}

	public String toInfoString(Context context) {
		if (isDefault()) {
			return context.getString(R.string.notification_options_default);			
		} else if (isOff()) {
			return context.getString(R.string.notification_options_off);						
		} else {
			return context.getString(R.string.notification_options_custom);						
		}
	}

	public String getSoundName(Context context) {
		if (sound == null) {
			return context.getString(R.string.notification_options_off);
		}
		Uri uri = Uri.parse(sound);
		if (Settings.System.DEFAULT_NOTIFICATION_URI.equals(uri)) {
			return context.getString(R.string.notification_options_default);
		}
		Ringtone ringtone = RingtoneManager.getRingtone(context, uri);
		return ringtone != null ? ringtone.getTitle(context) : context.getString(R.string.notification_options_off);
	}

	public void apply(NotificationCompat.Builder builder) {
		if (isOff()) {
			builder.setSilent(true);
		} else if (isDefault()) {
			builder.setDefaults(DEFAULT_ALL);
		} else {
			applySound(builder);
			applyVibration(builder);
			applyLed(builder);
		}
	}

	private void applySound(NotificationCompat.Builder builder) {
		builder.setSound(Uri.parse(sound), AudioManager.STREAM_NOTIFICATION);
	}

	private void applyVibration(NotificationCompat.Builder builder) {
		if (vibration == VibrationPattern.DEFAULT) {
			builder.setDefaults(DEFAULT_VIBRATE);
		} else {
			builder.setVibrate(vibration.pattern);
		}
	}

	private void applyLed(NotificationCompat.Builder builder) {
		if (ledColor == LedColor.DEFAULT) {
			builder.setDefaults(DEFAULT_LIGHTS);
		} else {
			builder.setLights(ledColor.color, 200, 200);
		}
	}
}
