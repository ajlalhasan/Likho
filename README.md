# Likho Keyboard (Android)

Ek AI keyboard: kisi bhi app (WhatsApp, Gmail, Instagram) mein likho, upar ki bar se language aur tone chuno, aur ✨ Convert dabao. Pura text (ya sirf select kiya hua hissa) badal jata hai. ↶ Undo se wapas.

## APK kaise banayein (bina Android Studio ke)

1. github.com par free account banayein.
2. Upar **+ > New repository** dabayein, naam `likho-keyboard` rakhein, **Public** chunein, **Create** dabayein.
3. "uploading an existing file" link dabayein. Zip khol kar is folder ki **saari cheezein** (app folder, .github folder, aur baqi files) browser mein drag karein. **Commit changes** dabayein.
4. Repository mein **Actions** tab kholein. "Build APK" chalta nazar aayega (3 se 5 minute).
5. Hara ✅ aane par us par click karein. Neeche **Artifacts** mein **Likho-Keyboard-APK** download karein. Ye zip hai, iske andar `app-debug.apk` hai.
6. APK WhatsApp par khud ko bhejein, phone par khol kar Install karein ("Unknown apps" ki ijazat maangega, de dein).

**Agar Actions tab mein kuch na chale:** `.github` folder kabhi upload nahi hota (Mac par chhupa hota hai). Tab **Add file > Create new file** dabayein, naam likhein `.github/workflows/build.yml`, aur is zip ke `build.yml` ka text paste kar ke Commit karein.

## Phone par setup

1. **Likho Keyboard** app kholein.
2. "Keyboard settings kholo" se Likho on karein.
3. "Keyboard choose karo" se Likho select karein.
4. Anthropic API key daal kar Save karein (console.anthropic.com se banti hai, per use thore se paise lagte hain).
5. Wahin neeche test karein.

## Features

- English QWERTY + numbers/symbols, hold karne par backspace chalta rehta hai, double tap shift = caps lock
- 🌐 button se doosre keyboard (jaise Urdu wala) par switch
- 19 languages, 15 tones (Professional, Formal, Friendly, Casual, Funny, Short, Grammar fix, Emojis...)
- Text select karein to sirf wahi convert hoga
- Dark mode khud follow karta hai

## Files

- `LikhoIME.kt` keyboard
- `MainActivity.kt` setup screen
- `Claude.kt` AI call
- `Options.kt` languages aur tones ki list
