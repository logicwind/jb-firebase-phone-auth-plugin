
# react-native-firebase-phone-auth

## Getting started

`$ npm install react-native-firebase-phone-auth --save`

### Mostly automatic installation

`$ react-native link react-native-firebase-phone-auth`


#### Android

1) Get `google-service.json` file from firebase console and add it to your project's android/app.
2) **Gradle import** 
	- project level  
		```
		dependencies {
        	classpath 'com.google.gms:google-services:3.1.0'
    	}
		```
	- app level (at bottom)  
		```
		apply plugin: 'com.google.gms.google-services'
		```

#### iOS

All steps are need to do in `xcode` for **objective-c**.
1) get `GoogleService-Info.plist` file from firebase console and place  to your xcodeProject.
2) add `pod 'Firebase/Core'` and `pod 'Firebase/Auth'` in pod file and install it using `pod install`
3) add below code in `AppDelegate.m`
	```
	@import Firebase;
	@import UserNotifications;
	.
	.
	add   `[FIRApp configure];`
	in `didFinishLaunchingWithOptions:(NSDictionary *)launchOptions`
	.
	.
	- (void)application:(UIApplication *)application didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken {
  		// Pass device token to auth.
  		[[FIRAuth auth] setAPNSToken:deviceToken type:FIRAuthAPNSTokenTypeProd];
  		// Further handling of the device token if needed by the app.
	}
	```

## Usage
```javascript
import JBFirebasePhoneAuth from 'react-native-firebase-phone-auth';
// TODO: What to do with the module?
JBFirebasePhoneAuth;

or

import NativeEventEmitter,NativeModules from 'react-native'  
//for android NativeEventEmitter
const EventEmitter = new NativeEventEmitte(NativeModules.JBFirebasePhoneAuth) 
const { JBFirebasePhoneAuth } = NativeModules
```
### Methods
- verify phoneNumber 
	```
    JBFirebasePhoneAuth.verifyPhoneNumber(phoneNumber).then(verificationId => {
        //success: code here
    }).catch(error => {
        //error: handle here
    })
    ```
- resend verificationCode (For android only)
    ```
    JBFirebasePhoneAuth.resendVerificationCode(phoneNo).then(verificationId => {
        //success: code here
    }).catch(error => {
        //error: handle here
    })
    ```
- verify phoneNumber with code 
    ```
    JBFirebasePhoneAuth.verifyPhoneNumberWithCode(verificationCode).then(user => {
      //success: code here
    }).catch(error => {
      //error: handle here
    })
    ```
- signOut
    ```
    JBFirebasePhoneAuth.signOut().then(userNumber => {
        //success: code here
    }).catch(error => {
        //error: handle here
    })
	```



### Manual installation


#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.jumpbyte.JBFirebasePhoneAuthPackage;` to the imports at the top of the file
  - Add `new JBFirebasePhoneAuthPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-firebase-phone-auth'
  	project(':react-native-firebase-phone-auth').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-firebase-phone-auth/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-firebase-phone-auth')
  	```

#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-firebase-phone-auth` and add `JBFirebasePhoneAuth.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libJBFirebasePhoneAuth.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<
  