import subprocess
import os
from os import listdir
from os.path import isfile, join
from fileinput import filename

def main():
    path = '/mnt/c/Users/Alessandro/Desktop/top25/'
    allfiles = [f for f in listdir(path) if isfile(join(path, f))]
    for file in allfiles:
        filename, file_extension = os.path.splitext(file)
        if file_extension == ".apk":
            print(filename)
            subprocess.call(['/usr/bin/java -cp ":../lib/*"  br.com.lealdn.AndroidInstrument2 -pp -android-jars "/mnt/c/Users/Alessandro/AppData/Local/Android/sdk/platforms" -process-dir ' + path + filename + '.apk -allow-phantom-refs -w'], shell=True) 

if __name__ == "__main__":
    main()