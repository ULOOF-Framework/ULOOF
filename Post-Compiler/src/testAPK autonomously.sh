#cd /mnt/c/Users/Alessandro/Desktop/workspaceUPMC/AndroidInstrument/src/br/com/lealdn
#javac -cp "/mnt/c/Users/Alessandro/Desktop/workspaceUPMC/AndroidInstrument/lib/*" beans/* CheckMethods.java utils/* AndroidInstrument.java
#cd /mnt/c/Users/Alessandro/Desktop/workspaceUPMC/AndroidInstrument/src
java -cp ":../lib/*"  br.com.lealdn.AndroidInstrument2 -pp -android-jars /mnt/c/Users/Alessandro/AppData/Local/Android/sdk/platforms -process-dir "/mnt/c/Users/Alessandro/Google Drive/APKs/top25/com.bitmango.go.wordcookies.apk" -allow-phantom-refs -w;