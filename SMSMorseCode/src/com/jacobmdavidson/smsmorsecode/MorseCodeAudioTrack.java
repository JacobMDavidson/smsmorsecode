package com.jacobmdavidson.smsmorsecode;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

/**
 * Converts a string to an AudioTrack loaded with a sine wave representing the audible
 * version of the string translated to morse code given a frequency and speed for the 
 * translation.
 * @author Jacob Davidson
 * @version 1.0.0
 */
public class MorseCodeAudioTrack{
	
	/**
	 * {@value #MILLIS_PER_SECOND}
	 * The number of milliseconds per second.
	 */	
	private static final double MILLIS_PER_SECOND = 1000.0;

	/**
	 * {@value #MILLIS_PER_MINUTE}
	 * The number of milliseconds per minute.
	 */
	public static final double MILLIS_PER_MINUTE = 60000.0;
	
	/**
	 * {@value #AVERAGE_UNITS_PER_WORD}
	 * The average number of morse code units per word, based on
	 * the word "Paris" which is 50 units long (including the space
	 * after the word).
	 */
	public static final double AVERAGE_UNITS_PER_WORD = 50.0;
	
	// The AudioTrack for the string converted to audible morse code
	private AudioTrack morseCode;
	
	// The provided string converted to 0's and 1's representing morse code 
	private String morseCodeString;
	
	// The frequency of the sine wave of the converted morse code
	private double freqHz;
	
	// The duration for an individual morse code unit in milliseconds
	private double durationMs;
	
	// The array that stores a sine wave of the converted morse code
	private short[] mBuffer;
	
	// The total number of frames in the morse code sine wave array
	private int totalFrameCount;
	
	/**
	 * Constructor sets the frequency of the sine wave, converts the provided words per
	 * minute to the duration per unit of morse code in milliseconds, initializes the 
	 * audiotrack, converts the provided string to a morse code sine wave, and loads the
	 * audiotrack with that sine wave.
	 * @param text the provided string of text that will be converted to morse code
	 * @param frequency the frequency at which to play the tone
	 * @param wordsPerMinute the speed of the vibrated translation in words per 
	 * minute (based on the word 'Paris' which is 50 units long including the 
	 * space after the word)
	 */
	public MorseCodeAudioTrack(String text, double frequency, int wordsPerMinute){
		setFreqHz(frequency);
		setDurationMs(wordsPerMinute);
		initializeAudioTrack();
		loadAudioTrack(text);
	}

	/**
	 * assign the provided frequency to the freqHz field
	 * @param frequency the sine wave frequency in hertz
	 */
	private void setFreqHz(double frequency) {
		freqHz = frequency;
	}
	
	/**
	 * Set the duration of an individual unit of the translated morse code
	 * @param wordsPerMinute speed of the translated vibration in words per minute
	 */
	private void setDurationMs(int wordsPerMinute) {
		
		durationMs = (MILLIS_PER_MINUTE/(AVERAGE_UNITS_PER_WORD * wordsPerMinute));
		
		/* The following adjusts the duration to eliminate glitching. This is not
		 * needed as the user is bound to a preset index of frequencies and speeds that
		 * ensure glitching will be eliminated. Enable this procedure only if allowing
		 * more frequencies and durations in the future
		 * 
		 * Adjust the duration for the given frequency to eliminate glitching (sine wave ends 
		 * at 0 for each unit)
		 *  
		 * # of frames for the given duration divided by the number of frames for a half period of
		 * the sine wave must be a whole number: 
		 * (Constants.SAMPLE_RATE * (durationMs / 1000)) % (Constants.SAMPLE_RATE / (2 * freqHz)) == 0
		 * This reduces to f * durationMs / 500 must be a whole number
		 * The number of frames per unit must also be a whole number
		 * (Constants.SAMPLE_RATE * durationMs / 1000.0) must be a whole number
		 * // The duration is multiplied by 100 for greater precision
		 * int duration = (int)((MILLIS_PER_MINUTE/(AVERAGE_UNITS_PER_WORD * wordsPerMinute)) * 100.0);
		 * double durationCheck;
		 * double frames;
		 *
		 * // Check to ensure sine wave ends at 0 for a morse code unit duration
		 * durationCheck = freqHz * ((double)duration / 100.0) / 500.0;
		 *
		 * // Calculate the number of frames for a morse code unit duration
		 * frames = (((double)Constants.SAMPLE_RATE) * (((double)duration / 100.0) / 1000.0));
		 *
		 *
		 * //  Must adjust duration until the number of frames per character is an even number, and 
		 * //  the sine wave is at 0 in the last frame. The duration is reduced until this happens.
		 * //  This eliminates the glitching but results in a message that may be slightly faster than
		 * //  the selected words per minute
		 *
		 * while((durationCheck != Math.round(durationCheck)) || (frames != Math.round(frames))){
		 *	
		 *	// The duration was multiplied by 100, so this actually only reduces is by .01 milliseconds
		 *	duration--; 
		 *	durationCheck = freqHz * ((double)duration / 100.0) / 500.0;
		 *	frames = (((double)Constants.SAMPLE_RATE) * (((double)duration / 100.0) / 1000.0));
		 * }
		 *
		 *
		 * //  Cast the duration as a double and divide by 100.0 before assigning it to the 
		 * //  durationMs field.
         *
		 * durationMs = ((double)duration / 100.0);
		 */
	}
	
	/**
	 * Sets the minimum biffer size and instantiates an AudioTrack that is assigned
	 * to the morseCode field.
	 */
	private void initializeAudioTrack() {
		
		// Set the buffer size
		int bufferSize =  AudioTrack.getMinBufferSize( Constants.SAMPLE_RATE, 
				AudioFormat.CHANNEL_OUT_MONO, 
				AudioFormat.ENCODING_PCM_16BIT);
		
		// Initialize the audio track
		morseCode = new AudioTrack(AudioManager.STREAM_MUSIC, Constants.SAMPLE_RATE, 
				AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
				bufferSize, AudioTrack.MODE_STREAM);
	}
	
	/**
	 * Converts the provided string to an equivalent morse code string, builds a sine
	 * wave given the frequency, duration, and sample rate settings (multiplying the relavent 
	 * portions of the sine wave by the 1 or 0 in the morse code string), and assigns it
	 * to the mBuffer field that will later be played by the audiotrack
	 * @param text Original text that will be converted to audible morse code
	 */
	public void loadAudioTrack(String text) {
		
		/* 
		 * Set morseCodeString (returns a string of 0s and 1s indicating the order in 
		 * which tones are played.
		 */
		morseCodeString = MorseCodeConverter.pattern(text);		
		
		/*
		 * Set indyFrameCount (frame count in the sine wave array for each individual 
		 * unit in the morseCodeString)
		 */
		int indyFrameCount = (int)(((double)Constants.SAMPLE_RATE) * 
				(durationMs / MILLIS_PER_SECOND));
		
		// Set totalFrameCount in the sine wave array
		int numCharacters = morseCodeString.length();
		totalFrameCount = indyFrameCount * numCharacters;
		
		/*
		 * Build the sine wave array
		 */
	    double[] mSound = new double[totalFrameCount];
	    mBuffer = new short[totalFrameCount];
	    int multiplier;
	    
	    // Multiply the generated sine wave by the numeric value of each character  
	    for(int j = 0; j < numCharacters; j++){
	    	multiplier = Character.getNumericValue(morseCodeString.charAt(j)); 
	    	
	    	// Create sine wave for each character
	    	for (int i = (j * (totalFrameCount / numCharacters)); 
	    			i < (j + 1) * (totalFrameCount / numCharacters); 
	    			i++) {
		    	// Sine wave value = sin(i * (2 * pi * freq / sample rate))
	    		mSound[i] = Math.sin((double)i * 
		    			(2.0 * Math.PI * freqHz/((double)Constants.SAMPLE_RATE))) * 
		    			(double)multiplier;
	    		
	    		/*
	    		 *  Multiply the above value by the maximum value for a short and assign it to
	    		 *   the mBuffer array (which is the array that will be played by the audiotrack)
	    		 */
		    	mBuffer[i] = (short) (mSound[i] * Short.MAX_VALUE);
		    }
	    }			
	}
	
	/**
	 * plays the audio track stored in the morseCode field
	 */
	public void playAudioTrack() {
		// Set the volume for the audio track
		morseCode.setStereoVolume(1.0f, 1.0f);
		
		// Begin playing the audio track
		morseCode.play();
		
		// Write mBuffer to the audio track
		morseCode.write(mBuffer, 0, mBuffer.length);
	}
	
	/**
	 * Pauses, flushes, and releases the audio track
	 */
	public void terminateAudioTrack(){
		
		if (morseCode != null)
		{
			morseCode.pause();
			morseCode.flush();
			morseCode.release();
		}
	}

	/**
	 * Get the total frame count for the sine wave in the array assigned to the mBuffer
	 * @return the total frame count for the loaded audio track
	 */
	public int getTotalFrameCount(){
		return totalFrameCount;
	}
	
	/**
	 * Get the duration for a unit of morse code as defined by the speed settings selected 
	 * by the user.
	 * @return duration per unit of morse code in milliseconds.
	 */
	public double getDuration(){
		return durationMs;
	}
	
	/**
	 * Get the AudioTrack object stored in the morseCode field
	 * @return the morseCode AudioTrack object.
	 */
    public AudioTrack getTrack(){
    	return morseCode;
    }


}


