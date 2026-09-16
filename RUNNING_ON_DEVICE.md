# Running Sotto on a Physical Android Device (USB)

**TL;DR**: Enable USB debugging, disable Samsung Auto Blocker (if applicable), connect via USB, and use `./gradlew installDebug` or the `android run` CLI tool to install the app.

---

### 1. Enable Developer Mode & USB Debugging
1. Go to **Settings > About phone > Software information** (or just **About phone** on non-Samsung devices).
2. Tap **Build number** 7 times to unlock Developer Options.
3. Go back to the main **Settings** menu and open **Developer options**.
4. Toggle **USB debugging** to **ON**.

### 2. Disable Auto Blocker (Samsung Devices Only)
*Samsung's One UI 6+ blocks USB installations by default.*
1. Go to **Settings > Security and privacy > Auto Blocker**.
2. Toggle it **OFF**.

### 3. Connect to Mac
1. Connect your phone to your Mac using a USB cable.
2. A prompt will appear on your phone screen: *"Allow USB debugging?"*
3. Check **Always allow from this computer** and tap **OK**.

### 4. Install the App
Open your Mac terminal, ensure you are inside the `Sotto` project folder, and run ONE of the following commands:

**Option A: Using standard Gradle (Recommended)**
```bash
./gradlew installDebug
```
*After it finishes, manually tap the Sotto app icon on your phone's home screen to launch it.*

**Option B: Using the Android CLI tool**
```bash
android run --apks=app/build/outputs/apk/debug/app-debug.apk --activity=com.example.sotto.MainActivity
```
*This command will install the APK and automatically launch it on your screen.*
