package com.jacobmdavidson.smsmorsecode;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.util.Linkify;
import android.view.MenuItem;
import android.widget.TextView;

/**
 * Displays information about the SMorSe application
 * @author Jacob Davidson
 * @version 1.0.0
 */
public class ActivityAbout extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
		
		// Add a link to the email address in the about activity
		TextView contactUs = (TextView) findViewById(R.id.contact);
		Linkify.addLinks(contactUs, Linkify.EMAIL_ADDRESSES);
	}

	/**
	 * Display a back button
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
