package com.axway.ate.util;

import android.content.Context;
import android.widget.Toast;

public class UiUtils {

	public static void showToast(Context ctx, String msg) {
		Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
	}

	public static void showToastLong(Context ctx, String msg) {
		Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
	}

	public static void showToast(Context ctx, int resId) {
		Toast.makeText(ctx, resId, Toast.LENGTH_SHORT).show();
	}

	
	public static void showToastLong(Context ctx, int resId) {
		Toast.makeText(ctx, resId, Toast.LENGTH_LONG).show();
	}
}
