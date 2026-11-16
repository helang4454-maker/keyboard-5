# Mod Keyboard (Android)

Papan kekunci Android sebenar berdasarkan `ModKeyboard.html`.
Ciri: Ctrl / Alt / Cmd, panel `Aa`, `F` (F1-F12, anak panah, Home/End/PgUp/PgDn/Del), `#` (simbol), `▦` (nombor),
Shift / Caps, ulang auto untuk Backspace dan anak panah, tema gelap/cerah, tiga pilihan tinggi kekunci.

## Bina APK tanpa komputer (GitHub Actions)
1. Buat repo baharu di GitHub, kemudian muat naik semua fail dalam folder ini (termasuk folder `.github`).
2. Buka tab **Actions** → **Build APK** → tunggu sehingga siap (hijau). Boleh tekan **Run workflow** jika tidak jalan sendiri.
3. Klik run tersebut, di bahagian **Artifacts** muat turun **ModKeyboard-APK** (fail zip), keluarkan `app-debug.apk`.

## Pasang di Samsung Galaxy A03
1. Pindahkan `app-debug.apk` ke telefon dan buka. Benarkan **Pasang dari sumber tidak diketahui** jika diminta.
2. Buka aplikasi **Mod Keyboard**:
   - Tekan **1. Hidupkan Mod Keyboard**, hidupkan suisnya dan terima amaran.
   - Tekan **2. Pilih Mod Keyboard**, dan pilih Mod Keyboard.
3. Untuk bertukar semula: tekan ikon papan kekunci di bawah kanan skrin, atau ikon 🌐 pada papan kekunci ini.

## Bina di komputer (pilihan)
Buka folder ini dalam Android Studio dan tekan Run, atau `gradle assembleDebug`.

## Had semasa
- Tiada blur sebenar (Android tidak membenarkan IME mengaburkan aplikasi di belakang), hanya latar pepejal.
- Tiada popup huruf ketika menekan.
- Ctrl/Alt/Cmd menghantar kekunci sebenar, tetapi sama ada aplikasi menerimanya bergantung pada aplikasi itu.
