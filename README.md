# QuickNovel Ultimate

**English** | [Русский](README_RU.md)

Fork of [QuickNovel](https://github.com/LagradOst/QuickNovel) focused on a custom brand experience, Fire themes, Blogger/custom providers, and clearer library history.

Ad-free FOSS Android app for downloading novels. It also functions as an Epub reader.

**Repo:** https://github.com/RaffyRod/NovelReader  
**Upstream:** https://github.com/LagradOst/QuickNovel  
**Current version:** 1.1.4

**Download:** https://github.com/RaffyRod/NovelReader/releases

### What’s new in this fork

- **Ultimate Blue** as the default app look, with matching launcher branding (orb + 本).
- **Fire themes** (Blue / Black / Lava) shared by settings icons, bottom menu, library sections, and launcher icons.
- **Custom providers:** add Blog (Blogger index) or Website sources, with validation and localized tutorials.
- **History:** separates **Reading** vs **Visited** novels (sections + pills), themed with Fire accents.
- Chapter list no longer shows release timestamps under each chapter.
- **Import-B** Fire drop-zone in Biblioteca; library search stays pinned.
- **TTS:** prefers Piper/Sherpa neural voices (Dave ES / Lessac EN), language from text, slower neural rate.
- App id: `com.lagradost.quicknovel.ultimate` (universal APK: arm64-v8a / armeabi-v7a / x86_64).

### Build

```bash
./gradlew :app:assembleRelease
```

Signed universal APKs for testing are produced under `app/build/outputs/apk/release/` (use `app-universal-release-unsigned.apk` + your keystore).

### E2E automation (WebdriverIO + Appium)

See [`e2e/README.md`](e2e/README.md). CI runs on **every push** via [`.github/workflows/e2e-android.yml`](.github/workflows/e2e-android.yml) and uploads **Allure** results.

```bash
cd e2e
npm install
npm test
npm run allure:report
```

**Providers used:**

- https://allnovel.org
- https://annas-archive.org
- https://bestlightnovel.com
- https://chrysanthemumgarden.com
- https://www.fanmtl.com
- https://fenrirealm.com
- https://free.kolnovel.com
- https://freewebnovel.com
- https://fucknovelpia.com
- https://graycity.net
- https://hiraethtranslation.com
- https://indowebnovel.id
- https://libread.com
- https://lightnovelstranslations.com
- https://lightnovelworld.org
- https://lnmtl.com
- https://meionovels.com
- https://mtlnovel.me
- https://www.nobadnovel.com
- https://novelbin.com
- https://novelbuddy.com
- https://novelfire.net
- https://novelfull.com
- https://novelsonline.org
- https://novlove.com
- https://pawread.com
- https://readfrom.net
- https://readnovelfull.com
- https://readonlinefreebook.com
- https://rewayat.club
- https://risenovel.com
- https://www.royalroad.com
- https://sakuranovel.id
- https://www.scribblehub.com
- https://www.skynovels.net
- https://www.sonicmtl.com
- https://wtr-lab.com
- https://wuxia.click
- https://www.wuxiabox.com

Plus **user-defined** Blog / Website providers configured in Settings.

**Screenshots:**

<img src="./.github/home.jpg" height="400"/><img src="./.github/search.jpg" height="400"/><img src="./.github/downloads.jpg" height="400"/><img src="./.github/result.jpg" height="400"/><img src="./.github/reader.jpg" height="400"/>

**Credits:**

Based on QuickNovel by [LagradOst](https://github.com/LagradOst/QuickNovel). Upstream Discord: https://discord.gg/5Hus6fM

**Legal notice:**

Any legal issues regarding the content on this application should be taken up with the actual file hosts and providers themselves as we are not affiliated with them.

In case of copyright infringement, please directly contact the responsible parties or the streaming websites.

The app is purely for educational and personal use.

QuickNovel does not host any content on the app, and has no control over what media is put up or taken down. QuickNovel functions like any other search engine, such as Google. QuickNovel does not host, upload or manage any videos, films or content. It simply crawls, aggregates and displayes links in a convenient, user-friendly interface.

It merely scrapes 3rd-party websites that are publicly accessable via any regular web browser. It is the responsibility of user to avoid any actions that might violate the laws governing his/her locality. Use QuickNovel at your own risk.
