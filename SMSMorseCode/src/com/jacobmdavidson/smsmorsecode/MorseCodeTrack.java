package com.jacobmdavidson.smsmorsecode;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Vibrator;
import android.util.Log;

public class MorseCodeTrack{
	private AudioTrack morseCode;
	private String originalText;
	private String morseCodeString;
	private long[] vibrateConversion;
	private double[] mSound;
	private short[] mBuffer;
	private double freqHz;
	private double durationMs;
	private int totalFrameCount;
	private int indyFrameCount;
	private int bufferSize;
	private long vibrateDuration;
	
	public MorseCodeTrack(String text, double frequency, int wordsPerMinute){
		
		//set freqHz
		setFreqHz(frequency);
		//set durationMs
		setDurationMs(wordsPerMinute);
		//set bufferSizeInBytes
		setBufferSize();
		//Initialize AudioTrack
		initializeAudioTrack();
		
		//Load audioTrack
		loadAudioTrack(text);

	}
	


	// Set the frequency based on the saved app preferences
	private void setFreqHz(double frequency) {
		freqHz = frequency;
		// Must make Constants.SAMPLE_RATE / (4 * f) even when setting frequency
		//freqHz = 735.0;
	}
	
	// Set the duration based on the saved app preferences
	private void setDurationMs(int wordsPerMinute) {
		
		// Adjust the duration for the given frequency to eliminate glitching (sine wave ends at 0 for each unit)
		// # of frames for the given duration divided by the number of frames for a half period of
		// the sine wave must be a whole number: 
		// (Constants.SAMPLE_RATE * (durationMs / 1000)) % (Constants.SAMPLE_RATE / (2 * freqHz)) == 0
		// This reduces to f * durationMs / 500 must be a whole number
		// The number of frames per unit must also be a whole number
		// (Constants.SAMPLE_RATE * durationMs / 1000.0) must be a whole number
		//int duration = (int)(selectedDuration * 100.0);
		
		// The duration is multiplied by 100 for greater precision
		int duration = (int)((60000.0/(50.0 * wordsPerMinute)) * 100.0);
		double durationCheck;
		double frames;
		
		// do not need to do the even check once the frame count is fixed below
		//int evenCheck;
		durationCheck = freqHz * ((double)duration / 100.0) / 500.0;
		
		frames = (((double)Constants.SAMPLE_RATE) * (((double)duration / 100.0) / 1000.0));
		
		// Must adjust duration until the number of frames per character is an even number, and
		// the last frame is when the sine wave is at 0.
		while((durationCheck != Math.round(durationCheck)) || (frames != Math.round(frames))){
			duration--; //duration is increase to ensure the final wpm is less than or equal to the requested wpm 
			durationCheck = freqHz * ((double)duration / 100.0) / 500.0;
			frames = (((double)Constants.SAMPLE_RATE) * (((double)duration / 100.0) / 1000.0));
		}
		//Log.d(Constants.LOG, "evenCheck: " + evenCheck + "\n");
		Log.d(Constants.LOG, "frames: " + frames + "\n");
		Log.d(Constants.LOG, "durationCheck: " + durationCheck + " = " + Math.round(durationCheck) + "\n");
		
		// Cast the duration as a double and divide by 100.0 before assigning it to the field durationMs
		durationMs = ((double)duration / 100.0);
		Log.d(Constants.LOG, "durationMs: " + durationMs + "\n");
		
	}
	
	// Set the buffer size
	private void setBufferSize() {
		bufferSize =  AudioTrack.getMinBufferSize(
				Constants.SAMPLE_RATE, 
				AudioFormat.CHANNEL_OUT_MONO, 
				AudioFormat.ENCODING_PCM_16BIT);
		
	}
	private void initializeAudioTrack() {
		//set AudioTrack
		/*morseCode = new AudioTrack(AudioManager.STREAM_MUSIC, Constants.SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                bufferSize, AudioTrack.MODE_STREAM);*/
         morseCode = new AudioTrack(AudioManager.STREAM_MUSIC, Constants.SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize, AudioTrack.MODE_STREAM);
	}
	
	public void loadAudioTrack(String text) {
		//set originalText
		originalText = text;
		
		// Set morseCodeString (returns a string of 0s and 1s indicating the order in which tones are played
		morseCodeString = MorseCodeConverter.pattern(originalText);
		
		// Set the vibrate conversion
		vibrateConversion(morseCodeString);
		
		// Set indyFrameCount (frame count for each character in the morseCodeString
		// this does not need to be an even number of frames, change it and change duration calc
		// indyFrameCount = (int)(((double)Constants.SAMPLE_RATE) * 2.0 * (durationMs / 1000.0)) & ~1;
		// indyFrameCount = (int)(((double)Constants.SAMPLE_RATE) * 2.0 * (durationMs / 1000.0));
		indyFrameCount = (int)(((double)Constants.SAMPLE_RATE) * (durationMs / 1000.0));
		
		// Set totalFrameCount
		int numCharacters = morseCodeString.length();
		totalFrameCount = indyFrameCount * numCharacters;
		
		// Set sound arrays
	    // Sine wave
	    mSound = new double[totalFrameCount];
	    mBuffer = new short[totalFrameCount];
	    int multiplier;
	    //Log.d(Constants.LOG, "morseCodeString: " + morseCodeString + "\n");
	    Log.d(Constants.LOG, "indyFrameCount: " + indyFrameCount + "\n");
	    Log.d(Constants.LOG, "numCharacters: " + numCharacters + "\n");
	    Log.d(Constants.LOG, "totalFrameCount: " + totalFrameCount + "\n");
	    Log.d(Constants.LOG, "frequency: " + freqHz + "\n");
	    Log.d(Constants.LOG, "duration: " + durationMs + "\n");
	    for(int j = 0; j < numCharacters; j++){
	    	multiplier = Character.getNumericValue(morseCodeString.charAt(j));
	    	//Log.d(Constants.LOG, multiplier + ", " );
	     
	    	// Create sound wave
		    for (int i = (j * (mSound.length / numCharacters)); i < (j + 1) * (mSound.length / numCharacters); i++) {
		    	//mSound[i] = Math.sin((2.0 * Math.PI * freqHz/((double)Constants.SAMPLE_RATE)*(double)i)) * (double)multiplier;
		    	mSound[i] = Math.sin((double)i * (2.0 * Math.PI * freqHz/((double)Constants.SAMPLE_RATE))) * (double)multiplier;
		    	mBuffer[i] = (short) (mSound[i] * Short.MAX_VALUE);
		    }
	     
	    }		
		
		
	}
	public void playAudioTrack(){
	     morseCode.setStereoVolume(1.0f, 1.0f);
	     morseCode.play();
	     morseCode.write(mBuffer, 0, mSound.length);
	     //morseCode.stop();
	     //morseCode.release();
	     //morseCode.flush();
	}
	public void terminateAudioTrack(){
		morseCode.stop();
		morseCode.release();
	}
	public void flushAudioTrack(){
		morseCode.stop();
		morseCode.flush();
	}
	public void releaseTrack(){
		morseCode.release();
	}
	
	public long[] getVibrateTrack(){
		return vibrateConversion;
	}
	public int getTotalFrameCount(){
		return totalFrameCount;
	}
	public double getDuration(){
		return durationMs;
	}
	
	
	//Converts string to vibration array
    private void vibrateConversion(String str) {
        int length = str.length();
        int arrayLength;
        int curCount;
        char curChar, prevChar;
        vibrateDuration = 0;
        if(length > 0){
           //Determine the required length of the pattern array
           arrayLength = 1;
           for(int i = 1; i < length; i++){
              if(str.charAt(i) != str.charAt(i - 1)){
                 arrayLength++;
              }
           }
           long[] pattern = new long[arrayLength];
           
           //Count the number of adjacent 0's and 1's and add this number to the pattern array
           //This indicates the number of units of vibrate or sleep
           int patternIndex = 0;
           curCount = 1;
           for (int i = 1; i < str.length(); i++){
              curChar = str.charAt(i);
              prevChar = str.charAt(i - 1);
              if(curChar == prevChar){
                 curCount++;
              } else {
                 pattern[patternIndex] = (curCount * (long)durationMs);
                 vibrateDuration += pattern[patternIndex++];
                 curCount = 1;   
              } 
           }
           
           //Add the last number to the pattern array
           pattern[patternIndex] = (curCount * (long)durationMs);
           vibrateDuration += pattern[patternIndex];
           
           //Change the first number of pattern to 0 to indicate that the vibration should start immediately
           pattern[0] = 0;
           vibrateConversion = pattern;
        } else {
        	//Return an array that does not vibrate at all
        	long[] pattern = {0, -1};
        	vibrateConversion = pattern;
        }   	
        
    }
    public AudioTrack getTrack(){
    	return morseCode;
    }
	public long getVibrateDuration(){
		return vibrateDuration;
	}

}


