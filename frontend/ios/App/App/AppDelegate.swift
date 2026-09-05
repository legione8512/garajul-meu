import UIKit
import Capacitor

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        // Override point for customization after application launch.
        return true
    }

    // MARK: - Remote notifications
    //
    // Three methods that Capacitor generates no stub for, and whose absence
    // fails silently. Read in the plugin's own source on 2026-09-05 rather than
    // taken from its README: FirebaseMessagingPlugin.load() subscribes to
    // NotificationCenter for `.capacitorDidRegisterForRemoteNotifications`, and
    // nothing in Capacitor posts that notification on the app's behalf. This
    // file is the only place it can come from.
    //
    // What breaks without them: the plugin's init calls
    // registerForRemoteNotifications(), so iOS does hand us an APNs token - it
    // just arrives here and stops. `Messaging.messaging().apnsToken` is never
    // set, and getToken() then waits for an APNs registration that already
    // happened. No crash, no log, no exception for reportDevice to catch. The
    // iPhone simply never registers, and every reminder for it is recorded as
    // sent. Exactly the lie section 10.7 exists to prevent, arriving through the
    // one path our tests cannot see.
    //
    // Android needs no counterpart: there the token comes back through the
    // plugin's own MessagingService, declared in its manifest.

    func application(_ application: UIApplication,
                     didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        NotificationCenter.default.post(
            name: .capacitorDidRegisterForRemoteNotifications,
            object: deviceToken)
    }

    // Posted for completeness, and it is worth being exact about what that buys,
    // because the first version of this comment claimed something false.
    //
    // **Nothing observes this notification.** Checked in the plugin's source on
    // 2026-09-05, after a real device failed: it subscribes to
    // `.capacitorDidRegisterForRemoteNotifications` and to a plain-string
    // "didReceiveRemoteNotification", and to nothing else. A registration
    // failure is therefore silent to it.
    //
    // That silence has a cost we paid the same day. With no push entitlement -
    // the Push Notifications capability had not been added in Xcode -
    // registerForRemoteNotifications() failed, `apnsToken` was never set, and
    // the only symptom anywhere was getToken() rejecting with "No APNS token
    // specified before fetching FCM Token". A message about ordering, for a
    // missing permission. The real cause was found by looking for the
    // entitlement, not by reading the error.
    //
    // Kept because posting it costs one line and the day something does listen -
    // the plugin, or code of ours - the signal is already being sent. It is not
    // what makes getToken() settle.
    func application(_ application: UIApplication,
                     didFailToRegisterForRemoteNotificationsWithError error: Error) {
        NotificationCenter.default.post(
            name: .capacitorDidFailToRegisterForRemoteNotifications,
            object: error)
    }

    // The name is a plain string because the plugin observes a plain string -
    // `Notification.Name.init("didReceiveRemoteNotification")`, with no constant
    // in Capacitor to import. It reads the payload from `userInfo` and hands
    // `object` back as the completion handler, so both must be passed exactly
    // this way round.
    func application(_ application: UIApplication,
                     didReceiveRemoteNotification userInfo: [AnyHashable: Any],
                     fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        NotificationCenter.default.post(
            name: Notification.Name.init("didReceiveRemoteNotification"),
            object: completionHandler,
            userInfo: userInfo)
    }

    func applicationWillResignActive(_ application: UIApplication) {
        // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
        // Use this method to pause ongoing tasks, disable timers, and invalidate graphics rendering callbacks. Games should use this method to pause the game.
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
        // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
    }

    func applicationWillEnterForeground(_ application: UIApplication) {
        // Called as part of the transition from the background to the active state; here you can undo many of the changes made on entering the background.
    }

    func applicationDidBecomeActive(_ application: UIApplication) {
        // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
    }

    func applicationWillTerminate(_ application: UIApplication) {
        // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
    }

    func application(_ application: UIApplication,
                     configurationForConnecting connectingSceneSession: UISceneSession,
                     options: UIScene.ConnectionOptions) -> UISceneConfiguration {
        let config = UISceneConfiguration(name: "Default Configuration",
                                          sessionRole: connectingSceneSession.role)
        config.delegateClass = SceneDelegate.self
        return config
    }
}
