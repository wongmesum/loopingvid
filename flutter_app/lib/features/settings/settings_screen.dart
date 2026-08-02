import 'package:flutter/material.dart';
import 'package:loopingvid/core/services/settings_service.dart';
import 'package:loopingvid/core/services/file_picker_service.dart';
import 'package:loopingvid/core/database/database_helper.dart';
import 'package:loopingvid/core/utils/responsive.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  final _settings = SettingsService();
  final _filePicker = FilePickerService();
  final _db = DatabaseHelper();

  String _outputDirectory = '';
  bool _highQualityPreview = true;
  bool _firestoreSync = false;
  final TextEditingController _geminiKeyController = TextEditingController();
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadSettings();
  }

  @override
  void dispose() {
    _geminiKeyController.dispose();
    super.dispose();
  }

  Future<void> _loadSettings() async {
    final outputDir = await _settings.getOutputDirectory();
    final hqPreview = await _settings.getHighQualityPreview();
    final fsSync = await _settings.getFirestoreSync();
    final geminiKey = await _settings.getGeminiApiKey();

    setState(() {
      _outputDirectory = outputDir;
      _highQualityPreview = hqPreview;
      _firestoreSync = fsSync;
      _geminiKeyController.text = geminiKey;
      _isLoading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    final r = context.responsive;
    if (_isLoading) {
      return Scaffold(
        appBar: AppBar(title: const Text('Settings')),
        body: const Center(child: CircularProgressIndicator()),
      );
    }

    return Scaffold(
      appBar: AppBar(title: const Text('Settings')),
      body: ListView(
        padding: r.screenPadding,
        children: [
          _buildSectionHeader('Storage'),
          _buildStorageCard(),
          SizedBox(height: r.sectionGap),
          _buildSectionHeader('Preview'),
          _buildPreviewCard(),
          SizedBox(height: r.sectionGap),
          _buildSectionHeader('Cloud Sync'),
          _buildCloudCard(),
          SizedBox(height: r.sectionGap),
          _buildSectionHeader('API Keys'),
          _buildApiKeysCard(),
          SizedBox(height: r.sectionGap),
          _buildSectionHeader('Data Management'),
          _buildDataCard(),
          SizedBox(height: r.sectionGap),
          _buildSectionHeader('About'),
          _buildAboutCard(),
        ],
      ),
    );
  }

  Widget _buildSectionHeader(String title) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Text(
        title,
        style: Theme.of(context).textTheme.titleSmall?.copyWith(
              color: Theme.of(context).colorScheme.primary,
            ),
      ),
    );
  }

  Widget _buildStorageCard() {
    return Card(
      child: Column(
        children: [
          ListTile(
            leading: const Icon(Icons.folder),
            title: const Text('Output Directory'),
            subtitle: Text(_outputDirectory),
            trailing: IconButton(
              icon: const Icon(Icons.edit),
              onPressed: _changeOutputDir,
            ),
          ),
          ListTile(
            leading: const Icon(Icons.folder_open),
            title: const Text('Browse Directory'),
            subtitle: const Text('Select folder from file manager'),
            trailing: IconButton(
              icon: const Icon(Icons.folder_shared),
              onPressed: _browseOutputDir,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPreviewCard() {
    return Card(
      child: SwitchListTile(
        secondary: const Icon(Icons.high_quality),
        title: const Text('High-Quality Preview'),
        subtitle: const Text('Uses more memory but shows better quality'),
        value: _highQualityPreview,
        onChanged: (v) async {
          await _settings.setHighQualityPreview(v);
          setState(() => _highQualityPreview = v);
          _showSaved();
        },
      ),
    );
  }

  Widget _buildCloudCard() {
    return Card(
      child: SwitchListTile(
        secondary: const Icon(Icons.cloud_sync),
        title: const Text('Firestore Sync'),
        subtitle: const Text('Sync job history to cloud (requires setup)'),
        value: _firestoreSync,
        onChanged: (v) async {
          await _settings.setFirestoreSync(v);
          setState(() => _firestoreSync = v);
          _showSaved();
        },
      ),
    );
  }

  Widget _buildApiKeysCard() {
    final r = context.responsive;
    return Card(
      child: Padding(
        padding: r.cardContentPadding,
        child: Column(
          children: [
            TextField(
              controller: _geminiKeyController,
              obscureText: true,
              decoration: InputDecoration(
                labelText: 'Gemini API Key',
                border: const OutlineInputBorder(),
                prefixIcon: const Icon(Icons.key),
                helperText: 'Used for AI caption generation',
                suffixIcon: IconButton(
                  icon: const Icon(Icons.save),
                  tooltip: 'Save API key',
                  onPressed: _saveGeminiKey,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDataCard() {
    return Card(
      child: Column(
        children: [
          ListTile(
            leading: const Icon(Icons.delete_sweep),
            title: const Text('Clear All History'),
            subtitle: const Text('Delete all render jobs and live sessions'),
            trailing: FilledButton.tonal(
              onPressed: _clearHistory,
              child: const Text('Clear'),
            ),
          ),
          ListTile(
            leading: const Icon(Icons.restart_alt),
            title: const Text('Reset All Settings'),
            subtitle: const Text('Restore defaults'),
            trailing: FilledButton.tonal(
              onPressed: _resetSettings,
              child: const Text('Reset'),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildAboutCard() {
    return Card(
      child: Column(
        children: [
          const ListTile(
            leading: Icon(Icons.info_outline),
            title: Text('LoopingVid Flutter'),
            subtitle: Text('Version 1.0.0'),
          ),
          const ListTile(
            leading: Icon(Icons.phone_android),
            title: Text('Platform'),
            subtitle: Text('Android (Flutter)'),
          ),
          const ListTile(
            leading: Icon(Icons.code),
            title: Text('Engine'),
            subtitle: Text('FFmpeg Kit Full GPL 6.0'),
          ),
          ListTile(
            leading: const Icon(Icons.privacy_tip),
            title: const Text('Privacy Policy'),
            trailing: const Icon(Icons.chevron_right),
            onTap: _showPrivacyPolicy,
          ),
        ],
      ),
    );
  }

  // === ACTIONS ===

  Future<void> _changeOutputDir() async {
    final controller = TextEditingController(text: _outputDirectory);
    final result = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Output Directory'),
        content: TextField(
          controller: controller,
          decoration: const InputDecoration(
            border: OutlineInputBorder(),
            labelText: 'Path',
            hintText: '/storage/emulated/0/LoopingVid/output',
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, controller.text),
            child: const Text('Save'),
          ),
        ],
      ),
    );

    if (result != null && result.isNotEmpty) {
      await _settings.setOutputDirectory(result);
      setState(() => _outputDirectory = result);
      _showSaved();
    }
  }

  Future<void> _browseOutputDir() async {
    final path = await _filePicker.pickDirectory();
    if (path != null) {
      await _settings.setOutputDirectory(path);
      setState(() => _outputDirectory = path);
      _showSaved();
    }
  }

  Future<void> _saveGeminiKey() async {
    await _settings.setGeminiApiKey(_geminiKeyController.text);
    _showSaved();
  }

  Future<void> _clearHistory() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Clear All History'),
        content: const Text(
            'This will permanently delete all render jobs and live session '
            'records. This cannot be undone.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            style: FilledButton.styleFrom(backgroundColor: Colors.red),
            child: const Text('Clear All'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      await _db.clearAllData();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('All history cleared')),
        );
      }
    }
  }

  Future<void> _resetSettings() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Reset Settings'),
        content: const Text(
            'This will reset all settings to default values. '
            'Stream keys and API keys will be removed.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Reset'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      await _settings.clearAll();
      await _loadSettings();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Settings reset to defaults')),
        );
      }
    }
  }

  void _showPrivacyPolicy() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Privacy Policy'),
        content: const SingleChildScrollView(
          child: Text(
            'LoopingVid processes all media locally on your device. '
            'No video, audio, or personal data is sent to external servers '
            'unless you explicitly enable cloud sync or live streaming.\n\n'
            'Stream keys are stored locally on your device. '
            'RTMP streaming sends data only to the server you configure.\n\n'
            'The Gemini API key, if provided, is used solely for caption '
            'generation and is stored locally.',
          ),
        ),
        actions: [
          FilledButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('OK'),
          ),
        ],
      ),
    );
  }

  void _showSaved() {
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Saved'),
          duration: Duration(seconds: 1),
        ),
      );
    }
  }
}
