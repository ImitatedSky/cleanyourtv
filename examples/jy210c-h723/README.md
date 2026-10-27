# JY210-C projector (Allwinner H723, Android 14)

[正體中文](README.zh-TW.md)

## Device
- **Model**: JY210-C (`persist.sys.model.name`), board H200, Allwinner H723 (`sun50iw15p1`, platform `hermes`)
- **Firmware**: `UP1A.231105.001.A1` `Projector.20260725.201331`, userdebug, manufacturer `askey`, brand/model both report `Google`
- **Android**: 14 (SDK 34), 32-bit only (`armeabi-v7a`)
- **Display**: 1280x720, 213dpi
- **Network**: Wi-Fi; nothing on the USB ports

## What was done
1. **Debloated 11 packages** (and the TCL MovieArk ad launcher, switched off with the home screen switch): vendor store, memory cleaner, SwapDesk, two
   preloaded live-TV apps, the built-in media apps, the factory app and a system-uid shell service (`debloat.txt`).
2. **Installed the launcher** with the theme in `theme/` (dock: Netflix, YouTube, Disney+, Prime Video).
3. **AdGuard Private DNS**; YouTube and Netflix still load.
4. **Rolled Play Store back** to the factory build (see below).
5. **Closed network ADB** and checked with `tools/adb-closed.py`.

## Home key: HOME.JM and `persist.sys.home.selectpkg`
The remote's Home key starts `com.jm.jmlauncher/.Launcher` (category `android.intent.category.HOME.JM`), which
forwards to the package named in `persist.sys.home.selectpkg` (factory value `com.tcl.movieark.launcher`).
`cmd package set-home-activity` prints `Success` and changes nothing the remote does. The launcher declares
HOME.JM too, so the clean way is to set it as Home and then disable `com.jm.jmlauncher` (the phone app does
this inside its home screen switch). Without touching the relay, ADB runs as root here, so this also works:

```sh
TV_HOST=<ip> tools/tv.py 'setprop persist.sys.home.selectpkg com.gathin.tvhome'
```

The property survives a reboot, and the boot sequence becomes FallbackHome, JM relay, the launcher. The JM relay
and `com.tv9c.launcher` stay enabled as the fallback.

## Play Store
Play Store 53.4.17 (pushed 2026-10-26) spins forever, then shows a network error. Logcat:
`Device is ineligible for platform Cronet`, then the Java fallback Cronet, then
`UnsupportedOperationException: The bidirectional stream API is not supported by the Java implementation of Cronet Engine`.
The network itself is fine. The factory copy (`/system/priv-app/Tubesky`, 34.1.09) works:

```sh
TV_HOST=<ip> tools/tv.py 'pm uninstall-system-updates com.android.vending'
```

Play updates itself again later; on the remote: Settings > Apps > Google Play Store > Uninstall updates.

## Netflix
`com.netflix.ninja` 4.0 opens and closes within half a second. The working app is the phone build
`com.netflix.mediaclient` 9.21, which has no leanback entry, so the theme lists it in `library_apps`
(with QuickShare and Chrome, also phone-only) to put it on Home and in the dock.

## Kept on purpose
`com.bozee.usbdisplay` (QuickShare): screen mirroring and phone remote, including volume. It runs as system uid
with `INJECT_EVENTS` and its own ports, so it does not need ADB. The full list is in the header of `debloat.txt`.

## Wallpaper
`theme/config.json` sets `wallpaper: true`. Put a 1280x720 `wallpaper.jpg` in `theme/` before pushing; the one
used on this unit is a personal photo and is not in the repository.

## Restore
```sh
TV_HOST=<ip> tools/restore.sh examples/jy210c-h723/debloat.txt com.tcl.movieark.launcher
TV_HOST=<ip> tools/tv.py 'setprop persist.sys.home.selectpkg com.tcl.movieark.launcher'
TV_HOST=<ip> tools/tv.py 'settings put global private_dns_mode opportunistic'
```

## Security
`ro.adb.secure=0` and adbd runs as root (`service.adb.root=1`): port 5555 gives any device on the LAN a root shell
with no prompt. Switch USB debugging off when done. `dbgserver` also listens on `0.0.0.0:65528`; it is a native
service and was left alone.
