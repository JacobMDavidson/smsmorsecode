package com.jacobmdavidson.smsmorsecode;

import android.os.Bundle;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;

/**
 * Enable/Disable the sms morse code receiver, and adjust settings for sms morse code playback, including 
 * frequency and duration. Provides ability to test settings with a user provided string.
 * @author Jacob Davidson
 * @version 1.0.0
 */
public class MainActivity extends Activity implements OnSeekBarChangeListener{
	
	// The frequency and speed setting seek bars
	private SeekBar frequencyBar, speedBar;
	
	// The frequency and speed setting seek bar labels
	private TextView frequencySetting, speedSetting;
	
	// The enable/disable toggle button
	private ToggleButton toggle;
	
	// The shared preferences where the settings are stored
	private SharedPreferences sharedPrefs;
	
	// The text box with the editable string that will be tested with the morse code settings
	private EditText textToTranslate;
	
	// Editor to edit shared preference settings
	private SharedPreferences.Editor editor;
	
	// Package Manager used to register/deregister the SmsMorseCodeReceiver
	private PackageManager pm;
	
	// Component that is registered/deregistered 
	private ComponentName componentName;
	
	// speed in words per minute
	private int wordsPerMinute;
	
	// Frequency in Hz
	private double frequency;
	
	// Google analytics tracker
	EasyTracker easyTracker = null;

	/**
	 * Called when the activity is created. Retrieve all saved settings, and set all toggle buttons, 
	 * seek bars, and labels according to the retrieved settings.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Instantiate the EasyTracker object
		easyTracker = EasyTracker.getInstance(this);
		
		// Get saved preferences
		sharedPrefs = getSharedPreferences("com.jacobmdavidson.smsmorsecode", MODE_PRIVATE);
		editor = getSharedPreferences("com.jacobmdavidson.smsmorsecode", MODE_PRIVATE).edit();

		// Instantiate and Set the toggle button according to the preferences
		toggle = (ToggleButton) findViewById(R.id.toggleButton1);
		toggle.setChecked(sharedPrefs.getBoolean("ToggleButtonState", false));
		
		//Instantiate the frequency SeekBar, set it according to the preferences, and register the listener
		frequencyBar = (SeekBar)findViewById(R.id.seekBar1);
		frequencyBar.setProgress(sharedPrefs.getInt("FrequencySetting", Constants.DEFAULT_FREQUENCY));
		frequencyBar.setOnSeekBarChangeListener(this);
		
		//Instantiate the speed SeekBar, set it according to the preferences, and register the listener
		speedBar = (SeekBar)findViewById(R.id.seekBar2);
		speedBar.setProgress(sharedPrefs.getInt("SpeedSetting", Constants.DEFAULT_SPEED));
		speedBar.setOnSeekBarChangeListener(this);
		
		//Instantiate the frequency TextView and set it accordingly
		frequencySetting = (TextView)findViewById(R.id.textView4);
		frequency = getFrequency(sharedPrefs.getInt("FrequencySetting", Constants.DEFAULT_FREQUENCY));
		frequencySetting.setText((int)frequency + " Hz");

		//Instantiate the speed TextView and set it accordingly
		speedSetting = (TextView)findViewById(R.id.textView6);
		wordsPerMinute = getWordsPerMinute(sharedPrefs.getInt("SpeedSetting", Constants.DEFAULT_SPEED));
		speedSetting.setText(wordsPerMinute + " Words Per Minute");
		
		//Instantiate the EditText object
		textToTranslate = (EditText)findViewById(R.id.editTextToTranslate);
		
		// Get the package manager
		pm = this.getPackageManager();
		
		// Instantiate the component name 
        componentName = new ComponentName(this, SmsMorseCodeReceiver.class);
		
	}
	
	/**
	 * Override the onStart method to add the easyTracker send method
	 */
	@Override
	public void onStart(){
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this); 
	}

	/**
	 * Stores the updated setting, and enables/disables the SmsMorseCodeReceiver according to 
	 * the toggle button's new state.
	 * @param view The view from which the click is received
	 */
	public void onToggleClicked(View view){
		// Send the event to google analytics
		easyTracker.send(MapBuilder
			      .createEvent("ui_action",     // Event category (required)
			                   "button_press",  // Event action (required)
			                   "enabled_button",   // Event label
			                   null)            // Event value
			      .build()
			  );
		
		// Get the state of the toggle button
		boolean enabled = ((ToggleButton) view).isChecked();

        // If the state of the toggle is now enabled, enable the SmsMorseCodeReceiver
        if (enabled) {
	    	// Save state of toggle button
	        editor.putBoolean("ToggleButtonState", true);
	        editor.commit();
	        

	        // Enable the BroadcastReceiver  	
            pm.setComponentEnabledSetting(componentName,
            		PackageManager.COMPONENT_ENABLED_STATE_ENABLED, 
            		PackageManager.DONT_KILL_APP);
	    
        // Else the state of the toggle is now disabled, disable the SmsMorseCodeReceiver
	    } else {
	    	
	    	// Save state of toggle button
	        editor.putBoolean("ToggleButtonState", false);
	        editor.commit();  
	        
	        // Disable the BroadcastReceiver
            pm.setComponentEnabledSetting(componentName,
            		PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 
            		PackageManager.DONT_KILL_APP);
	       
	    }
	}
	
	/**
	 * When the test settings button is clicked, create an intent and add the string
	 * that will be used for testing to that intent. Finally, start the SmsMorseCodeService	
	 * @param view The view from which the click is received
	 */
	public void onButtonClicked(View view){
		// Send the event to google analytics
		easyTracker.send(MapBuilder
			      .createEvent("ui_action",     // Event category (required)
			                   "button_press",  // Event action (required)
			                   "test_settings_button",   // Event label
			                   null)            // Event value
			      .build()
			  );		
		// Create the intent 
		Intent service = new Intent(this, SmsMorseCodeService.class);
		
		// Get the text to translate string, and add it to the intent
		String text = textToTranslate.getText().toString();
    	service.putExtra("body", text);
    	
    	// Start the SmsMorseCodeService
    	this.startService(service);
  
	}
	
	/**
	 * change the seek bar labels during the onProgressChanged event
	 */
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		
		// If the seekBar is the frequency bar, update the frequency label as the setting changes
		if (seekBar.equals(frequencyBar)){
			frequencySetting.setText((int)getFrequency(progress) + " Hz");
			
		// Else If the seekBar is the speed bar, update the speed label as the setting changes
		} else if(seekBar.equals(speedBar)){
			speedSetting.setText(getWordsPerMinute(progress) + " Words Per Minute");
			
		}
		
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// Method is purposely empty
	}

	/**
	 * On the stop tracking touch event, update the shared preference settings
	 * with the selected setting.
	 */
	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// Send the event to google analytics
		easyTracker.send(MapBuilder
			      .createEvent("ui_action",     // Event category (required)
			                   "button_press",  // Event action (required)
			                   "settings_seekbar",   // Event label
			                   null)            // Event value
			      .build()
			  );		
		// Get the progress of the seek bar whose setting changed
		int progress = seekBar.getProgress();
		
		// If this is the frequency bar, save the frequency setting, and set the frequency field
		if (seekBar.equals(frequencyBar)){
	        editor.putInt("FrequencySetting", progress);
	        editor.commit();
	        frequency = getFrequency(progress);
	        
	    // Else if this is the speed bar, save the speed setting, and set the wordsPerMinute field
		} else if(seekBar.equals(speedBar)){
			// Save speed setting
	        editor.putInt("SpeedSetting", progress);
	        editor.commit();
	        wordsPerMinute = getWordsPerMinute(progress);
		}
		
	}
	
	/**
	 * Changes the speed and frequency settings to the defaults of 30 words per minute
	 * and 700 Hz respectively.
	 * @param view The view from which the click is received
	 */
	public void onDefaultButtonClicked(View view){
		// Send the event to google analytics
		easyTracker.send(MapBuilder
			      .createEvent("ui_action",     // Event category (required)
			                   "button_press",  // Event action (required)
			                   "defaults_button",   // Event label
			                   null)            // Event value
			      .build()
			  );		
		// Save the default settings to frequency and speed 
		editor.putInt("FrequencySetting", Constants.DEFAULT_FREQUENCY);
        editor.putInt("SpeedSetting", Constants.DEFAULT_SPEED);
        editor.commit();
        
        // Change the seek bars to the default setting locations
        frequencyBar.setProgress(Constants.DEFAULT_FREQUENCY);
        speedBar.setProgress(Constants.DEFAULT_SPEED);
        
        // Update the frequency and wordsPerMinute fields with the default settings
        frequency = getFrequency(Constants.DEFAULT_FREQUENCY);
        wordsPerMinute = getWordsPerMinute(Constants.DEFAULT_SPEED);
        
        // Update the labels for each of the seek bars with the default settings
        frequencySetting.setText((int)frequency + " Hz");
        speedSetting.setText(wordsPerMinute + " Words Per Minute");
	}
	
	/**
	 * Convert the speed bar progress setting to words per minute.
	 * @param duration Progress setting of the speed bar.
	 * @return Speed of morse code play back in words per minute.
	 */
    public static int getWordsPerMinute(int duration){
    	return (duration + 1) * 10;
    }
    
    /**
     * Convert the frequency bar progress setting to frequency. 
     * @param frequency Progress setting of the frequency bar
     * @return Frequency of morse code play back in Hz
     */
    public static double getFrequency(int frequency){
    	return (frequency + 8.0) * 50.0;
    }
 
	/**
	 * Override the onStart method to add the easyTracker send method
	 */
    @Override
    public void onStop() {
      super.onStop();
      EasyTracker.getInstance(this).activityStop(this); 
    }
}

