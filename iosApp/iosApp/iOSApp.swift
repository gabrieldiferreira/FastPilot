import SwiftUI
import FirebaseCore
import GoogleSignIn

@main
struct iOSApp: App {
    init() {
        FirebaseApp.configure()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea(.all)
                // Google sign-in returns to the app through its URL scheme
                .onOpenURL { GIDSignIn.sharedInstance.handle($0) }
        }
    }
}
