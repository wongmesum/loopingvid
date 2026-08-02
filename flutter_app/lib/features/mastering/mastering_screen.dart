import 'package:flutter/material.dart';

class MasteringScreen extends StatefulWidget {
  const MasteringScreen({super.key});

  @override
  State<MasteringScreen> createState() => _MasteringScreenState();
}

class _MasteringScreenState extends State<MasteringScreen> {
  String? _selectedAudioPath;
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

  final List<String> _presets = [
    'Neutral',
    'Clear Vocal',
    'Deep Bass',
    'Bright Pop',
    'Warm Jazz',
  ];

  final List<String> _outputFormats = ['WAV', 'MP3', 'M4A', 'FLAC'];

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Audio Input
          Card(
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
                      title: const Text('Audio selected'),
                      subtitle: const Text('Duration: 3:45'),
                      trailing: IconButton(
                        icon: const Icon(Icons.close),
                        onPressed: () =>
                            setState(() => _selectedAudioPath = null),
                      ),
                    ),
                    // Waveform visualization placeholder
                    Container(
                      height: 80,
                      decoration: BoxDecoration(
                        color: Theme.of(context)
                            .colorScheme
                            .surfaceContainerHighest,
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: const Center(
                        child: Icon(Icons.graphic_eq, size: 40),
                      ),
                    ),
                  ],
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),

          // Preset Selector
          Card(
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
                    children: _presets.map((preset) {
                      return ChoiceChip(
                        label: Text(preset),
                        selected: _preset == preset,
                        onSelected: (selected) {
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
          ),
          const SizedBox(height: 16),

          // 5-Band EQ
          Card(
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
          ),
          const SizedBox(height: 16),

          // Dynamics
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Dynamics Compressor',
                      style: Theme.of(context).textTheme.titleMedium),
                  const SizedBox(height: 12),
                  Text(
                      'Threshold: ${_compressorThreshold.toInt()} dB'),
                  Slider(
                    value: _compressorThreshold,
                    min: -60,
                    max: 0,
                    onChanged: (v) =>
                        setState(() => _compressorThreshold = v),
                  ),
                  Text('Ratio: ${_compressorRatio.toStringAsFixed(1)}:1'),
                  Slider(
                    value: _compressorRatio,
                    min: 1,
                    max: 20,
                    onChanged: (v) =>
                        setState(() => _compressorRatio = v),
                  ),
                  const Divider(),
                  Text('Target LUFS: ${_targetLufs.toInt()} LUFS'),
                  Slider(
                    value: _targetLufs,
                    min: -24,
                    max: -6,
                    onChanged: (v) => setState(() => _targetLufs = v),
                  ),
                  SwitchListTile(
                    title: const Text('Noise Reduction'),
                    value: _enableNoiseReduction,
                    onChanged: (v) =>
                        setState(() => _enableNoiseReduction = v),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),

          // Output Format & Export
          Card(
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
                    value: _outputFormat,
                    items: _outputFormats
                        .map((f) =>
                            DropdownMenuItem(value: f, child: Text(f)))
                        .toList(),
                    onChanged: (v) {
                      if (v != null) setState(() => _outputFormat = v);
                    },
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),

          // Export Button
          FilledButton.icon(
            onPressed:
                _selectedAudioPath != null && !_isProcessing
                    ? _startMastering
                    : null,
            icon: _isProcessing
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(
                        strokeWidth: 2, color: Colors.white),
                  )
                : const Icon(Icons.audiotrack),
            label: Text(_isProcessing ? 'Processing...' : 'Export Master'),
          ),
        ],
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
              onChanged: onChanged,
            ),
          ),
        ),
        Text(label, style: Theme.of(context).textTheme.bodySmall),
      ],
    );
  }

  void _pickAudio() {
    setState(() {
      _selectedAudioPath = '/storage/emulated/0/Music/sample.wav';
    });
  }

  void _applyPreset(String preset) {
    switch (preset) {
      case 'Deep Bass':
        _band1 = 8;
        _band2 = 4;
        _band3 = 0;
        _band4 = -2;
        _band5 = -4;
        break;
      case 'Clear Vocal':
        _band1 = -2;
        _band2 = 0;
        _band3 = 4;
        _band4 = 6;
        _band5 = 2;
        break;
      case 'Bright Pop':
        _band1 = 0;
        _band2 = 2;
        _band3 = 4;
        _band4 = 6;
        _band5 = 8;
        break;
      case 'Warm Jazz':
        _band1 = 4;
        _band2 = 6;
        _band3 = 2;
        _band4 = 0;
        _band5 = -2;
        break;
      default:
        _band1 = 0;
        _band2 = 0;
        _band3 = 0;
        _band4 = 0;
        _band5 = 0;
    }
  }

  Future<void> _startMastering() async {
    setState(() => _isProcessing = true);
    await Future.delayed(const Duration(seconds: 2));
    if (!mounted) return;
    setState(() => _isProcessing = false);
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
          content: Text('Mastered audio exported as $_outputFormat!')),
    );
  }
}
