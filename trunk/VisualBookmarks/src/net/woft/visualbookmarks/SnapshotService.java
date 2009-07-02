package net.woft.visualbookmarks;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.os.IBinder;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class SnapshotService extends Service {

	private static final String LOGTAG = "SnapshotService";
	private WebView webView;

	@Override
	public void onCreate() {
		super.onCreate();
		webView = new WebView(this);
		WebSettings webSettings = webView.getSettings();
		webSettings.setSavePassword(false);
		webSettings.setSaveFormData(false);
		webSettings.setJavaScriptEnabled(true);
		webSettings.setSupportZoom(false);
		webView.setWebViewClient(new MyWebViewClient());
		webView.layout(0, 0, 320, 480);
		requestSnapShot("http://www.google.com/", "");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	public void requestSnapShot(String url, String imagePath) {
		webView.loadUrl(url);
	}

	class MyWebViewClient extends WebViewClient {
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			Log.v(LOGTAG, "onPageStarted: " + url);
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream("/sdcard/view.png");
			} catch (FileNotFoundException fnfe) {
				Log.e(LOGTAG, "Failed to save view.png", fnfe);
				return;
			}
			Canvas canvas = new Canvas();
			Bitmap bitmap = Bitmap.createBitmap(320, 480, Config.ARGB_8888);
			canvas.setBitmap(bitmap);
			view.draw(canvas);
			if(!bitmap.compress(CompressFormat.PNG, 90, fos)) {
				Log.w(LOGTAG, "Failed to save PNG file.");
				return;
			}
			try {
				fos.flush();
				fos.close();
			} catch (IOException ioe) {
				Log.e(LOGTAG, "Failed to finish up image file.", ioe);
				return;
			}
		}
	}
}
