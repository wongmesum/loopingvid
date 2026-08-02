import 'dart:async';
import 'dart:io';
import 'package:ffmpeg_kit_flutter_full_gpl/ffmpeg_kit.dart';
import 'package:ffmpeg_kit_flutter_full_gpl/ffmpeg_kit_config.dart';
import 'package:ffmpeg_kit_flutter_full_gpl/ffprobe_kit.dart';
import 'package:ffmpeg_kit_flutter_full_gpl/return_code.dart';
import 'package:ffmpeg_kit_flutter_full_gpl/statistics.dart';
import 'package:path_provider/path_provider.dart';
import 'package:path/path.dart' as p;

/// Callback for FFmpeg progress updates (0.0 to 1.0)
typedef ProgressCallback = void Function(double progress);

/// Service for FFmpeg media processing operations
class FFmpegService {
  static final FFmpegService _instance = FFmpegService._internal();
  factory FFmpegService() => _instance;
  FFmpegService._internal();

  /// Get media duration in milliseconds using ffprobe
  Future<int> getMediaDuration(String inputPath) async {
    final session = await FFprobeKit.getMediaInformation(inputPath);
    final info = session.getMediaInformation();
    if (info == null) return 0;
    final durationStr = info.getDuration();
    if (durationStr == null) return 0;
    return (double.parse(durationStr) * 1000).toInt();
  }

  /// Get output directory, creating it if needed
  Future<String> getOutputDir() async {
    final dir = await getApplicationDocumentsDirectory();
    final outputDir = Directory(p.join(dir.path, 'LoopingVid', 'output'));
    if (!await outputDir.exists()) {
      await outputDir.create(recursive: true);
    }
    return outputDir.path;
  }

  /// Generate unique output filename
  String generateOutputFilename(String prefix, String extension) {
    final timestamp = DateTime.now().millisecondsSinceEpoch;
    return '${prefix}_$timestamp.$extension';
  }

  // =========================================
  // LOOP OPERATIONS
  // =========================================

  /// Create a normal loop (repeat video N times to reach target duration)
  Future<String?> createNormalLoop({
    required String inputPath,
    required int targetDurationSec,
    required bool muteAudio,
    required String quality,
    ProgressCallback? onProgress,
  }) async {
    final duration = await getMediaDuration(inputPath);
    if (duration == 0) return null;

    final outputDir = await getOutputDir();
    final outputFile = p.join(
        outputDir, generateOutputFilename('loop_normal', 'mp4'));

    final durationMs = duration;
    final loopCount = (targetDurationSec * 1000 / durationMs).ceil();

    final resolution = _getResolution(quality);
    final audioFlag = muteAudio ? '-an' : '-c:a aac -b:a 128k';

    final command = '-stream_loop $loopCount -i "$inputPath" '
        '-t $targetDurationSec '
        '-vf "scale=$resolution" '
        '-c:v libx264 -preset medium -crf 23 '
        '$audioFlag '
        '-y "$outputFile"';

    final success = await _executeWithProgress(
        command, targetDurationSec * 1000, onProgress);
    return success ? outputFile : null;
  }

  /// Create a crossfade loop
  Future<String?> createCrossfadeLoop({
    required String inputPath,
    required int targetDurationSec,
    required double crossfadeDuration,
    required bool muteAudio,
    required String quality,
    ProgressCallback? onProgress,
  }) async {
    final duration = await getMediaDuration(inputPath);
    if (duration == 0) return null;

    final outputDir = await getOutputDir();
    final outputFile = p.join(
        outputDir, generateOutputFilename('loop_crossfade', 'mp4'));

    final durationSec = duration / 1000.0;
    final resolution = _getResolution(quality);
    final audioFlag = muteAudio ? '-an' : '-c:a aac -b:a 128k';
    final cf = crossfadeDuration.toStringAsFixed(1);

    // Use xfade filter for crossfade between two copies of the input
    final offset = (durationSec - crossfadeDuration).toStringAsFixed(2);
    final command = '-i "$inputPath" -i "$inputPath" '
        '-filter_complex "'
        '[0:v][1:v]xfade=transition=fade:duration=$cf:offset=$offset,'
        'scale=$resolution[v]" '
        '-map "[v]" $audioFlag '
        '-t $targetDurationSec '
        '-c:v libx264 -preset medium -crf 23 '
        '-y "$outputFile"';

    final success = await _executeWithProgress(
        command, targetDurationSec * 1000, onProgress);
    return success ? outputFile : null;
  }

  /// Create a ping-pong loop (forward + reverse)
  Future<String?> createPingPongLoop({
    required String inputPath,
    required int targetDurationSec,
    required bool muteAudio,
    required String quality,
    ProgressCallback? onProgress,
  }) async {
    final duration = await getMediaDuration(inputPath);
    if (duration == 0) return null;

    final outputDir = await getOutputDir();
    final outputFile = p.join(
        outputDir, generateOutputFilename('loop_pingpong', 'mp4'));

    final resolution = _getResolution(quality);
    final audioFlag = muteAudio ? '-an' : '-c:a aac -b:a 128k';

    // Create forward + reverse concatenation
    final command = '-i "$inputPath" '
        '-filter_complex "'
        '[0:v]split[fwd][rev];'
        '[rev]reverse[reversed];'
        '[fwd][reversed]concat=n=2:v=1:a=0,'
        'loop=loop=-1:size=${duration ~/ 33}:start=0,'
        'trim=duration=$targetDurationSec,'
        'scale=$resolution[v]" '
        '-map "[v]" $audioFlag '
        '-c:v libx264 -preset medium -crf 23 '
        '-y "$outputFile"';

    final success = await _executeWithProgress(
        command, targetDurationSec * 1000, onProgress);
    return success ? outputFile : null;
  }

  // =========================================
  // AUDIO MASTERING OPERATIONS
  // =========================================

  /// Apply audio mastering with EQ, compression, limiting, and normalization
  Future<String?> masterAudio({
    required String inputPath,
    required List<double> eqBands, // [60Hz, 250Hz, 1kHz, 4kHz, 12kHz] in dB
    required double compressorThreshold,
    required double compressorRatio,
    required double targetLufs,
    required bool noiseReduction,
    required String outputFormat, // 'wav', 'mp3', 'm4a', 'flac'
    ProgressCallback? onProgress,
  }) async {
    final duration = await getMediaDuration(inputPath);
    if (duration == 0) return null;

    final outputDir = await getOutputDir();
    final outputFile = p.join(
        outputDir, generateOutputFilename('mastered', outputFormat));

    // Build EQ filter chain
    final eq = 'superequalizer='
        '1b=${eqBands[0]}:'
        '2b=${eqBands[1]}:'
        '3b=${eqBands[2]}:'
        '4b=${eqBands[3]}:'
        '5b=${eqBands[4]}';

    // Build compressor
    final compressor = 'acompressor='
        'threshold=${compressorThreshold}dB:'
        'ratio=$compressorRatio:'
        'attack=20:release=250';

    // Build limiter
    const limiter = 'alimiter=limit=0.95:attack=5:release=50';

    // Build loudness normalization
    final loudnorm = 'loudnorm=I=$targetLufs:TP=-1.5:LRA=11';

    // Noise reduction filter
    final nrFilter = noiseReduction ? ',anlmdn=s=7:p=0.002:r=0.002' : '';

    // Audio codec based on format
    final codec = _getAudioCodec(outputFormat);

    final command = '-i "$inputPath" '
        '-af "$eq,$compressor,$limiter,$loudnorm$nrFilter" '
        '$codec '
        '-y "$outputFile"';

    final success =
        await _executeWithProgress(command, duration, onProgress);
    return success ? outputFile : null;
  }

  // =========================================
  // VIDEO EDITOR OPERATIONS
  // =========================================

  /// Compose video with audio track, text overlays, and optional spectrum
  Future<String?> composeVideo({
    required String videoPath,
    String? audioPath,
    String? titleText,
    double titleFontSize = 24,
    String? watermarkText,
    bool showSpectrum = false,
    String spectrumStyle = 'Bars',
    required String quality,
    ProgressCallback? onProgress,
  }) async {
    final duration = await getMediaDuration(videoPath);
    if (duration == 0) return null;

    final outputDir = await getOutputDir();
    final outputFile = p.join(
        outputDir, generateOutputFilename('composed', 'mp4'));

    final resolution = _getResolution(quality);
    final filters = <String>[];

    // Scale filter
    filters.add('scale=$resolution');

    // Title text overlay
    if (titleText != null && titleText.isNotEmpty) {
      final escaped = titleText.replaceAll("'", "\\'");
      filters.add(
          "drawtext=text='$escaped':fontsize=$titleFontSize:"
          "fontcolor=white:x=20:y=20:"
          "shadowcolor=black:shadowx=2:shadowy=2");
    }

    // Watermark text overlay
    if (watermarkText != null && watermarkText.isNotEmpty) {
      final escaped = watermarkText.replaceAll("'", "\\'");
      filters.add(
          "drawtext=text='$escaped':fontsize=12:"
          "fontcolor=white@0.5:x=w-tw-10:y=h-th-10");
    }

    // Audio spectrum overlay
    if (showSpectrum && audioPath != null) {
      final specFilter = _getSpectrumFilter(spectrumStyle);
      filters.add(specFilter);
    }

    final vf = filters.join(',');
    final audioInput = audioPath != null ? '-i "$audioPath"' : '';
    final audioMap = audioPath != null
        ? '-map 0:v -map 1:a -c:a aac -b:a 192k -shortest'
        : '-c:a copy';

    final command = '-i "$videoPath" $audioInput '
        '-vf "$vf" '
        '-c:v libx264 -preset medium -crf 23 '
        '$audioMap '
        '-y "$outputFile"';

    final success =
        await _executeWithProgress(command, duration, onProgress);
    return success ? outputFile : null;
  }

  // =========================================
  // LIVE STREAMING OPERATIONS
  // =========================================

  /// Start infinite loop RTMP stream
  Future<void> startRtmpStream({
    required String mediaPath,
    required String rtmpUrl,
    required String streamKey,
    required int bitrate,
    void Function(Statistics)? onStatistics,
    void Function()? onComplete,
    void Function(String error)? onError,
  }) async {
    final fullUrl = '$rtmpUrl/$streamKey';

    final command = '-re -stream_loop -1 -i "$mediaPath" '
        '-c:v libx264 -preset veryfast -b:v ${bitrate}k '
        '-maxrate ${bitrate}k -bufsize ${bitrate * 2}k '
        '-c:a aac -b:a 128k -ar 44100 '
        '-f flv "$fullUrl"';

    FFmpegKitConfig.enableStatisticsCallback((statistics) {
      onStatistics?.call(statistics);
    });

    final session = await FFmpegKit.executeAsync(command, (session) async {
      final returnCode = await session.getReturnCode();
      if (ReturnCode.isSuccess(returnCode)) {
        onComplete?.call();
      } else {
        final logs = await session.getLogsAsString();
        onError?.call(logs.isEmpty ? 'Unknown streaming error' : logs);
      }
    });

    // Store session ID for later cancellation
    _activeStreamSessionId = session.getSessionId();
  }

  int? _activeStreamSessionId;

  /// Stop the active RTMP stream
  Future<void> stopRtmpStream() async {
    if (_activeStreamSessionId != null) {
      await FFmpegKit.cancel(_activeStreamSessionId!);
      _activeStreamSessionId = null;
    } else {
      await FFmpegKit.cancel();
    }
  }

  /// Check if a stream is currently active
  bool get isStreaming => _activeStreamSessionId != null;

  // =========================================
  // CANCEL OPERATIONS
  // =========================================

  /// Cancel all running FFmpeg sessions
  Future<void> cancelAll() async {
    await FFmpegKit.cancel();
    _activeStreamSessionId = null;
  }

  // =========================================
  // PRIVATE HELPERS
  // =========================================

  /// Execute FFmpeg command with progress tracking
  Future<bool> _executeWithProgress(
    String command,
    int totalDurationMs,
    ProgressCallback? onProgress,
  ) async {
    final completer = Completer<bool>();

    FFmpegKitConfig.enableStatisticsCallback((statistics) {
      if (totalDurationMs > 0 && onProgress != null) {
        final time = statistics.getTime();
        final progress = (time / totalDurationMs).clamp(0.0, 1.0);
        onProgress(progress);
      }
    });

    await FFmpegKit.executeAsync(command, (session) async {
      final returnCode = await session.getReturnCode();
      completer.complete(ReturnCode.isSuccess(returnCode));
    });

    return completer.future;
  }

  /// Get video resolution string from quality preset name
  String _getResolution(String quality) {
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

  /// Get audio codec flags for output format
  String _getAudioCodec(String format) {
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

  /// Get spectrum filter string based on style
  String _getSpectrumFilter(String style) {
    switch (style) {
      case 'Wave':
        return 'showwaves=s=1920x200:mode=line:rate=25';
      case 'Circle':
        return 'avectorscope=s=300x300:zoom=1.5:rc=40:gc=160:bc=240';
      case 'Bars':
      default:
        return 'showfreqs=s=1920x200:mode=bar:fscale=log';
    }
  }
}
