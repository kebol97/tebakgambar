# Sistem Akumulasi Skor dan Tier

Mengubah sistem penyimpanan skor dari "Skor Tertinggi" menjadi "Akumulasi Skor Total". Ini memungkinkan pemain untuk mencapai skor ribuan dengan bermain berulang kali dan naik ke Tier yang lebih tinggi (Semi-Pro, Profesional, Legenda).

## Proposed Changes

### [FirebaseManager](file:///C:/Users/BEY/KuisTebakBola/app/src/main/java/com/cococue/kuistebakbola/FirebaseManager.kt)

#### [MODIFY] [FirebaseManager.kt](file:///C:/Users/BEY/KuisTebakBola/app/src/main/java/com/cococue/kuistebakbola/FirebaseManager.kt)
- Mengubah fungsi `uploadScore` agar tidak menghitung tier di awal (karena tier bergantung pada total skor akumulasi).
- Mengganti fungsi `updateIfHigher` menjadi `updateScoreAccumulated` yang melakukan:
    1. Mengambil skor lama dari Firestore.
    2. Menambahkan skor sesi baru ke skor lama.
    3. Menghitung Tier baru berdasarkan hasil penjumlahan tersebut.
    4. Menyimpan data yang sudah diperbarui.

## Verification Plan

### Manual Verification
- Menjalankan aplikasi.
- Bermain satu sesi kuis dan menyelesaikan 10 soal.
- Memeriksa di Leaderboard atau Profile apakah skor bertambah dari sesi sebelumnya (akumulasi).
- Memastikan Tier berubah jika total skor melewati ambang batas (misal: melewati 500 menjadi Semi-Pro).
