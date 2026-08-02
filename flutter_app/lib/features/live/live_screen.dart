import 'dart:async';
import 'package:flutter/material.dart';
import 'package:uuid/uuid.dart';
import 'package:ffmpeg_kit_flutter_full_gpl/statistics.dart';
import 'package:loopingvid/core/services/file_picker_service.dart';
import 'package:loopingvid/core/services/ffmpeg_service.dart';
import 'package:loopingvid/core/services/settings_service.dart';
import 'package:loopingvid/core/database/database_helper.dart';
import 'package:loopingvid/core/database/models.dart';
import 'package:loopingvid/core/utils/responsive.dart';

enum StreamPlatform { youtube, tiktok, customRtmp }

enum StreamStatus { idle, connecting, live, error }

class LiveScreen extends StatefulWidget {
  const LiveScreen({super.key});

  @override
  State<LiveScreen> createState() => _LiveScreenState();
}

class _LiveScreenState extends State<LiveScreen> {
  final _filePicker = FilePickerService();
  final _ffmpeg = FFmpegService();
  final _settings = SettingsService();
  final _db = DatabaseHelper();

  StreamPlatform _platform = StreamPlatform.youtube;
  StreamStatus _status = StreamStatus.idle;
  final TextEditingController _rtmpUrlController = TextEditingController();
  final TextEditingController _streamKeyController = TextEditingController();
  String? _selectedMediaPath;
  String? _mediaFileName;
  int _loopCount = 0;
  int _bitrate = 4500;
  double _uptimeSeconds = 0;
  double _currentBitrateKbps = 0;
  String? _errorMessage;
  String? _activeSessionId;

  Timer? _uptimeTimer;

  @override
  void initState() {
    super.initState();
    _loadSavedKeys();
  }

  @override
  void dispose() {
    _rtmpUrlController.dispose();
    _streamKeyController.dispose();
    _uptimeTimer?.cancel();
    super.dispose();
  }

  Future<void> _loadSavedKeys() async {
    final ytKey = await _settings.getYoutubeStreamKey();
    final ttKey = await _settings.getTiktokStreamKey();
    final customUrl = await _settings.getCustomRtmpUrl();
    final customKey = await _settings.getCustomStreamKey();
    final bitrate = await _settings.getDefaultBitrate();

    setState(() {
      _bitrate = bitrate;
    });

    // Pre-fill based on current platform
    _updateKeyFields(ytKey, ttKey, customUrl, customKey);
  }

  void _updateKeyFields(
      String ytKey, String ttKey, String customUrl, String customKey) {
    switch (_platform) {
      case StreamPlatform.youtube:
        _streamKeyController.text = ytKey;
        _rtmpUrlController.text = 'rtmp://a.rtmp.youtube.com/live2';
        break;
      case StreamPlatform.tiktok:
        _streamKeyController.text = ttKey;
        _rtmpUrlController.text = '';
        break;
      case StreamPlatform.customRtmp:
        _streamKeyController.text = customKey;
        _rtmpUrlController.text = customUrl;
        break;
    }
  }

  @override
  Widget build(BuildContext context) {
    final r = context.responsive;
    return SingleChildScrollView(
      padding: r.screenPadding,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (_status == StreamStatus.live) _buildLiveBanner(),
          if (_status == StreamStatus.error) _buildErrorBanner(),
          SizedBox(height: r.sectionGap),
          _buildPlatformCard(),
          SizedBox(height: r.sectionGap),
          _buildStreamConfigCard(),
          SizedBox(height: r.sectionGap),
          _buildMediaSourceCard(),
          SizedBox(height: r.sectionGap),
          _buildTelemetryCard(),
          const SizedBox(height: 24),
          _buildActionButton(),
        ],
      ),
    );
  }

  Widget _buildLiveBanner() {
    final r = context.responsive;
    return Card(
      color: Colors.red.shade50,
      child: Padding(
        padding: r.cardContentPadding,
        child: Row(
          children: [
            Container(
              width: 12,
              height: 12,
              decoration: const BoxDecoration(
                color: Colors.red,
                shape: BoxShape.circle,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('LIVE',
                      style: TextStyle(
                          fontWeight: FontWeight.bold,
                          color: Colors.red,
                          fontSize: 18)),
                  Text(
                    'Loop #$_loopCount | '
                    '${_currentBitrateKbps.toInt()} kbps | '
                    'Uptime: ${_formatUptime()}',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildErrorBanner() {
    return Card(
      color: Colors.red.shade50,
      child: ListTile(
        leading: const Icon(Icons.error, color: Colors.red),
        title: const Text('Stream Error'),
        subtitle: Text(_errorMessage ?? 'Connection lost'),
        trailing: TextButton(
          onPressed: () => setState(() {
            _status = StreamStatus.idle;
            _errorMessage = null;
          }),
          child: const Text('Dismiss'),
        ),
      ),
    );
  }

  Widget _buildPlatformCard() {
    final r = context.responsive;
    return Card(
      child: Padding(
        padding: r.cardContentPadding,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Broadcast Target',
                style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 12),
            SegmentedButton<StreamPlatform>(
              segments: const [
                ButtonSegment(
                  value: StreamPlatform.youtube,
                  label: Text('YouTube'),
                  icon: Icon(Icons.play_arrow),
                ),
                ButtonSegment(
                  value: StreamPlatform.tiktok,
                  label: Text('TikTok'),
                  icon: Icon(Icons.music_note),
                ),
                ButtonSegment(
                  value: StreamPlatform.customRtmp,
                  label: Text('Custom'),
                  icon: Icon(Icons.settings_input_antenna),
                ),
              ],
              selected: {_platform},
              onSelectionChanged: _status != StreamStatus.idle
                  ? null
                  : (s) async {
                      setState(() => _platform = s.first);
                      await _loadSavedKeys();
                    },
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStreamConfigCard() {
    final r = context.responsive;
    return Card(
      child: Padding(
        padding: r.cardContentPadding,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Stream Configuration',
                style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 12),
            if (_platform == StreamPlatform.customRtmp) ...[
              TextField(
                controller: _rtmpUrlController,
                enabled: _status == StreamStatus.idle,
                decoration: const InputDecoration(
                  labelText: 'RTMP URL',
                  hintText: 'rtmp://live.example.com/app',
                  border: OutlineInputBorder(),
                  prefixIcon: Icon(Icons.link),
                ),
              ),
              const SizedBox(height: 12),
            ],
            if (_platform == StreamPlatform.youtube)
              Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: Text(
                  'Server: rtmp://a.rtmp.youtube.com/live2',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ),
            TextField(
              controller: _streamKeyController,
              enabled: _status == StreamStatus.idle,
              obscureText: true,
              decoration: InputDecoration(
                labelText: 'Stream Key',
                border: const OutlineInputBorder(),
                prefixIcon: const Icon(Icons.key),
                helperText: _platform == StreamPlatform.tiktok
                    ? 'Requires TikTok Live Studio / RTMP access'
                    : null,
                suffixIcon: IconButton(
                  icon: const Icon(Icons.save),
                  tooltip: 'Save key for next time',
                  onPressed: _saveStreamKey,
                ),
              ),
            ),
            const SizedBox(height: 12),
            Text('Bitrate: $_bitrate kbps'),
            Slider(
              value: _bitrate.toDouble(),
              min: 1000,
              max: 8000,
              divisions: 14,
              label: '$_bitrate kbps',
              onChanged: _status != StreamStatus.idle
                  ? null
                  : (v) => setState(() => _bitrate = v.toInt()),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMediaSourceCard() {
    final r = context.responsive;
    return Card(
      child: Padding(
        padding: r.cardContentPadding,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Media Source',
                style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 12),
            ListTile(
              leading: const Icon(Icons.video_library),
              title: Text(_mediaFileName ?? 'No media selected'),
              subtitle: _selectedMediaPath != null
                  ? const Text(
                      'Will stream in infinite loop without gaps')
                  : const Text('Select a video or audio file to stream'),
              trailing: FilledButton.tonal(
                onPressed:
                    _status == StreamStatus.idle ? _pickMedia : null,
                child: const Text('Pick'),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTelemetryCard() {
    final r = context.responsive;
    if (_status != StreamStatus.live) return const SizedBox.shrink();

    return Card(
      child: Padding(
        padding: r.cardContentPadding,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Live Telemetry',
                style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 12),
            _buildTelemetryRow(
                Icons.loop, 'Loop Count', '$_loopCount'),
            _buildTelemetryRow(Icons.speed, 'Current Bitrate',
                '${_currentBitrateKbps.toInt()} kbps'),
            _buildTelemetryRow(
                Icons.timer, 'Uptime', _formatUptime()),
            _buildTelemetryRow(Icons.cell_tower, 'Platform',
                _platform.name.toUpperCase()),
            _buildTelemetryRow(Icons.video_file, 'Source',
                _mediaFileName ?? '-'),
          ],
        ),
      ),
    );
  }

  Widget _buildTelemetryRow(IconData icon, String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        children: [
          Icon(icon, size: 18, color: Colors.grey),
          const SizedBox(width: 8),
          Text('$label: ',
              style: const TextStyle(fontWeight: FontWeight.w500)),
          Text(value),
        ],
      ),
    );
  }

  Widget _buildActionButton() {
    final r = context.responsive;
    if (_status == StreamStatus.live) {
      return SizedBox(
        height: r.buttonHeight,
        child: FilledButton.icon(
          onPressed: _stopStream,
          icon: const Icon(Icons.stop),
          label: const Text('Stop Stream'),
          style: FilledButton.styleFrom(backgroundColor: Colors.red),
        ),
      );
    }

    if (_status == StreamStatus.connecting) {
      return SizedBox(
        height: r.buttonHeight,
        child: FilledButton.icon(
          onPressed: null,
          icon: const SizedBox(
            width: 20,
            height: 20,
            child: CircularProgressIndicator(
                strokeWidth: 2, color: Colors.white),
          ),
          label: const Text('Connecting...'),
        ),
      );
    }

    return SizedBox(
      height: r.buttonHeight,
      child: FilledButton.icon(
        onPressed: _canGoLive() ? _startStream : null,
        icon: const Icon(Icons.live_tv),
        label: const Text('Go Live'),
      ),
    );
  }

  // === ACTIONS ===

  bool _canGoLive() {
    return _selectedMediaPath != null &&
        _streamKeyController.text.isNotEmpty &&
        _status == StreamStatus.idle;
  }

  Future<void> _pickMedia() async {
    final path = await _filePicker.pickMedia();
    if (path == null) return;
    setState(() {
      _selectedMediaPath = path;
      _mediaFileName = path.split('/').last;
    });
  }

  Future<void> _saveStreamKey() async {
    final key = _streamKeyController.text;
    switch (_platform) {
      case StreamPlatform.youtube:
        await _settings.setYoutubeStreamKey(key);
        break;
      case StreamPlatform.tiktok:
        await _settings.setTiktokStreamKey(key);
        break;
      case StreamPlatform.customRtmp:
        await _settings.setCustomStreamKey(key);
        await _settings.setCustomRtmpUrl(_rtmpUrlController.text);
        break;
    }
    await _settings.setDefaultBitrate(_bitrate);

    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Stream key saved')),
      );
    }
  }

  Future<void> _startStream() async {
    if (_selectedMediaPath == null) return;

    setState(() {
      _status = StreamStatus.connecting;
      _errorMessage = null;
    });

    // Determine RTMP URL
    String rtmpUrl;
    switch (_platform) {
      case StreamPlatform.youtube:
        rtmpUrl = 'rtmp://a.rtmp.youtube.com/live2';
        break;
      case StreamPlatform.tiktok:
        rtmpUrl = 'rtmp://push.tiktok.com/live';
        break;
      case StreamPlatform.customRtmp:
        rtmpUrl = _rtmpUrlController.text;
        break;
    }

    // Create live session record
    final sessionId = const Uuid().v4();
    _activeSessionId = sessionId;
    final session = LiveSession(
      id: sessionId,
      platform: _platform.name,
      rtmpUrl: rtmpUrl,
      streamKey: '***', // Don't store actual key in DB
      mediaPath: _selectedMediaPath!,
      status: 'connecting',
      bitrate: _bitrate,
      startedAt: DateTime.now(),
    );
    await _db.insertLiveSession(session.toMap());

    // Start FFmpeg RTMP stream
    await _ffmpeg.startRtmpStream(
      mediaPath: _selectedMediaPath!,
      rtmpUrl: rtmpUrl,
      streamKey: _streamKeyController.text,
      bitrate: _bitrate,
      onStatistics: _handleStatistics,
      onComplete: () {
        if (mounted) _handleStreamEnd(null);
      },
      onError: (error) {
        if (mounted) _handleStreamEnd(error);
      },
    );

    // If we get here without error, stream is live
    setState(() {
      _status = StreamStatus.live;
      _loopCount = 1;
      _uptimeSeconds = 0;
    });

    await _db.updateLiveSession(sessionId, {'status': 'live'});

    // Start uptime counter
    _uptimeTimer = Timer.periodic(const Duration(seconds: 1), (_) {
      if (mounted && _status == StreamStatus.live) {
        setState(() => _uptimeSeconds += 1);
        // Estimate loop count from media duration
        _updateLoopCount();
      }
    });
  }

  void _handleStatistics(Statistics statistics) {
    if (!mounted) return;
    final bitrate = statistics.getBitrate();
    setState(() {
      _currentBitrateKbps = bitrate / 1000.0;
    });
  }

  void _handleStreamEnd(String? error) {
    _uptimeTimer?.cancel();
    if (error != null) {
      setState(() {
        _status = StreamStatus.error;
        _errorMessage = error.length > 200
            ? '${error.substring(0, 200)}...'
            : error;
      });
    } else {
      setState(() => _status = StreamStatus.idle);
    }

    // Update session in DB
    if (_activeSessionId != null) {
      _db.updateLiveSession(_activeSessionId!, {
        'status': error != null ? 'error' : 'ended',
        'loop_count': _loopCount,
        'uptime_seconds': _uptimeSeconds,
        'ended_at': DateTime.now().toIso8601String(),
      });
      _activeSessionId = null;
    }
  }

  Future<void> _stopStream() async {
    _uptimeTimer?.cancel();
    await _ffmpeg.stopRtmpStream();

    if (_activeSessionId != null) {
      await _db.updateLiveSession(_activeSessionId!, {
        'status': 'ended',
        'loop_count': _loopCount,
        'uptime_seconds': _uptimeSeconds,
        'ended_at': DateTime.now().toIso8601String(),
      });
      _activeSessionId = null;
    }

    setState(() {
      _status = StreamStatus.idle;
      _loopCount = 0;
      _uptimeSeconds = 0;
      _currentBitrateKbps = 0;
    });

    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Stream ended')),
      );
    }
  }

  Future<void> _updateLoopCount() async {
    if (_selectedMediaPath == null) return;
    try {
      final durationMs = await _ffmpeg.getMediaDuration(_selectedMediaPath!);
      if (durationMs > 0) {
        final newCount =
            ((_uptimeSeconds * 1000) / durationMs).floor() + 1;
        if (newCount != _loopCount) {
          setState(() => _loopCount = newCount);
        }
      }
    } catch (_) {
      // Ignore errors during loop count estimation
    }
  }

  String _formatUptime() {
    final hours = (_uptimeSeconds / 3600).floor();
    final minutes = ((_uptimeSeconds % 3600) / 60).floor();
    final seconds = (_uptimeSeconds % 60).floor();
    return '${hours.toString().padLeft(2, '0')}:'
        '${minutes.toString().padLeft(2, '0')}:'
        '${seconds.toString().padLeft(2, '0')}';
  }
}
