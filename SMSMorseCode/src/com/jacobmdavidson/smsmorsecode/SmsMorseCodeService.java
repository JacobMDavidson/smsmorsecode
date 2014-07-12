package com.jacobmdavidson.smsmorsecode;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.AudioTrack;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

// Google Analytics is not used in this release
// import com.google.analytics.tracking.android.EasyTracker;
// import com.google.analytics.tracking.android.MapBuilder;

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

public class SmsMorseCodeService extends Service implements AudioTrack.OnPlaybackPositionUpdateListener, 
		OnAudioFocusChangeListener {
	
	// Receiver that detects when screen is turned on or off (used to stop message play back)
	private BroadcastReceiver screenReceiver;
	
	// The sender and body for each text message to be played
	private String body;
	
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
	
	// Timer used to play back each vibration in the queue
	private Timer myTimer;
	
	// Flag used to determine if vibration or audio is already running
	private boolean serviceStartredFlag = false;
	
	// Used to listen to the phone state (ringing, off hook, etc)
	private PhoneStateListener phoneListener;
	private TelephonyManager telephonyManager;
	
	// Counts the number of times the screen state has changed
	private int screenStateChangeCount = 0;
	
	// Google analytics tracker (not used in this release)
	// EasyTracker easyTracker = null;
	
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
		// Instantiate the EasyTracker object (not used in this release)
		// easyTracker = EasyTracker.getInstance(this);
		
		// Create a broadcast receiver that detects when the screen is turned on or off
		final IntentFilter theFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
		theFilter.addAction(Intent.ACTION_SCREEN_ON);
		screenReceiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				
				// Increment the number of times the screen state has changed
				screenStateChangeCount++;
				
				// If the screen state has changed twice, indicates the user wants to quit
				if(screenStateChangeCount >= 2)
				{
					if (ringerPreference == AudioManager.RINGER_MODE_NORMAL){
						
						// if the ringer preference is normal, terminate the audio 
						terminateAudio();
					} else {
						/*
						 *  This Should never be called, if phone is in silent mode the service 
						 *  terminates immediately. 
						 */
						SmsMorseCodeService.this.stopSelf();
					}
				
				// Else if in vibrate and the screen was turned off, terminate on one screen change
				} else if (ringerPreference == AudioManager.RINGER_MODE_VIBRATE && 
						intent.getAction() == Intent.ACTION_SCREEN_OFF){
					terminateVibrate();	
				}
			}
		};
		
		// Register the broadcast receiver
		this.registerReceiver(screenReceiver, theFilter);
		
		// Initialize the queue that will store messaged
		messages = new LinkedList<Intent>();
		
		// Get shared preferences as set in the main activity
		sharedPrefs = getSharedPreferences("com.jacobmdavidson.smsmorsecode", MODE_PRIVATE);
		
		// Instantiate the AudioManager and set the ringerPreference before the service begins
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
		 *  If this is the first time onStart is called, set the serviceStartedFlag to true and call 
		 *  getNextMessage() directly
		 */
		if(!serviceStartredFlag){
			serviceStartredFlag = true;
			getNextMessage();
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
		
		// set the body field to the body in the Intent
		body = intent.getStringExtra("body");
	
		// If the ringer preference is normal, play the morse code
		if(ringerPreference == AudioManager.RINGER_MODE_NORMAL){
			playMorseCode();
		
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
	public void playMorseCode() {
		// Send the event to google analytics (not used in this release)
		/*
		 * easyTracker.send(MapBuilder
		 *	      .createEvent("service_action",     // Event category (required)
		 *	                   "play_message",  // Event action (required)
		 *	                   "message_audible",   // Event label
		 *	                   null)            // Event value
		 *	      .build()
		 *	  );
		 */
		
		// Load the morseCodeTrack with the body of the message
		morseCodeAudioTrack.loadAudioTrack(body);
		
		// Set a playback position listener for the track
		morseCodeAudioTrack.getTrack().setPlaybackPositionUpdateListener(this);
		
		// Set a notification marker for the end of the track
		morseCodeAudioTrack.getTrack().setNotificationMarkerPosition(morseCodeAudioTrack.getTotalFrameCount());
    	
    	// Play the morse code audio track in a new thread
    	Thread playThread = new Thread(new Runnable(){
			@Override
			public void run() {
				morseCodeAudioTrack.playAudioTrack();
			}
    		
    	});
    	playThread.start();
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
		// Send the event to google analytics (not used in this release)
		/*
		 * easyTracker.send(MapBuilder
		 *	      .createEvent("service_action",     // Event category (required)
		 *	                   "play_message",  // Event action (required)
		 *	                   "message_vibrate",   // Event label
		 *	                   null)            // Event value
		 *	      .build()
		 *	  );		
		 */	
				
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
		
		// Abandon audio focus of the music stream
		audioManager.abandonAudioFocus(this);
		
		// Terminate the morse code track
		morseCodeAudioTrack.terminateAudioTrack();		
		
		// Stop the service
		this.stopSelf();
	}
	
	/**
	 * Releases the morse code track, stops the vibrator, sets the ringer mode back to the ringer
	 * preference, unregisters the phone state listener, and stops the service.
	 */
	public void terminateVibrate(){
		// Cancel the TimerTask to prevent queued messages from playing
		myTimer.cancel();
		
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
		
		// Unregister the screen receiver
        this.unregisterReceiver(screenReceiver);
	}
}
