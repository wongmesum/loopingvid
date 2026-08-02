import 'package:flutter/material.dart';

enum LoopStyle { normal, crossfade, pingPong }

class LoopScreen extends StatefulWidget {
  const LoopScreen({super.key});

  @override
  State<LoopScreen> createState() => _LoopScreenState();
}

class _LoopScreenState extends State<LoopScreen> {
  String? _selectedVideoPath;
  double _targetDuration = 60.0;
  LoopStyle _loopStyle = LoopStyle.normal;
  double _crossfadeDuration = 1.0;
  bool _muteAudio = false;
  String _quality = 'High (1080p)';
  bool _isRendering = false;
  double _renderProgress = 0.0;

  final List<String> _qualityPresets = [
    'Low (480p)',
    'Medium (720p)',
    'High (1080p)',
    'Ultra (4K)',
  ];

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Video Input Card
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Input Video',
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                  const SizedBox(height: 12),
                  if (_selectedVideoPath == null)
                    InkWell(
                      onTap: _pickVideo,
                      borderRadius: BorderRadius.circular(12),
                      child: Container(
                        height: 180,
                        decoration: BoxDecoration(
                          color: Theme.of(context)
                              .colorScheme
                              .surfaceContainerHighest,
                          borderRadius: BorderRadius.circular(12),
                          border: Border.all(
                            color: Theme.of(context).colorScheme.outline,
                            style: BorderStyle.solid,
                          ),
                        ),
                        child: const Center(
                          child: Column(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Icon(Icons.video_library, size: 48),
                              SizedBox(height: 8),
                              Text('Tap to select video'),
                            ],
                          ),
                        ),
                      ),
                    )
                  else
                    Container(
                      height: 180,
                      decoration: BoxDecoration(
                        color: Colors.black87,
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Center(
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            const Icon(Icons.videocam,
                                size: 48, color: Colors.white),
                            const SizedBox(height: 8),
                            Text(
                              'Video selected',
                              style: TextStyle(color: Colors.white.withAlpha(200)),
                            ),
                            TextButton(
                              onPressed: _pickVideo,
                              child: const Text('Change'),
                            ),
                          ],
                        ),
                      ),
                    ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),

          // Loop Configuration Card
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Loop Configuration',
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                  const SizedBox(height: 16),

                  // Target Duration
                  Text(
                      'Target Duration: ${_targetDuration.toInt()} seconds'),
                  Slider(
                    value: _targetDuration,
                    min: 10,
                    max: 600,
                    divisions: 59,
                    label: '${_targetDuration.toInt()}s',
                    onChanged: (value) {
                      setState(() => _targetDuration = value);
                    },
                  ),
                  const SizedBox(height: 12),

                  // Loop Style
                  Text('Loop Style',
                      style: Theme.of(context).textTheme.bodyLarge),
                  const SizedBox(height: 8),
                  SegmentedButton<LoopStyle>(
                    segments: const [
                      ButtonSegment(
                          value: LoopStyle.normal, label: Text('Normal')),
                      ButtonSegment(
                          value: LoopStyle.crossfade,
                          label: Text('Crossfade')),
                      ButtonSegment(
                          value: LoopStyle.pingPong,
                          label: Text('Ping-Pong')),
                    ],
                    selected: {_loopStyle},
                    onSelectionChanged: (styles) {
                      setState(() => _loopStyle = styles.first);
                    },
                  ),
                  const SizedBox(height: 12),

                  // Crossfade Duration (only if crossfade selected)
                  if (_loopStyle == LoopStyle.crossfade) ...[
                    Text(
                        'Crossfade: ${_crossfadeDuration.toStringAsFixed(1)}s'),
                    Slider(
                      value: _crossfadeDuration,
                      min: 0.5,
                      max: 5.0,
                      divisions: 9,
                      label: '${_crossfadeDuration.toStringAsFixed(1)}s',
                      onChanged: (value) {
                        setState(() => _crossfadeDuration = value);
                      },
                    ),
                    const SizedBox(height: 12),
                  ],

                  // Mute Audio
                  SwitchListTile(
                    title: const Text('Mute Audio'),
                    subtitle:
                        const Text('Remove audio track from output'),
                    value: _muteAudio,
                    onChanged: (value) {
                      setState(() => _muteAudio = value);
                    },
                  ),
                  const SizedBox(height: 12),

                  // Quality Preset
                  DropdownButtonFormField<String>(
                    decoration: const InputDecoration(
                      labelText: 'Quality Preset',
                      border: OutlineInputBorder(),
                    ),
                    value: _quality,
                    items: _qualityPresets
                        .map((q) =>
                            DropdownMenuItem(value: q, child: Text(q)))
                        .toList(),
                    onChanged: (value) {
                      if (value != null) setState(() => _quality = value);
                    },
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),

          // Render Button
          if (_isRendering)
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  children: [
                    LinearProgressIndicator(value: _renderProgress),
                    const SizedBox(height: 8),
                    Text(
                        'Rendering... ${(_renderProgress * 100).toInt()}%'),
                    const SizedBox(height: 8),
                    OutlinedButton(
                      onPressed: _cancelRender,
                      child: const Text('Cancel'),
                    ),
                  ],
                ),
              ),
            )
          else
            FilledButton.icon(
              onPressed: _selectedVideoPath != null ? _startRender : null,
              icon: const Icon(Icons.play_arrow),
              label: const Text('Start Loop Render'),
            ),
        ],
      ),
    );
  }

  void _pickVideo() {
    // Simulated video pick
    setState(() {
      _selectedVideoPath = '/storage/emulated/0/DCIM/sample_video.mp4';
    });
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Video selected successfully')),
    );
  }

  void _startRender() {
    setState(() {
      _isRendering = true;
      _renderProgress = 0.0;
    });
    // Simulate render progress
    _simulateRender();
  }

  Future<void> _simulateRender() async {
    for (int i = 1; i <= 10; i++) {
      await Future.delayed(const Duration(milliseconds: 300));
      if (!mounted) return;
      setState(() => _renderProgress = i / 10);
    }
    setState(() => _isRendering = false);
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Loop render completed!')),
      );
    }
  }

  void _cancelRender() {
    setState(() {
      _isRendering = false;
      _renderProgress = 0.0;
    });
  }
}
