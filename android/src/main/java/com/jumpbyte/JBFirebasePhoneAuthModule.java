
package com.jumpbyte;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class JBFirebasePhoneAuthModule extends ReactContextBaseJavaModule {

    private static final String TAG = "JBFirebasePhoneAuth";

    public static final String REACT_CLASS = "JBFirebasePhoneAuth";
    private static ReactApplicationContext reactContext;
    public static final String ON_VERIFICATION_COMPLETED = "onFirebasePhoneVerificationCompleted";
    public static final String ON_CODE_AUTO_RETRIEVAL_TIMEOUT = "onFirebasePhoneCodeAutoRetrievalTimeOut";

    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private PhoneAuthProvider.ForceResendingToken phoneAuthResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks phoneAuthProviderCallbacks;
    private String firebaseVerificationId;

    public JBFirebasePhoneAuthModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put("ON_VERIFICATION_COMPLETED", ON_VERIFICATION_COMPLETED);
        constants.put("ON_CODE_AUTO_RETRIEVAL_TIMEOUT", ON_CODE_AUTO_RETRIEVAL_TIMEOUT);
        return constants;
    }

    private void sendEvent(String eventName, Object params) {
        getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName,
                params);
    }

    @ReactMethod
    public void getSession(final Promise promise) {
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            final JSONObject object = new JSONObject();
            try {
                object.put("phoneNumber", firebaseUser.getPhoneNumber());
                object.put("providerId", firebaseUser.getProviderId());
                object.put("uid", firebaseUser.getUid());
                firebaseUser.getToken(true).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                    public void onComplete(@NonNull Task<GetTokenResult> task) {
                        if (task.isSuccessful()) {
                            String idToken = task.getResult().getToken();
                            Log.i(TAG, "idToken >>> " + idToken);
                            try {
                                object.put("idToken", idToken);
                                promise.resolve(object.toString());
                            } catch (JSONException e) {
                                promise.reject("401", e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    }
                });
            } catch (JSONException e) {
                promise.reject("401", e.getMessage());
                e.printStackTrace();
            }

        } else {
            Log.i(TAG, "User is not sign in");
            promise.resolve(firebaseUser);
        }
    }

    @ReactMethod
    public void updateGooglePlayServices() {
        getCurrentActivity().startActivity(
                new Intent("android.intent.action.VIEW", Uri.parse("market://details?id=com.google.android.gms")));
    }

    @ReactMethod
    public void verifyPhoneNumber(String phoneNumber, Promise promise) {
        final int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getCurrentActivity());
        Log.i(TAG, GooglePlayServicesUtil.GOOGLE_PLAY_SERVICES_VERSION_CODE + "");
        if (status != ConnectionResult.SUCCESS) {
            Log.e(TAG, GooglePlayServicesUtil.getErrorString(status));
            // ask user to update google play services.
            promise.reject("401", GooglePlayServicesUtil.getErrorString(status));
        } else {
            Log.i("TAG", GooglePlayServicesUtil.getErrorString(status));
            // google play services is updated.
            Log.i(TAG, "verifyPhoneNumber");
            Log.i(TAG, "phoneNumber >> " + phoneNumber);
            firebaseAuth = FirebaseAuth.getInstance();

            PhoneAuthProvider.getInstance(firebaseAuth).verifyPhoneNumber(phoneNumber, 60, TimeUnit.SECONDS,
                    getCurrentActivity(), verificationCallback(promise));
        }
    }

    @ReactMethod
    public void resendVerificationCode(String phoneNumber, Promise promise) {
        Log.i(TAG, "resendVerificationCode");
        PhoneAuthProvider.getInstance().verifyPhoneNumber(phoneNumber, 60, TimeUnit.SECONDS, getCurrentActivity(),
                verificationCallback(promise), phoneAuthResendToken);
    }

    @ReactMethod
    public void verifyPhoneNumberWithCode(String code, final Promise promise) {
        Log.i(TAG, "verifyPhoneNumberWithCode");
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(firebaseVerificationId, code);
        Log.i(TAG, "signInWithCode");
        signIn(credential, promise);
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks verificationCallback(final Promise promise) {
        phoneAuthProviderCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                Log.i(TAG, "onVerificationCompleted");
                Log.i(TAG, "signInWithPhoneAuth");
                signIn(credential, promise);
                sendEvent(ON_VERIFICATION_COMPLETED, credential.getSmsCode());
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                Log.e(TAG, "onVerificationFailed" + e.getMessage());
                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    promise.reject("101", "firebase_auth_phone_verification_failed : Invalid phone number.");
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    promise.reject("102", "firebase_auth_phone_verification_failed : Quota exceeded.");
                } else {
                    promise.reject("103", e.getMessage());
                }

            }

            @Override
            public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(verificationId, forceResendingToken);
                Log.i(TAG, "onCodeSent");
                firebaseVerificationId = verificationId;
                phoneAuthResendToken = forceResendingToken;
                promise.resolve(verificationId);
            }

            @Override
            public void onCodeAutoRetrievalTimeOut(String verificationId) {
                super.onCodeAutoRetrievalTimeOut(verificationId);
                Log.i(TAG, "onCodeAutoRetrievalTimeOut >>> " + verificationId);
                sendEvent(ON_CODE_AUTO_RETRIEVAL_TIMEOUT, verificationId);
            }
        };
        return phoneAuthProviderCallbacks;
    }

    private void signIn(PhoneAuthCredential credential, final Promise promise) {
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Log.i(TAG, "signInWithCredential:success");
                    firebaseUser = task.getResult().getUser();
                    final JSONObject object = new JSONObject();
                    try {
                        object.put("phoneNumber", firebaseUser.getPhoneNumber());
                        object.put("providerId", firebaseUser.getProviderId());
                        object.put("uid", firebaseUser.getUid());
                        promise.resolve(object.toString());
                    } catch (JSONException e) {
                        promise.reject("203", e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.getException());
                    if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                        Log.w(TAG, "Invalid code.");
                        promise.reject("201", "FirebaseAuthInvalidCredentialsException : Invalid code");
                    }
                    promise.reject("202", "signInWithCredential:failure " + task.getException());
                }
            }
        });
    }

    @ReactMethod
    public void signOut(final Promise promise) {
        Log.i(TAG, "signOut > " + firebaseUser.getPhoneNumber());
        firebaseAuth.signOut();
        promise.resolve("SignOut Successfully.");
    }
}