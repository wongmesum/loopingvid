/// Represents a render job (loop, mastering, editor export)
class RenderJob {
  final String id;
  final String title;
  final String type; // 'loop', 'mastering', 'editor'
  final String inputPath;
  final String? outputPath;
  final String status; // 'pending', 'running', 'completed', 'failed', 'cancelled'
  final double progress;
  final String? settings; // JSON encoded settings
  final DateTime createdAt;
  final DateTime? completedAt;
  final String? errorMessage;

  RenderJob({
    required this.id,
    required this.title,
    required this.type,
    required this.inputPath,
    this.outputPath,
    this.status = 'pending',
    this.progress = 0.0,
    this.settings,
    required this.createdAt,
    this.completedAt,
    this.errorMessage,
  });

  factory RenderJob.fromMap(Map<String, dynamic> map) {
    return RenderJob(
      id: map['id'] as String,
      title: map['title'] as String,
      type: map['type'] as String,
      inputPath: map['input_path'] as String,
      outputPath: map['output_path'] as String?,
      status: map['status'] as String,
      progress: (map['progress'] as num).toDouble(),
      settings: map['settings'] as String?,
      createdAt: DateTime.parse(map['created_at'] as String),
      completedAt: map['completed_at'] != null
          ? DateTime.parse(map['completed_at'] as String)
          : null,
      errorMessage: map['error_message'] as String?,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'title': title,
      'type': type,
      'input_path': inputPath,
      'output_path': outputPath,
      'status': status,
      'progress': progress,
      'settings': settings,
      'created_at': createdAt.toIso8601String(),
      'completed_at': completedAt?.toIso8601String(),
      'error_message': errorMessage,
    };
  }

  RenderJob copyWith({
    String? status,
    double? progress,
    String? outputPath,
    DateTime? completedAt,
    String? errorMessage,
  }) {
    return RenderJob(
      id: id,
      title: title,
      type: type,
      inputPath: inputPath,
      outputPath: outputPath ?? this.outputPath,
      status: status ?? this.status,
      progress: progress ?? this.progress,
      settings: settings,
      createdAt: createdAt,
      completedAt: completedAt ?? this.completedAt,
      errorMessage: errorMessage ?? this.errorMessage,
    );
  }
}

/// Represents a live streaming session
class LiveSession {
  final String id;
  final String platform; // 'youtube', 'tiktok', 'custom'
  final String? rtmpUrl;
  final String? streamKey;
  final String mediaPath;
  final String status; // 'idle', 'connecting', 'live', 'ended', 'error'
  final int bitrate;
  final int loopCount;
  final double uptimeSeconds;
  final DateTime? startedAt;
  final DateTime? endedAt;

  LiveSession({
    required this.id,
    required this.platform,
    this.rtmpUrl,
    this.streamKey,
    required this.mediaPath,
    this.status = 'idle',
    this.bitrate = 4500,
    this.loopCount = 0,
    this.uptimeSeconds = 0.0,
    this.startedAt,
    this.endedAt,
  });

  factory LiveSession.fromMap(Map<String, dynamic> map) {
    return LiveSession(
      id: map['id'] as String,
      platform: map['platform'] as String,
      rtmpUrl: map['rtmp_url'] as String?,
      streamKey: map['stream_key'] as String?,
      mediaPath: map['media_path'] as String,
      status: map['status'] as String,
      bitrate: map['bitrate'] as int,
      loopCount: map['loop_count'] as int,
      uptimeSeconds: (map['uptime_seconds'] as num).toDouble(),
      startedAt: map['started_at'] != null
          ? DateTime.parse(map['started_at'] as String)
          : null,
      endedAt: map['ended_at'] != null
          ? DateTime.parse(map['ended_at'] as String)
          : null,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'platform': platform,
      'rtmp_url': rtmpUrl,
      'stream_key': streamKey,
      'media_path': mediaPath,
      'status': status,
      'bitrate': bitrate,
      'loop_count': loopCount,
      'uptime_seconds': uptimeSeconds,
      'started_at': startedAt?.toIso8601String(),
      'ended_at': endedAt?.toIso8601String(),
    };
  }

  LiveSession copyWith({
    String? status,
    int? loopCount,
    double? uptimeSeconds,
    DateTime? endedAt,
  }) {
    return LiveSession(
      id: id,
      platform: platform,
      rtmpUrl: rtmpUrl,
      streamKey: streamKey,
      mediaPath: mediaPath,
      status: status ?? this.status,
      bitrate: bitrate,
      loopCount: loopCount ?? this.loopCount,
      uptimeSeconds: uptimeSeconds ?? this.uptimeSeconds,
      startedAt: startedAt,
      endedAt: endedAt ?? this.endedAt,
    );
  }

  String get formattedUptime {
    final hours = (uptimeSeconds / 3600).floor();
    final minutes = ((uptimeSeconds % 3600) / 60).floor();
    final seconds = (uptimeSeconds % 60).floor();
    return '${hours.toString().padLeft(2, '0')}:'
        '${minutes.toString().padLeft(2, '0')}:'
        '${seconds.toString().padLeft(2, '0')}';
  }
}
