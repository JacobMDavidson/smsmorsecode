//THis class is not used

package com.jacobmdavidson.smsmorsecode;

public class MorseCodeVibrateConverter {
 
    //Start with a string {001111000011100000}
    //Convert to long[] {2, 4, 4, 3, 5}
    //Change the first to 0 (means start immediately)
    //Then just have to multiply each item by the duration in ms
    //Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    
    //v.vibrate(pattern, -1);
    static long[] patternConversion(String str) {
        int length = str.length();
        int arrayLength;
        int curCount;
        char curChar, prevChar;
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
                 pattern[patternIndex++] = curCount;
                 curCount = 1;   
              } 
           }
           
           //Add the last number to the pattern array
           pattern[patternIndex] = curCount;
           
           //Change the first number of pattern to 0 to indicate that the vibration should start immediately
           pattern[0] = 0;
           	return pattern;
        } else {
        	//Return an array that does not vibrate at all
        	long[] pattern = {0, -1};
        	return pattern;
        }   	
    	
    }

}
