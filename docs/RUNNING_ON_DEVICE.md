# Running Sotto on a Physical Android Device (USB)

**TL;DR**: Enable USB debugging, disable Samsung Auto Blocker (if applicable), connect via USB, and install with `./gradlew installDebug`.

---

## 1. Enable Developer Mode & USB Debugging
1. Go to **Settings > About phone** (or **Software information**).
2. Tap **Build number** 7 times to unlock Developer Options.
3. Open **Settings > Developer options** and toggle **USB debugging** to **ON**.

## 2. Disable Auto Blocker (Samsung One UI 6+)
1. Go to **Settings > Security and privacy > Auto Blocker**.
2. Toggle it **OFF** (blocks USB sideloading by default).

## 3. Connect Device
1. Connect the device to your computer via USB.
2. When prompted on the device (*"Allow USB debugging?"*), select **Always allow** and tap **OK**.

## 4. Install
Run from the project root:

```bash
./gradlew installDebug
```

> [!TIP]
> Alternatively, if using the Android CLI tool:
> ```bash
> android run --apks=app/build/outputs/apk/debug/app-debug.apk --activity=com.amh.sotto.MainActivity
> ```

