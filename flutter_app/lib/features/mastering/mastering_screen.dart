import 'package:flutter/material.dart';
import 'package:uuid/uuid.dart';
import 'package:just_audio/just_audio.dart';
import 'dart:io';
import 'package:loopingvid/core/services/file_picker_service.dart';
import 'package:loopingvid/core/services/ffmpeg_service.dart';
import 'package:loopingvid/core/database/database_helper.dart';
import 'package:loopingvid/core/database/models.dart';

class MasteringScreen extends StatefulWidget {
  const MasteringScreen({super.key});

  @override
  State<MasteringScreen> createState() => _MasteringScreenState();
}

class _MasteringScreenState extends State<MasteringScreen> {
  final _filePicker = FilePickerService();
  final _ffmpeg = FFmpegService();
  final _db = DatabaseHelper();
  final _audioPlayer = AudioPlayer();

  String? _selectedAudioPath;
  String? _audioFileName;
  Duration? _audioDuration;
  String _preset = 'Neutral';
  double _band1 = 0; // 60Hz
  double _band2 = 0; // 250Hz
  double _band3 = 0; // 1kHz
  double _band4 = 0; // 4kHz
  double _band5 = 0; // 12kHz
  double _compressorThreshold = -20;
  double _compressorRatio = 4;
  double _targetLufs = -14;
  bool _enableNoiseReduction = false;
  String _outputFormat = 'WAV';
  bool _isProcessing = false;
  double _processProgress = 0.0;
  String? _outputPath;
  String? _errorMessage;
  bool _isPlaying = false;

  final List<String> _presets = [
    'Neutral',
    'Clear Vocal',
    'Deep Bass',
    'Bright Pop',
    'Warm Jazz',
  ];

  final List<String> _outputFormats = ['WAV', 'MP3', 'M4A', 'FLAC'];

  @override
  void dispose() {
    _audioPlayer.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          _buildAudioSourceCard(),
          const SizedBox(height: 16),
          _buildPresetCard(),
          const SizedBox(height: 16),
          _buildEqualizerCard(),
          const SizedBox(height: 16),
          _buildDynamicsCard(),
          const SizedBox(height: 16),
          _buildOutputCard(),
          const SizedBox(height: 16),
          if (_isProcessing) _buildProgressCard(),
          if (!_isProcessing) _buildExportButton(),
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

  Widget _buildAudioSourceCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Audio Source',
                style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 12),
            if (_selectedAudioPath == null)
              OutlinedButton.icon(
                onPressed: _pickAudio,
                icon: const Icon(Icons.audio_file),
                label: const Text('Select Audio File'),
              )
            else ...[
              ListTile(
                leading: const Icon(Icons.music_note),
                title: Text(_audioFileName ?? 'Audio file'),
                subtitle: Text(
                    'Duration: ${_formatDuration(_audioDuration)}'),
                trailing: IconButton(
                  icon: const Icon(Icons.close),
                  onPressed: _clearAudio,
                ),
              ),
              // Playback controls
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  IconButton(
                    icon: Icon(
                        _isPlaying ? Icons.pause_circle : Icons.play_circle,
                        size: 40),
                    onPressed: _togglePlayback,
                  ),
                  IconButton(
                    icon: const Icon(Icons.stop_circle, size: 40),
                    onPressed: _stopPlayback,
                  ),
                  const SizedBox(width: 16),
                  TextButton.icon(
                    icon: const Icon(Icons.swap_horiz),
                    label: const Text('Change'),
                    onPressed: _pickAudio,
                  ),
                ],
              ),
              // Seek bar
              StreamBuilder<Duration>(
                stream: _audioPlayer.positionStream,
                builder: (context, snapshot) {
                  final position = snapshot.data ?? Duration.zero;
                  final total = _audioDuration ?? Duration.zero;
                  return Column(
                    children: [
                      Slider(
                        value: total.inMilliseconds > 0
                            ? (position.inMilliseconds /
                                    total.inMilliseconds)
                                .clamp(0.0, 1.0)
                            : 0.0,
                        onChanged: (v) {
                          final newPos = Duration(
                              milliseconds:
                                  (v * total.inMilliseconds).toInt());
                          _audioPlayer.seek(newPos);
                        },
                      ),
                      Padding(
                        padding:
                            const EdgeInsets.symmetric(horizontal: 16),
                        child: Row(
                          mainAxisAlignment:
                              MainAxisAlignment.spaceBetween,
                          children: [
                            Text(_formatDuration(position),
                                style: Theme.of(context)
                                    .textTheme
                                    .bodySmall),
                            Text(_formatDuration(total),
                                style: Theme.of(context)
                                    .textTheme
                                    .bodySmall),
                          ],
                        ),
                      ),
                    ],
                  );
                },
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildPresetCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Mastering Preset',
                style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 12),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: _presets.map((preset) {
                return ChoiceChip(
                  label: Text(preset),
                  selected: _preset == preset,
                  onSelected: _isProcessing
                      ? null
                      : (selected) {
                          if (selected) {
                            setState(() {
                              _preset = preset;
                              _applyPreset(preset);
                            });
                          }
                        },
                );
              }).toList(),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildEqualizerCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('5-Band Equalizer',
                style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 16),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                _buildEqBand('60Hz', _band1,
                    (v) => setState(() => _band1 = v)),
                _buildEqBand('250Hz', _band2,
                    (v) => setState(() => _band2 = v)),
                _buildEqBand('1kHz', _band3,
                    (v) => setState(() => _band3 = v)),
                _buildEqBand('4kHz', _band4,
                    (v) => setState(() => _band4 = v)),
                _buildEqBand('12kHz', _band5,
                    (v) => setState(() => _band5 = v)),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildEqBand(
      String label, double value, ValueChanged<double> onChanged) {
    return Column(
      children: [
        Text('${value.toInt()}dB',
            style: Theme.of(context).textTheme.bodySmall),
        RotatedBox(
          quarterTurns: 3,
          child: SizedBox(
            width: 100,
            child: Slider(
              value: value,
              min: -12,
              max: 12,
              divisions: 24,
              onChanged: _isProcessing ? null : onChanged,
            ),
          ),
        ),
        Text(label, style: Theme.of(context).textTheme.bodySmall),
      ],
    );
  }

  Widget _buildDynamicsCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Dynamics Compressor',
                style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 12),
            Text('Threshold: ${_compressorThreshold.toInt()} dB'),
            Slider(
              value: _compressorThreshold,
              min: -60,
              max: 0,
              divisions: 60,
              onChanged: _isProcessing
                  ? null
                  : (v) => setState(() => _compressorThreshold = v),
            ),
            Text('Ratio: ${_compressorRatio.toStringAsFixed(1)}:1'),
            Slider(
              value: _compressorRatio,
              min: 1,
              max: 20,
              divisions: 38,
              onChanged: _isProcessing
                  ? null
                  : (v) => setState(() => _compressorRatio = v),
            ),
            const Divider(),
            Text('Target LUFS: ${_targetLufs.toInt()} LUFS'),
            Slider(
              value: _targetLufs,
              min: -24,
              max: -6,
              divisions: 18,
              onChanged: _isProcessing
                  ? null
                  : (v) => setState(() => _targetLufs = v),
            ),
            SwitchListTile(
              title: const Text('Noise Reduction'),
              subtitle: const Text(
                  'Remove background noise (adds processing time)'),
              value: _enableNoiseReduction,
              onChanged: _isProcessing
                  ? null
                  : (v) => setState(() => _enableNoiseReduction = v),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildOutputCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Output',
                style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 12),
            DropdownButtonFormField<String>(
              decoration: const InputDecoration(
                labelText: 'Format',
                border: OutlineInputBorder(),
              ),
              initialValue: _outputFormat,
              items: _outputFormats
                  .map((f) => DropdownMenuItem(value: f, child: Text(f)))
                  .toList(),
              onChanged: _isProcessing
                  ? null
                  : (v) {
                      if (v != null) setState(() => _outputFormat = v);
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
            LinearProgressIndicator(value: _processProgress),
            const SizedBox(height: 8),
            Text('Mastering... ${(_processProgress * 100).toInt()}%'),
            const SizedBox(height: 8),
            OutlinedButton(
              onPressed: () async {
                await _ffmpeg.cancelAll();
                setState(() {
                  _isProcessing = false;
                  _processProgress = 0.0;
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
      onPressed:
          _selectedAudioPath != null && !_isProcessing ? _startMastering : null,
      icon: const Icon(Icons.audiotrack),
      label: const Text('Export Master'),
    );
  }

  Widget _buildSuccessCard() {
    return Card(
      color: Colors.green.shade50,
      child: ListTile(
        leading: const Icon(Icons.check_circle, color: Colors.green),
        title: const Text('Mastering Complete!'),
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
        title: const Text('Mastering Failed'),
        subtitle: Text(_errorMessage ?? 'Unknown error'),
      ),
    );
  }

  // === ACTIONS ===

  Future<void> _pickAudio() async {
    final path = await _filePicker.pickAudio();
    if (path == null) return;

    setState(() {
      _selectedAudioPath = path;
      _audioFileName = path.split('/').last;
      _outputPath = null;
      _errorMessage = null;
    });

    // Load audio for preview playback
    try {
      final duration = await _audioPlayer.setFilePath(path);
      setState(() => _audioDuration = duration);
    } catch (e) {
      // File might not be playable by just_audio but FFmpeg can still process it
      final durationMs = await _ffmpeg.getMediaDuration(path);
      setState(
          () => _audioDuration = Duration(milliseconds: durationMs));
    }
  }

  void _clearAudio() {
    _audioPlayer.stop();
    setState(() {
      _selectedAudioPath = null;
      _audioFileName = null;
      _audioDuration = null;
      _isPlaying = false;
      _outputPath = null;
      _errorMessage = null;
    });
  }

  Future<void> _togglePlayback() async {
    if (_isPlaying) {
      await _audioPlayer.pause();
    } else {
      await _audioPlayer.play();
    }
    setState(() => _isPlaying = !_isPlaying);
  }

  Future<void> _stopPlayback() async {
    await _audioPlayer.stop();
    await _audioPlayer.seek(Duration.zero);
    setState(() => _isPlaying = false);
  }

  Future<void> _playOutput() async {
    if (_outputPath == null) return;
    await _audioPlayer.setFilePath(_outputPath!);
    await _audioPlayer.play();
    setState(() => _isPlaying = true);
  }

  void _applyPreset(String preset) {
    switch (preset) {
      case 'Deep Bass':
        _band1 = 8; _band2 = 4; _band3 = 0; _band4 = -2; _band5 = -4;
        _compressorThreshold = -18; _compressorRatio = 3;
        break;
      case 'Clear Vocal':
        _band1 = -2; _band2 = 0; _band3 = 4; _band4 = 6; _band5 = 2;
        _compressorThreshold = -24; _compressorRatio = 5;
        break;
      case 'Bright Pop':
        _band1 = 0; _band2 = 2; _band3 = 4; _band4 = 6; _band5 = 8;
        _compressorThreshold = -20; _compressorRatio = 4;
        break;
      case 'Warm Jazz':
        _band1 = 4; _band2 = 6; _band3 = 2; _band4 = 0; _band5 = -2;
        _compressorThreshold = -16; _compressorRatio = 2.5;
        break;
      default: // Neutral
        _band1 = 0; _band2 = 0; _band3 = 0; _band4 = 0; _band5 = 0;
        _compressorThreshold = -20; _compressorRatio = 4;
    }
  }

  Future<void> _startMastering() async {
    if (_selectedAudioPath == null) return;

    // Stop playback before processing
    await _stopPlayback();

    setState(() {
      _isProcessing = true;
      _processProgress = 0.0;
      _outputPath = null;
      _errorMessage = null;
    });

    final jobId = const Uuid().v4();
    final job = RenderJob(
      id: jobId,
      title: 'Master: $_audioFileName',
      type: 'mastering',
      inputPath: _selectedAudioPath!,
      status: 'running',
      createdAt: DateTime.now(),
      settings:
          '{"preset":"$_preset","format":"$_outputFormat",'
          '"lufs":$_targetLufs,"noise_reduction":$_enableNoiseReduction}',
    );
    await _db.insertRenderJob(job.toMap());

    try {
      final result = await _ffmpeg.masterAudio(
        inputPath: _selectedAudioPath!,
        eqBands: [_band1, _band2, _band3, _band4, _band5],
        compressorThreshold: _compressorThreshold,
        compressorRatio: _compressorRatio,
        targetLufs: _targetLufs,
        noiseReduction: _enableNoiseReduction,
        outputFormat: _outputFormat.toLowerCase(),
        onProgress: (p) {
          if (mounted) setState(() => _processProgress = p);
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
            _isProcessing = false;
            _processProgress = 1.0;
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
            _errorMessage = 'Mastering failed. Check input file format.';
            _isProcessing = false;
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
          _isProcessing = false;
        });
      }
    }
  }

  String _formatDuration(Duration? duration) {
    if (duration == null) return '--:--';
    final m = duration.inMinutes.remainder(60).toString().padLeft(2, '0');
    final s = duration.inSeconds.remainder(60).toString().padLeft(2, '0');
    if (duration.inHours > 0) {
      return '${duration.inHours}:$m:$s';
    }
    return '$m:$s';
  }
}
