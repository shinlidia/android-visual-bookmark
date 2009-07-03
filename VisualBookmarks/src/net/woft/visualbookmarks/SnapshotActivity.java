package net.woft.visualbookmarks;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Browser;
import android.provider.Browser.BookmarkColumns;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

public class SnapshotActivity extends Activity {

	private static final String LOGTAG = "SnapshotActivity";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		findViewById(R.id.snapshot_button).setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				ContentResolver cr = getContentResolver();
				Cursor cursor = cr.query(Browser.BOOKMARKS_URI, new String[]{BookmarkColumns.URL},
						null, null, null);
				cursor.moveToFirst();
				while(!cursor.isAfterLast()) {
					Log.v(LOGTAG, "url=" + cursor.getString(0));
					cursor.moveToNext();
				}
				cursor.close();
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}

}