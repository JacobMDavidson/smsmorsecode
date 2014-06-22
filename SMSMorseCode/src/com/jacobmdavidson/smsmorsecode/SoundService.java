package com.jacobmdavidson.smsmorsecode;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.IBinder;
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


@SuppressWarnings("deprecation")
public class SoundService extends Service implements OnInitListener, OnUtteranceCompletedListener, AudioTrack.OnPlaybackPositionUpdateListener, OnAudioFocusChangeListener {
	private TextToSpeech ttobj;
	private String sender, body;
	private Queue<Intent> messages;
	private SharedPreferences sharedPrefs;
	private MorseCodeTrack morseCodeTrack;
	private AudioManager am;
	//OnAudioFocusChangeListener afChangeListener;
	private PhoneStateListener phoneListener;
	private TelephonyManager tm;
	
	// Called when service is created
	@Override
	public void onCreate() {
		Log.d(Constants.LOG, "Sound Service created");	
		messages = new LinkedList<Intent>();
		ttobj = new TextToSpeech(this, this);
		// Get saved preferences
		sharedPrefs = getSharedPreferences("com.jacobmdavidson.smsmorsecode", MODE_PRIVATE);

		phoneListener = new PhoneStateListener(){
			@Override
			public void onCallStateChanged(int state, String incomingNumber) {
				Log.d(Constants.LOG, "incoming call");
				switch(state){
					//If the phone is not idle, stop the service
					case TelephonyManager.CALL_STATE_RINGING:
						//incomingCall();
						//break;
					case TelephonyManager.CALL_STATE_OFFHOOK:
						incomingCall();
						break;
					default:
						break;
				}
				
			}
		};
		tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		tm.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);
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

       
    	// Build the AudioTrack
    	morseCodeTrack = new MorseCodeTrack("", freqHz, wordsPerMinute);		
    	
    	// Request focus of the music stream
    	am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
    	
    	am.requestAudioFocus(this,
                    // Use the music stream.
                    AudioManager.STREAM_MUSIC,
                    // Request permanent focus.
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
    	
    	// Set ringerMode to silent to disable text message notifications
    	am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
    }
		
	// Called every time service is called
	@Override
	public void onStart(Intent intent, int startId) {
		messages.add(intent);	
	}
	
	// Called when text to speech engine is initialized
	@Override
	public void onInit(int status) {
		// set the listener for when speech is complete
		ttobj.setOnUtteranceCompletedListener(this);
		 
		
	    if (status == TextToSpeech.SUCCESS) {
	            int result = ttobj.setLanguage(Locale.US);
	            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
	                getNextMessage();
	            } 
	     } 

	     
	}
	
	public void getNextMessage()
	{	
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
			
		
		
		// Speak the sender's name or number
		HashMap<String, String>params = new HashMap<String, String>(); 
		params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "theUtId");
		ttobj.speak(sender, TextToSpeech.QUEUE_FLUSH, params);
		
	}
	
	// Called after sender is spoken
	@Override
	public void onUtteranceCompleted(String arg0) {
		// When done speaking sender, load the morseCodeTrack
		morseCodeTrack.loadAudioTrack(body);
		
		// set setNotificationMarkerPosition according to audio length
		morseCodeTrack.getTrack().setPlaybackPositionUpdateListener(this);
		morseCodeTrack.getTrack().setNotificationMarkerPosition(morseCodeTrack.getTotalFrameCount());
    	
    	// Play the morseCodeTrack
    	morseCodeTrack.playAudioTrack();
	}
	
	// When the morseCodeTrack has finished playing, check for more messages
	@Override
	public void onMarkerReached(AudioTrack track) {
		Log.d("com.jacobmdavidson.smsmorsecode", "marker reached:" + messages.size());
		track.stop();
		track.flush();
		
		if(messages.size() == 0){
			//Release the music stream
			
			track.release();
			
			
			//Turn the music stream back on
			//am.setStreamMute(AudioManager.STREAM_MUSIC, false);
			this.stopSelf();
		}
		else
			getNextMessage();
	}
	
	// Called when service is stopped
	@Override
	public void onDestroy() {
		
		if (ttobj != null) {
            ttobj.stop();
            ttobj.shutdown();
        }
		// Turn ringer back on
		am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
		//am.setStreamSolo(AudioManager.STREAM_RING, false);
		//am.abandonAudioFocus(afChangeListener);
		am.abandonAudioFocus(this);
		//morseCodeTrack.terminateAudioTrack();
        Log.d(Constants.LOG, "Service is destroyed");	
        super.onDestroy();
	}
	


	// Look up the contact's name
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
	
	
	// This method is not needed
	@Override
	public void onPeriodicNotification(AudioTrack track) {
		// TODO Auto-generated method stub
		
	}
	// Method is not required for this service
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	
	//If the audiofocus is changed, terminate the audiotrack and stop the service
    public void onAudioFocusChange(int focusChange) {
    	
    	switch(focusChange){
    	
	    	case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
	    		//morseCodeTrack.getTrack().pause();
	    		//break;
	    	case AudioManager.AUDIOFOCUS_GAIN:
	    		//morseCodeTrack.getTrack().play();
	    		//break;
	    	case AudioManager.AUDIOFOCUS_LOSS:
	        	//Log.d(Constants.LOG, "loss");
	        	morseCodeTrack.terminateAudioTrack();
	            am.abandonAudioFocus(this);
	            this.stopSelf();
	            break;
	        default:
	        	break;
        }

    }
    
    // If a call is coming in, stop the audio track, turn the ringer on, and terminate the service
    public void incomingCall(){
    	morseCodeTrack.terminateAudioTrack();
    	//am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
    	Log.d(Constants.LOG, "stopping vibration");
    	this.stopSelf();
    }	

}

