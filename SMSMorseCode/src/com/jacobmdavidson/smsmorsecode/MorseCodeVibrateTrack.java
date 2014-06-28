package com.jacobmdavidson.smsmorsecode;

import android.content.Context;
import android.os.Vibrator;

/**
 * Used in conjunction with the MorseCodeCoverter class, the MorseCodeVibrateTrack
 * class converts a string of text into an array suitable for use with a Vibrator
 * object to vibrate the text translated as morse code, and uses a Vibrator object
 * to vibrate the translated morse code.
 * @author Jacob Davidson
 * @version 1.0.0
 * @
 */
public class MorseCodeVibrateTrack{
	
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
	
	/*
	 *  The string converted to a series of 0's and 1's representing the message
	 *  translated to morse code
	 */
	private String morseCodeString;
	
	// Vibrator object to play vibration
	private Vibrator vibrator;
	
	// The array that is passed to the Vibrator object to vibrate the morse code
	private long[] vibrateConversion;
	
	// The duration setting for the basic unit of the morse code translation
	private double durationMs;
	
	// The duration of the vibration in milliseconds
	private long vibrateDuration;
	
	/**
	 * Sets the duration of the basic unit of the morse code translation,
	 * loads the vibration track, and instantiates the Vibrator object
	 * @param text the string of text to be converted to a morse code vibration
	 * @param wordsPerMinute the speed of the vibrated translation in words per 
	 * minute (based on the word 'Paris' which is 50 units long including the 
	 * space after the word)
	 */
	public MorseCodeVibrateTrack(Context context, String text, int wordsPerMinute){
		vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
		setDurationMs(wordsPerMinute);
		loadVibrateTrack(text);
	}
	

	/**
	 * Set the duration of an individual unit of the translated morse code
	 * @param wordsPerMinute speed of the translated vibration in words per minute
	 */
	private void setDurationMs(int wordsPerMinute) {
		durationMs = (MILLIS_PER_MINUTE/(AVERAGE_UNITS_PER_WORD * wordsPerMinute));
	}

	/**
	 * Translates the provided text into an equivalent morse code string and converts the
	 * morse code string to an array suitable for vibration via a Vibrator object
	 * @param text the string of text to be converted to morse code
	 */
	public void loadVibrateTrack(String text) {
		
		/*
		 *  Set morseCodeString (returns a string of 0s and 1s indicating the order in 
		 *  which vibrations are played)
		 */
		morseCodeString = MorseCodeConverter.pattern(text);
		
		// Convert the string to an array for a Vibrator object
		vibrateConversion(morseCodeString);		
	}
	
	/**
	 * Convert a string of 0's and 1's to an array of longs for use with a Vibrator
	 * object, and assign it to the vibrateConversion field. Calculate the total duration
	 * required to vibrate this array and assign it to the vibrateDuration field.
	 * @param str String of 0's and 1's representing the converted morse code. Each 1 represents
	 * a unit of vibrate, and each 0 represents a unit of non vibration.
	 */
    private void vibrateConversion(String str) {
    	
    	// Total number of morse code units represented in the string
        int length = str.length();
        int arrayLength, curCount;
        char curChar, prevChar;
        
        // vibrateDuration starts at 0, and the duration is summed as we loop through the array
        vibrateDuration = 0;
        
        /*
         * If the provided string is not empty, build the vibration array and determine 
         * the duration to vibrate that array
         */
        if(length > 0){
           
        	/*
        	 * Determine the required length of the pattern array. Note that
        	 * the array increases in size only when the string switches from 
        	 * 0 to 1, or from 1 to 0.
        	 */
        	arrayLength = 1;
			for(int i = 1; i < length; i++){
				if(str.charAt(i) != str.charAt(i - 1)){
					arrayLength++;
				}
			}
			
			
			//  Initialize the pattern array with the calculated length
			long[] pattern = new long[arrayLength];
           
           /*
            * For each position in the pattern array, count the number of adjacent 0's or 1's 
            * and add this number to the pattern to that position in the array. This indicates 
            * the number of units of vibrate or sleep (adjacent 0's or 1's) for each position.
            */
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
           
           //Add the last number to the pattern array and vibrateDuration
           pattern[patternIndex] = (curCount * (long)durationMs);
           vibrateDuration += pattern[patternIndex];
           
           /*
            * Change the first number of pattern to 0 to indicate that the vibration 
            * should start immediately. All strings coming from MorseCodeCoverter start with 0.
            */
           pattern[0] = 0;
           vibrateConversion = pattern;
           
        /* 
         * Else an empty string was passed to the method. set the vibrateConversion to an
         * empty pattern, and the vibrateDuration to 0.
         */
        } else {
        	long[] pattern = {0, -1};
        	vibrateConversion = pattern;
        	vibrateDuration = 0;
        }   	
        
    }
    
	/**
	 * Returns the array suitable for vibration via a Vibrator object
	 * @return The array to use in the vibrate method of a Vibrator object
	 */
	public long[] getVibrateTrack(){
		return vibrateConversion;
	}
	
	/**
	 * Get the total duration required to vibrate the entire vibrate array
	 * @return vibration duration of the vibrate array
	 */
	public long getVibrateDuration(){
		return vibrateDuration;
	}
	
	/**
	 * Vibrates the track stored in the vibrateConversion array
	 */
	public void playVibrateTrack(){
		vibrator.vibrate(getVibrateTrack(), -1);
	}
	
	/**
	 * Turns the vibrator off
	 */
	public void cancelVibrateTrack(){
		vibrator.cancel();
	}

}


