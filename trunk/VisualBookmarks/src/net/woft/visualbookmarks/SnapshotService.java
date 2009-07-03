package net.woft.visualbookmarks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RemoteViews;

public class SnapshotService extends Service {

	private static final String LOGTAG = "SnapshotService";
	private static final String FALLBACK_IMG_PATH = "/sdcard/fallback.png";
	private static int counter = 0;
	private static final int MSG_TIMEOUT = 0;
	private static final int PAGE_LOAD_TIMEOUT = 60000;
	private static final String[] URLS = {
		"http://www.google.com/",
		"http://www.yahoo.com/",
		"http://www.ebay.com/",
		"http://www.facebook.com/",
		"http://www.myspace.com/"
	};

	private WebView webView;
	private int pageStartCount;
	private Handler handler;
	private String imagePath;
	private boolean updating;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.v(LOGTAG, "creating SnapshotService...");
		initWebView();
		handler = new MsgHandler();
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		Log.v(LOGTAG, "starting SnapshotService...");
		if(updating) {
			Log.v(LOGTAG, "last update still in progress, ignored.");
			return;
		}
		Random rnd = new Random();
		String url = URLS[rnd.nextInt(URLS.length)];
		prepTmpFile();
		requestSnapshot(url);
	}
	
	protected void prepTmpFile() {
		if(imagePath != null) {
			new File(imagePath).delete();
		}
		File tmpFile = null;
		try {
			tmpFile = File.createTempFile("snapshot_", ".png");
		} catch (IOException ioe) {
			Log.w(LOGTAG, ioe);
		}
		if(tmpFile == null)
			tmpFile = new File("/sdcard/tmp.png");
		imagePath = tmpFile.toString();
	}
	
	@Override
	public void onDestroy() {
		Log.v(LOGTAG, "stoping SnapshotService...");
		super.onDestroy();
	}

	protected void initWebView() {
		webView = new WebView(this);
		WebSettings webSettings = webView.getSettings();
		webSettings.setSavePassword(false);
		webSettings.setSaveFormData(false);
		webSettings.setJavaScriptEnabled(true);
		webSettings.setSupportZoom(false);
		webView.setWebViewClient(new PageLoadWebViewClient());
		webView.layout(0, 0, 320, 480);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	protected void requestSnapshot(String url) {
		webView.loadUrl(url);
		//set up timeout
		Message msg = handler.obtainMessage(MSG_TIMEOUT);
		handler.sendMessageDelayed(msg, PAGE_LOAD_TIMEOUT);
	}
	
	protected void doSnapshot() {
		if(!snapShot()) {
			Log.w(LOGTAG, "WebView snapshot unsuccessful. Using fallback image.");
			imagePath = FALLBACK_IMG_PATH;
		} else {
			Log.v(LOGTAG, "WebView snapshot successful. Updating widget.");
		}
		updateWidget();
	}

	protected boolean snapShot() {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(imagePath);
		} catch (FileNotFoundException fnfe) {
			Log.e(LOGTAG, "Failed to create image file.", fnfe);
			return false;
		}
		Canvas canvas = new Canvas();
		Bitmap bitmap = Bitmap.createBitmap(320, 480, Config.ARGB_8888);
		canvas.setBitmap(bitmap);
		webView.draw(canvas);
		if(!bitmap.compress(CompressFormat.PNG, 90, fos)) {
			Log.w(LOGTAG, "Failed to compress image file.");
			return false;
		}
		try {
			fos.flush();
			fos.close();
		} catch (IOException ioe) {
			Log.e(LOGTAG, "Failed to close image file.", ioe);
			return false;
		}
		return true;
	}
	
	protected void handleTimeout() {
		Log.w(LOGTAG, "WebView snapshot timed out. Using fallback image.");
		imagePath = FALLBACK_IMG_PATH;
		updateWidget();
	}
	
	protected void updateWidget() {
		RemoteViews widget = new RemoteViews(this.getPackageName(), R.layout.widget);
//		Bitmap bmp = BitmapFactory.decodeFile(imagePath);
		widget.setTextViewText(R.id.text_view, Integer.toString(counter++));
//		widget.setImageViewBitmap(R.id.image_view, bmp);
		widget.setImageViewUri(R.id.image_view, Uri.parse(imagePath));
		AppWidgetManager.getInstance(this).updateAppWidget(
				new ComponentName(this, BookmarkWidget.class), widget);
		updating = false;
	}

	class PageLoadWebViewClient extends WebViewClient {

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			Log.v(LOGTAG, "onPageStarted: " + url);
			pageStartCount++;
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			Log.v(LOGTAG, "onPageFinished: " + url);
			handler.postDelayed(new RedirectChecker(), 500);
		}
	}

	class RedirectChecker implements Runnable {

		private int initialStartCount;

		public RedirectChecker() {
			initialStartCount = pageStartCount;
		}

		public void run() {
			if (initialStartCount == pageStartCount) {
				//perform cleanup
				handler.removeMessages(MSG_TIMEOUT);
				webView.stopLoading();
				doSnapshot();
			}
		}
	}

	class MsgHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_TIMEOUT:
				handleTimeout();
				break;
			}
			super.handleMessage(msg);
		}
	}
}
