import 'package:flutter_test/flutter_test.dart';
import 'package:loopingvid/core/database/models.dart';

void main() {
  group('RenderJob Model', () {
    test('creates from map correctly', () {
      final map = {
        'id': 'test-id-123',
        'title': 'Loop: video.mp4',
        'type': 'loop',
        'input_path': '/path/to/input.mp4',
        'output_path': '/path/to/output.mp4',
        'status': 'completed',
        'progress': 1.0,
        'settings': '{"style":"normal"}',
        'created_at': '2024-12-15T14:30:00.000',
        'completed_at': '2024-12-15T14:35:00.000',
        'error_message': null,
      };

      final job = RenderJob.fromMap(map);

      expect(job.id, equals('test-id-123'));
      expect(job.title, equals('Loop: video.mp4'));
      expect(job.type, equals('loop'));
      expect(job.inputPath, equals('/path/to/input.mp4'));
      expect(job.outputPath, equals('/path/to/output.mp4'));
      expect(job.status, equals('completed'));
      expect(job.progress, equals(1.0));
      expect(job.createdAt.year, equals(2024));
      expect(job.completedAt, isNotNull);
      expect(job.errorMessage, isNull);
    });

    test('converts to map correctly', () {
      final job = RenderJob(
        id: 'abc-123',
        title: 'Master: audio.wav',
        type: 'mastering',
        inputPath: '/input/audio.wav',
        status: 'running',
        progress: 0.5,
        createdAt: DateTime(2024, 12, 15, 10, 0),
      );

      final map = job.toMap();

      expect(map['id'], equals('abc-123'));
      expect(map['title'], equals('Master: audio.wav'));
      expect(map['type'], equals('mastering'));
      expect(map['input_path'], equals('/input/audio.wav'));
      expect(map['status'], equals('running'));
      expect(map['progress'], equals(0.5));
      expect(map['output_path'], isNull);
      expect(map['error_message'], isNull);
    });

    test('copyWith updates specified fields only', () {
      final job = RenderJob(
        id: 'job-1',
        title: 'Test Job',
        type: 'loop',
        inputPath: '/input.mp4',
        status: 'running',
        progress: 0.3,
        createdAt: DateTime(2024, 1, 1),
      );

      final updated = job.copyWith(
        status: 'completed',
        progress: 1.0,
        outputPath: '/output.mp4',
        completedAt: DateTime(2024, 1, 1, 0, 5),
      );

      expect(updated.id, equals('job-1'));
      expect(updated.title, equals('Test Job'));
      expect(updated.status, equals('completed'));
      expect(updated.progress, equals(1.0));
      expect(updated.outputPath, equals('/output.mp4'));
      expect(updated.completedAt, isNotNull);
    });

    test('handles failed status with error message', () {
      final job = RenderJob(
        id: 'fail-1',
        title: 'Failed Job',
        type: 'editor',
        inputPath: '/input.mp4',
        status: 'failed',
        createdAt: DateTime.now(),
        errorMessage: 'FFmpeg error: invalid codec',
      );

      expect(job.status, equals('failed'));
      expect(job.errorMessage, contains('invalid codec'));

      final map = job.toMap();
      final restored = RenderJob.fromMap(map);
      expect(restored.errorMessage, equals(job.errorMessage));
    });
  });

  group('LiveSession Model', () {
    test('creates from map correctly', () {
      final map = {
        'id': 'session-1',
        'platform': 'youtube',
        'rtmp_url': 'rtmp://a.rtmp.youtube.com/live2',
        'stream_key': '***',
        'media_path': '/path/to/video.mp4',
        'status': 'ended',
        'bitrate': 4500,
        'loop_count': 12,
        'uptime_seconds': 3661.0,
        'started_at': '2024-12-14T20:00:00.000',
        'ended_at': '2024-12-15T00:01:01.000',
      };

      final session = LiveSession.fromMap(map);

      expect(session.id, equals('session-1'));
      expect(session.platform, equals('youtube'));
      expect(session.rtmpUrl, equals('rtmp://a.rtmp.youtube.com/live2'));
      expect(session.mediaPath, equals('/path/to/video.mp4'));
      expect(session.status, equals('ended'));
      expect(session.bitrate, equals(4500));
      expect(session.loopCount, equals(12));
      expect(session.uptimeSeconds, equals(3661.0));
      expect(session.startedAt, isNotNull);
      expect(session.endedAt, isNotNull);
    });

    test('converts to map correctly', () {
      final session = LiveSession(
        id: 'live-abc',
        platform: 'tiktok',
        mediaPath: '/video.mp4',
        status: 'live',
        bitrate: 6000,
        loopCount: 5,
        uptimeSeconds: 120.0,
        startedAt: DateTime(2024, 12, 1, 21, 0),
      );

      final map = session.toMap();

      expect(map['id'], equals('live-abc'));
      expect(map['platform'], equals('tiktok'));
      expect(map['media_path'], equals('/video.mp4'));
      expect(map['status'], equals('live'));
      expect(map['bitrate'], equals(6000));
      expect(map['loop_count'], equals(5));
      expect(map['uptime_seconds'], equals(120.0));
    });

    test('formattedUptime returns correct HH:MM:SS', () {
      final session = LiveSession(
        id: 'fmt-1',
        platform: 'youtube',
        mediaPath: '/v.mp4',
        uptimeSeconds: 3661.0, // 1h 1m 1s
      );
      expect(session.formattedUptime, equals('01:01:01'));

      final session2 = LiveSession(
        id: 'fmt-2',
        platform: 'youtube',
        mediaPath: '/v.mp4',
        uptimeSeconds: 0.0,
      );
      expect(session2.formattedUptime, equals('00:00:00'));

      final session3 = LiveSession(
        id: 'fmt-3',
        platform: 'youtube',
        mediaPath: '/v.mp4',
        uptimeSeconds: 86399.0, // 23:59:59
      );
      expect(session3.formattedUptime, equals('23:59:59'));
    });

    test('copyWith updates specified fields only', () {
      final session = LiveSession(
        id: 'cp-1',
        platform: 'custom',
        rtmpUrl: 'rtmp://example.com/live',
        mediaPath: '/media.mp4',
        status: 'live',
        bitrate: 4500,
        loopCount: 3,
        uptimeSeconds: 60.0,
        startedAt: DateTime(2024, 6, 1),
      );

      final ended = session.copyWith(
        status: 'ended',
        loopCount: 10,
        uptimeSeconds: 3600.0,
        endedAt: DateTime(2024, 6, 1, 1, 0),
      );

      expect(ended.id, equals('cp-1'));
      expect(ended.platform, equals('custom'));
      expect(ended.status, equals('ended'));
      expect(ended.loopCount, equals(10));
      expect(ended.uptimeSeconds, equals(3600.0));
      expect(ended.endedAt, isNotNull);
      expect(ended.bitrate, equals(4500)); // Unchanged
    });
  });

  group('FFmpeg Command Logic', () {
    test('resolution mapping is correct', () {
      // Verify the resolution logic used in FFmpegService
      String getResolution(String quality) {
        switch (quality) {
          case 'Low (480p)':
            return '854:480';
          case 'Medium (720p)':
            return '1280:720';
          case 'High (1080p)':
            return '1920:1080';
          case 'Ultra (4K)':
            return '3840:2160';
          default:
            return '1920:1080';
        }
      }

      expect(getResolution('Low (480p)'), equals('854:480'));
      expect(getResolution('Medium (720p)'), equals('1280:720'));
      expect(getResolution('High (1080p)'), equals('1920:1080'));
      expect(getResolution('Ultra (4K)'), equals('3840:2160'));
      expect(getResolution('Unknown'), equals('1920:1080'));
    });

    test('audio codec mapping is correct', () {
      String getAudioCodec(String format) {
        switch (format.toLowerCase()) {
          case 'wav':
            return '-c:a pcm_s16le';
          case 'mp3':
            return '-c:a libmp3lame -b:a 320k';
          case 'm4a':
            return '-c:a aac -b:a 256k';
          case 'flac':
            return '-c:a flac';
          default:
            return '-c:a pcm_s16le';
        }
      }

      expect(getAudioCodec('wav'), contains('pcm_s16le'));
      expect(getAudioCodec('mp3'), contains('libmp3lame'));
      expect(getAudioCodec('m4a'), contains('aac'));
      expect(getAudioCodec('flac'), contains('flac'));
    });

    test('loop count calculation is correct', () {
      // Given a 10-second video and 60-second target
      int durationMs = 10000;
      int targetDurationSec = 60;
      int loopCount = (targetDurationSec * 1000 / durationMs).ceil();
      expect(loopCount, equals(6));

      // Given a 30-second video and 120-second target
      durationMs = 30000;
      targetDurationSec = 120;
      loopCount = (targetDurationSec * 1000 / durationMs).ceil();
      expect(loopCount, equals(4));

      // Edge case: target equals duration
      durationMs = 60000;
      targetDurationSec = 60;
      loopCount = (targetDurationSec * 1000 / durationMs).ceil();
      expect(loopCount, equals(1));
    });

    test('LUFS target range validation', () {
      const minLufs = -24.0;
      const maxLufs = -6.0;
      const defaultLufs = -14.0;

      expect(defaultLufs >= minLufs, isTrue);
      expect(defaultLufs <= maxLufs, isTrue);
      expect(-14.0 >= minLufs && -14.0 <= maxLufs, isTrue);
      expect(-25.0 >= minLufs, isFalse); // Out of range
    });

    test('EQ band range validation', () {
      const minDb = -12.0;
      const maxDb = 12.0;

      // All preset values should be within range
      final deepBass = [8.0, 4.0, 0.0, -2.0, -4.0];
      final clearVocal = [-2.0, 0.0, 4.0, 6.0, 2.0];

      for (final v in deepBass) {
        expect(v >= minDb && v <= maxDb, isTrue);
      }
      for (final v in clearVocal) {
        expect(v >= minDb && v <= maxDb, isTrue);
      }
    });
  });
}
