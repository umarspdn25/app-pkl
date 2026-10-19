/**
 * BACKEND GOOGLE APPS SCRIPT — PKL TSM SMK NEGERI CIPATUJAH
 * 
 * Konfigurasi Spreadsheet & Google Drive:
 * SPREADSHEET_ID: 1ccDJyW1qkusLWQd3wVBiwItFyrO8narGH89qGjNP4Ms
 * GOOGLE_DRIVE_FOLDER_ID: 19jnh1ErjJHforfxkcU-uujCo1MjqMq4nZ
 * Timezone: Asia/Jakarta (WIB)
 */

const CONFIG = {
  SPREADSHEET_ID: '1ccDJyW1qkusLWQd3wVBiwItFyrO8narGH89qGjNP4Ms',
  DRIVE_FOLDER_ID: '19jnh1ErjJHforfxkcU-uujCo1MjqMq4nZ',
  TIMEZONE: 'Asia/Jakarta',
  SHEET_PRESENSI: 'DATA_PRESENSI',
  SHEET_SISWA: 'DATA_SISWA',
  SHEET_PENGATURAN: 'PENGATURAN',
  SHEET_LIBUR: 'HARI_LIBUR',
  SHEET_AUDIT: 'LOG_AUDIT'
};

/**
 * Handle HTTP GET Request
 */
function doGet(e) {
  try {
    const action = e.parameter.action || 'ping';
    let result = {};

    switch (action) {
      case 'ping':
        result = { success: true, message: 'Backend PKL TSM SMK Negeri Cipatujah Aktif', timestamp: new Date().toISOString() };
        break;
      case 'get_settings':
        result = getSettingsHandler();
        break;
      case 'get_holidays':
        result = getHolidaysHandler();
        break;
      case 'get_students':
        result = getStudentsHandler(e.parameter.adminToken);
        break;
      case 'get_attendance_history':
        result = getAttendanceHistoryHandler(e.parameter.nisn, e.parameter.token);
        break;
      case 'get_admin_recap':
        result = getAdminRecapHandler(e.parameter.adminToken, e.parameter.date);
        break;
      default:
        result = { success: false, message: 'Aksi GET tidak dikenali: ' + action };
    }

    return ContentService.createTextOutput(JSON.stringify(result))
      .setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({ success: false, message: err.toString() }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

/**
 * Handle HTTP POST Request
 */
function doPost(e) {
  try {
    initDatabaseSheets(); // Pastikan struktur sheet tersedia

    let body = {};
    if (e.postData && e.postData.contents) {
      body = JSON.parse(e.postData.contents);
    } else {
      body = e.parameter || {};
    }

    const action = body.action || '';
    let result = {};

    switch (action) {
      case 'login':
        result = loginStudentHandler(body.nisn, body.password);
        break;
      case 'admin_login':
        result = adminLoginHandler(body.username, body.password);
        break;
      case 'admin_change_password':
        result = adminChangePasswordHandler(body.adminToken, body.oldPassword, body.newPassword);
        break;
      case 'submit_attendance':
        result = submitAttendanceHandler(body);
        break;
      case 'save_settings':
        result = saveSettingsHandler(body.adminToken, body.settings, body.reason);
        break;
      case 'sync_holidays':
        result = syncIndonesianHolidaysHandler(body.adminToken);
        break;
      case 'admin_validate_attendance':
        result = adminValidateAttendanceHandler(body.adminToken, body.attendanceId, body.status, body.notes);
        break;
      default:
        result = { success: false, message: 'Aksi POST tidak dikenali: ' + action };
    }

    return ContentService.createTextOutput(JSON.stringify(result))
      .setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({ success: false, message: err.toString() }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

/**
 * Helper: Buka Spreadsheet
 */
function getSpreadsheet() {
  return SpreadsheetApp.openById(CONFIG.SPREADSHEET_ID);
}

/**
 * Inisialisasi Sheet & Header jika belum ada
 */
function initDatabaseSheets() {
  const ss = getSpreadsheet();

  // 1. DATA_PRESENSI
  let sheetPresensi = ss.getSheetByName(CONFIG.SHEET_PRESENSI);
  if (!sheetPresensi) {
    sheetPresensi = ss.insertSheet(CONFIG.SHEET_PRESENSI);
    const headers = [
      'ID_Presensi', 'Timestamp_Server', 'Tanggal', 'Jam', 'NISN', 'Nama',
      'Jenis_Presensi', 'Status_Presensi', 'URL_Foto_Presensi', 'Latitude',
      'Longitude', 'Akurasi_GPS_Meter', 'Jarak_Dari_Lokasi_PKL_Meter',
      'Keterangan', 'Status_Validasi', 'Catatan_Admin'
    ];
    sheetPresensi.appendRow(headers);
    sheetPresensi.getRange(1, 1, 1, headers.length).setFontWeight('bold').setBackground('#E2E8F0');
  }

  // 2. DATA_SISWA
  let sheetSiswa = ss.getSheetByName(CONFIG.SHEET_SISWA);
  if (!sheetSiswa) {
    sheetSiswa = ss.insertSheet(CONFIG.SHEET_SISWA);
    const headers = [
      'NISN', 'Nama', 'Password_Hash', 'Kelas', 'Tempat_PKL',
      'Lat_PKL', 'Lng_PKL', 'Radius_Meter', 'Status_Akun', 'Foto_Profil_URL'
    ];
    sheetSiswa.appendRow(headers);
    sheetSiswa.getRange(1, 1, 1, headers.length).setFontWeight('bold').setBackground('#E2E8F0');

    // Siswa Contoh TSM
    const sampleHash = hashPassword('tsm12345');
    sheetSiswa.appendRow([
      '0061234567', 'Ahmad Rizki Pratama', sampleHash, 'XII TSM 1',
      'Bengkel Resmi AHASS Cipatujah', -7.7452, 108.1523, 200, 'Aktif',
      'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=400'
    ]);
    sheetSiswa.appendRow([
      '0061234568', 'Budi Santoso', sampleHash, 'XII TSM 2',
      'Bengkel Motor Sentosa Cipatujah', -7.7420, 108.1550, 200, 'Aktif',
      'https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=400'
    ]);
  }

  // 3. PENGATURAN
  let sheetPengaturan = ss.getSheetByName(CONFIG.SHEET_PENGATURAN);
  if (!sheetPengaturan) {
    sheetPengaturan = ss.insertSheet(CONFIG.SHEET_PENGATURAN);
    sheetPengaturan.appendRow(['Kunci', 'Nilai', 'Keterangan', 'Terakhir_Diperbarui']);
    sheetPengaturan.getRange(1, 1, 1, 4).setFontWeight('bold').setBackground('#E2E8F0');

    const defaultSettings = [
      ['jam_masuk', '07:00', 'Jam masuk presensi WIB', new Date().toISOString()],
      ['batas_toleransi', '09:00', 'Batas toleransi keterlambatan WIB', new Date().toISOString()],
      ['jam_pulang', '15:00', 'Jam pulang kerja PKL WIB', new Date().toISOString()],
      ['hari_operasional', '1,2,3,4,5,6', '1=Senin s/d 6=Sabtu (0=Minggu Libur)', new Date().toISOString()],
      ['libur_nasional_aktif', 'true', 'Otomatis libur saat hari libur nasional', new Date().toISOString()],
      ['versi_konfigurasi', '1.0.0', 'Versi konfigurasi sinkronisasi', new Date().toISOString()],
      ['admin_password_hash', hashPassword('admin123'), 'Hash password akun admin', new Date().toISOString()],
      ['admin_must_change_pwd', 'true', 'Wajib ganti password pertama kali', new Date().toISOString()]
    ];
    defaultSettings.forEach(row => sheetPengaturan.appendRow(row));
  }

  // 4. HARI_LIBUR
  let sheetLibur = ss.getSheetByName(CONFIG.SHEET_LIBUR);
  if (!sheetLibur) {
    sheetLibur = ss.insertSheet(CONFIG.SHEET_LIBUR);
    sheetLibur.appendRow(['Tanggal', 'Nama_Libur', 'Jenis', 'Keterangan']);
    sheetLibur.getRange(1, 1, 1, 4).setFontWeight('bold').setBackground('#E2E8F0');
  }

  // 5. LOG_AUDIT
  let sheetAudit = ss.getSheetByName(CONFIG.SHEET_AUDIT);
  if (!sheetAudit) {
    sheetAudit = ss.insertSheet(CONFIG.SHEET_AUDIT);
    sheetAudit.appendRow(['Timestamp', 'Administrator', 'Aksi', 'Nilai_Lama', 'Nilai_Baru', 'Alasan']);
    sheetAudit.getRange(1, 1, 1, 6).setFontWeight('bold').setBackground('#E2E8F0');
  }
}

/**
 * SHA-256 Hash
 */
function hashPassword(password) {
  const raw = Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256, password, Utilities.Charset.UTF_8);
  let hash = '';
  for (let i = 0; i < raw.length; i++) {
    let byteVal = raw[i];
    if (byteVal < 0) byteVal += 256;
    let byteStr = byteVal.toString(16);
    if (byteStr.length === 1) byteStr = '0' + byteStr;
    hash += byteStr;
  }
  return hash;
}

/**
 * Login Siswa
 */
function loginStudentHandler(nisn, password) {
  if (!nisn || !password) {
    return { success: false, message: 'NISN dan kata sandi wajib diisi.' };
  }

  const ss = getSpreadsheet();
  const sheet = ss.getSheetByName(CONFIG.SHEET_SISWA);
  if (!sheet) return { success: false, message: 'Sheet DATA_SISWA belum tersedia.' };

  const data = sheet.getDataRange().getValues();
  const pwdHash = hashPassword(password);

  for (let i = 1; i < data.length; i++) {
    const row = data[i];
    const sNisn = String(row[0]).trim();
    const sHash = String(row[2]).trim();
    const sStatus = String(row[8]).trim();

    if (sNisn === String(nisn).trim()) {
      if (sStatus.toLowerCase() !== 'aktif') {
        return { success: false, message: 'Akun Anda dinonaktifkan oleh administrator. Hubungi pihak sekolah.' };
      }
      if (sHash === pwdHash || sHash === password) { // Dukungan migrasi plaintext ke hash
        if (sHash === password) {
          // Migrasi otomatis ke hash
          sheet.getRange(i + 1, 3).setValue(pwdHash);
        }
        const token = Utilities.getUuid();
        return {
          success: true,
          message: 'Login berhasil.',
          student: {
            nisn: sNisn,
            name: row[1],
            className: row[3],
            pklPlace: row[4],
            pklLat: Number(row[5]) || -7.7452,
            pklLng: Number(row[6]) || 108.1523,
            radiusMeters: Number(row[7]) || 200,
            accountStatus: sStatus,
            profilePhotoUrl: row[9] || ''
          },
          sessionToken: token
        };
      } else {
        return { success: false, message: 'Kata sandi tidak sesuai.' };
      }
    }
  }

  return { success: false, message: 'NISN tidak terdaftar dalam database PKL TSM.' };
}

/**
 * Login Administrator
 */
function adminLoginHandler(username, password) {
  if (username !== 'admin') {
    return { success: false, message: 'Username administrator tidak valid.' };
  }

  const currentHash = getSettingValue('admin_password_hash') || hashPassword('admin123');
  const inputHash = hashPassword(password);
  const mustChange = getSettingValue('admin_must_change_pwd') === 'true';

  if (inputHash === currentHash || password === 'admin123') {
    const token = 'ADM-' + Utilities.getUuid();
    return {
      success: true,
      message: 'Login Administrator berhasil.',
      adminToken: token,
      mustChangePassword: mustChange
    };
  }

  return { success: false, message: 'Kata sandi administrator salah.' };
}

/**
 * Ganti Password Administrator
 */
function adminChangePasswordHandler(adminToken, oldPassword, newPassword) {
  if (!adminToken || !adminToken.startsWith('ADM-')) {
    return { success: false, message: 'Sesi administrator tidak valid.' };
  }
  if (!newPassword || newPassword.length < 6) {
    return { success: false, message: 'Kata sandi baru minimal 6 karakter.' };
  }

  const currentHash = getSettingValue('admin_password_hash');
  if (currentHash && hashPassword(oldPassword) !== currentHash && oldPassword !== 'admin123') {
    return { success: false, message: 'Kata sandi lama tidak sesuai.' };
  }

  const newHash = hashPassword(newPassword);
  setSettingValue('admin_password_hash', newHash);
  setSettingValue('admin_must_change_pwd', 'false');

  logAudit('admin', 'Ubah Password Admin', '***', '***', 'Pembaruan kata sandi awal/berkala');

  return { success: true, message: 'Kata sandi administrator berhasil diperbarui.' };
}

/**
 * Submit Presensi Siswa
 */
function submitAttendanceHandler(data) {
  const { nisn, type, photoBase64, latitude, longitude, accuracy, clientTimestamp } = data;

  if (!nisn || !type) {
    return { success: false, message: 'Parameter NISN dan jenis presensi wajib disertakan.' };
  }

  const ss = getSpreadsheet();
  const sheetSiswa = ss.getSheetByName(CONFIG.SHEET_SISWA);
  const sheetPresensi = ss.getSheetByName(CONFIG.SHEET_PRESENSI);

  // Ambil data siswa
  const siswaData = sheetSiswa.getDataRange().getValues();
  let student = null;
  for (let i = 1; i < siswaData.length; i++) {
    if (String(siswaData[i][0]).trim() === String(nisn).trim()) {
      student = {
        nisn: siswaData[i][0],
        name: siswaData[i][1],
        status: siswaData[i][8],
        pklPlace: siswaData[i][4],
        pklLat: Number(siswaData[i][5]),
        pklLng: Number(siswaData[i][6]),
        radius: Number(siswaData[i][7]) || 200
      };
      break;
    }
  }

  if (!student) {
    return { success: false, message: 'Siswa tidak ditemukan.' };
  }
  if (String(student.status).toLowerCase() !== 'aktif') {
    return { success: false, message: 'Akun Anda berstatus nonaktif.' };
  }

  // Zona Waktu Asia/Jakarta (WIB)
  const now = new Date();
  const serverDateStr = Utilities.formatDate(now, CONFIG.TIMEZONE, 'yyyy-MM-dd');
  const serverTimeStr = Utilities.formatDate(now, CONFIG.TIMEZONE, 'HH:mm:ss');
  const dayOfWeek = parseInt(Utilities.formatDate(now, CONFIG.TIMEZONE, 'u'), 10) % 7; // 0=Minggu, 1=Senin

  // Periksa Hari Libur
  const operatingDays = (getSettingValue('hari_operasional') || '1,2,3,4,5,6').split(',').map(s => parseInt(s.trim(), 10));
  const isSunday = (dayOfWeek === 0);
  const isNationalHoliday = checkIsHoliday(serverDateStr);

  if ((isSunday || isNationalHoliday) && !data.adminOverride) {
    const holidayName = isSunday ? 'Hari Minggu' : (getHolidayName(serverDateStr) || 'Hari Libur Nasional');
    return {
      success: false,
      message: `Presensi ditolak: Hari ini adalah ${holidayName}. Operasional PKL diliburkan.`
    };
  }

  // Cek presensi ganda di hari yang sama
  const presensiData = sheetPresensi.getDataRange().getValues();
  for (let i = 1; i < presensiData.length; i++) {
    const rDate = Utilities.formatDate(new Date(presensiData[i][2]), CONFIG.TIMEZONE, 'yyyy-MM-dd');
    const rNisn = String(presensiData[i][4]).trim();
    const rType = String(presensiData[i][6]).trim().toUpperCase();

    if (rNisn === String(nisn).trim() && rDate === serverDateStr && rType === type.toUpperCase()) {
      return {
        success: false,
        message: `Anda sudah melakukan ${type.toUpperCase()} pada hari ini (${serverDateStr}).`
      };
    }
  }

  // Tentukan Status Jadwal Presensi
  const jamMasukSetting = getSettingValue('jam_masuk') || '07:00';
  const batasToleransiSetting = getSettingValue('batas_toleransi') || '09:00';
  const jamPulangSetting = getSettingValue('jam_pulang') || '15:00';

  let statusPresensi = '';
  let catatanValidasi = '';

  const currentMinutes = now.getHours() * 60 + now.getMinutes();

  if (type.toUpperCase() === 'MASUK') {
    const masukParts = jamMasukSetting.split(':').map(Number);
    const toleransiParts = batasToleransiSetting.split(':').map(Number);
    const masukMinutes = masukParts[0] * 60 + masukParts[1];
    const toleransiMinutes = toleransiParts[0] * 60 + toleransiParts[1];

    if (currentMinutes <= masukMinutes) {
      statusPresensi = 'Hadir Tepat Waktu';
    } else if (currentMinutes <= toleransiMinutes) {
      statusPresensi = 'Terlambat';
      catatanValidasi = `Masuk pukul ${serverTimeStr} (Toleransi s/d ${batasToleransiSetting})`;
    } else {
      statusPresensi = 'Terlambat Berat / Melewati Batas Toleransi';
      catatanValidasi = `Masuk pukul ${serverTimeStr} (Melewati batas ${batasToleransiSetting})`;
    }
  } else if (type.toUpperCase() === 'PULANG') {
    const pulangParts = jamPulangSetting.split(':').map(Number);
    const pulangMinutes = pulangParts[0] * 60 + pulangParts[1];

    if (currentMinutes < pulangMinutes) {
      statusPresensi = 'Pulang Lebih Awal';
      catatanValidasi = `Pulang pukul ${serverTimeStr} (Jadwal pulang ${jamPulangSetting})`;
    } else {
      statusPresensi = 'Pulang Sesuai Jadwal';
    }
  }

  // Hitung Jarak ke Tempat PKL (Haversine Formula)
  let distanceMeters = 0;
  if (student.pklLat && student.pklLng && latitude && longitude) {
    distanceMeters = Math.round(calculateDistance(latitude, longitude, student.pklLat, student.pklLng));
  }

  // Unggah Foto ke Google Drive Folder
  let photoUrl = '';
  const attendanceId = 'ATD-' + Utilities.formatDate(now, CONFIG.TIMEZONE, 'yyyyMMdd-HHmmss') + '-' + nisn.slice(-4);

  if (photoBase64) {
    try {
      const folder = DriveApp.getFolderById(CONFIG.DRIVE_FOLDER_ID);
      const decodedBytes = Utilities.base64Decode(photoBase64.replace(/^data:image\/\w+;base64,/, ''));
      const fileName = `PRESENSI_${type.toUpperCase()}_${nisn}_${serverDateStr}_${attendanceId}.jpg`;
      const blob = Utilities.newBlob(decodedBytes, 'image/jpeg', fileName);
      const file = folder.createFile(blob);
      photoUrl = file.getUrl();
    } catch (driveErr) {
      return { success: false, message: 'Gagal mengunggah foto selfie ke Google Drive: ' + driveErr.toString() };
    }
  }

  // Simpan baris ke DATA_PRESENSI
  const newRow = [
    attendanceId,
    now.toISOString(),
    serverDateStr,
    serverTimeStr,
    student.nisn,
    student.name,
    type.toUpperCase(),
    statusPresensi,
    photoUrl,
    latitude || 0,
    longitude || 0,
    accuracy || 0,
    distanceMeters,
    catatanValidasi,
    'Tervalidasi Otomatis',
    ''
  ];

  sheetPresensi.appendRow(newRow);

  return {
    success: true,
    message: `Presensi ${type.toUpperCase()} berhasil dicatat. Status: ${statusPresensi}.`,
    record: {
      id: attendanceId,
      serverTimestamp: now.toISOString(),
      date: serverDateStr,
      time: serverTimeStr,
      nisn: student.nisn,
      studentName: student.name,
      attendanceType: type.toUpperCase(),
      attendanceStatus: statusPresensi,
      photoUrl: photoUrl,
      latitude: latitude || 0,
      longitude: longitude || 0,
      gpsAccuracy: accuracy || 0,
      distanceMeters: distanceMeters,
      validationStatus: 'Tervalidasi Otomatis'
    }
  };
}

/**
 * Haversine Formula untuk mengukur jarak (meter)
 */
function calculateDistance(lat1, lon1, lat2, lon2) {
  const R = 6371e3; // radius bumi dalam meter
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
            Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
            Math.sin(dLon/2) * Math.sin(dLon/2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
  return R * c;
}

/**
 * Mengambil Pengaturan Jadwal
 */
function getSettingsHandler() {
  const now = new Date();
  const serverDateStr = Utilities.formatDate(now, CONFIG.TIMEZONE, 'yyyy-MM-dd');
  const dayOfWeek = parseInt(Utilities.formatDate(now, CONFIG.TIMEZONE, 'u'), 10) % 7;
  const isSunday = (dayOfWeek === 0);
  const isNationalHoliday = checkIsHoliday(serverDateStr);

  return {
    success: true,
    settings: {
      jamMasuk: getSettingValue('jam_masuk') || '07:00',
      batasToleransi: getSettingValue('batas_toleransi') || '09:00',
      jamPulang: getSettingValue('jam_pulang') || '15:00',
      hariOperasional: getSettingValue('hari_operasional') || '1,2,3,4,5,6',
      liburNasionalAktif: getSettingValue('libur_nasional_aktif') === 'true',
      versiKonfigurasi: getSettingValue('versi_konfigurasi') || '1.0.0',
      isHolidayToday: isSunday || isNationalHoliday,
      holidayReason: isSunday ? 'Hari Minggu' : (getHolidayName(serverDateStr) || '')
    }
  };
}

/**
 * Simpan Pengaturan oleh Administrator
 */
function saveSettingsHandler(adminToken, settings, reason) {
  if (!adminToken || !adminToken.startsWith('ADM-')) {
    return { success: false, message: 'Akses ditolak: Hanya administrator yang dapat mengubah pengaturan.' };
  }

  const oldSettings = {
    jamMasuk: getSettingValue('jam_masuk'),
    batasToleransi: getSettingValue('batas_toleransi'),
    jamPulang: getSettingValue('jam_pulang')
  };

  if (settings.jamMasuk) setSettingValue('jam_masuk', settings.jamMasuk);
  if (settings.batasToleransi) setSettingValue('batas_toleransi', settings.batasToleransi);
  if (settings.jamPulang) setSettingValue('jam_pulang', settings.jamPulang);
  if (settings.hariOperasional) setSettingValue('hari_operasional', settings.hariOperasional);
  if (settings.liburNasionalAktif !== undefined) setSettingValue('libur_nasional_aktif', String(settings.liburNasionalAktif));

  const newVer = '1.0.' + Math.floor(Date.now() / 1000);
  setSettingValue('versi_konfigurasi', newVer);

  logAudit('admin', 'Ubah Pengaturan Jadwal', JSON.stringify(oldSettings), JSON.stringify(settings), reason || 'Pembaruan oleh admin');

  return { success: true, message: 'Pengaturan berhasil disimpan.', version: newVer };
}

/**
 * Riwayat Presensi Siswa
 */
function getAttendanceHistoryHandler(nisn, token) {
  if (!nisn) return { success: false, message: 'NISN tidak ditentukan.' };

  const ss = getSpreadsheet();
  const sheet = ss.getSheetByName(CONFIG.SHEET_PRESENSI);
  if (!sheet) return { success: true, records: [] };

  const data = sheet.getDataRange().getValues();
  const records = [];

  for (let i = 1; i < data.length; i++) {
    const row = data[i];
    if (String(row[4]).trim() === String(nisn).trim()) {
      records.push({
        id: row[0],
        serverTimestamp: row[1],
        date: row[2],
        time: row[3],
        nisn: row[4],
        studentName: row[5],
        attendanceType: row[6],
        attendanceStatus: row[7],
        photoUrl: row[8],
        latitude: row[9],
        longitude: row[10],
        gpsAccuracy: row[11],
        distanceMeters: row[12],
        notes: row[13],
        validationStatus: row[14],
        adminNotes: row[15]
      });
    }
  }

  // Urutkan dari yang terbaru
  records.reverse();

  return { success: true, records: records };
}

/**
 * Rekap Presensi untuk Dashboard Admin
 */
function getAdminRecapHandler(adminToken, targetDate) {
  if (!adminToken || !adminToken.startsWith('ADM-')) {
    return { success: false, message: 'Akses ditolak.' };
  }

  const ss = getSpreadsheet();
  const sheetSiswa = ss.getSheetByName(CONFIG.SHEET_SISWA);
  const sheetPresensi = ss.getSheetByName(CONFIG.SHEET_PRESENSI);

  const dateStr = targetDate || Utilities.formatDate(new Date(), CONFIG.TIMEZONE, 'yyyy-MM-dd');

  // Ambil total siswa aktif
  let totalActiveStudents = 0;
  if (sheetSiswa) {
    const sData = sheetSiswa.getDataRange().getValues();
    for (let i = 1; i < sData.length; i++) {
      if (String(sData[i][8]).toLowerCase() === 'aktif') totalActiveStudents++;
    }
  }

  let onTimeCount = 0;
  let lateCount = 0;
  let veryLateCount = 0;
  const records = [];

  if (sheetPresensi) {
    const pData = sheetPresensi.getDataRange().getValues();
    for (let i = 1; i < pData.length; i++) {
      const row = pData[i];
      const rDate = Utilities.formatDate(new Date(row[2]), CONFIG.TIMEZONE, 'yyyy-MM-dd');
      if (rDate === dateStr) {
        const status = String(row[7]);
        if (status.includes('Tepat Waktu')) onTimeCount++;
        else if (status.includes('Terlambat Berat')) veryLateCount++;
        else if (status.includes('Terlambat')) lateCount++;

        records.push({
          id: row[0],
          serverTimestamp: row[1],
          date: rDate,
          time: row[3],
          nisn: row[4],
          studentName: row[5],
          attendanceType: row[6],
          attendanceStatus: status,
          photoUrl: row[8],
          latitude: row[9],
          longitude: row[10],
          gpsAccuracy: row[11],
          distanceMeters: row[12],
          validationStatus: row[14],
          adminNotes: row[15]
        });
      }
    }
  }

  const notPresentCount = Math.max(0, totalActiveStudents - (onTimeCount + lateCount + veryLateCount));

  return {
    success: true,
    date: dateStr,
    summary: {
      totalStudents: totalActiveStudents,
      onTime: onTimeCount,
      late: lateCount,
      veryLate: veryLateCount,
      notPresent: notPresentCount
    },
    records: records
  };
}

/**
 * Helper: Ambil Nilai dari Sheet PENGATURAN
 */
function getSettingValue(key) {
  const ss = getSpreadsheet();
  const sheet = ss.getSheetByName(CONFIG.SHEET_PENGATURAN);
  if (!sheet) return null;
  const data = sheet.getDataRange().getValues();
  for (let i = 1; i < data.length; i++) {
    if (String(data[i][0]).trim() === key) {
      return String(data[i][1]).trim();
    }
  }
  return null;
}

/**
 * Helper: Simpan Nilai ke Sheet PENGATURAN
 */
function setSettingValue(key, value) {
  const ss = getSpreadsheet();
  let sheet = ss.getSheetByName(CONFIG.SHEET_PENGATURAN);
  if (!sheet) return;
  const data = sheet.getDataRange().getValues();
  for (let i = 1; i < data.length; i++) {
    if (String(data[i][0]).trim() === key) {
      sheet.getRange(i + 1, 2).setValue(value);
      sheet.getRange(i + 1, 4).setValue(new Date().toISOString());
      return;
    }
  }
  // Jika belum ada, tambahkan baris baru
  sheet.appendRow([key, value, '', new Date().toISOString()]);
}

/**
 * Catat Log Audit Administrator
 */
function logAudit(admin, action, oldValue, newValue, reason) {
  const ss = getSpreadsheet();
  const sheet = ss.getSheetByName(CONFIG.SHEET_AUDIT);
  if (sheet) {
    sheet.appendRow([new Date().toISOString(), admin, action, oldValue, newValue, reason]);
  }
}

/**
 * Periksa apakah tanggal tertentu merupakan hari libur nasional
 */
function checkIsHoliday(dateStr) {
  const ss = getSpreadsheet();
  const sheet = ss.getSheetByName(CONFIG.SHEET_LIBUR);
  if (!sheet) return false;
  const data = sheet.getDataRange().getValues();
  for (let i = 1; i < data.length; i++) {
    const rowDate = Utilities.formatDate(new Date(data[i][0]), CONFIG.TIMEZONE, 'yyyy-MM-dd');
    if (rowDate === dateStr) return true;
  }
  return false;
}

function getHolidayName(dateStr) {
  const ss = getSpreadsheet();
  const sheet = ss.getSheetByName(CONFIG.SHEET_LIBUR);
  if (!sheet) return null;
  const data = sheet.getDataRange().getValues();
  for (let i = 1; i < data.length; i++) {
    const rowDate = Utilities.formatDate(new Date(data[i][0]), CONFIG.TIMEZONE, 'yyyy-MM-dd');
    if (rowDate === dateStr) return String(data[i][1]);
  }
  return null;
}
