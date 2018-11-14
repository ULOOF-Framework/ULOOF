# ULOOF REPOSITORY

## Projects inside ULOOF repository
* **ServerSide**: Offloading Server (Android Studio Project);

* **Post-Compiler**: Post-Compiler (Eclipse Project);  

* **OffloadingLibrary**: Offloading Framework Library (DecisionEngine is here);  
(2 Android Studio Projects: OffloadingFrameworkUtility and OffloadingFramework)  

* **Soot_multiDex**: modified version of Soot library needed for the post-compiler;

* **Kryo_serializationCheck**: modified version of Kryo library for serialization;

* **SampleApllications**: City Route and Fibonacci (.apk);  
      
    _**To note**: ServerSide and Post-Compiler projects, provided in the ULOOF repository, already contain the needed dependencies (.jar) to the offloading framework library,
	to the modified version of Soot library and to the modified version of Kyro library. They do not need to be regenerated, the source code is anyway provided._  
    
    _**To note**: City Route and Fibonacci apps are demonstration applications that work with a VM at LIP6 that should be up and running. Only to try the framework, just install them and launch them.  
Obviously, you will need to authorize installing APK not coming from Google play._

## Licensing
        
Project | License
--------|--------
ServerSide | GNU General Public License 
Post-Compiler | GNU General Public License
OffloadingLibrary | GNU Lesser General Public License
Soot_multiDex | GNU Lesser General Public License
Kryo_serializationCheck | BSD-3 Clause

## Technical detail
**Recommended OS**: Linux distribution (for example, path separator could create problems in other OSs)

### Needed/Suggested Softwares
* Android Studio (and Android SDK);
* Eclipse;
* Android Emulator (e.g. Genymotion);

### How to obtain offFrameKryoMod.jar from the OffloadingLibrary AndroidStudio Project
1. Import in AS only OffloadingFrameworkUtility (this corresponds to the offFramKryoMod.jar included in Post-Compiler and ServerSide);
2. Apply any changes;
3. Build the APK as usual;
4. Use the dex2jar tool to obtain a jar, open it and delete the following packages:
	* android;
	* br.com.ld.algorithmtest;
	* pl;
5. Name the .jar as offFrameKryoMod.jar and put it in the 'lib' folder of the Post-Compiler and in the 'app/libs" of the ServerSide.

### Post-Compiler Run Configuration
Create a new Java Run Configuration indicating as arguments:  
  
-pp -android-jars **HERE_INSERT_ANDROID_SDK_PATH**/platforms -process-dir ./apk/**HERE_INSERT_APK_NAME.apk** 
-debug -process-dir lib/offFramKryoMod.jar -d sootOutput -allow-phantom-refs -p cg enabled:false -w -force-overwrite  
  
  Example of ADNROID_SDK_PATH: /home/alessio/Android/Sdk/platforms  
  
  Moreover, set the Main class as AndroidInstrumentv2.


### How to use the Offloading Framework
1. Launch Post-Compiler putting the APK of the application to offload inside the 'apk' folder
2. After the execution, copy the generated temp.jar that is inside the 'temp' folder of Post-Compiler to the folder 'ServerSide\app\libs'
	(in the gradle dependencies of the ServerSide there is the temp.jar file, it contains what ServerSide needs to execute app's methods)
3. Recompile ServerSide Android Project  
3.1. If recompiling with the new temp.jar, it gets a build error (Multiple dex files defined), go inside the temp.jar and delete what generates the conflict (e. g. manually deleting)
4. If using an emulator to run the ServerSide (e.g. Genymotion), launch 'ServerSide/redirect_packet.bash' that is used to redirect packet to the Genymotion emulator
5. Launch ServerSide (e.g. on Genymotion)  
5.1. To check that the Server is up and reachable, try to ping it with HERE_THE_SERVERSIDE_IP_ADDRESS/8080/ping
		It should response with HTTP OK.
6. The modified apk that has to be installed on the Android Phone could be found in 'sootOutput' folder of Post-Compiler. Install and launch it on the Android phone

**IMPORTANT**: It has to be provided a settings.txt file on the extern memory of the Android phone (sdcard usually) with two lines as follow:
  
  IP of the machine that runs Genymotion (or any other emulator)  
  8080
  
# Contributors
José Leal D. Neto, Daniel F. Macedo, Stefano Secci, Rami Langar, José Marcos Nogueira - Decision Engine, Overall Framework  
Se-young Yu - Sample Applications  
Alessio Diamanti, Alessandro Zanni - Sample Applications, Automated Annotations
## Credits to be included

If you use the present code for your work, please cite the following paper in any publication as a credit:

J.L.D. Neto, S. Yu, D.F. Macedo, J.M.S. Nogueira, R. Langar, S. Secci,  
**?ULOOF: a User Level Online Offloading Framework for Mobile Edge Computing?**,  
IEEE Transactions on Mobile Computing, Vol. 17, No. 11, pp:  2660-2674, Nov. 2018.

