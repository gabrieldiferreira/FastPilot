import UIKit
import ComposeApp
import FirebaseAuth
import FirebaseCore
import GoogleSignIn

/// Google sign-in for the Kotlin side (IosAuthBridge): GoogleSignIn account picker → Firebase sign-in.
/// The OAuth client comes from GoogleService-Info.plist (CLIENT_ID), and Info.plist must have its
/// REVERSED_CLIENT_ID as a URL scheme so Google can return to the app.
final class FirebaseAuthBridge: NSObject, IosAuthBridge {

    func currentUser() -> AuthUser? {
        Auth.auth().currentUser.map(Self.authUser)
    }

    func signInWithGoogle(
        onSuccess: @escaping (AuthUser) -> Void,
        onError: @escaping (String) -> Void,
        onCancel: @escaping () -> Void
    ) {
        guard let clientID = FirebaseApp.app()?.options.clientID else {
            onError("GoogleService-Info.plist has no CLIENT_ID (enable Google in Firebase Authentication and download it again)")
            return
        }
        // Without the URL scheme GoogleSignIn raises an exception instead of returning an error
        let reversedClientID = clientID.components(separatedBy: ".").reversed().joined(separator: ".")
        guard Self.urlSchemes().contains(reversedClientID) else {
            onError("Info.plist is missing the URL scheme \(reversedClientID)")
            return
        }
        guard let presenter = Self.topViewController() else {
            onError("No screen to present the Google sign-in from")
            return
        }

        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
        GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { result, error in
            if let error = error as NSError? {
                if error.domain == kGIDSignInErrorDomain && error.code == GIDSignInError.canceled.rawValue {
                    onCancel()
                } else {
                    onError(error.localizedDescription)
                }
                return
            }
            guard let user = result?.user, let idToken = user.idToken?.tokenString else {
                onError("Google returned no ID token")
                return
            }
            let credential = GoogleAuthProvider.credential(withIDToken: idToken, accessToken: user.accessToken.tokenString)
            Auth.auth().signIn(with: credential) { authResult, error in
                if let error {
                    onError(error.localizedDescription)
                } else if let firebaseUser = authResult?.user {
                    onSuccess(Self.authUser(firebaseUser))
                } else {
                    onError("Firebase returned no user")
                }
            }
        }
    }

    func signOut() {
        GIDSignIn.sharedInstance.signOut()
        try? Auth.auth().signOut()
    }

    func deleteAccount(onDone: @escaping (String?) -> Void) {
        guard let user = Auth.auth().currentUser else {
            onDone(nil)
            return
        }
        user.delete { error in
            if let error = error as NSError? {
                onDone(error.code == AuthErrorCode.requiresRecentLogin.rawValue
                    ? "sign out, sign in again and retry"
                    : error.localizedDescription)
            } else {
                GIDSignIn.sharedInstance.signOut()
                onDone(nil)
            }
        }
    }

    private static func authUser(_ user: User) -> AuthUser {
        AuthUser(uid: user.uid, name: user.displayName, email: user.email, photoUrl: user.photoURL?.absoluteString)
    }

    private static func urlSchemes() -> [String] {
        let types = Bundle.main.object(forInfoDictionaryKey: "CFBundleURLTypes") as? [[String: Any]] ?? []
        return types.flatMap { $0["CFBundleURLSchemes"] as? [String] ?? [] }
    }

    private static func topViewController() -> UIViewController? {
        let window = UIApplication.shared.connectedScenes
            .compactMap { ($0 as? UIWindowScene)?.keyWindow }
            .first
        var top = window?.rootViewController
        while let presented = top?.presentedViewController { top = presented }
        return top
    }
}
