
#import "JBFirebasePhoneAuth.h"
#import <FirebaseAuth/FirebaseAuth.h>

@implementation JBFirebasePhoneAuth

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}
RCT_EXPORT_MODULE()

NSString *TAG = @"JB_FIREBASE_PHONE_AUTH: ";
NSUserDefaults *defaults;

RCT_EXPORT_METHOD(verifyPhoneNumber:(NSString *)phoneNumber
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  NSLog(@"%@ verifyPhoneNumber : %@", TAG, phoneNumber);
  [[FIRPhoneAuthProvider provider]
   verifyPhoneNumber:phoneNumber
   completion:^(NSString * _Nullable verificationID, NSError * _Nullable error) {
     if (error) {
       NSLog(@"%@ verifyPhoneNumber: ERROR >>> %@", TAG, error);
       reject(@"103", [NSString stringWithFormat:@"Firebase PhoneNumber Verification Failed: %@", error.localizedDescription], error);
       return;
     }
     NSLog(@"%@ verificationID >> %@", TAG, verificationID);
     defaults = [NSUserDefaults standardUserDefaults];
     [defaults setObject:verificationID forKey:@"authVerificationID"];
     resolve([NSString stringWithFormat:@"Verification code has been sent. \n verificationID: %@", verificationID]);
   }];
}

RCT_EXPORT_METHOD(verifyPhoneNumberWithCode:
                  (NSString *)code
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  NSLog(@"%@ verifcation code: %@ ", TAG, code);
  NSString *verificationID = [defaults stringForKey:@"authVerificationID"];
  FIRAuthCredential *credential = [[FIRPhoneAuthProvider provider]
                                   credentialWithVerificationID:verificationID
                                   verificationCode:code];

  [[FIRAuth auth] signInWithCredential:credential
                            completion:^(FIRUser *user, NSError *error) {
                              if (error) {
                                NSLog(@"%@ verifyPhoneNumber: ERROR >>> %@", TAG, error.localizedDescription);
                                reject(@"202", [NSString stringWithFormat:@"Firebase Code Verification Failed: %@", error.localizedDescription], error);
                                return;
                              }
                              
                              NSDictionary *result = [NSDictionary dictionaryWithObjectsAndKeys:
                                                  user.phoneNumber, @"phoneNumber",
                                                  user.providerID, @"providerID",
                                                  user.uid, @"uid",
                                                  nil];
                              NSLog(@"%@ SIGN_IN_USER: %@", TAG, result);
                              resolve(result);
                            }];

}

RCT_EXPORT_METHOD(getSession:
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  FIRUser *currentUser = [FIRAuth auth].currentUser;
  if (currentUser) {
    NSLog(@"User is avalibale %@", currentUser.phoneNumber);
    [currentUser getTokenForcingRefresh:YES
                             completion:^(NSString *_Nullable idToken,
                                          NSError *_Nullable error) {
                               if (error) {
                                 NSLog(@"%@ FIREBASE TOKEN EROOR: %@", TAG, error.localizedDescription);
                                 reject(@"401", [NSString stringWithFormat:@"Firebase Token Failed: %@", error.localizedDescription], error);
                                 return;
                               }
                               NSDictionary *result = [NSDictionary dictionaryWithObjectsAndKeys:
                                                       currentUser.phoneNumber, @"phoneNumber",
                                                       currentUser.providerID, @"providerID",
                                                       currentUser.uid, @"uid",
                                                       idToken, @"idToken",
                                                       nil];
                               NSLog(@"%@ SIGIN_USER: %@", TAG, result);
                               resolve(result);
                             }];
  }else {
    NSLog(@"User is not avalibale %@", currentUser);
    resolve(currentUser);
  }
}

RCT_EXPORT_METHOD(signOut:
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  NSError *signOutError;
  BOOL status = [[FIRAuth auth] signOut:&signOutError];
  if (!status) {
    NSLog(@"%@ Error signing out: %@", TAG, signOutError);
    reject(@"301", [NSString stringWithFormat:@"Firebase SignOut Failed: %@", signOutError.localizedDescription], signOutError);
    return;
  }
  NSLog(@"%@ SignOut successfully", TAG);
  resolve(@"SignOut successfully.");
}

@end
  