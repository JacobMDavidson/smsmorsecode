package com.jacobmdavidson.smsmorsecode;


import android.media.AudioManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends Activity implements OnSeekBarChangeListener{
	
	private SeekBar frequencyBar, speedBar;
	private TextView frequencySetting, speedSetting;
	private ToggleButton toggle;
	private SharedPreferences sharedPrefs;
	private EditText textToTranslate;
	private SharedPreferences.Editor editor;
	private PackageManager pm;
	private ComponentName componentName;
	private int wordsPerMinute;
	private double frequency;
	//@ TODO
	/** To clean up this class
	 *  Create a setFrequency method that sets frequency progress, text, and updates preferences in one spot
	 *  Create a setSpeed method that sets frequency progress, text, and updates preferences in one spot
	 *  Remove all of the "Magic numbers"
	 *  Once above is done, and MorseCode class is good, update MyService to match
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Get saved preferences
		sharedPrefs = getSharedPreferences("com.jacobmdavidson.smsmorsecode", MODE_PRIVATE);
		editor = getSharedPreferences("com.jacobmdavidson.smsmorsecode", MODE_PRIVATE).edit();

		// Instantiate and Set the toggle button accordingly
		toggle = (ToggleButton) findViewById(R.id.toggleButton1);
		toggle.setChecked(sharedPrefs.getBoolean("ToggleButtonState", false));
		
		//Instantiate the SeekBars and set them accordingly
		frequencyBar = (SeekBar)findViewById(R.id.seekBar1);
		frequencyBar.setProgress(sharedPrefs.getInt("FrequencySetting", Constants.DEFAULT_FREQUENCY));
		frequencyBar.setOnSeekBarChangeListener(this);
		
		speedBar = (SeekBar)findViewById(R.id.seekBar2);
		speedBar.setProgress(sharedPrefs.getInt("SpeedSetting", Constants.DEFAULT_SPEED));
		speedBar.setOnSeekBarChangeListener(this);
		
		//Instantiate the TextViews and set them accordingly
		frequencySetting = (TextView)findViewById(R.id.textView4);


		frequency = getFrequency(sharedPrefs.getInt("FrequencySetting", Constants.DEFAULT_FREQUENCY));
		frequencySetting.setText((int)frequency + " Hz");

		speedSetting = (TextView)findViewById(R.id.textView6);
		wordsPerMinute = getWordsPerMinute(sharedPrefs.getInt("SpeedSetting", Constants.DEFAULT_SPEED));
		speedSetting.setText(wordsPerMinute + " Words Per Minute");
		
		//Instantiate the EditText object
		textToTranslate = (EditText)findViewById(R.id.editTextToTranslate);
		
	}

	public void onToggleClicked(View view){
		boolean on = ((ToggleButton) view).isChecked();
		pm = this.getPackageManager();
        componentName = new ComponentName(this, SmsReceiver.class);

        if (on) {
	    	// Save state of toggle button
	        editor.putBoolean("ToggleButtonState", true);
	        editor.commit();
	        

	        // Enable the BroadcastReceiver  	
            pm.setComponentEnabledSetting(componentName,
            		PackageManager.COMPONENT_ENABLED_STATE_ENABLED, 
            		PackageManager.DONT_KILL_APP);
	        
	    } else {
	    	
	    	// Save state of toggle button
	        editor.putBoolean("ToggleButtonState", false);
	        editor.commit();  

	        
	        // Disable the BroadcastReceiver
            pm.setComponentEnabledSetting(componentName,
            		PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 
            		PackageManager.DONT_KILL_APP);
	       
	    }
	    // Display debugging message in logcat
	    // Log.d(Constants.LOG,text);
	}
	
	
	public void onButtonClicked(View view){
		/**Instead of all this code, send an intent to the service
		 * Include a flag that states this comes from a test message
		 * */
		Context context = getApplicationContext();
		Intent service = new Intent(context, MyService.class);
		String text = textToTranslate.getText().toString();
    	service.putExtra("sender", "Here is your translated Message");
    	service.putExtra("body", text);
    	service.putExtra("caller", "MainActivity");
    	context.startService(service);
    	/*
		long[] track;
		//double freqHz;
		//int wordsPerMinute;
		//Get the frequency and set freqHz accordingly
		
		//freqHz = getFrequency(sharedPrefs);
		
		
		// Determine duration per unit based on 'PARIS' method (50 units makes up the word paris
		// msec/unit = (1 min / number of words) * (1 word / 50 units) * (60000 msec / min)
		// @ TODO change duration to 10,20,30,40 or 50
		//wordsPerMinute = (duration + 1) * 10;
		//wordsPerMinute = getWordsPerMinute(sharedPrefs);
       
    	// Build the AudioTrack and play it
    	MorseCodeTrack morseCodeTrack = new MorseCodeTrack(text, frequency, wordsPerMinute);
    	
    	AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
    	if(am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL){
    		morseCodeTrack.playAudioTrack();
    		morseCodeTrack.terminateAudioTrack();
    	}
    	else if(am.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE){
    		track = morseCodeTrack.getVibrateTrack();
    		Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
    		
    		//for(int i= 0; i < track.length; i++)
    		//	Log.d(Constants.LOG, track[i] + "");
    		v.vibrate(track, -1);
    	}
    	*/

	}
	
	//Seekbar methods
	@Override
	public void onProgressChanged(SeekBar seekBar, 
			int progress,
			boolean fromUser) {
		
		// Set the text for the appropriate seek bar
		if (seekBar.equals(frequencyBar)){
			frequencySetting.setText((int)getFrequency(progress) + " Hz");
		} else if(seekBar.equals(speedBar)){
			speedSetting.setText(getWordsPerMinute(progress) + " Words Per Minute");
			
		}
		
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// Method is purposely empty
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		int progress = seekBar.getProgress();
		if (seekBar.equals(frequencyBar)){
			// Save frequency setting
	        editor.putInt("FrequencySetting", progress);
	        editor.commit();
	        frequency = getFrequency(progress);
		} else if(seekBar.equals(speedBar)){
			// Save speed setting
	        editor.putInt("SpeedSetting", progress);
	        editor.commit();
	        wordsPerMinute = getWordsPerMinute(progress);
		}
		
	}
	
	public void onDefaultButtonClicked(View view){
		//SharedPreferences.Editor editor = getSharedPreferences("com.jacobmdavidson.smsmorsecode", MODE_PRIVATE).edit();
        editor.putInt("FrequencySetting", Constants.DEFAULT_FREQUENCY);
        editor.putInt("SpeedSetting", Constants.DEFAULT_SPEED);
        editor.commit();
        frequencyBar.setProgress(Constants.DEFAULT_FREQUENCY);
        speedBar.setProgress(Constants.DEFAULT_SPEED);
        frequency = getFrequency(Constants.DEFAULT_FREQUENCY);
        wordsPerMinute = getWordsPerMinute(Constants.DEFAULT_SPEED);
        frequencySetting.setText((int)frequency + " Hz");
        speedSetting.setText(wordsPerMinute + " Words Per Minute");
	}
    public static int getWordsPerMinute(int duration){
    	/*int duration = sharedPrefs.getInt("SpeedSetting", Constants.DEFAULT_SPEED);
    	if(duration < 4){
    		return (duration + 1) * 10;
    	} else {
    		return (duration + 2) * 10;
    	}*/
    	return (duration + 1) * 10;
    }
    public static double getFrequency(int frequency){
    	return (frequency + 8.0) * 50.0;
    }
}
/*	
	private void playSound(String pattern, int freqHz, int durationMs) {
		Log.d(Constants.LOG, "playing sound");
	    // AudioTrack definition
	    int mBufferSize = AudioTrack.getMinBufferSize(44100,
	                        AudioFormat.CHANNEL_OUT_MONO,    
	                        AudioFormat.ENCODING_PCM_16BIT);

	    AudioTrack mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
	                        AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
	                        mBufferSize, AudioTrack.MODE_STREAM);
	    
	    //Need to get actual length, and vary the amplitude based on pattern
	    int count = (int)(((double)Constants.SAMPLE_RATE) * 2.0 * (durationMs / 1000.0)) & ~1;
    	int numCharacters = pattern.length();
    	
    	// Total length should be number of frames 
    	int totalLength = count * numCharacters;
    	
	     // Sine wave
	     double[] mSound = new double[totalLength];
	     short[] mBuffer = new short[totalLength];
	     int multiplier;
	     for(int j=0; j<numCharacters; j++){
	    	 multiplier = Character.getNumericValue(pattern.charAt(j));
	     // Create sound wave
		     for (int i = (j * (mSound.length / numCharacters)); i < (j + 1) * (mSound.length / numCharacters); i++) {
		         mSound[i] = Math.sin((2.0*Math.PI * freqHz/((double)Constants.SAMPLE_RATE)*(double)i)) * multiplier;
		         mBuffer[i] = (short) (mSound[i]*Short.MAX_VALUE);
		     }
	     }
	     
	     
	     //for (int i = 0; i < mSound.length; i++) {
	       //  mSound[i] = Math.sin((2.0*Math.PI * 800/44100.0*(double)i));
	         //mBuffer[i] = (short) (mSound[i]*Short.MAX_VALUE);
	     //}
	     
		
	     mAudioTrack.setStereoVolume(1.0f, 1.0f);
	     mAudioTrack.play();

	     mAudioTrack.write(mBuffer, 0, mSound.length);
	     mAudioTrack.stop();
	     mAudioTrack.release();
	     

	}
	

}*/
/*
 * 	private AudioTrack generateMorseCode(String morseCode)
    {
    	final double freqHz = (double)((sharedPrefs.getInt("FrequencySetting", 14) + 2) * 50);
    	int durationMs; // Base duration  
    	
    	//Calculate duration
    	durationMs = 50 * 1000 / (60 * (sharedPrefs.getInt("SpeedSetting", 10) + 5));
    	
    	int count = (int)(44100.0 * 2.0 * (durationMs / 1000.0)) & ~1;
    	int numCharacters = morseCode.length();
    	
    	// Total length should be number of frames 
    	int totalLength = count * numCharacters;
    	
    	Log.d("com.jacobmdavidson.smsmorsecode", "count:" + count + " numCharacters:" + numCharacters + " total Length: " + totalLength);
    	
    	short[] samples = new short[totalLength];
    	int bufferSizeInBytes = totalLength * (Short.SIZE / 8);
    	int location; 
    	int multiplier;
    	
    	for(int i = 0; i < totalLength; i += 2){
    		location = (i / count);
    		multiplier = Character.getNumericValue(morseCode.charAt(location));
    		short sample = (short)(Math.sin(2 * Math.PI * i / (44100.0 / freqHz)) * 0x7FFF * multiplier);
    		samples[i + 0] = sample;
    		samples[i + 1] = sample;
    	}
    	
    	// totalLength * (Short.SIZE / 8) is the number of bytes for the sample
    	AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
    		AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
    		bufferSizeInBytes, AudioTrack.MODE_STATIC);
    	track.write(samples, 0, totalLength);
    	// set setNotificationMarkerPosition according to audio length
    	track.setPlaybackPositionUpdateListener(this);
    	
    	// Not sure why I have to divide by 4.05 (should be / 4 for total frames)
 	    track.setNotificationMarkerPosition((int)(bufferSizeInBytes / 4.05));
 	    
    	return track;
    }*/
