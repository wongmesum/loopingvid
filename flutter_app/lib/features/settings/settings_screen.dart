import 'package:flutter/material.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  String _outputDirectory = '/storage/emulated/0/LoopingVid/output';
  bool _highQualityPreview = true;
  bool _firestoreSync = false;
  final TextEditingController _geminiKeyController = TextEditingController();

  @override
  void dispose() {
    _geminiKeyController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Settings'),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Storage Section
          _buildSectionHeader('Storage'),
          Card(
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
              ],
            ),
          ),
          const SizedBox(height: 16),

          // Preview Section
          _buildSectionHeader('Preview'),
          Card(
            child: Column(
              children: [
                SwitchListTile(
                  secondary: const Icon(Icons.high_quality),
                  title: const Text('High-Quality Preview'),
                  subtitle: const Text('Uses more memory'),
                  value: _highQualityPreview,
                  onChanged: (v) =>
                      setState(() => _highQualityPreview = v),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // Cloud Sync Section
          _buildSectionHeader('Cloud Sync'),
          Card(
            child: Column(
              children: [
                SwitchListTile(
                  secondary: const Icon(Icons.cloud_sync),
                  title: const Text('Firestore Sync'),
                  subtitle:
                      const Text('Sync job history to cloud'),
                  value: _firestoreSync,
                  onChanged: (v) =>
                      setState(() => _firestoreSync = v),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // API Keys Section
          _buildSectionHeader('API Keys'),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: TextField(
                controller: _geminiKeyController,
                obscureText: true,
                decoration: const InputDecoration(
                  labelText: 'Gemini API Key',
                  border: OutlineInputBorder(),
                  prefixIcon: Icon(Icons.key),
                  helperText: 'Used for AI caption generation',
                ),
              ),
            ),
          ),
          const SizedBox(height: 16),

          // System Info Section
          _buildSectionHeader('System Info'),
          Card(
            child: Column(
              children: [
                const ListTile(
                  leading: Icon(Icons.phone_android),
                  title: Text('Platform'),
                  subtitle: Text('Android'),
                ),
                const ListTile(
                  leading: Icon(Icons.memory),
                  title: Text('Architecture'),
                  subtitle: Text('arm64-v8a'),
                ),
                ListTile(
                  leading: const Icon(Icons.info_outline),
                  title: const Text('App Version'),
                  subtitle: const Text('1.0.0 (Flutter)'),
                  trailing: OutlinedButton(
                    onPressed: () {},
                    child: const Text('Check Update'),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // About Section
          Card(
            child: Column(
              children: [
                ListTile(
                  leading: const Icon(Icons.privacy_tip),
                  title: const Text('Privacy Policy'),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () {},
                ),
                ListTile(
                  leading: const Icon(Icons.favorite),
                  title: const Text('Support Project'),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () {},
                ),
              ],
            ),
          ),
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

  void _changeOutputDir() {
    showDialog(
      context: context,
      builder: (context) {
        final controller =
            TextEditingController(text: _outputDirectory);
        return AlertDialog(
          title: const Text('Output Directory'),
          content: TextField(
            controller: controller,
            decoration: const InputDecoration(
              border: OutlineInputBorder(),
              labelText: 'Path',
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('Cancel'),
            ),
            FilledButton(
              onPressed: () {
                setState(() => _outputDirectory = controller.text);
                Navigator.pop(context);
              },
              child: const Text('Save'),
            ),
          ],
        );
      },
    );
  }
}
