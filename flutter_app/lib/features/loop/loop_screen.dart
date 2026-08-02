import 'package:flutter/material.dart';
import 'package:uuid/uuid.dart';
import 'package:video_player/video_player.dart';
import 'dart:io';
import 'package:loopingvid/core/services/file_picker_service.dart';
import 'package:loopingvid/core/services/ffmpeg_service.dart';
import 'package:loopingvid/core/database/database_helper.dart';
import 'package:loopingvid/core/database/models.dart';

enum LoopStyle { normal, crossfade, pingPong }

class LoopScreen extends StatefulWidget {
  const LoopScreen({super.key});

  @override
  State<LoopScreen> createState() => _LoopScreenState();
}

class _LoopScreenState extends State<LoopScreen> {
  final _filePicker = FilePickerService();
  final _ffmpeg = FFmpegService();
  final _db = DatabaseHelper();

  String? _selectedVideoPath;
  double _targetDuration = 60.0;
  LoopStyle _loopStyle = LoopStyle.normal;
  double _crossfadeDuration = 1.0;
  bool _muteAudio = false;
  String _quality = 'High (1080p)';
  bool _isRendering = false;
  double _renderProgress = 0.0;
  String? _outputPath;
  String? _errorMessage;

  VideoPlayerController? _previewController;

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
          _buildVideoInputCard(),
          const SizedBox(height: 16),
          _buildLoopConfigCard(),
          const SizedBox(height: 16),
          if (_isRendering) _buildProgressCard(),
          if (!_isRendering) _buildRenderButton(),
          if (_outputPath != null) ...[
            const SizedBox(height: 16),
            _buildOutputCard(),
          ],
          if (_errorMessage != null) ...[
            const SizedBox(height: 16),
            _buildErrorCard(),
          ],
        ],
      ),
    );
  }

  Widget _buildVideoInputCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Input Video',
                style: Theme.of(context).textTheme.titleMedium),
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
                    ),
                  ),
                  child: const Center(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(Icons.video_library, size: 48),
                        SizedBox(height: 8),
                        Text('Tap to select video from device'),
                      ],
                    ),
                  ),
                ),
              )
            else ...[
              if (_previewController != null &&
                  _previewController!.value.isInitialized)
                ClipRRect(
                  borderRadius: BorderRadius.circular(12),
                  child: AspectRatio(
                    aspectRatio: _previewController!.value.aspectRatio,
                    child: VideoPlayer(_previewController!),
                  ),
                )
              else
                Container(
                  height: 180,
                  decoration: BoxDecoration(
                    color: Colors.black87,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: const Center(
                    child: CircularProgressIndicator(),
                  ),
                ),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(
                    child: Text(
                      _selectedVideoPath!.split('/').last,
                      style: Theme.of(context).textTheme.bodySmall,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  IconButton(
                    icon: const Icon(Icons.play_arrow),
                    onPressed: () {
                      if (_previewController?.value.isPlaying ?? false) {
                        _previewController?.pause();
                      } else {
                        _previewController?.play();
                      }
                      setState(() {});
                    },
                  ),
                  TextButton(
                    onPressed: _pickVideo,
                    child: const Text('Change'),
                  ),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildLoopConfigCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Loop Configuration',
                style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 16),
            Text('Target Duration: ${_targetDuration.toInt()} seconds'),
            Slider(
              value: _targetDuration,
              min: 10,
              max: 600,
              divisions: 59,
              label: '${_targetDuration.toInt()}s',
              onChanged: _isRendering
                  ? null
                  : (value) => setState(() => _targetDuration = value),
            ),
            const SizedBox(height: 12),
            Text('Loop Style',
                style: Theme.of(context).textTheme.bodyLarge),
            const SizedBox(height: 8),
            SegmentedButton<LoopStyle>(
              segments: const [
                ButtonSegment(
                    value: LoopStyle.normal, label: Text('Normal')),
                ButtonSegment(
                    value: LoopStyle.crossfade, label: Text('Crossfade')),
                ButtonSegment(
                    value: LoopStyle.pingPong, label: Text('Ping-Pong')),
              ],
              selected: {_loopStyle},
              onSelectionChanged: _isRendering
                  ? null
                  : (styles) =>
                      setState(() => _loopStyle = styles.first),
            ),
            if (_loopStyle == LoopStyle.crossfade) ...[
              const SizedBox(height: 12),
              Text(
                  'Crossfade: ${_crossfadeDuration.toStringAsFixed(1)}s'),
              Slider(
                value: _crossfadeDuration,
                min: 0.5,
                max: 5.0,
                divisions: 9,
                label: '${_crossfadeDuration.toStringAsFixed(1)}s',
                onChanged: _isRendering
                    ? null
                    : (value) =>
                        setState(() => _crossfadeDuration = value),
              ),
            ],
            const SizedBox(height: 12),
            SwitchListTile(
              title: const Text('Mute Audio'),
              subtitle: const Text('Remove audio track from output'),
              value: _muteAudio,
              onChanged: _isRendering
                  ? null
                  : (value) => setState(() => _muteAudio = value),
            ),
            const SizedBox(height: 12),
            DropdownButtonFormField<String>(
              decoration: const InputDecoration(
                labelText: 'Quality Preset',
                border: OutlineInputBorder(),
              ),
              initialValue: _quality,
              items: _qualityPresets
                  .map((q) => DropdownMenuItem(value: q, child: Text(q)))
                  .toList(),
              onChanged: _isRendering
                  ? null
                  : (value) {
                      if (value != null) setState(() => _quality = value);
                    },
            ),
          ],
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
            LinearProgressIndicator(value: _renderProgress),
            const SizedBox(height: 8),
            Text('Rendering... ${(_renderProgress * 100).toInt()}%'),
            const SizedBox(height: 8),
            OutlinedButton(
              onPressed: _cancelRender,
              child: const Text('Cancel'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildRenderButton() {
    return FilledButton.icon(
      onPressed: _selectedVideoPath != null ? _startRender : null,
      icon: const Icon(Icons.play_arrow),
      label: const Text('Start Loop Render'),
    );
  }

  Widget _buildOutputCard() {
    return Card(
      color: Colors.green.shade50,
      child: ListTile(
        leading: const Icon(Icons.check_circle, color: Colors.green),
        title: const Text('Render Complete!'),
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
        title: const Text('Render Failed'),
        subtitle: Text(_errorMessage ?? 'Unknown error'),
      ),
    );
  }

  // === ACTIONS ===

  Future<void> _pickVideo() async {
    final path = await _filePicker.pickVideo();
    if (path == null) return;

    setState(() {
      _selectedVideoPath = path;
      _outputPath = null;
      _errorMessage = null;
    });

    // Initialize video preview
    _previewController?.dispose();
    _previewController = VideoPlayerController.file(File(path));
    await _previewController!.initialize();
    _previewController!.setLooping(true);
    setState(() {});
  }

  Future<void> _startRender() async {
    if (_selectedVideoPath == null) return;

    setState(() {
      _isRendering = true;
      _renderProgress = 0.0;
      _outputPath = null;
      _errorMessage = null;
    });

    // Create render job in database
    final jobId = const Uuid().v4();
    final job = RenderJob(
      id: jobId,
      title: 'Loop: ${_selectedVideoPath!.split('/').last}',
      type: 'loop',
      inputPath: _selectedVideoPath!,
      status: 'running',
      createdAt: DateTime.now(),
      settings:
          '{"style":"${_loopStyle.name}","duration":${_targetDuration.toInt()},'
          '"quality":"$_quality","mute":$_muteAudio}',
    );
    await _db.insertRenderJob(job.toMap());

    try {
      String? result;

      switch (_loopStyle) {
        case LoopStyle.normal:
          result = await _ffmpeg.createNormalLoop(
            inputPath: _selectedVideoPath!,
            targetDurationSec: _targetDuration.toInt(),
            muteAudio: _muteAudio,
            quality: _quality,
            onProgress: (p) {
              if (mounted) setState(() => _renderProgress = p);
            },
          );
          break;
        case LoopStyle.crossfade:
          result = await _ffmpeg.createCrossfadeLoop(
            inputPath: _selectedVideoPath!,
            targetDurationSec: _targetDuration.toInt(),
            crossfadeDuration: _crossfadeDuration,
            muteAudio: _muteAudio,
            quality: _quality,
            onProgress: (p) {
              if (mounted) setState(() => _renderProgress = p);
            },
          );
          break;
        case LoopStyle.pingPong:
          result = await _ffmpeg.createPingPongLoop(
            inputPath: _selectedVideoPath!,
            targetDurationSec: _targetDuration.toInt(),
            muteAudio: _muteAudio,
            quality: _quality,
            onProgress: (p) {
              if (mounted) setState(() => _renderProgress = p);
            },
          );
          break;
      }

      if (result != null) {
        await _db.updateRenderJob(jobId, {
          'status': 'completed',
          'output_path': result,
          'progress': 1.0,
          'completed_at': DateTime.now().toIso8601String(),
        });
        if (mounted) {
          setState(() {
            _outputPath = result;
            _isRendering = false;
            _renderProgress = 1.0;
          });
        }
      } else {
        await _db.updateRenderJob(jobId, {
          'status': 'failed',
          'error_message': 'FFmpeg returned null output',
          'completed_at': DateTime.now().toIso8601String(),
        });
        if (mounted) {
          setState(() {
            _errorMessage = 'Render failed. Check input file format.';
            _isRendering = false;
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
          _isRendering = false;
        });
      }
    }
  }

  Future<void> _cancelRender() async {
    await _ffmpeg.cancelAll();
    setState(() {
      _isRendering = false;
      _renderProgress = 0.0;
    });
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
