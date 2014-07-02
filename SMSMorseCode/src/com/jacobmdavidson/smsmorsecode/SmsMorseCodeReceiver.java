package com.jacobmdavidson.smsmorsecode;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;

/**
 * Receives text message broadcasts, creates an intent to start a SmsMorseCodeService, adds
 * the body of the text message to that intent, and starts the service.
 * @author Jacob Davidson
 * @version 1.0.0
 */
public class SmsMorseCodeReceiver extends BroadcastReceiver {
	
    @Override
    public void onReceive(Context context, Intent intent) {

    	// Instantiate a telephonyManager object to determine if a call is in progress
    	TelephonyManager  telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
    	
    	// If there is not a call in progress, start the service
    	if (telephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE) {
	    	Intent service = new Intent(context, SmsMorseCodeService.class);
	        Bundle extras = intent.getExtras();
	        
	        // If there is no text message, return without starting the service
	        if (extras == null)
	        	return;
	        
	        // Get the protocol description units from the intent
	        Object[] pdus = (Object[]) extras.get("pdus");
	        
	        /*
	         *  Get the message body from each message in the pdus array, add it to the service intent, and start
	         *  the SmsMorseCodeService
	         */
	        for (int i = 0; i < pdus.length; i++) {
	        	SmsMessage SMessage = SmsMessage.createFromPdu((byte[]) pdus[i]);
	        	//String sender = SMessage.getOriginatingAddress();
	        	String body = SMessage.getMessageBody().toString();
	        	//service.putExtra("sender", sender);
	        	service.putExtra("body", body);
	        	context.startService(service);
	        }
		}
	}
}
