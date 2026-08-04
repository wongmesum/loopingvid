import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:loopingvid/core/database/database_helper.dart';
import 'package:loopingvid/core/database/models.dart';

enum HistoryFilter { all, renders, liveStreams }

class HistoryScreen extends StatefulWidget {
  const HistoryScreen({super.key});

  @override
  State<HistoryScreen> createState() => _HistoryScreenState();
}

class _HistoryScreenState extends State<HistoryScreen> {
  final _db = DatabaseHelper();

  HistoryFilter _filter = HistoryFilter.all;
  List<RenderJob> _renderJobs = [];
  List<LiveSession> _liveSessions = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadHistory();
  }

  Future<void> _loadHistory() async {
    setState(() => _isLoading = true);

    final renderMaps = await _db.getRenderJobs();
    final liveMaps = await _db.getLiveSessions();

    setState(() {
      _renderJobs = renderMaps.map((m) => RenderJob.fromMap(m)).toList();
      _liveSessions = liveMaps.map((m) => LiveSession.fromMap(m)).toList();
      _isLoading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // Filter Chips
        Padding(
          padding: const EdgeInsets.all(16),
          child: SegmentedButton<HistoryFilter>(
            segments: [
              ButtonSegment(
                value: HistoryFilter.all,
                label: Text('All (${_renderJobs.length + _liveSessions.length})'),
              ),
              ButtonSegment(
                value: HistoryFilter.renders,
                label: Text('Renders (${_renderJobs.length})'),
              ),
              ButtonSegment(
                value: HistoryFilter.liveStreams,
                label: Text('Live (${_liveSessions.length})'),
              ),
            ],
            selected: {_filter},
            onSelectionChanged: (s) => setState(() => _filter = s.first),
          ),
        ),

        // Content
        Expanded(
          child: _isLoading
              ? const Center(child: CircularProgressIndicator())
              : RefreshIndicator(
                  onRefresh: _loadHistory,
                  child: _buildList(),
                ),
        ),
      ],
    );
  }

  Widget _buildList() {
    final items = _getFilteredItems();

    if (items.isEmpty) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.history, size: 64,
                color: Theme.of(context).colorScheme.outline),
            const SizedBox(height: 16),
            Text('No history yet',
                style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 8),
            Text('Completed renders and live sessions will appear here.',
                style: Theme.of(context).textTheme.bodySmall),
          ],
        ),
      );
    }

    return ListView.builder(
      itemCount: items.length,
      padding: const EdgeInsets.symmetric(horizontal: 16),
      itemBuilder: (context, index) => items[index],
    );
  }

  List<Widget> _getFilteredItems() {
    final List<Widget> items = [];
    final dateFormat = DateFormat('MMM d, yyyy HH:mm');

    // Add render jobs
    if (_filter == HistoryFilter.all || _filter == HistoryFilter.renders) {
      for (final job in _renderJobs) {
        items.add(_RenderJobCard(
          job: job,
          dateFormat: dateFormat,
          onDelete: () => _deleteRenderJob(job.id),
        ));
      }
    }

    // Add live sessions
    if (_filter == HistoryFilter.all || _filter == HistoryFilter.liveStreams) {
      for (final session in _liveSessions) {
        items.add(_LiveSessionCard(
          session: session,
          dateFormat: dateFormat,
          onDelete: () => _deleteLiveSession(session.id),
        ));
      }
    }

    return items;
  }

  Future<void> _deleteRenderJob(String id) async {
    final confirmed = await _showDeleteConfirmation();
    if (!confirmed) return;

    await _db.deleteRenderJob(id);
    await _loadHistory();

    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Render job deleted')),
      );
    }
  }

  Future<void> _deleteLiveSession(String id) async {
    final confirmed = await _showDeleteConfirmation();
    if (!confirmed) return;

    await _db.deleteLiveSession(id);
    await _loadHistory();

    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Live session deleted')),
      );
    }
  }

  Future<bool> _showDeleteConfirmation() async {
    return await showDialog<bool>(
          context: context,
          builder: (context) => AlertDialog(
            title: const Text('Delete'),
            content:
                const Text('Are you sure you want to delete this item?'),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context, false),
                child: const Text('Cancel'),
              ),
              FilledButton(
                onPressed: () => Navigator.pop(context, true),
                child: const Text('Delete'),
              ),
            ],
          ),
        ) ??
        false;
  }
}

class _RenderJobCard extends StatelessWidget {
  final RenderJob job;
  final DateFormat dateFormat;
  final VoidCallback onDelete;

  const _RenderJobCard({
    required this.job,
    required this.dateFormat,
    required this.onDelete,
  });

  @override
  Widget build(BuildContext context) {
    final statusColor = _getStatusColor(job.status);
    final typeIcon = _getTypeIcon(job.type);

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: statusColor.withAlpha(30),
          child: Icon(typeIcon, color: statusColor),
        ),
        title: Text(job.title, maxLines: 1, overflow: TextOverflow.ellipsis),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '${job.type.toUpperCase()} | ${job.status.toUpperCase()}',
              style: TextStyle(color: statusColor, fontSize: 12),
            ),
            const SizedBox(height: 2),
            Text(
              dateFormat.format(job.createdAt),
              style: Theme.of(context).textTheme.bodySmall,
            ),
            if (job.outputPath != null)
              Text(
                job.outputPath!.split('/').last,
                style: Theme.of(context).textTheme.bodySmall,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            if (job.errorMessage != null)
              Text(
                job.errorMessage!,
                style: TextStyle(
                    color: Colors.red.shade700, fontSize: 11),
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
          ],
        ),
        trailing: IconButton(
          icon: const Icon(Icons.delete_outline),
          onPressed: onDelete,
        ),
        isThreeLine: true,
      ),
    );
  }

  Color _getStatusColor(String status) {
    switch (status) {
      case 'completed':
        return Colors.green;
      case 'running':
        return Colors.blue;
      case 'failed':
        return Colors.red;
      case 'cancelled':
        return Colors.orange;
      default:
        return Colors.grey;
    }
  }

  IconData _getTypeIcon(String type) {
    switch (type) {
      case 'loop':
        return Icons.loop;
      case 'mastering':
        return Icons.equalizer;
      case 'editor':
        return Icons.movie_edit;
      default:
        return Icons.task;
    }
  }
}

class _LiveSessionCard extends StatelessWidget {
  final LiveSession session;
  final DateFormat dateFormat;
  final VoidCallback onDelete;

  const _LiveSessionCard({
    required this.session,
    required this.dateFormat,
    required this.onDelete,
  });

  @override
  Widget build(BuildContext context) {
    final statusColor = _getStatusColor(session.status);

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: Colors.red.shade50,
          child: const Icon(Icons.live_tv, color: Colors.red),
        ),
        title: Text(
          '${session.platform.toUpperCase()} Stream',
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Duration: ${session.formattedUptime} | '
              'Loops: ${session.loopCount}',
            ),
            Row(
              children: [
                Icon(
                  session.status == 'ended'
                      ? Icons.check_circle
                      : Icons.error_outline,
                  size: 14,
                  color: statusColor,
                ),
                const SizedBox(width: 4),
                Text(
                  session.startedAt != null
                      ? dateFormat.format(session.startedAt!)
                      : 'Unknown date',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ],
            ),
            Text(
              session.mediaPath.split('/').last,
              style: Theme.of(context).textTheme.bodySmall,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ],
        ),
        trailing: IconButton(
          icon: const Icon(Icons.delete_outline),
          onPressed: onDelete,
        ),
        isThreeLine: true,
      ),
    );
  }

  Color _getStatusColor(String status) {
    switch (status) {
      case 'ended':
        return Colors.green;
      case 'live':
        return Colors.red;
      case 'error':
        return Colors.orange;
      default:
        return Colors.grey;
    }
  }
}
