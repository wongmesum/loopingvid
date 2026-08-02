import 'package:flutter/material.dart';

enum StreamPlatform { youtube, tiktok, customRtmp }

enum StreamStatus { idle, connecting, live, error }

class LiveScreen extends StatefulWidget {
  const LiveScreen({super.key});

  @override
  State<LiveScreen> createState() => _LiveScreenState();
}

class _LiveScreenState extends State<LiveScreen> {
  StreamPlatform _platform = StreamPlatform.youtube;
  StreamStatus _status = StreamStatus.idle;
  final TextEditingController _rtmpUrlController = TextEditingController();
  final TextEditingController _streamKeyController = TextEditingController();
  String? _selectedMediaPath;
  int _loopCount = 0;
  int _bitrate = 4500;
  double _uptimeSeconds = 0;

  @override
  void dispose() {
    _rtmpUrlController.dispose();
    _streamKeyController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Stream Status Banner
          if (_status == StreamStatus.live)
            Card(
              color: Colors.red.shade50,
              child: Padding(
                padding: const EdgeInsets.all(16),
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
                                  color: Colors.red)),
                          Text(
                            'Loop #$_loopCount | '
                            '${_bitrate}kbps | '
                            'Uptime: ${_formatUptime()}',
                            style: Theme.of(context).textTheme.bodySmall,
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
          const SizedBox(height: 16),

          // Platform Selection
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
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
                    onSelectionChanged: (s) =>
                        setState(() => _platform = s.first),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),

          // RTMP Configuration
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Stream Configuration',
                      style: Theme.of(context).textTheme.titleMedium),
                  const SizedBox(height: 12),
                  if (_platform == StreamPlatform.customRtmp)
                    TextField(
                      controller: _rtmpUrlController,
                      decoration: const InputDecoration(
                        labelText: 'RTMP URL',
                        hintText: 'rtmp://live.example.com/app',
                        border: OutlineInputBorder(),
                        prefixIcon: Icon(Icons.link),
                      ),
                    ),
                  if (_platform == StreamPlatform.customRtmp)
                    const SizedBox(height: 12),
                  TextField(
                    controller: _streamKeyController,
                    obscureText: true,
                    decoration: InputDecoration(
                      labelText: 'Stream Key',
                      border: const OutlineInputBorder(),
                      prefixIcon: const Icon(Icons.key),
                      helperText: _platform == StreamPlatform.tiktok
                          ? 'Requires TikTok Live Studio access'
                          : null,
                    ),
                  ),
                  const SizedBox(height: 12),
                  Text('Bitrate: ${_bitrate}kbps'),
                  Slider(
                    value: _bitrate.toDouble(),
                    min: 1000,
                    max: 8000,
                    divisions: 14,
                    label: '${_bitrate}kbps',
                    onChanged: (v) =>
                        setState(() => _bitrate = v.toInt()),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),

          // Media Source
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Media Source',
                      style: Theme.of(context).textTheme.titleMedium),
                  const SizedBox(height: 12),
                  OutlinedButton.icon(
                    onPressed: () => setState(
                        () => _selectedMediaPath = 'looped_video.mp4'),
                    icon: const Icon(Icons.video_library),
                    label: Text(_selectedMediaPath ?? 'Select media'),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Selected media will stream in infinite loop '
                    'without gaps.',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 24),

          // Go Live / Stop Button
          if (_status == StreamStatus.live)
            FilledButton.icon(
              onPressed: _stopStream,
              icon: const Icon(Icons.stop),
              label: const Text('Stop Stream'),
              style: FilledButton.styleFrom(
                backgroundColor: Colors.red,
              ),
            )
          else
            FilledButton.icon(
              onPressed: _canGoLive() ? _startStream : null,
              icon: _status == StreamStatus.connecting
                  ? const SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(
                          strokeWidth: 2, color: Colors.white),
                    )
                  : const Icon(Icons.live_tv),
              label: Text(_status == StreamStatus.connecting
                  ? 'Connecting...'
                  : 'Go Live'),
            ),
        ],
      ),
    );
  }

  bool _canGoLive() {
    return _selectedMediaPath != null &&
        _streamKeyController.text.isNotEmpty &&
        _status == StreamStatus.idle;
  }

  Future<void> _startStream() async {
    setState(() => _status = StreamStatus.connecting);
    await Future.delayed(const Duration(seconds: 2));
    if (!mounted) return;
    setState(() {
      _status = StreamStatus.live;
      _loopCount = 1;
      _uptimeSeconds = 0;
    });
    _simulateUptime();
  }

  Future<void> _simulateUptime() async {
    while (_status == StreamStatus.live && mounted) {
      await Future.delayed(const Duration(seconds: 1));
      if (!mounted) return;
      setState(() {
        _uptimeSeconds += 1;
        if (_uptimeSeconds % 30 == 0) _loopCount++;
      });
    }
  }

  void _stopStream() {
    setState(() {
      _status = StreamStatus.idle;
      _loopCount = 0;
      _uptimeSeconds = 0;
    });
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Stream ended')),
    );
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
