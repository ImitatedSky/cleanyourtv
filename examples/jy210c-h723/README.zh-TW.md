# JY210-C 投影機（Allwinner H723，Android 14）

[English](README.md)

設定日期：2026-10-27。

## 做了什麼

1. **Play 商店修好**：2026-10-26 自動更新到 53.4.17 後在這台 32 位元韌體上連不上網（一直轉圈或顯示「發生錯誤」）。已退回出廠版 34.1.09。
2. **停用 11 個 App**（只停用不移除）：見 `debloat.txt`；原本的 TCL 廣告桌面另外在換桌面時停用。
3. **換成 CleanYourTV 桌面**（`com.gathin.tvhome`），自訂桌布（照片沒放進 repo，推主題前把 1280x720 的 `wallpaper.jpg` 放進 `theme/`）、Dock 放 Netflix / YouTube / Disney+ / Prime Video。
4. **AdGuard 擋廣告 DNS**（`dns.adguard-dns.com`）。
5. **關閉網路 ADB**：這台出廠 ADB 不用授權、而且直接是 root，同網段任何人都能完全控制。

## 這台韌體的特殊處

- 遙控器 Home 鍵不是走 Android 的 Home 設定，而是先到 JM 中繼（`com.jm.jmlauncher`，`HOME.JM`），
  再轉給 `persist.sys.home.selectpkg` 指定的 App。所以換桌面要改這個屬性：
  `setprop persist.sys.home.selectpkg com.gathin.tvhome`
- Netflix 能用的是手機版 `com.netflix.mediaclient`；`com.netflix.ninja` 4.0 是開了就關的空殼。
  手機版沒有電視入口，所以用主題的 `library_apps` 讓它出現在桌面。

## 之後要改東西：先重開 ADB

遙控器：設定 → 裝置偏好設定 → 關於 → 在「版本」按 OK 七次 → 返回 → 開發人員選項 → 開啟 USB 偵錯。
改完記得再關掉。

## 常用指令（在 cleanyourtv 資料夾，Git Bash）

```sh
export TV_HOST=<投影機 IP> MSYS_NO_PATHCONV=1

# 換 Dock 順序、桌布：改 theme/config.json 或 theme/wallpaper.jpg，然後
tools/theme.sh examples/jy210c-h723/theme

# 重新啟用某個 App
tools/tv.py 'pm enable --user 0 <套件名>'

# 全部還原（含原本的 TCL 桌面）
tools/restore.sh examples/jy210c-h723/debloat.txt com.tcl.movieark.launcher
tools/tv.py 'setprop persist.sys.home.selectpkg com.tcl.movieark.launcher'

# 關掉擋廣告 DNS
tools/tv.py 'settings put global private_dns_mode opportunistic'
```

## Play 商店又壞掉時

Play 商店會在背景自己更新，可能又被推到壞掉的版本。用遙控器就能修：
設定 → 應用程式 → Google Play 商店 → **解除安裝更新**。

## 備份

`backup/jy210c-20261027-195504/`：改動前的完整套件清單、getprop、所有 settings。
