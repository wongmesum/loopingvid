import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';

class DatabaseHelper {
  static final DatabaseHelper _instance = DatabaseHelper._internal();
  factory DatabaseHelper() => _instance;
  DatabaseHelper._internal();

  static Database? _database;

  Future<Database> get database async {
    if (_database != null) return _database!;
    _database = await _initDatabase();
    return _database!;
  }

  Future<Database> _initDatabase() async {
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, 'loopingvid.db');

    return await openDatabase(
      path,
      version: 1,
      onCreate: _onCreate,
    );
  }

  Future<void> _onCreate(Database db, int version) async {
    await db.execute('''
      CREATE TABLE render_jobs (
        id TEXT PRIMARY KEY,
        title TEXT NOT NULL,
        type TEXT NOT NULL,
        input_path TEXT NOT NULL,
        output_path TEXT,
        status TEXT NOT NULL DEFAULT 'pending',
        progress REAL NOT NULL DEFAULT 0.0,
        settings TEXT,
        created_at TEXT NOT NULL,
        completed_at TEXT,
        error_message TEXT
      )
    ''');

    await db.execute('''
      CREATE TABLE live_sessions (
        id TEXT PRIMARY KEY,
        platform TEXT NOT NULL,
        rtmp_url TEXT,
        stream_key TEXT,
        media_path TEXT NOT NULL,
        status TEXT NOT NULL DEFAULT 'idle',
        bitrate INTEGER NOT NULL DEFAULT 4500,
        loop_count INTEGER NOT NULL DEFAULT 0,
        uptime_seconds REAL NOT NULL DEFAULT 0.0,
        started_at TEXT,
        ended_at TEXT
      )
    ''');

    await db.execute('''
      CREATE TABLE app_settings (
        key TEXT PRIMARY KEY,
        value TEXT NOT NULL
      )
    ''');
  }

  // === RENDER JOBS ===

  Future<int> insertRenderJob(Map<String, dynamic> job) async {
    final db = await database;
    return await db.insert('render_jobs', job,
        conflictAlgorithm: ConflictAlgorithm.replace);
  }

  Future<int> updateRenderJob(String id, Map<String, dynamic> values) async {
    final db = await database;
    return await db.update('render_jobs', values,
        where: 'id = ?', whereArgs: [id]);
  }

  Future<int> deleteRenderJob(String id) async {
    final db = await database;
    return await db.delete('render_jobs', where: 'id = ?', whereArgs: [id]);
  }

  Future<List<Map<String, dynamic>>> getRenderJobs() async {
    final db = await database;
    return await db.query('render_jobs', orderBy: 'created_at DESC');
  }

  Future<Map<String, dynamic>?> getRenderJobById(String id) async {
    final db = await database;
    final results =
        await db.query('render_jobs', where: 'id = ?', whereArgs: [id]);
    return results.isEmpty ? null : results.first;
  }

  // === LIVE SESSIONS ===

  Future<int> insertLiveSession(Map<String, dynamic> session) async {
    final db = await database;
    return await db.insert('live_sessions', session,
        conflictAlgorithm: ConflictAlgorithm.replace);
  }

  Future<int> updateLiveSession(
      String id, Map<String, dynamic> values) async {
    final db = await database;
    return await db.update('live_sessions', values,
        where: 'id = ?', whereArgs: [id]);
  }

  Future<int> deleteLiveSession(String id) async {
    final db = await database;
    return await db.delete('live_sessions', where: 'id = ?', whereArgs: [id]);
  }

  Future<List<Map<String, dynamic>>> getLiveSessions() async {
    final db = await database;
    return await db.query('live_sessions', orderBy: 'started_at DESC');
  }

  // === APP SETTINGS ===

  Future<void> setSetting(String key, String value) async {
    final db = await database;
    await db.insert(
      'app_settings',
      {'key': key, 'value': value},
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  Future<String?> getSetting(String key) async {
    final db = await database;
    final results = await db.query('app_settings',
        where: 'key = ?', whereArgs: [key]);
    return results.isEmpty ? null : results.first['value'] as String;
  }

  Future<Map<String, String>> getAllSettings() async {
    final db = await database;
    final results = await db.query('app_settings');
    return {
      for (final row in results) row['key'] as String: row['value'] as String
    };
  }

  // === UTILITY ===

  Future<void> clearAllData() async {
    final db = await database;
    await db.delete('render_jobs');
    await db.delete('live_sessions');
    await db.delete('app_settings');
  }

  Future<void> close() async {
    final db = await database;
    await db.close();
    _database = null;
  }
}
