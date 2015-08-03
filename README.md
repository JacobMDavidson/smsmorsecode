## Listen to SMS Messages in Morse Code

[SMorSe](https://play.google.com/store/apps/details?id=com.jacobmdavidson.smsmorsecode) is an Android application that converts incoming text messages to Morse code based on the ITU-R M.1677-1 recommendation. Simply enable the app, and all incoming text messages will be converted to Morse code (even if the app is not running). If the ringer mode is set to normal, the message is played at the frequency and speed settings chosen. If the ringer mode is set to vibrate, the message is vibrated at the speed setting chosen. Each message begins with the starting signal ( — • — • — ), after which the message is played. The cross signal ( • — • — • ) indicates the end of the message.

## Features:

1. If the user is on a call when the message is received, the message is not played.
2. If the user receives a call when the message is playing, the message is stopped.
3. To manually cancel playback of a message, the user changes the screen state twice using the phone's power button.
4. The application does not have to be running to receive text messages in morse code.
5. If music is playing when a message is received, the music is paused during message playback and resumed after message playback is completed.

## Permissions

This app uses the RECEIVE_SMS, VIBRATE, and READ_PHONE_STATE permissions. RECEIVE_SMS is required to read incoming text messages, and VIBRATE is used to vibrate those messages as morse code when the phone is set to vibrate. The READ_PHONE_STATE permission is used to stop the service when the user is on a call, or when a call is received during morse code playback.
