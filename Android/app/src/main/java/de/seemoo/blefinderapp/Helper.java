package de.seemoo.blefinderapp;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.EditText;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.net.CookieHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.seemoo.blefinderapp.cloud.LostService;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
//import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class Helper {

	public final static String API_ROOT = "https://blefinderapp.example.net/";

	public static CookieJar stupidCookieJar = new CookieJar() {
		public HashMap<String, Cookie> storedCookies = new HashMap<>();
		@Override
		public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
			for(Cookie c : cookies) {
				storedCookies.put(c.name(), c);
			}
		}

		@Override
		public List<Cookie> loadForRequest(HttpUrl url) {
			return new ArrayList<>(storedCookies.values());
		}
	};

	private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for ( int j = 0; j < bytes.length; j++ ) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	public static Retrofit getRetrofit() {
		Gson gson = new GsonBuilder()
				.setDateFormat("yyyy-MM-dd HH:mm:ss")
				.create();
		Retrofit retrofit = new Retrofit.Builder()
				.client(new OkHttpClient.Builder().cookieJar(stupidCookieJar).build())
				.baseUrl(API_ROOT)
        		.addConverterFactory(GsonConverterFactory.create(gson))
				//.addCallAdapterFactory(RxJava2CallAdapterFactory.create())
				.build();
				return retrofit;
	}

	public static LostService getLostService() {
		Retrofit retro = getRetrofit();
		return retro.create(LostService.class);
	}

	public static void dumpBundle(String tag, Bundle bundle) {
		if (bundle != null) {
			for (String key : bundle.keySet()) {
				Object value = bundle.get(key);
				Log.d(tag, String.format("%s => (%s) %s", key, value.getClass().getName(),
						value.toString()));
			}
		}
	}

	public static void MessageBox(Context ctx, String string) {
		new AlertDialog.Builder(ctx)
				.setMessage(string)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				})
				.show();
	}
	interface IInputCallback {
		void done(String newValue);
	}
	public static void InputBox(Context ctx, String title, String defaultValue, final IInputCallback callback) {
		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle(title);

		// Set up the input
		final EditText input = new EditText(ctx);
		input.setText(defaultValue);
		// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
		//input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		builder.setView(input);

		// Set up the buttons
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				callback.done(input.getText().toString());
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});

		builder.show();
	}

	public static void debugNotification(Context ctx, String message, String message2) {
		NotificationCompat.Builder b=new NotificationCompat.Builder(ctx, MyApplicationKt.CHAN_DEBUG);

		b.setPriority(Notification.PRIORITY_LOW)
				.setContentTitle(message)
				.setContentText(message2)
				.setSmallIcon(R.drawable.ic_notify_icon_info);

		NotificationManager noti = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
		noti.notify(0, b.build());
	}

	public static boolean nullOrEmpty(String s) {
		return s == null || s.isEmpty();
	}
}
