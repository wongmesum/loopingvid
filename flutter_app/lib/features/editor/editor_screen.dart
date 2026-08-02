import 'package:flutter/material.dart';
import 'package:uuid/uuid.dart';
import 'package:video_player/video_player.dart';
import 'dart:io';
import 'package:loopingvid/core/services/file_picker_service.dart';
import 'package:loopingvid/core/services/ffmpeg_service.dart';
import 'package:loopingvid/core/database/database_helper.dart';
import 'package:loopingvid/core/database/models.dart';

class EditorScreen extends StatefulWidget {
  const EditorScreen({super.key});

  @override
  State<EditorScreen> createState() => _EditorScreenState();
}

class _EditorScreenState extends State<EditorScreen> {
  final _filePicker = FilePickerService();
  final _ffmpeg = FFmpegService();
  final _db = DatabaseHelper();

  String? _videoPath;
  String? _audioPath;
  String _titleText = '';
  double _titleFontSize = 24;
  String _watermarkText = '';
  bool _showSpectrum = false;
  String _spectrumStyle = 'Bars';
  String _quality = 'High (1080p)';
  bool _isExporting = false;
  double _exportProgress = 0.0;
  String? _outputPath;
  String? _errorMessage;

  VideoPlayerController? _previewController;

  final List<String> _spectrumStyles = ['Bars', 'Wave', 'Circle'];
  final List<String> _qualityPresets = [
    'Low (480p)',
    'Medium (720p)',
    'High (1080p)',
    'Ultra (4K)',
  ];

  @override
  void dispose() {
    _previewController?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          _buildPreviewCard(),
          const SizedBox(height: 16),
          _buildMediaSourcesCard(),
          const SizedBox(height: 16),
          _buildTextOverlaysCard(),
          const SizedBox(height: 16),
          _buildSpectrumCard(),
          const SizedBox(height: 16),
          _buildQualityCard(),
          const SizedBox(height: 16),
          if (_isExporting) _buildProgressCard(),
          if (!_isExporting) _buildExportButton(),
          if (_outputPath != null) ...[
            const SizedBox(height: 16),
            _buildSuccessCard(),
          ],
          if (_errorMessage != null) ...[
            const SizedBox(height: 16),
            _buildErrorCard(),
          ],
        ],
      ),
    );
  }

  Widget _buildPreviewCard() {
    return Card(
      clipBehavior: Clip.antiAlias,
      child: Column(
        children: [
          Container(
            height: 200,
            color: Colors.black,
            child: _previewController != null &&
                    _previewController!.value.isInitialized
                ? Stack(
                    children: [
                      Center(
                        child: AspectRatio(
                          aspectRatio:
                              _previewController!.value.aspectRatio,
                          child: VideoPlayer(_previewController!),
                        ),
                      ),
                      // Title overlay preview
                      if (_titleText.isNotEmpty)
                        Positioned(
                          top: 16,
                          left: 16,
                          child: Text(
                            _titleText,
                            style: TextStyle(
                              color: Colors.white,
                              fontSize: _titleFontSize,
                              fontWeight: FontWeight.bold,
                              shadows: const [
                                Shadow(
                                    blurRadius: 4, color: Colors.black),
                              ],
                            ),
                          ),
                        ),
                      // Watermark overlay preview
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
                      // Playback controls
                      Positioned(
                        bottom: 8,
                        left: 8,
                        child: IconButton(
                          icon: Icon(
                            _previewController!.value.isPlaying
                                ? Icons.pause
                                : Icons.play_arrow,
                            color: Colors.white,
                          ),
                          onPressed: () {
                            if (_previewController!.value.isPlaying) {
                              _previewController!.pause();
                            } else {
                              _previewController!.play();
                            }
                            setState(() {});
                          },
                        ),
                      ),
                    ],
                  )
                : const Center(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(Icons.play_circle_outline,
                            size: 64, color: Colors.white54),
                        SizedBox(height: 8),
                        Text('Select a video to preview',
                            style: TextStyle(color: Colors.white54)),
                      ],
                    ),
                  ),
          ),
        ],
      ),
    );
  }

  Widget _buildMediaSourcesCard() {
    return Card(
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
              title: Text(_videoPath != null
                  ? _videoPath!.split('/').last
                  : 'No video selected'),
              subtitle: _videoPath != null
                  ? const Text('Tap to change')
                  : null,
              trailing: FilledButton.tonal(
                onPressed: _isExporting ? null : _pickVideo,
                child: const Text('Pick'),
              ),
            ),
            const Divider(),
            ListTile(
              leading: const Icon(Icons.audio_file),
              title: Text(_audioPath != null
                  ? _audioPath!.split('/').last
                  : 'No audio selected (optional)'),
              subtitle: _audioPath != null
                  ? const Text('Replaces original audio')
                  : null,
              trailing: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  if (_audioPath != null)
                    IconButton(
                      icon: const Icon(Icons.close),
                      onPressed: () =>
                          setState(() => _audioPath = null),
                    ),
                  FilledButton.tonal(
                    onPressed: _isExporting ? null : _pickAudio,
                    child: const Text('Pick'),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTextOverlaysCard() {
    return Card(
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
                hintText: 'Enter title to overlay on video',
                border: OutlineInputBorder(),
                prefixIcon: Icon(Icons.title),
              ),
              onChanged: (v) => setState(() => _titleText = v),
              enabled: !_isExporting,
            ),
            const SizedBox(height: 12),
            Text('Font Size: ${_titleFontSize.toInt()}px'),
            Slider(
              value: _titleFontSize,
              min: 12,
              max: 72,
              divisions: 60,
              onChanged: _isExporting
                  ? null
                  : (v) => setState(() => _titleFontSize = v),
            ),
            const SizedBox(height: 12),
            TextField(
              decoration: const InputDecoration(
                labelText: 'Watermark Text',
                hintText: 'Small text in bottom-right corner',
                border: OutlineInputBorder(),
                prefixIcon: Icon(Icons.branding_watermark),
              ),
              onChanged: (v) => setState(() => _watermarkText = v),
              enabled: !_isExporting,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSpectrumCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Audio Spectrum',
                style: Theme.of(context).textTheme.titleMedium),
            SwitchListTile(
              title: const Text('Show Spectrum Visualizer'),
              subtitle: const Text(
                  'Requires audio track. Rendered via FFmpeg.'),
              value: _showSpectrum,
              onChanged: _isExporting
                  ? null
                  : (v) => setState(() => _showSpectrum = v),
            ),
            if (_showSpectrum)
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: SegmentedButton<String>(
                  segments: _spectrumStyles
                      .map((s) =>
                          ButtonSegment(value: s, label: Text(s)))
                      .toList(),
                  selected: {_spectrumStyle},
                  onSelectionChanged: _isExporting
                      ? null
                      : (s) =>
                          setState(() => _spectrumStyle = s.first),
                ),
              ),
            if (_showSpectrum && _audioPath == null)
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Text(
                  'Note: Select an audio track above for spectrum visualization.',
                  style: TextStyle(
                      color: Theme.of(context).colorScheme.error,
                      fontSize: 12),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildQualityCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: DropdownButtonFormField<String>(
          decoration: const InputDecoration(
            labelText: 'Output Quality',
            border: OutlineInputBorder(),
          ),
          initialValue: _quality,
          items: _qualityPresets
              .map((q) => DropdownMenuItem(value: q, child: Text(q)))
              .toList(),
          onChanged: _isExporting
              ? null
              : (v) {
                  if (v != null) setState(() => _quality = v);
                },
        ),
      ),
    );
  }

  Widget _buildProgressCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            LinearProgressIndicator(value: _exportProgress),
            const SizedBox(height: 8),
            Text('Exporting... ${(_exportProgress * 100).toInt()}%'),
            const SizedBox(height: 8),
            OutlinedButton(
              onPressed: () async {
                await _ffmpeg.cancelAll();
                setState(() {
                  _isExporting = false;
                  _exportProgress = 0.0;
                });
              },
              child: const Text('Cancel'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildExportButton() {
    return FilledButton.icon(
      onPressed: _videoPath != null && !_isExporting ? _exportVideo : null,
      icon: const Icon(Icons.movie_creation),
      label: const Text('Export Composition'),
    );
  }

  Widget _buildSuccessCard() {
    return Card(
      color: Colors.green.shade50,
      child: ListTile(
        leading: const Icon(Icons.check_circle, color: Colors.green),
        title: const Text('Export Complete!'),
        subtitle: Text(_outputPath!.split('/').last),
        trailing: IconButton(
          icon: const Icon(Icons.play_circle),
          onPressed: _playOutput,
        ),
      ),
    );
  }

  Widget _buildErrorCard() {
    return Card(
      color: Colors.red.shade50,
      child: ListTile(
        leading: const Icon(Icons.error, color: Colors.red),
        title: const Text('Export Failed'),
        subtitle: Text(_errorMessage ?? 'Unknown error'),
      ),
    );
  }

  // === ACTIONS ===

  Future<void> _pickVideo() async {
    final path = await _filePicker.pickVideo();
    if (path == null) return;

    setState(() {
      _videoPath = path;
      _outputPath = null;
      _errorMessage = null;
    });

    _previewController?.dispose();
    _previewController = VideoPlayerController.file(File(path));
    await _previewController!.initialize();
    _previewController!.setLooping(true);
    setState(() {});
  }

  Future<void> _pickAudio() async {
    final path = await _filePicker.pickAudio();
    if (path == null) return;
    setState(() {
      _audioPath = path;
      _outputPath = null;
      _errorMessage = null;
    });
  }

  Future<void> _exportVideo() async {
    if (_videoPath == null) return;

    _previewController?.pause();

    setState(() {
      _isExporting = true;
      _exportProgress = 0.0;
      _outputPath = null;
      _errorMessage = null;
    });

    final jobId = const Uuid().v4();
    final job = RenderJob(
      id: jobId,
      title: 'Editor: ${_videoPath!.split('/').last}',
      type: 'editor',
      inputPath: _videoPath!,
      status: 'running',
      createdAt: DateTime.now(),
      settings:
          '{"title":"$_titleText","watermark":"$_watermarkText",'
          '"spectrum":$_showSpectrum,"style":"$_spectrumStyle",'
          '"quality":"$_quality"}',
    );
    await _db.insertRenderJob(job.toMap());

    try {
      final result = await _ffmpeg.composeVideo(
        videoPath: _videoPath!,
        audioPath: _audioPath,
        titleText: _titleText.isNotEmpty ? _titleText : null,
        titleFontSize: _titleFontSize,
        watermarkText: _watermarkText.isNotEmpty ? _watermarkText : null,
        showSpectrum: _showSpectrum && _audioPath != null,
        spectrumStyle: _spectrumStyle,
        quality: _quality,
        onProgress: (p) {
          if (mounted) setState(() => _exportProgress = p);
        },
      );

      if (result != null && await File(result).exists()) {
        await _db.updateRenderJob(jobId, {
          'status': 'completed',
          'output_path': result,
          'progress': 1.0,
          'completed_at': DateTime.now().toIso8601String(),
        });
        if (mounted) {
          setState(() {
            _outputPath = result;
            _isExporting = false;
            _exportProgress = 1.0;
          });
        }
      } else {
        await _db.updateRenderJob(jobId, {
          'status': 'failed',
          'error_message': 'FFmpeg returned no output',
          'completed_at': DateTime.now().toIso8601String(),
        });
        if (mounted) {
          setState(() {
            _errorMessage = 'Export failed. Check input files.';
            _isExporting = false;
          });
        }
      }
    } catch (e) {
      await _db.updateRenderJob(jobId, {
        'status': 'failed',
        'error_message': e.toString(),
        'completed_at': DateTime.now().toIso8601String(),
      });
      if (mounted) {
        setState(() {
          _errorMessage = 'Error: $e';
          _isExporting = false;
        });
      }
    }
  }

  Future<void> _playOutput() async {
    if (_outputPath == null) return;
    _previewController?.dispose();
    _previewController = VideoPlayerController.file(File(_outputPath!));
    await _previewController!.initialize();
    _previewController!.setLooping(true);
    await _previewController!.play();
    setState(() {});
  }
}
