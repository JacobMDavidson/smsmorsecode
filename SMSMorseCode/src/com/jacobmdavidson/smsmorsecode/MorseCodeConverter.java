package com.jacobmdavidson.smsmorsecode;

import android.util.Log;


/** Class that implements the text to morse code conversion */
class MorseCodeConverter {
    static final String DOT = "1";
    static final String DASH = "111";
    static final String GAP = "0";
    static final String LETTER_GAP = "000";
    static final String WORD_GAP = "0000000";
    static final String END_OF_TEXT = "00000";
    static final String START_OF_TEXT = "00";

    /** The characters from 'A' to 'Z' */
    private static final String[] LETTERS = new String[] {
        /* A */ DOT + GAP + DASH,
        /* B */ DASH + GAP + DOT + GAP + DOT + GAP + DOT,
        /* C */ DASH + GAP + DOT + GAP + DASH + GAP + DOT,
        /* D */ DASH + GAP + DOT + GAP + DOT,
        /* E */ DOT,
        /* F */ DOT + GAP + DOT + GAP + DASH + GAP + DOT,
        /* G */ DASH + GAP + DASH + GAP + DOT,
        /* H */ DOT + GAP + DOT + GAP + DOT + GAP + DOT,
        /* I */ DOT + GAP + DOT,
        /* J */ DOT + GAP + DASH + GAP + DASH + GAP + DASH,
        /* K */ DASH + GAP + DOT + GAP + DASH,
        /* L */ DOT + GAP + DASH + GAP + DOT + GAP + DOT,
        /* M */ DASH + GAP + DASH,
        /* N */ DASH + GAP + DOT,
        /* O */ DASH + GAP + DASH + GAP + DASH,
        /* P */ DOT + GAP + DASH + GAP + DASH + GAP + DOT,
        /* Q */ DASH + GAP + DASH + GAP + DOT + GAP + DASH,
        /* R */ DOT + GAP + DASH + GAP + DOT,
        /* S */ DOT + GAP + DOT + GAP + DOT,
        /* T */ DASH,
        /* U */ DOT + GAP + DOT + GAP + DASH,
        /* V */ DOT + GAP + DOT + GAP + DOT + GAP + DASH,
        /* W */ DOT + GAP + DASH + GAP + DASH,
        /* X */ DASH + GAP + DOT + GAP + DOT + GAP + DASH,
        /* Y */ DASH + GAP + DOT + GAP + DASH + GAP + DASH,
        /* Z */ DASH + GAP + DASH + GAP + DOT + GAP + DOT,
    };

    /** The characters from '0' to '9' */
    private static final String[] NUMBERS = new String[] {
        /* 0 */ DASH + GAP + DASH + GAP + DASH + GAP + DASH + GAP + DASH,
        /* 1 */ DOT + GAP + DASH + GAP + DASH + GAP + DASH + GAP + DASH,
        /* 2 */ DOT + GAP + DOT + GAP + DASH + GAP + DASH + GAP + DASH,
        /* 3 */ DOT + GAP + DOT + GAP + DOT + GAP + DASH + GAP + DASH,
        /* 4 */ DOT + GAP + DOT + GAP + DOT + GAP + DOT + GAP + DASH,
        /* 5 */ DOT + GAP + DOT + GAP + DOT + GAP + DOT + GAP + DOT,
        /* 6 */ DASH + GAP + DOT + GAP + DOT + GAP + DOT + GAP + DOT,
        /* 7 */ DASH + GAP + DASH + GAP + DOT + GAP + DOT + GAP + DOT,
        /* 8 */ DASH + GAP + DASH + GAP + DASH + GAP + DOT + GAP + DOT,
        /* 9 */ DASH + GAP + DASH + GAP + DASH + GAP + DASH + GAP + DOT,
    };

    private static final String ERROR_GAP = GAP;

    /** Return the pattern data for a given character */
    static String pattern(char c) {
        if (c >= 'A' && c <= 'Z') {
            return LETTERS[c - 'A'];
        }
        if (c >= 'a' && c <= 'z') {
            return LETTERS[c - 'a'];
        }
        else if (c >= '0' && c <= '9') {
            return NUMBERS[c - '0'];
        }
        else {
            return ERROR_GAP;
        }
    }

    static String pattern(String str) {
    	String result = new String();
    	if(str.length() != 0)
        {
	    	boolean lastWasWhitespace;
	        int strlen = str.length();
	
	        lastWasWhitespace = true;
	        result += START_OF_TEXT;
	        for (int i=0; i<strlen; i++) {
	            char c = str.charAt(i);
	            if (Character.isWhitespace(c)) {
	                if (!lastWasWhitespace) {
	                    result += WORD_GAP;
	                    //pos++;
	                    lastWasWhitespace = true;
	                }
	            } else {
	                if (!lastWasWhitespace) {
	                    result += LETTER_GAP;
	                    //pos++;
	                }
	                lastWasWhitespace = false;
	                String letter = pattern(c);
	                //System.arraycopy(letter, 0, result, pos, letter.length);
	                result += letter;
	                //pos += letter.length;
	            }
	        }
	        result += END_OF_TEXT;
        } else {
        	result += ERROR_GAP;
        }
        return result;
    }

}