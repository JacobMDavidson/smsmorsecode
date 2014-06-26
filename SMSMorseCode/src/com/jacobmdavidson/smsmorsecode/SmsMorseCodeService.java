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
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.provider.ContactsContract.PhoneLookup;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

/**
 * <p>This class instantiates a service that converts sms text messages sent via a broadcast receiver 
 * to Morse Code. The class respects the users ringer settings via the following:</p> 
 * <ol><li> If the ringer is set to normal, this service audibly plays the converted morse code.</li>
 * <li>If the ringer is set to vibrate, this service vibrates the converted morse code.</li>
 * <li>If the ringer is set to silent, this service is immediately destoryed.</li></ol>
 * <p>If a call is received while the service is running, the ringer is set back to the
 * initial user preferences, and the service is destroyed.</p>
 * @author Jacob Davidson
 * @version 1.0.0
 */

@SuppressWarnings("deprecation")
public class SmsMorseCodeService extends Service implements OnInitListener, 
		OnUtteranceCompletedListener, AudioTrack.OnPlaybackPositionUpdateListener, 
		OnAudioFocusChangeListener {

	// Used to speak the sender's name or phone number
	private TextToSpeech textToSpeech;
	
	// The sender and body for each text message to be played
	private String sender, body;
	
	// Stores the ringer setting prior to playing the morsecode
	private int ringerPreference;
	
	// Stores the queue of messages to be played
	private Queue<Intent> messages;
	
	// Stores the saved Shared preferences as set in the applications main activity
	private SharedPreferences sharedPrefs;
	
	// Stores the body converted to a morse code track for play back or vibration
	private MorseCodeAudioTrack morseCodeAudioTrack;
	
	// Stores the body converted to a morse code vibration track
	private MorseCodeVibrateTrack morseCodeVibrateTrack;
	
	// Stores the AudioManager object from which the ringer settings are determined
	private AudioManager audioManager;
	
	// Used to vibrate the morse code track when ringerPreference is vibrate
	// private Vibrator vibrator;
	
	// Timer used to play back each vibration in the queue
	private Timer myTimer;
	
	// Flag used to determine if vibration is already running
	private boolean vibrateFlag = false;
	
	// Used to listen to the phone state (ringing, off hook, etc)
	private PhoneStateListener phoneListener;
	
	// Used to listen for changes in volume
	private SettingsContentObserver mSettingsContentObserver;
	private TelephonyManager telephonyManager;
	
	/**
	 * This service is not bound to an activity. Everything happens in the background
	 */
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	/**
	 * OnCreate is only called when the service is initially started. Instiantiate the messages queue,
	 * get the shared preferences, instantiate the morse code track, Check the ringer mode to determine id
	 * this will be a vibrate morse code message, audio morse code message, or if this service will be destroyed.
	 * For an audio message set up the SettingsContentObserver to detect changes in volume, request audio focus
	 * of the music stream, and initialize the text to speech engine. For a vibrate message instantiate a timer 
	 * used to check for another message when vibration is complete, and instantiate the Vibrator object. Finally, 
	 * set the ringer mode to silent to prevent notifications for incoming text messages to be played and set
	 * up a telephony manager that listens for incoming calls, and calls the incomingCall method when it detects
	 * one.
	 */
	@Override
	public void onCreate() {	
		
		// Initialize the queue that will store messaged
		messages = new LinkedList<Intent>();
		
		// Get shared preferences as set in the main activity
		sharedPrefs = getSharedPreferences("com.jacobmdavidson.smsmorsecode", MODE_PRIVATE);
		
		// Instantiate the audiomanager and set the ringerPreference before the service begins
    	audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
    	ringerPreference = audioManager.getRingerMode();

		//Get the shared frequency and speedSettings, and instantiate the morseCodeTrack
		int frequencySetting = sharedPrefs.getInt("FrequencySetting", Constants.DEFAULT_FREQUENCY);
		int speedSetting = sharedPrefs.getInt("SpeedSetting", Constants.DEFAULT_SPEED);
		double freqHz = MainActivity.getFrequency(frequencySetting);
		int wordsPerMinute = MainActivity.getWordsPerMinute(speedSetting);		  		

    	// If the ringerPreference is set to silent, stop the service
    	if(ringerPreference == AudioManager.RINGER_MODE_SILENT){
    		this.stopSelf();
    	}
    	
    	// If the ringerPreference is set to normal, play the morse code audio track
    	else if(ringerPreference == AudioManager.RINGER_MODE_NORMAL){
    		/* 
    		 * Instantiate the settingsContentObserver and register it to detect volume 
    		 * changes (volume down will be used to stop the service)
    		 */
    		mSettingsContentObserver = new SettingsContentObserver(this,new Handler());
    		getApplicationContext().getContentResolver().registerContentObserver(
    				android.provider.Settings.System.CONTENT_URI, true, mSettingsContentObserver );
    		
    		/*
    		 * Request audio focus of the music stream to temporarily stop music while
    		 * the morse code track is played, and set it to listen for audio changes
    		 */
    		audioManager.requestAudioFocus(this,
                    // Use the music stream.
                    AudioManager.STREAM_MUSIC,
                    // Request permanent focus.
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
    		
    		// Instantiate the morseCodeAudioTrack
    		morseCodeAudioTrack = new MorseCodeAudioTrack("", freqHz, wordsPerMinute);
    		
    		// Instantiate the textToSpeech object used to speak the name or number of the sender
    		textToSpeech = new TextToSpeech(this, this);
    	} 
    	
    	// Else, the ringer preference is set to vibrate, vibrate the morse code track
    	else {
    		/*
    		 * Instantiate a timer used to check for the next message after vibration of the current 
    		 * message is done
    		 */
    		myTimer = new Timer();
    		
    		// Instantiate the morseCodeVibrateTrack
    		morseCodeVibrateTrack = new MorseCodeVibrateTrack(this, "", wordsPerMinute);
    	}
    	
    	// Set the ringer mode to silent to silence incoming text message notifications
    	audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
    	
    	/*
    	 *  Instantiate a PhoneStateListener that calls the incomingCall() method if the phone
    	 *  state changes to ringing or offhook
    	 */
    	phoneListener = new PhoneStateListener(){
			@Override
			public void onCallStateChanged(int state, String incomingNumber) {
				switch(state){
					case TelephonyManager.CALL_STATE_RINGING:
					case TelephonyManager.CALL_STATE_OFFHOOK:
						incomingCall();
						break;
					default:
						break;
				}
				
			}
		};
    	
		// Instantiate the telephonyManager and set the state it should listen for
		telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
    	telephonyManager.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);	
	}
	
	/**
	 * onStart is called each time an intent is sent to the service, add the intent
	 * to the messages queue and, if the ringer mode is set to vibrate, call getNextMethod
	 * directly to begin vibration playback. If the ringer mode is set to normal, getNextMethod
	 * will be called in the onInit method when the text to speech engine has been initialized
	 */
	@Override
	public void onStart(Intent intent, int startId) {
		
		// Add the intent to the messages queue
		messages.add(intent);	
		
		/*
		 *  If the ringerPreference is Vibrate, and this is the first time onStart is called, 
		 *  set the vibrateFlag to true and call getNextMessage() directly
		 */
		if(ringerPreference == AudioManager.RINGER_MODE_VIBRATE && !vibrateFlag){
			vibrateFlag = true;
			getNextMessage();
		}
	}
	
	/**
	 * onInit is called when the TextToSpeech engine initialization is complete. Determine
	 * if initialization was a success, and if so, call getNextMessage 
	 */
	@Override
	public void onInit(int status) {
		
		/*
		 * Add a listener to the textToSpeech object to listen for when the utterence is complete
		 */
		textToSpeech.setOnUtteranceCompletedListener(this);
	 
		// If the engine is successfully initialized, set the language to US
	    if (status == TextToSpeech.SUCCESS) {
	    	int result = textToSpeech.setLanguage(Locale.US);
	    	
	    	// If the setting the language was successful, get the next message
		    if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
		        getNextMessage();
		        
		    // If setting the language was not successful, display a toast that the language is not supported
		    } else {
		    	//@ TODO provide toast stating language error
		    }
		    
		// there was an error in initializing text to speech, display a toast.
	    } else {
	        //@ TODO provide toast stating text to speech error
	    }
	}
	
	/**
	 * Get the next message, if it's a settings test, set the sender to an empty string. Otherwise
	 * create a sender string and get the contact name. set the body field to the body of the message.
	 * If the ringer preference is normal, use textToSpeech to speak the sender (onUtteranceCompleted will
	 * be called when this is finished). Otherwise, the ringer preference is vibrate, so call the 
	 * vibrateMorseCode method.
	 */
	public void getNextMessage()
	{
		// Get the Intent at the head of the messages queue
		Intent intent = messages.poll();
		
		// Get the number and caller from the intent
		String number = intent.getStringExtra("sender");
		String caller = intent.getStringExtra("caller");
		
		// set the sender field accordingly
		if(caller.equals("MainActivity")){
			sender = "";
		} else {
			sender = "new message from " + getContactName(number);
		}
		
		// set the body field to the body in the Intent
		body = intent.getStringExtra("body");
	
		// If the ringer preference is normal, speak the sender
		if(ringerPreference == AudioManager.RINGER_MODE_NORMAL){
			HashMap<String, String>params = new HashMap<String, String>(); 
			params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "theUtId");
			textToSpeech.speak(sender, TextToSpeech.QUEUE_FLUSH, params);
		
		//otherwise, the ringer preference is vibrate. Vibrate the message
		}else{
			vibrateMorseCode();
		}

	}
	
	/**
	 * Called when textToSpeech is finished speaking the username or number. Convert
	 * the body of the message to a morse code track, set a notification marker position
	 * to the end of the track, and play the audio track. The onMarkerReached method will be
	 * called when the morse code track is done playing.
	 */
	@Override
	public void onUtteranceCompleted(String arg0) {
		
		// Load the morseCodeTrack with the body of the message
		morseCodeAudioTrack.loadAudioTrack(body);
		
		// Set a playback position listener for the track
		morseCodeAudioTrack.getTrack().setPlaybackPositionUpdateListener(this);
		
		// Set a notification marker for the end of the track
		morseCodeAudioTrack.getTrack().setNotificationMarkerPosition(morseCodeAudioTrack.getTotalFrameCount());
		
		// Play the morse code audio track
    	morseCodeAudioTrack.playAudioTrack();
	}
	
	/**
	 * Looks up the provided number in the user's contacts and returns the display name if the user is found.
	 * If the user is not found, returns a formated phone number that the text to speech enginer will read
	 * correctly.
	 * @param phoneNumber the number used to lookup the user name
	 * @return a string with the display name if found, or phone number if user is not in contacts
	 */
	public String getContactName(String phoneNumber) {
		
		// Perform the query of using the phoneNumber 
		Uri lookupUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, 
				Uri.encode(phoneNumber));
		Cursor cursor = this.getContentResolver().query(lookupUri, 
						new String[] { PhoneLookup.DISPLAY_NAME }, null, null, null);
		
		String displayName;
		
		// If more than one matching numbers were found, select the user name from the first instance.
		if (cursor.getCount() > 0) {
			cursor.moveToFirst();
			displayName = cursor.getString(cursor.getColumnIndex(PhoneLookup.DISPLAY_NAME));
			
		// Else the name does not exist, return a readable phone number
		} else {
			StringBuilder number = new StringBuilder(phoneNumber);
			int idx = number.length() - 4;
			number.insert(idx, "-");
			idx = idx - 3;
			number.insert(idx, "-");
			idx = idx - 3;
			displayName = number.toString();
		}
		
		return displayName;
	}
	
	/**
	 * Called when that morse code audio track finishes playing, stop and flush
	 * the track. If the messages queue is empty, call the terminateAudio method,
	 * otherwise call the getNextMessage method
	 */
	@Override
	public void onMarkerReached(AudioTrack track) {
		
		// stop and flush the AudioTrack
		track.stop();
		track.flush();
		
		// if the messages queue is empty, terminate the audio and stop the service
		if(messages.size() == 0){
			terminateAudio();
		
		// else, get the next message and play it
		} else {
			getNextMessage();
		}
	}
	
	/**
	 * This method is not used
	 */
	@Override
	public void onPeriodicNotification(AudioTrack track) {
		// Method is not used
		
	}
	
	/**
	 * Listens for audio focus changes, and if the focus change is
	 * AUDIOFOCUS_LOSS_TRANSIENT or AUDIOFOCUS_LOSS, it terminates the audio
	 * which also stops the service
	 */
    public void onAudioFocusChange(int focusChange) {
    	switch(focusChange){
	    	case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
	    	case AudioManager.AUDIOFOCUS_LOSS:
	        	terminateAudio();
	            break;
	        default:
	        	break;
        }
    }
    
    /**
     * Called when detecting an incoming call. This will terminate the audio or vibration
     * playback which then stops the service allowing the call to come through.
     */
    public void incomingCall(){
    	if(ringerPreference == AudioManager.RINGER_MODE_NORMAL)
    	{
    		terminateAudio();	
    	} else {
    		terminateVibrate();
    	}
    }

    /**
     * Loads the message body into the morse code track, schedules a timer to check for the
     * next message at the end of the vibration, and vibrates the morse code track
     */
	public void vibrateMorseCode(){
		
		// Load the body into the morse code track
		morseCodeVibrateTrack.loadVibrateTrack(body);
		
		// Get the duration of the vibration
		long timeDelay = morseCodeVibrateTrack.getVibrateDuration();
		
		// Schedule the timer to check for the next message when the vibration is complete
		myTimer.schedule(new TimerTask(){
			
			@Override
			public void run(){	
				
				// If there are no more messages, terminate the service
				if(messages.size() == 0){
					terminateVibrate();
					
				// Else get the next message to be vibrated
				} else {
					getNextMessage();
				}
			}
		}, timeDelay);
		
		// Vibrate the track
		morseCodeVibrateTrack.playVibrateTrack();
	}
	
	/**
	 * terminates textToSpeech, abandons audio focus for the music stream, terminates the 
	 * morse code audio track, unregisters the telephonyManager listener, sets the ringer
	 * back to the ringerPreference, unregisters the volume observer, and stops the service
	 */
	public void terminateAudio(){
		
		// Set ringer back to the ringer preference
		audioManager.setRingerMode(ringerPreference);
		
		// Unregister the phone state listener
		telephonyManager.listen(phoneListener, PhoneStateListener.LISTEN_NONE);
		
		// Terminate and shutdown textToSpeech
		if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
		
		// Abandon audio focus of the music stream
		audioManager.abandonAudioFocus(this);
		
		// Terminate the morse code track
		morseCodeAudioTrack.terminateAudioTrack();
		
		// Unregister the volume change observer
		this.getContentResolver().unregisterContentObserver(mSettingsContentObserver);		
		
		// Stop the service
		this.stopSelf();
	}
	
	/**
	 * Releases the morse code track, stops the vibrator, sets the ringer mode back to the ringer
	 * preference, unregisters the phone state listener, and stops the service.
	 */
	public void terminateVibrate(){
		
		// Set ringer mode back to ringer preference
		audioManager.setRingerMode(ringerPreference);
		
		// Unregister the phone state listener
		telephonyManager.listen(phoneListener, PhoneStateListener.LISTEN_NONE);
		
		// Cancel the existing vibration
		morseCodeVibrateTrack.cancelVibrateTrack();
		
		// Stop the service
		this.stopSelf();
	}

	/**
	 * Called when the service is destroyed
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	/**
	 * Nested class used to detect volume changes and stop the service when
	 * the volume down key is pressed.
	 * @author Jacob Davidson
	 * @version 1.0.0
	 */
	public class SettingsContentObserver extends ContentObserver {
	    private int previousVolume;
	    private Context context;
	    
	    /**
	     * Sets the previousVolume and context fields
	     * @param context
	     * @param handler
	     */
	    public SettingsContentObserver(Context context, Handler handler) {
	        super(handler);
	        this.context = context;
	        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        	previousVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
	    }

	    @Override
	    public boolean deliverSelfNotifications() {
	        return super.deliverSelfNotifications();
	    }
	    
	    /**
	     * Detects a change in the volume setting of the music stream. If the volume is decrease, indicates
	     * the user wants to stop the service before completion so the terminateAudio method is called.
	     */
	    @Override
	    public void onChange(boolean selfChange) {
	        super.onChange(selfChange);
	        
	        // Get the current volume after the change
	        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
	        int currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
	        
	        // Compare the current volume to the previous volume.
	        int delta = previousVolume - currentVolume;
	        
	        // If delta is greater than 0, the volume down key was pressed, terminate the audio and stop the service
	        if(delta > 0)
	        {
	            terminateAudio();
	        
	        // Else set the previous volume to the current volume to detect a volume decrease on the next push
	        } else {
	        	previousVolume = currentVolume;
	        }
	    }
	}
	


	

}
