package com.jacobmdavidson.smsmorsecode;

/**
 * Converts a string of characters to a string of 0's and 1's representing
 * the original string translated to morse code following the ITU-R specification.
 * 
 * @author Jacob Davidson
 *
 */
public class MorseCodeConverter {
	
	/** A DOT is 1 unit of sound (represented by the string {@value #DIT}). */
    public static final String DIT = "1";
    
    /** A DASH is 3 units of sound (represented by the string {@value #DAH}). */
    public static final String DAH = "111";
    
    /** A GAP is 1 unit of silence (represented by the string {@value #GAP}). */
    public static final String GAP = "0";
    
    /** A LETTER_GAP is 3 units of silence 
     * (represented by the string {@value #LETTER_GAP}). */
    public static final String LETTER_GAP = "000";
    
    /** A WORD_GAP is 7 units of silence (represented by the string 
     * {@value #WORD_GAP}). */
    public static final String WORD_GAP = "0000000";
    
    /** END_OF_TEXT is the standard end of message transmission .-.-. 
     * (represented by the string {@value #END_OF_MESSAGE}). */ 
    public static final String END_OF_MESSAGE = WORD_GAP + DIT + GAP + DAH + GAP + 
    		DIT + GAP + DAH + GAP + DIT + WORD_GAP;
    
    /** START_OF_TEXT is the standard commencing transmission -.-.- 
     * (represented by the string {@value #START_OF_MESSAGE}). */
    public static final String START_OF_MESSAGE = WORD_GAP + DAH + GAP + DIT + GAP + 
    		DAH + GAP + DIT + GAP + DAH + WORD_GAP;
    
    /** Gap to use when an invalid character is encountered */
    private static final String ERROR_GAP = GAP;
    
    /** The morse code representation of the characters from '!' to 'Z' */
    private static final String[] CHARACTERS = new String[] {
    	/* ! */ DAH + GAP + DIT + GAP + DAH + GAP + DIT + GAP + DAH + GAP + DAH,
    	/* " */ DIT + GAP + DAH + GAP + DIT + GAP + DIT + GAP + DAH + GAP + DIT,
    	/* # */ ERROR_GAP,
    	/* $ */ DIT + GAP + DIT + GAP + DIT + GAP + DAH + GAP + DIT + GAP + DIT + GAP + DAH,
    	/* % */ ERROR_GAP,
    	/* & */ DIT + GAP + DAH + GAP + DIT + GAP + DIT + GAP + DIT,
    	/* ' */ DIT + GAP + DAH + GAP + DAH + GAP + DAH + GAP + DAH + GAP + DIT,
    	/* ( */ DAH + GAP + DIT + GAP + DAH + GAP + DAH + GAP + DIT,
    	/* ) */ DAH + GAP + DIT + GAP + DAH + GAP + DAH + GAP + DIT + GAP + DAH,
    	/* * */ ERROR_GAP,
    	/* + */ DIT + GAP + DAH + GAP + DIT + GAP + DAH + GAP + DIT,
    	/* , */ DAH + GAP + DAH + GAP + DIT + GAP + DIT + GAP + DAH + GAP + DAH,
    	/* - */ DAH + GAP + DIT + GAP + DIT + GAP + DIT + GAP + DIT + GAP + DAH,
    	/* . */ DIT + GAP + DAH + GAP + DIT + GAP + DAH + GAP + DIT + GAP + DAH,
    	/* / */ DAH + GAP + DIT + GAP + DIT + GAP + DAH + GAP + DIT,	
        /* 0 */ DAH + GAP + DAH + GAP + DAH + GAP + DAH + GAP + DAH,
        /* 1 */ DIT + GAP + DAH + GAP + DAH + GAP + DAH + GAP + DAH,
        /* 2 */ DIT + GAP + DIT + GAP + DAH + GAP + DAH + GAP + DAH,
        /* 3 */ DIT + GAP + DIT + GAP + DIT + GAP + DAH + GAP + DAH,
        /* 4 */ DIT + GAP + DIT + GAP + DIT + GAP + DIT + GAP + DAH,
        /* 5 */ DIT + GAP + DIT + GAP + DIT + GAP + DIT + GAP + DIT,
        /* 6 */ DAH + GAP + DIT + GAP + DIT + GAP + DIT + GAP + DIT,
        /* 7 */ DAH + GAP + DAH + GAP + DIT + GAP + DIT + GAP + DIT,
        /* 8 */ DAH + GAP + DAH + GAP + DAH + GAP + DIT + GAP + DIT,
        /* 9 */ DAH + GAP + DAH + GAP + DAH + GAP + DAH + GAP + DIT,  
        /* : */ DAH + GAP + DAH + GAP + DAH + GAP + DIT + GAP + DIT + GAP + DIT,
        /* ; */ DAH + GAP + DIT + GAP + DAH + GAP + DIT + GAP + DAH + GAP + DIT,
        /* < */ ERROR_GAP,
        /* = */ DAH + GAP + DIT + GAP + DIT + GAP + DIT + GAP + DAH,
        /* > */ ERROR_GAP,
        /* ? */ DIT + GAP + DIT + GAP + DAH + GAP + DAH + GAP + DIT + GAP + DIT,
        /* @ */ DIT + GAP + DAH + GAP + DAH + GAP + DIT + GAP + DAH + GAP + DIT,
        /* A */ DIT + GAP + DAH,
        /* B */ DAH + GAP + DIT + GAP + DIT + GAP + DIT,
        /* C */ DAH + GAP + DIT + GAP + DAH + GAP + DIT,
        /* D */ DAH + GAP + DIT + GAP + DIT,
        /* E */ DIT,
        /* F */ DIT + GAP + DIT + GAP + DAH + GAP + DIT,
        /* G */ DAH + GAP + DAH + GAP + DIT,
        /* H */ DIT + GAP + DIT + GAP + DIT + GAP + DIT,
        /* I */ DIT + GAP + DIT,
        /* J */ DIT + GAP + DAH + GAP + DAH + GAP + DAH,
        /* K */ DAH + GAP + DIT + GAP + DAH,
        /* L */ DIT + GAP + DAH + GAP + DIT + GAP + DIT,
        /* M */ DAH + GAP + DAH,
        /* N */ DAH + GAP + DIT,
        /* O */ DAH + GAP + DAH + GAP + DAH,
        /* P */ DIT + GAP + DAH + GAP + DAH + GAP + DIT,
        /* Q */ DAH + GAP + DAH + GAP + DIT + GAP + DAH,
        /* R */ DIT + GAP + DAH + GAP + DIT,
        /* S */ DIT + GAP + DIT + GAP + DIT,
        /* T */ DAH,
        /* U */ DIT + GAP + DIT + GAP + DAH,
        /* V */ DIT + GAP + DIT + GAP + DIT + GAP + DAH,
        /* W */ DIT + GAP + DAH + GAP + DAH,
        /* X */ DAH + GAP + DIT + GAP + DIT + GAP + DAH,
        /* Y */ DAH + GAP + DIT + GAP + DAH + GAP + DAH,
        /* Z */ DAH + GAP + DAH + GAP + DIT + GAP + DIT,
    };

    /**
     * Consults the CHARACTERS array and returns the pattern string for the provided
     * character.
     * @param character the character for which the pattern string is to be retrieved
     * @return pattern String for the provided character
     */
    public static String pattern(char character) {
	    if (character >= '!' && character <= 'Z') {
	        return CHARACTERS[character - '!'];
	    } else if (character >= 'a' && character <= 'z') {
	    	int index = (character - 'a') + ('A' - '!');
	        return CHARACTERS[index];
	    } else {
	        return ERROR_GAP;
	    }
    }
    
    /**
     * Converts a string of characters to a morse code representation of that string 
     * @param myString The string of characters that will be converted
     * @return The morse code conversion of myString as represented by 0's and 1's 
     */
    public static String pattern(String myString) {
    	String result = new String();
    	
    	// If the string is not empty, begin the translation
    	if(myString.length() != 0)
        {
    		// Add the begin message code to the beginning of the string
	        result += START_OF_MESSAGE;
	        
    		// A flag to determine when a WORD_GAP is required
	    	boolean lastWasWhitespace = true;
	    	
	    	/*
	    	 *  Loop through the string translating each character to morse code
	    	 *  and inserting LETTER_GAPS, and WORD_GAPS where necessary.
	    	 */
	        int strlen = myString.length();
	        for (int i=0; i<strlen; i++) {
	            char character = myString.charAt(i);
	            if (Character.isWhitespace(character)) {
	                if (!lastWasWhitespace) {
	                    result += WORD_GAP;
	                    lastWasWhitespace = true;
	                }
	            } else {
	                if (!lastWasWhitespace) {
	                    result += LETTER_GAP;
	                }
	                lastWasWhitespace = false;
	                
	                // Retrieve the translated character and add it to the result string
	                result += pattern(character);
	            }
	        }
	        
	        // Add the end message code to the end of the string
	        result += END_OF_MESSAGE;
        } else {
        	
        	// The provided string is empty, return a string with only the ERROR_GAP code
        	result += ERROR_GAP;
        }
    	
        return result;
    }

}