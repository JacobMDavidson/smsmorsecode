package com.jacobmdavidson.smsmorsecode;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.ContactsContract.PhoneLookup;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
/**
 * @ TODO
 * onstart instantiate the MorseCodeTrack
 * On each subsequent call to onutterence completed, initialize the audiotrack to the new string without instantiating a new morsecodetrack
 * @author jacobdavidson
 *
 */

//@ TODO may not need OnUtteranceComleted

@SuppressWarnings("deprecation")
public class MyService extends Service implements OnInitListener, OnUtteranceCompletedListener, AudioTrack.OnPlaybackPositionUpdateListener, OnAudioFocusChangeListener {
	TextToSpeech ttobj; //Must use in custom service
	String sender;
	String body;
	//String morseCodePattern;
	//AudioTrack morseCode;
	int ringerPreference;
	Queue<Intent> messages;
	private SharedPreferences sharedPrefs;
	MorseCodeTrack morseCodeTrack;
	AudioManager am;
	OnAudioFocusChangeListener afChangeListener;
	Vibrator v;
	long[] track;
	Timer myTimer;
	boolean vibrateFlag = false;
	
	private PhoneStateListener phoneListener;
	
	// SettingsContentObserver to listen for changes in volume
	private SettingsContentObserver mSettingsContentObserver;
	
	
	private TelephonyManager tm;
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	@Override
	public void onCreate() {	
		messages = new LinkedList<Intent>();
		
		// Get saved preferences
		sharedPrefs = getSharedPreferences("com.jacobmdavidson.smsmorsecode", MODE_PRIVATE);
		v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		phoneListener = new PhoneStateListener(){
			@Override
			public void onCallStateChanged(int state, String incomingNumber) {
				switch(state){
					//If the phone is not idle, stop the service
					case TelephonyManager.CALL_STATE_RINGING:
					case TelephonyManager.CALL_STATE_OFFHOOK:
						Log.d(Constants.LOG, "end");
						incomingCall();
						break;
					default:
						break;
				}
				
			}
		};
		tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		
		/**
		 *  Load the audioTrack
		 */
		double freqHz;
		int wordsPerMinute;
		//Get the frequency and set freqHz accordingly
		int frequencySetting = sharedPrefs.getInt("FrequencySetting", Constants.DEFAULT_FREQUENCY);
		freqHz = MainActivity.getFrequency(frequencySetting);
		
		int speedSetting = sharedPrefs.getInt("SpeedSetting", Constants.DEFAULT_SPEED);
		
		// Determine duration per unit based on 'PARIS' method (50 units makes up the word paris
		// msec/unit = (1 min / number of words) * (1 word / 50 units) * (60000 msec / min)
		wordsPerMinute = MainActivity.getWordsPerMinute(speedSetting);

       
    	// Build the AudioTrack and play it
    	morseCodeTrack = new MorseCodeTrack("", freqHz, wordsPerMinute);		
    	
    	
    	am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
    	ringerPreference = am.getRingerMode();
    	
    	

    	if(ringerPreference == AudioManager.RINGER_MODE_SILENT){
    		this.stopSelf();
    	}
    	else if(ringerPreference == AudioManager.RINGER_MODE_NORMAL){
    		// Instantiate the settingsContentObserver to detect volume change presses
    		mSettingsContentObserver = new SettingsContentObserver(this,new Handler());
    		getApplicationContext().getContentResolver().registerContentObserver(
    				android.provider.Settings.System.CONTENT_URI, true, mSettingsContentObserver );
    		
    		am.requestAudioFocus(this,
                    // Use the music stream.
                    AudioManager.STREAM_MUSIC,
                    // Request permanent focus.
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
    		ttobj = new TextToSpeech(this, this);
    	} else {
    		// This is a vibrate message, create the timer
    		myTimer = new Timer();
    	}
    	// Turn off the ringer and add the PhoneStateListener
    	am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
    	tm.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);
    	


    	
		
	}
	@Override
	public void onStart(Intent intent, int startId) {
		Log.d(Constants.LOG, "onStart Called");
		messages.add(intent);	
		// if it's a vibrate message, call getNextMessage() directly
		if(ringerPreference == AudioManager.RINGER_MODE_VIBRATE && !vibrateFlag){
			vibrateFlag = true;
			getNextMessage();
		}
	}
	
	public void getNextMessage()
	{
		
		Log.d(Constants.LOG, "getNextMessage Called");
		Intent intent = messages.poll();
		String number = intent.getStringExtra("sender");
		String caller = intent.getStringExtra("caller");
		if(caller.equals("MainActivity")){
			//sender = number;
			sender = "";
		} else {
			sender = "new message from " + getContactName(number);
		}
		body = intent.getStringExtra("body");
		
		
		
		
		if(ringerPreference == AudioManager.RINGER_MODE_NORMAL){
			Log.d(Constants.LOG, "wrong one Called");
			HashMap<String, String>params = new HashMap<String, String>(); 
			params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "theUtId");
			
			ttobj.speak(sender, TextToSpeech.QUEUE_FLUSH, params);
		
		//otherwise do vibrate
		}else{

			vibrateMorseCode();
			
		}

	}
	
	@Override
	public void onDestroy() {
		
        Log.d(Constants.LOG, "Service is destroyed");	
        //super.onDestroy();
	}
	@Override
	public void onUtteranceCompleted(String arg0) {
		/*Log.d(Constants.LOG, "utterance completed");
		//@ TODO figure out why onMarkerReached is not being called

		// Build and play the morse code (try setting the marker here and making sizeInBytes a field
		String morseCodePattern = MorseCodeConverter.pattern(body);
	    AudioTrack morseCode = generateMorseCode(morseCodePattern);
	    
		morseCode.play();	*/
		
		//
		//
		//

		/*
		double freqHz;
		int wordsPerMinute;
		//Get the frequency and set freqHz accordingly
		int frequency = sharedPrefs.getInt("FrequencySetting", Constants.DEFAULT_FREQUENCY);
		freqHz = MainActivity.getFrequency(frequency);
		
		int duration = sharedPrefs.getInt("SpeedSetting", Constants.DEFAULT_SPEED);
		
		// Determine duration per unit based on 'PARIS' method (50 units makes up the word paris
		// msec/unit = (1 min / number of words) * (1 word / 50 units) * (60000 msec / min)
		wordsPerMinute = MainActivity.getWordsPerMinute(duration);

       
    	// Build the AudioTrack and play it
    	MorseCodeTrack morseCodeTrack = new MorseCodeTrack(body, freqHz, wordsPerMinute);*/
		morseCodeTrack.loadAudioTrack(body);
		// set setNotificationMarkerPosition according to audio length
		AudioTrack track = morseCodeTrack.getTrack();
    	track.setPlaybackPositionUpdateListener(this);
    	track.setNotificationMarkerPosition(morseCodeTrack.getTotalFrameCount());
    	morseCodeTrack.playAudioTrack();
    	//onMarkerReached();
    	
	}
	

	@Override
	public void onInit(int status) {
		ttobj.setOnUtteranceCompletedListener(this);
		 
	    if (status == TextToSpeech.SUCCESS) {
	            int result = ttobj.setLanguage(Locale.US);
	            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
	                getNextMessage();
	            } 
	     } 

	     
	}
	
	public String getContactName(String mobileno) {
		
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
					Uri.encode(mobileno));
		  Cursor cr = getApplicationContext().getContentResolver().query(uri,
						new String[] { PhoneLookup.DISPLAY_NAME }, null,  
									 null, null);
		  String displayName;
		  if (cr.getCount() > 0) {
			cr.moveToFirst();
			displayName = cr.getString(cr
					.getColumnIndex(PhoneLookup.DISPLAY_NAME));
		  } else { //Name does not exist, return "readable" version of number
			  StringBuilder number = new StringBuilder(mobileno);
			  int idx = number.length() - 4;
			  number.insert(idx, "-");
			  idx = idx - 3;
			  number.insert(idx, "-");
			  idx = idx - 3;
			  displayName = number.toString();
		  }
		  //Log.d(Constants.LOG, displayName);
		  return displayName;
		}
	
	/*
	private AudioTrack generateMorseCode(String morseCode)
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
    }
    */
	@Override
	public void onMarkerReached(AudioTrack track) {
		Log.d("com.jacobmdavidson.smsmorsecode", "marker reached:" + messages.size());
		track.stop();
		track.flush();
		if(messages.size() == 0){
			terminateAudio();
			//this.stopSelf();
		}
		else
			getNextMessage();
	}
	@Override
	public void onPeriodicNotification(AudioTrack track) {
		// TODO Auto-generated method stub
		
	}

	/* How MainActivity determines vibration
	long[] track;
	AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
	if(am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL)
		morseCodeTrack.playAudioTrack();
	else if(am.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE){
		track = morseCodeTrack.getVibrateTrack();
		Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
	*/
    /**
     * Show a notification while this service is running.
     
    @SuppressWarnings("deprecation")
	private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.local_service_started);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.ic_action_network_wifi_unlock, text,
                System.currentTimeMillis());
        
        Intent i = new Intent(this, MainActivity.class);

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, 0);
        
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP| Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.local_service_label),
                       text, contentIntent);
        

        notification.flags |= Notification.FLAG_NO_CLEAR;
        // Send the notification.
        startForeground(1, notification);
        
        //mNM.notify(1337, notification);
        
        //Log.d(Constants.LOG, "Notification should be showing");
        
    }
	*/

	
	
	//This now works
    public void onAudioFocusChange(int focusChange) {
    	
    	switch(focusChange){
    	
	    	case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
	    	case AudioManager.AUDIOFOCUS_LOSS:
	        	terminateAudio();
	            //this.stopSelf();
	            break;
	        default:
	        	break;
        }

    }
    
    // If a call is coming in, stop the audio track, turn the ringer on, and terminate the service
    public void incomingCall(){

    	Log.d(Constants.LOG, "stopping service for call");

    	if(ringerPreference == AudioManager.RINGER_MODE_NORMAL)
    	{
    		Log.d(Constants.LOG, "terminating wrong");
    		terminateAudio();
    		
    	} else {
    		terminateVibrate();
    	}
    	//this.stopSelf();
    }

	public void vibrateMorseCode(){
		morseCodeTrack.loadAudioTrack(body);
		track = morseCodeTrack.getVibrateTrack();
		long timeDelay = morseCodeTrack.getVibrateDuration();
		//vibrateHandler.sleep(timeDelay);
		//Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		myTimer.schedule(new TimerTask(){
			@Override
			public void run(){
				Log.d(Constants.LOG, "Timer run");
			if(messages.size() == 0){
				terminateVibrate();
				//MyService.this.stopSelf();
			}
			else
				getNextMessage();}
		}, timeDelay);
		v.vibrate(track, -1);
		//SystemClock.sleep(timeDelay);
		//am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
		/*if(messages.size() == 0){
			terminateVibrate();
			//am.setRingerMode(ringerPreference);
			//tm.listen(phoneListener, PhoneStateListener.LISTEN_NONE);
			this.stopSelf();
		}
		else
			getNextMessage();*/	
		
	}
	public void terminateAudio(){
		Log.d(Constants.LOG, "terminating wrong");
		if (ttobj != null) {
            ttobj.stop();
            ttobj.shutdown();
        }
		am.abandonAudioFocus(this);
		morseCodeTrack.terminateAudioTrack();
		tm.listen(phoneListener, PhoneStateListener.LISTEN_NONE);
		am.setRingerMode(ringerPreference);
		getApplicationContext().getContentResolver().unregisterContentObserver(mSettingsContentObserver);
		this.stopSelf();
	}
	public void terminateVibrate(){
		morseCodeTrack.getTrack().release();
		v.cancel();
		am.setRingerMode(ringerPreference);
		tm.listen(phoneListener, PhoneStateListener.LISTEN_NONE);
		this.stopSelf();
	}

	// Nested class to listen to volume changes
	public class SettingsContentObserver extends ContentObserver {
	    int previousVolume;
	    Context context;
	    
	    public SettingsContentObserver(Context c, Handler handler) {
	        super(handler);
	        context=c;
	        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        	previousVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
	    }

	    @Override
	    public boolean deliverSelfNotifications() {
	        return super.deliverSelfNotifications();
	    }

	    @Override
	    public void onChange(boolean selfChange) {
	        super.onChange(selfChange);
	        Log.d(Constants.LOG, "Volume Key Pressed");
	        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
	        int currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
	        int delta=previousVolume-currentVolume;

	        
	        if(delta>0)
	        {
	            Log.d(Constants.LOG, "Volume Down Pressed!");
	            terminateAudio();
	        }
	    }
	}
	


	

}
