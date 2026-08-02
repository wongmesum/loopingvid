import 'package:flutter/material.dart';

class EditorScreen extends StatefulWidget {
  const EditorScreen({super.key});

  @override
  State<EditorScreen> createState() => _EditorScreenState();
}

class _EditorScreenState extends State<EditorScreen> {
  String? _videoPath;
  String? _audioPath;
  String _titleText = '';
  String _watermarkText = '';
  double _titleFontSize = 24;
  Color _titleColor = Colors.white;
  bool _showSpectrum = false;
  String _spectrumStyle = 'Bars';
  bool _isExporting = false;

  final List<String> _spectrumStyles = ['Bars', 'Wave', 'Circle'];

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Preview Canvas
          Card(
            clipBehavior: Clip.antiAlias,
            child: Container(
              height: 200,
              color: Colors.black,
              child: Stack(
                children: [
                  const Center(
                    child: Icon(Icons.play_circle_outline,
                        size: 64, color: Colors.white54),
                  ),
                  if (_titleText.isNotEmpty)
                    Positioned(
                      top: 16,
                      left: 16,
                      child: Text(
                        _titleText,
                        style: TextStyle(
                          color: _titleColor,
                          fontSize: _titleFontSize,
                          fontWeight: FontWeight.bold,
                          shadows: const [
                            Shadow(blurRadius: 4, color: Colors.black),
                          ],
                        ),
                      ),
                    ),
                  if (_watermarkText.isNotEmpty)
                    Positioned(
                      bottom: 8,
                      right: 8,
                      child: Text(
                        _watermarkText,
                        style: TextStyle(
                          color: Colors.white.withAlpha(128),
                          fontSize: 12,
                        ),
                      ),
                    ),
                  if (_showSpectrum)
                    Positioned(
                      bottom: 30,
                      left: 16,
                      right: 16,
                      child: Container(
                        height: 40,
                        decoration: BoxDecoration(
                          color: Colors.deepPurple.withAlpha(77),
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: const Center(
                          child:
                              Icon(Icons.graphic_eq, color: Colors.white70),
                        ),
                      ),
                    ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),

          // Media Sources
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Media Sources',
                      style: Theme.of(context).textTheme.titleMedium),
                  const SizedBox(height: 12),
                  ListTile(
                    leading: const Icon(Icons.video_file),
                    title: Text(_videoPath ?? 'No video selected'),
                    trailing: OutlinedButton(
                      onPressed: () => setState(
                          () => _videoPath = 'sample_video.mp4'),
                      child: const Text('Pick'),
                    ),
                  ),
                  ListTile(
                    leading: const Icon(Icons.audio_file),
                    title: Text(_audioPath ?? 'No audio selected'),
                    trailing: OutlinedButton(
                      onPressed: () => setState(
                          () => _audioPath = 'background_music.mp3'),
                      child: const Text('Pick'),
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),

          // Text Overlays
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Text Overlays',
                      style: Theme.of(context).textTheme.titleMedium),
                  const SizedBox(height: 12),
                  TextField(
                    decoration: const InputDecoration(
                      labelText: 'Title Text',
                      border: OutlineInputBorder(),
                    ),
                    onChanged: (v) => setState(() => _titleText = v),
                  ),
                  const SizedBox(height: 12),
                  Text(
                      'Font Size: ${_titleFontSize.toInt()}'),
                  Slider(
                    value: _titleFontSize,
                    min: 12,
                    max: 64,
                    onChanged: (v) =>
                        setState(() => _titleFontSize = v),
                  ),
                  const SizedBox(height: 8),
                  TextField(
                    decoration: const InputDecoration(
                      labelText: 'Watermark Text',
                      border: OutlineInputBorder(),
                    ),
                    onChanged: (v) => setState(() => _watermarkText = v),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),

          // Audio Spectrum Overlay
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Audio Spectrum',
                      style: Theme.of(context).textTheme.titleMedium),
                  SwitchListTile(
                    title: const Text('Show Spectrum Visualizer'),
                    value: _showSpectrum,
                    onChanged: (v) =>
                        setState(() => _showSpectrum = v),
                  ),
                  if (_showSpectrum)
                    SegmentedButton<String>(
                      segments: _spectrumStyles
                          .map((s) =>
                              ButtonSegment(value: s, label: Text(s)))
                          .toList(),
                      selected: {_spectrumStyle},
                      onSelectionChanged: (s) =>
                          setState(() => _spectrumStyle = s.first),
                    ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),

          // Export
          FilledButton.icon(
            onPressed: _videoPath != null && !_isExporting
                ? _exportVideo
                : null,
            icon: _isExporting
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(
                        strokeWidth: 2, color: Colors.white),
                  )
                : const Icon(Icons.movie_creation),
            label: Text(
                _isExporting ? 'Exporting...' : 'Export Composition'),
          ),
        ],
      ),
    );
  }

  Future<void> _exportVideo() async {
    setState(() => _isExporting = true);
    await Future.delayed(const Duration(seconds: 3));
    if (!mounted) return;
    setState(() => _isExporting = false);
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Video exported successfully!')),
    );
  }
}
