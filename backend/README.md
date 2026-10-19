# PANDUAN DEPLOYMENT BACKEND GOOGLE APPS SCRIPT
## APLIKASI PRESENSI PKL TSM — SMK NEGERI CIPATUJAH

Dokumen ini berisi panduan teknis menghubungkan aplikasi Android PKL TSM dengan Google Sheets dan Google Drive.

---

### 1. Konfigurasi Google Cloud & Spreadsheet

Konfigurasi yang telah disiapkan:
- **SPREADSHEET_ID**: `1ccDJyW1qkusLWQd3wVBiwItFyrO8narGH89qGjNP4Ms`
- **SPREADSHEET_URL**: `https://docs.google.com/spreadsheets/d/1ccDJyW1qkusLWQd3wVBiwItFyrO8narGH89qGjNP4Ms/edit`
- **GOOGLE_DRIVE_FOLDER_ID**: `19jnh1ErjJHforfxkcU-uujCo1MjqMq4nZ`
- **GOOGLE_DRIVE_FOLDER_URL**: `https://drive.google.com/drive/folders/19jnh1ErjJHforfxkcU-uujCo1MjqMq4nZ`

---

### 2. Langkah Deployment Backend Google Apps Script (GAS)

1. Buka spreadsheet: [Link Spreadsheet PKL TSM](https://docs.google.com/spreadsheets/d/1ccDJyW1qkusLWQd3wVBiwItFyrO8narGH89qGjNP4Ms/edit).
2. Di menu atas spreadsheet, klik **Extensions (Ekstensi)** > **Apps Script**.
3. Hapus kode default di editor, lalu salin dan tempel seluruh isi file `backend/Code.gs`.
4. Simpan proyek dengan nama `Backend_PKL_TSM_Cipatujah`.
5. Klik tombol **Deploy (Terapkan)** di pojok kanan atas > **New deployment (Penerapan baru)**.
6. Pilih jenis: **Web app (Aplikasi Web)**.
7. Isi form:
   - **Description**: `Versi 1.0 PKL TSM`
   - **Execute as (Jalankan sebagai)**: `Me (Email Anda)` *(Agar script memiliki akses menulis ke Drive & Sheets)*
   - **Who has access (Siapa yang memiliki akses)**: `Anyone (Siapa saja)` *(Wajib agar siswa dapat mengirim presensi dari ponsel Android tanpa popup login Google)*
8. Klik **Deploy**.
9. Jika muncul izin akses ("Authorization required"), klik **Review Permissions**, pilih akun Google Anda, klik **Advanced** > **Go to Backend_PKL_TSM_Cipatujah (unsafe)** > **Allow**.
10. Salin **Web App URL** yang diberikan (format: `https://script.google.com/macros/s/AKfycb.../exec`).
11. Buka aplikasi Android PKL TSM, masuk ke menu **Pengaturan Backend / Admin**, lalu masukkan URL tersebut (atau simpan di pengaturan aplikasi).

---

### 3. Akun Default Siswa & Administrator

#### Akun Siswa (Pengujian Awal):
- **NISN**: `0061234567`
  - **Kata Sandi**: `tsm12345`
  - **Nama**: Ahmad Rizki Pratama
  - **Tempat PKL**: Bengkel Resmi AHASS Cipatujah
- **NISN**: `0061234568`
  - **Kata Sandi**: `tsm12345`
  - **Nama**: Budi Santoso
  - **Tempat PKL**: Bengkel Motor Sentosa Cipatujah

#### Akun Administrator:
- **Username**: `admin`
- **Password Default**: `admin123`
*(Sistem akan mewajibkan administrator mengganti kata sandi pada login pertama)*

---

### 4. Struktur Sheet yang Otomatis Dibuat

1. **DATA_PRESENSI**: Rekap kehadiran, jam server WIB, selfie drive URL, koordinat GPS, jarak ke bengkel.
2. **DATA_SISWA**: Data master siswa, NISN, hashed password, bengkel PKL, koordinat bengkel, status akun.
3. **PENGATURAN**: Jam masuk (07:00), batas toleransi (09:00), jam pulang (15:00), hari operasional, hash admin.
4. **HARI_LIBUR**: Daftar tanggal merah dan cuti bersama nasional.
5. **LOG_AUDIT**: Jejak audit setiap perubahan jadwal atau koreksi data oleh administrator.
