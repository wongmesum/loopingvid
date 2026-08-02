import 'package:shared_preferences/shared_preferences.dart';

/// Service for managing app settings with SharedPreferences
class SettingsService {
  static final SettingsService _instance = SettingsService._internal();
  factory SettingsService() => _instance;
  SettingsService._internal();

  SharedPreferences? _prefs;

  Future<SharedPreferences> get prefs async {
    _prefs ??= await SharedPreferences.getInstance();
    return _prefs!;
  }

  // === Output Directory ===

  static const _keyOutputDir = 'output_directory';
  static const _defaultOutputDir = '/storage/emulated/0/LoopingVid/output';

  Future<String> getOutputDirectory() async {
    final p = await prefs;
    return p.getString(_keyOutputDir) ?? _defaultOutputDir;
  }

  Future<void> setOutputDirectory(String path) async {
    final p = await prefs;
    await p.setString(_keyOutputDir, path);
  }

  // === High Quality Preview ===

  static const _keyHqPreview = 'high_quality_preview';

  Future<bool> getHighQualityPreview() async {
    final p = await prefs;
    return p.getBool(_keyHqPreview) ?? true;
  }

  Future<void> setHighQualityPreview(bool value) async {
    final p = await prefs;
    await p.setBool(_keyHqPreview, value);
  }

  // === Firestore Sync ===

  static const _keyFirestoreSync = 'firestore_sync';

  Future<bool> getFirestoreSync() async {
    final p = await prefs;
    return p.getBool(_keyFirestoreSync) ?? false;
  }

  Future<void> setFirestoreSync(bool value) async {
    final p = await prefs;
    await p.setBool(_keyFirestoreSync, value);
  }

  // === Gemini API Key ===

  static const _keyGeminiApiKey = 'gemini_api_key';

  Future<String> getGeminiApiKey() async {
    final p = await prefs;
    return p.getString(_keyGeminiApiKey) ?? '';
  }

  Future<void> setGeminiApiKey(String key) async {
    final p = await prefs;
    await p.setString(_keyGeminiApiKey, key);
  }

  // === Stream Keys (encrypted in production, plain for now) ===

  static const _keyYoutubeStreamKey = 'youtube_stream_key';
  static const _keyTiktokStreamKey = 'tiktok_stream_key';
  static const _keyCustomRtmpUrl = 'custom_rtmp_url';
  static const _keyCustomStreamKey = 'custom_stream_key';

  Future<String> getYoutubeStreamKey() async {
    final p = await prefs;
    return p.getString(_keyYoutubeStreamKey) ?? '';
  }

  Future<void> setYoutubeStreamKey(String key) async {
    final p = await prefs;
    await p.setString(_keyYoutubeStreamKey, key);
  }

  Future<String> getTiktokStreamKey() async {
    final p = await prefs;
    return p.getString(_keyTiktokStreamKey) ?? '';
  }

  Future<void> setTiktokStreamKey(String key) async {
    final p = await prefs;
    await p.setString(_keyTiktokStreamKey, key);
  }

  Future<String> getCustomRtmpUrl() async {
    final p = await prefs;
    return p.getString(_keyCustomRtmpUrl) ?? '';
  }

  Future<void> setCustomRtmpUrl(String url) async {
    final p = await prefs;
    await p.setString(_keyCustomRtmpUrl, url);
  }

  Future<String> getCustomStreamKey() async {
    final p = await prefs;
    return p.getString(_keyCustomStreamKey) ?? '';
  }

  Future<void> setCustomStreamKey(String key) async {
    final p = await prefs;
    await p.setString(_keyCustomStreamKey, key);
  }

  // === Default Bitrate ===

  static const _keyDefaultBitrate = 'default_bitrate';

  Future<int> getDefaultBitrate() async {
    final p = await prefs;
    return p.getInt(_keyDefaultBitrate) ?? 4500;
  }

  Future<void> setDefaultBitrate(int bitrate) async {
    final p = await prefs;
    await p.setInt(_keyDefaultBitrate, bitrate);
  }

  // === Clear All ===

  Future<void> clearAll() async {
    final p = await prefs;
    await p.clear();
  }
}
