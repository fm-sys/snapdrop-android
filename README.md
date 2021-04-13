[![GitHub release (latest by date)](https://img.shields.io/github/v/release/fm-sys/snapdrop-android)](https://github.com/fm-sys/snapdrop-android/releases/latest) 
[![CI build](https://github.com/fm-sys/snapdrop-android/workflows/APK%20Build/badge.svg?branch=master)](https://github.com/fm-sys/snapdrop-android) 
[![Adopt website changes](https://github.com/fm-sys/snapdrop/actions/workflows/sync_fork.yml/badge.svg)](https://github.com/fm-sys/snapdrop/actions) 
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/6a918bb3dc624cba87b5139f2cb4597d)](https://www.codacy.com/gh/fm-sys/snapdrop-android/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=fm-sys/snapdrop-android&amp;utm_campaign=Badge_Grade) 
[![GitHub issues](https://img.shields.io/github/issues/fm-sys/snapdrop-android)](https://github.com/fm-sys/snapdrop-android/issues) 
[![GitHub license](https://img.shields.io/github/license/fm-sys/snapdrop-android)](https://github.com/fm-sys/snapdrop-android/blob/master/LICENSE)
[![Twitter URL](https://img.shields.io/twitter/url/https/twitter.com/fold_left.svg?style=social&label=Follow%20%40SnapdropAndroid)](https://twitter.com/SnapdropAndroid)

# Snapdrop for Android
<img align="right" src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png">

**Snapdrop for Android** is an android client for the free and open source local file sharing solution https://snapdrop.net/. 

>Do you also sometimes have the problem that you just need to quickly transfer a file from your phone to the PC?
>
> - USB? - Old fashioned!
> - Bluetooth? - Too much cumbersome and slow!
> - E-mail? - Please not another email I write to myself!
> - Snapdrop!

Snapdrop is a local file sharing solution which completely works in your browser. A bit like Apple's Airdrop, but not only for Apple devices. Windows, Linux, Android, IPhone, Mac - no problem at all!

However, even if it theoretically would fully work in your browser and you don't have to install anything, you will love this app if you want to use Snapdrop more often in your daily life. Thanks to perfect integration into the Android operating system, files are sent even faster. Directly from within other apps you can select Snapdrop to share with. Thanks to its radical simplicity, "Snapdrop for Android" makes the everyday life of hundreds of users easier. As an open source project we don't have any commercial interests but want to make the world a little bit better. Join and convince yourself!

## Where can I download the app?
Snapdrop is now available on [Google Play](https://play.google.com/store/apps/details?id=com.fmsys.snapdrop). This is the preferred way to install. For more advanced users, new releases are also always listed up at the [GitHub releases page](https://github.com/fm-sys/snapdrop-android/releases/). If you prefer an F-Droid repo, you can use the binary repo [IzzyOnDroid](https://apt.izzysoft.de/fdroid/repo?fingerprint=3BF0D6ABFEAE2F401707B6D966BE743BF0EEE49C2561B9BA39073711F628937A) which you can add to your F-Droid client. However, I take no responsibility for the content offered in that repo.

<p align="center">
  <a href="https://play.google.com/store/apps/details?id=com.fmsys.snapdrop">
    <img height="100" src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png">
  </a>
  <a href="https://apt.izzysoft.de/fdroid/index/apk/com.fmsys.snapdrop">
    <img height="100" src="docs/download_graphic.png">
  </a>
</p>

## Screenshots
<img src="https://raw.githubusercontent.com/fm-sys/snapdrop-android/master/fastlane/metadata/android/en-US/images/featureGraphic.png" width="43.3%"></img> <img src="https://raw.githubusercontent.com/fm-sys/snapdrop-android/master/fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="10%"></img> <img src="https://raw.githubusercontent.com/fm-sys/snapdrop-android/master/fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="10%"></img> <img src="https://raw.githubusercontent.com/fm-sys/snapdrop-android/master/fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width="10%"></img> <img src="https://raw.githubusercontent.com/fm-sys/snapdrop-android/master/fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" width="10%"></img> <img src="https://raw.githubusercontent.com/fm-sys/snapdrop-android/master/fastlane/metadata/android/en-US/images/phoneScreenshots/5.png" width="10%"></img> 

## Contributing
**Snapdrop for Android** originally coded by [fm-sys](https://github.com/fm-sys) would like to become a community project. I invite your participation through issues and pull requests! Also bug reports are very welcome! But note that this is **not** the right place to report bugs regarding the **Snapdrop website** which occur independently of this app.

### Localization
We use [Crowdin](https://crowdin.com/project/snapdrop-android) to manage all our translations. If you want to contribute, feel free to translate **Snapdrop for Android** into your favorite language. If the language you're looking for does not yet exist, just post a note via [Crowdin private message](https://crowdin.com/messages/create/14335754/436610) or add a small comment to [this issue](https://github.com/fm-sys/snapdrop-android/issues/43). I will activate the language as soon as possible.

<p align="center">
  <a href="https://crowdin.com/project/snapdrop-android" rel="nofollow">
    <img width="200" height="57" src="https://badges.crowdin.net/badge/dark/crowdin-on-light.png" srcset="https://badges.crowdin.net/badge/dark/crowdin-on-light.png 1x,https://badges.crowdin.net/badge/crowdin-on-light@2x.png 2x"  alt="Crowdin | Agile localization for tech companies" />
  </a>
</p>


### Development
If you want to help with development, this would be more than welcome! I am very glad about every pull request. Just fork the repo and start coding. However, if you plan to implement larger changes, please tell us in the [issue tracker](https://github.com/fm-sys/snapdrop-android/issues) before hacking on your great new feature. 

But please do not change any translations manually in the `.xml` resource files. Nothing bad would happen though, but anyway these changes will be overwritten by Crowdin. If you add/change any resource strings within your PR, Crowdin will automatically take care of all related translations. 


## Credits
- [RobinLinus](https://github.com/RobinLinus) for Snapdrop website [development](https://github.com/RobinLinus/snapdrop) and [hosting](https://snapdrop.net/), and the [launcher icon](https://github.com/RobinLinus/snapdrop/blob/663db5cbb39ab804b20f9cb6466effd9ed0e2d0c/client/images/logo_blue_512x512.png) used for this app
- [material design icons](https://material.io/resources/icons/) 
