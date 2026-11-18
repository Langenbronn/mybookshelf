import SwiftUI

// iOS entry point. The UI itself is written in Kotlin (composeApp/src/commonMain).
@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
