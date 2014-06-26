package com.jacobmdavidson.smsmorsecode;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SmsReceiver extends BroadcastReceiver {
	Intent service;

	   
    @Override
    public void onReceive(Context context, Intent intent) {
    	//AudioManager am = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
		//if(am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {	
    	TelephonyManager  tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
    	
    	//If there is not a call in progress, start the service
    	if(tm.getCallState() == TelephonyManager.CALL_STATE_IDLE){
	    	service = new Intent(context, SmsMorseCodeService.class);
	        Bundle extras = intent.getExtras();
	        
	        // If there is no text message, return without starting the service
	        if (extras == null)
	        	return;
	        Object[] pdus = (Object[]) extras.get("pdus");
	        for (int i = 0; i < pdus.length; i++) {
	        	SmsMessage SMessage = SmsMessage.createFromPdu((byte[]) pdus[i]);
	        	String sender = SMessage.getOriginatingAddress();
	        	String body = SMessage.getMessageBody().toString();
	        	service.putExtra("sender", sender);
	        	service.putExtra("body", body);
	        	service.putExtra("caller", "SmsReceiver");
	        	context.startService(service);
	        }
		}
                

	}
       

}
