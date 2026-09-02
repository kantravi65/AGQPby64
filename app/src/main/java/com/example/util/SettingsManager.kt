package com.example.util

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_INSTITUTION = "user_institution"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_AVATAR_PRESET = "avatar_preset"
        private const val KEY_CUSTOM_AVATAR_URI = "custom_avatar_uri"
        private const val KEY_AVATAR_BG_COLOR = "avatar_bg_color"

        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_PIN_CODE = "pin_code"
        private const val KEY_LOCK_ON_BACKGROUND = "lock_on_background"

        private const val KEY_DEFAULT_INSTITUTE = "default_institute"
        private const val KEY_DEFAULT_PAPER_CODE = "default_paper_code"

        // Watermark Keys
        private const val KEY_WATERMARK_ENABLED = "watermark_enabled"
        private const val KEY_WATERMARK_TEXT = "watermark_text"
        private const val KEY_WATERMARK_IS_CURSIVE = "watermark_is_cursive"
        private const val KEY_WATERMARK_SIZE = "watermark_size"
        private const val KEY_WATERMARK_OPACITY = "watermark_opacity"
        private const val KEY_WATERMARK_PATTERN = "watermark_pattern"
        private const val KEY_WATERMARK_ANGLE = "watermark_angle"

        // Storage Folder Keys
        private const val KEY_STORAGE_FOLDER_PATH = "storage_folder_path"
        private const val KEY_AUTO_SAVE_TO_FOLDER = "auto_save_to_folder"

        // FTP Storage Keys
        private const val KEY_FTP_HOST = "ftp_host"
        private const val KEY_FTP_PORT = "ftp_port"
        private const val KEY_FTP_USER = "ftp_user"
        private const val KEY_FTP_PASS = "ftp_pass"
        private const val KEY_FTP_REMOTE_DIR = "ftp_remote_dir"
        private const val KEY_FTP_AUTO_SYNC = "ftp_auto_sync"
        private const val KEY_FTP_LAST_SYNC = "ftp_last_sync"

        // Data Recovery Keys
        private const val KEY_LAST_BACKUP_JSON = "last_backup_json"
        private const val KEY_LAST_BACKUP_TIME = "last_backup_time"
        private const val KEY_RECYCLE_BIN_JSON = "recycle_bin_json"

        // Google Account & Drive Keys
        private const val KEY_GOOGLE_SIGNED_IN = "google_signed_in"
        private const val KEY_GOOGLE_ACCOUNT_EMAIL = "google_account_email"
        private const val KEY_GOOGLE_ACCOUNT_NAME = "google_account_name"
        private const val KEY_GOOGLE_PHOTO_URL = "google_photo_url"
        private const val KEY_GOOGLE_DRIVE_SYNC_ENABLED = "google_drive_sync_enabled"
        private const val KEY_GOOGLE_DRIVE_LAST_SYNC = "google_drive_last_sync"
        private const val KEY_GOOGLE_DRIVE_CLOUD_JSON = "google_drive_cloud_json"
        private const val KEY_GOOGLE_WEB_CLIENT_ID = "google_web_client_id"
        const val HARDCODED_WEB_CLIENT_ID = "122400723541-257dubi3l1abck2ltpa2190gguvuuout.apps.googleusercontent.com"
    }

    // Profile Settings
    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "Prof. Alexander Wright") ?: "Prof. Alexander Wright"
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userRole: String
        get() = prefs.getString(KEY_USER_ROLE, "Head of Examination Board") ?: "Head of Examination Board"
        set(value) = prefs.edit().putString(KEY_USER_ROLE, value).apply()

    var userInstitution: String
        get() = prefs.getString(KEY_USER_INSTITUTION, "Department of Academic Science") ?: "Department of Academic Science"
        set(value) = prefs.edit().putString(KEY_USER_INSTITUTION, value).apply()

    var userEmail: String
        get() = prefs.getString(KEY_USER_EMAIL, "a.wright@university.edu") ?: "a.wright@university.edu"
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var avatarPreset: String
        get() = prefs.getString(KEY_AVATAR_PRESET, "academic_male") ?: "academic_male"
        set(value) = prefs.edit().putString(KEY_AVATAR_PRESET, value).apply()

    var customAvatarUri: String
        get() = prefs.getString(KEY_CUSTOM_AVATAR_URI, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_AVATAR_URI, value).apply()

    var avatarBgColorHex: String
        get() = prefs.getString(KEY_AVATAR_BG_COLOR, "#3F51B5") ?: "#3F51B5"
        set(value) = prefs.edit().putString(KEY_AVATAR_BG_COLOR, value).apply()

    // App Lock & Security
    var isAppLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, value).apply()

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    var pinCode: String
        get() = prefs.getString(KEY_PIN_CODE, "1234") ?: "1234"
        set(value) = prefs.edit().putString(KEY_PIN_CODE, value).apply()

    var lockOnBackground: Boolean
        get() = prefs.getBoolean(KEY_LOCK_ON_BACKGROUND, true)
        set(value) = prefs.edit().putBoolean(KEY_LOCK_ON_BACKGROUND, value).apply()

    // Default Paper Headers
    var defaultInstitute: String
        get() = prefs.getString(KEY_DEFAULT_INSTITUTE, "ACADEMIC EXAMINATION 2026") ?: "ACADEMIC EXAMINATION 2026"
        set(value) = prefs.edit().putString(KEY_DEFAULT_INSTITUTE, value).apply()

    var defaultPaperCode: String
        get() = prefs.getString(KEY_DEFAULT_PAPER_CODE, "QP-178566") ?: "QP-178566"
        set(value) = prefs.edit().putString(KEY_DEFAULT_PAPER_CODE, value).apply()

    // Permanent Watermark Settings
    var watermarkEnabled: Boolean
        get() = prefs.getBoolean(KEY_WATERMARK_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_WATERMARK_ENABLED, value).apply()

    var watermarkText: String
        get() = prefs.getString(KEY_WATERMARK_TEXT, "Ravikant") ?: "Ravikant"
        set(value) = prefs.edit().putString(KEY_WATERMARK_TEXT, value).apply()

    var watermarkIsCursive: Boolean
        get() = prefs.getBoolean(KEY_WATERMARK_IS_CURSIVE, true)
        set(value) = prefs.edit().putBoolean(KEY_WATERMARK_IS_CURSIVE, value).apply()

    var watermarkSize: Float
        get() = prefs.getFloat(KEY_WATERMARK_SIZE, 26f)
        set(value) = prefs.edit().putFloat(KEY_WATERMARK_SIZE, value).apply()

    var watermarkOpacity: Float
        get() = prefs.getFloat(KEY_WATERMARK_OPACITY, 0.20f)
        set(value) = prefs.edit().putFloat(KEY_WATERMARK_OPACITY, value).apply()

    var watermarkPattern: String
        get() = prefs.getString(KEY_WATERMARK_PATTERN, "MULTIPLE_GRID") ?: "MULTIPLE_GRID"
        set(value) = prefs.edit().putString(KEY_WATERMARK_PATTERN, value).apply()

    var watermarkAngle: Float
        get() = prefs.getFloat(KEY_WATERMARK_ANGLE, -35f)
        set(value) = prefs.edit().putFloat(KEY_WATERMARK_ANGLE, value).apply()

    // Device Storage Folder Settings
    var storageFolderPath: String
        get() = prefs.getString(KEY_STORAGE_FOLDER_PATH, "/storage/emulated/0/QuestionRepository/Data") ?: "/storage/emulated/0/QuestionRepository/Data"
        set(value) = prefs.edit().putString(KEY_STORAGE_FOLDER_PATH, value).apply()

    var autoSaveToFolder: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SAVE_TO_FOLDER, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SAVE_TO_FOLDER, value).apply()

    // FTP Server Storage Link Settings
    var ftpHost: String
        get() = prefs.getString(KEY_FTP_HOST, "") ?: ""
        set(value) = prefs.edit().putString(KEY_FTP_HOST, value).apply()

    var ftpPort: Int
        get() = prefs.getInt(KEY_FTP_PORT, 21)
        set(value) = prefs.edit().putInt(KEY_FTP_PORT, value).apply()

    var ftpUser: String
        get() = prefs.getString(KEY_FTP_USER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_FTP_USER, value).apply()

    var ftpPass: String
        get() = prefs.getString(KEY_FTP_PASS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_FTP_PASS, value).apply()

    var ftpRemoteDir: String
        get() = prefs.getString(KEY_FTP_REMOTE_DIR, "") ?: ""
        set(value) = prefs.edit().putString(KEY_FTP_REMOTE_DIR, value).apply()

    var ftpAutoSync: Boolean
        get() = prefs.getBoolean(KEY_FTP_AUTO_SYNC, false)
        set(value) = prefs.edit().putBoolean(KEY_FTP_AUTO_SYNC, value).apply()

    var ftpLastSyncTime: Long
        get() = prefs.getLong(KEY_FTP_LAST_SYNC, 0L)
        set(value) = prefs.edit().putLong(KEY_FTP_LAST_SYNC, value).apply()

    var ftpUsePassive: Boolean
        get() = prefs.getBoolean("ftp_use_passive", true)
        set(value) = prefs.edit().putBoolean("ftp_use_passive", value).apply()

    var ftpUseFtps: Boolean
        get() = prefs.getBoolean("ftp_use_ftps", false)
        set(value) = prefs.edit().putBoolean("ftp_use_ftps", value).apply()

    // Data Recovery & Recycle Bin Snapshots
    var lastBackupJson: String
        get() = prefs.getString(KEY_LAST_BACKUP_JSON, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_BACKUP_JSON, value).apply()

    var lastBackupTime: Long
        get() = prefs.getLong(KEY_LAST_BACKUP_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_BACKUP_TIME, value).apply()

    var recycleBinJson: String
        get() = prefs.getString(KEY_RECYCLE_BIN_JSON, "[]") ?: "[]"
        set(value) = prefs.edit().putString(KEY_RECYCLE_BIN_JSON, value).apply()

    // Google Sign-In & Cloud Database
    var isGoogleSignedIn: Boolean
        get() = prefs.getBoolean(KEY_GOOGLE_SIGNED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_GOOGLE_SIGNED_IN, value).apply()

    var googleAccountEmail: String
        get() = prefs.getString(KEY_GOOGLE_ACCOUNT_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GOOGLE_ACCOUNT_EMAIL, value).apply()

    var googleAccountName: String
        get() = prefs.getString(KEY_GOOGLE_ACCOUNT_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GOOGLE_ACCOUNT_NAME, value).apply()

    var googlePhotoUrl: String
        get() = prefs.getString(KEY_GOOGLE_PHOTO_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GOOGLE_PHOTO_URL, value).apply()

    var isGoogleDriveSyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_GOOGLE_DRIVE_SYNC_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_GOOGLE_DRIVE_SYNC_ENABLED, value).apply()

    var googleDriveLastSyncTime: Long
        get() = prefs.getLong(KEY_GOOGLE_DRIVE_LAST_SYNC, System.currentTimeMillis())
        set(value) = prefs.edit().putLong(KEY_GOOGLE_DRIVE_LAST_SYNC, value).apply()

    var googleDriveCloudJson: String
        get() = prefs.getString(KEY_GOOGLE_DRIVE_CLOUD_JSON, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GOOGLE_DRIVE_CLOUD_JSON, value).apply()

    var googleDriveBackupPath: String
        get() = prefs.getString("google_drive_backup_path", "My Drive/QuestionBank_Backup/backup.json") ?: "My Drive/QuestionBank_Backup/backup.json"
        set(value) = prefs.edit().putString("google_drive_backup_path", value).apply()

    var googleWebClientId: String
        get() = prefs.getString(KEY_GOOGLE_WEB_CLIENT_ID, HARDCODED_WEB_CLIENT_ID)?.ifBlank { HARDCODED_WEB_CLIENT_ID } ?: HARDCODED_WEB_CLIENT_ID
        set(value) = prefs.edit().putString(KEY_GOOGLE_WEB_CLIENT_ID, value).apply()

    var isFtpConnectionValid: Boolean
        get() = prefs.getBoolean("is_ftp_connection_valid", false)
        set(value) = prefs.edit().putBoolean("is_ftp_connection_valid", value).apply()

    var autoSyncIntervalMins: Int
        get() = prefs.getInt("auto_sync_interval_mins", 30)
        set(value) = prefs.edit().putInt("auto_sync_interval_mins", value).apply()
}
