# Android - Remote Notify 🔔
Very specialized app that will notify about remote android device diagnostic data, specifically low battery level and storage.

> [!TIP]  
> Why❓  
> <img src="https://github.com/user-attachments/assets/0c14f049-02fc-4184-af26-ba9a6f7e530c" height="180" align="right">
> Sometimes android devices you want to monitor might not be co-located with you, and you want to be notified ahead of time of those metrics so that you can take action. For example:
> * 🪫 Battery: If it's going beyond certain level, you want to ensure the device is charging or charged up
> * 💾 Storage: If the device is running low on storage, you may want to clear up storage so that device continues to function properly.

<a href="https://play.google.com/store/apps/details?id=dev.hossain.remotenotify&pcampaignid=web_share" target="_blank"><img src="project-resources/google-play/GetItOnGooglePlay_Badge_Web_color_English.png" height="45"></a>

> [!CAUTION]  
> LIMITATION: This app uses the [`WorkManager`](https://developer.android.com/reference/androidx/work/WorkManager) to schedule the periodic check for device health. 
> The Work Manager is not guaranteed to run at exact time, it may be delayed or not run at all when the device is in deep DOZE mode or using battery saver. 
> So, please use the app with this limitation in mind.

## Quick Summary

**✨ Remote Notify: Your Remote Android Device Watchdog! 🛡️**

Tired of your remote Android devices running out of juice 🔋 or storage 💾 when you're not around?
**Remote Notify** has got your back! This smart app keeps an eye on your remote device's battery and storage levels and alerts you when it's time to take action — no matter where you are!

Stay in control with notifications via 📬 Email, Twilio (SMS via API) 📲, Telegram 📢, webhooks 🔗, and more!

### Demo 📽️
Here is what you might expect when notification gets triggers. In this demo, I have used telegram bot to notify me! 📥

<img src="https://github.com/user-attachments/assets/0ad9c4d1-94ca-42e0-a81f-fed56bcbe959#gh-light-mode-only" width="300" atl="Telegram Screenshot with Notification"> 
<img src="https://github.com/user-attachments/assets/ae35b0f7-2c3c-4026-86f4-10c08054eb10#gh-dark-mode-only" width="300" atl="Telegram Screenshot with Notification"> 
