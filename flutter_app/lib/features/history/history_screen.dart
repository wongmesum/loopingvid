import 'package:flutter/material.dart';

enum HistoryFilter { all, renders, liveStreams }

class HistoryScreen extends StatefulWidget {
  const HistoryScreen({super.key});

  @override
  State<HistoryScreen> createState() => _HistoryScreenState();
}

class _HistoryScreenState extends State<HistoryScreen> {
  HistoryFilter _filter = HistoryFilter.all;

  final List<Map<String, dynamic>> _historyItems = [
    {
      'type': 'render',
      'title': 'Loop: sunset_timelapse.mp4',
      'subtitle': 'Normal loop | 120s | 1080p',
      'date': '2024-12-15 14:30',
      'status': 'completed',
    },
    {
      'type': 'live',
      'title': 'YouTube Stream',
      'subtitle': 'Duration: 4h 32m | Loops: 54',
      'date': '2024-12-14 20:00',
      'status': 'completed',
    },
    {
      'type': 'render',
      'title': 'Master: podcast_ep12.wav',
      'subtitle': 'Clear Vocal preset | WAV export',
      'date': '2024-12-14 10:15',
      'status': 'completed',
    },
    {
      'type': 'render',
      'title': 'Editor: promo_video.mp4',
      'subtitle': 'Text overlay + Spectrum | 1080p',
      'date': '2024-12-13 16:45',
      'status': 'failed',
    },
    {
      'type': 'live',
      'title': 'TikTok Stream',
      'subtitle': 'Duration: 1h 15m | Loops: 12',
      'date': '2024-12-12 21:00',
      'status': 'completed',
    },
  ];

  @override
  Widget build(BuildContext context) {
    final filteredItems = _historyItems.where((item) {
      switch (_filter) {
        case HistoryFilter.all:
          return true;
        case HistoryFilter.renders:
          return item['type'] == 'render';
        case HistoryFilter.liveStreams:
          return item['type'] == 'live';
      }
    }).toList();

    return Column(
      children: [
        // Filter Chips
        Padding(
          padding: const EdgeInsets.all(16),
          child: SegmentedButton<HistoryFilter>(
            segments: const [
              ButtonSegment(value: HistoryFilter.all, label: Text('All')),
              ButtonSegment(
                  value: HistoryFilter.renders, label: Text('Renders')),
              ButtonSegment(
                  value: HistoryFilter.liveStreams,
                  label: Text('Live')),
            ],
            selected: {_filter},
            onSelectionChanged: (s) =>
                setState(() => _filter = s.first),
          ),
        ),

        // History List
        Expanded(
          child: filteredItems.isEmpty
              ? const Center(child: Text('No history items'))
              : ListView.builder(
                  itemCount: filteredItems.length,
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  itemBuilder: (context, index) {
                    final item = filteredItems[index];
                    return _buildHistoryCard(item);
                  },
                ),
        ),
      ],
    );
  }

  Widget _buildHistoryCard(Map<String, dynamic> item) {
    final isLive = item['type'] == 'live';
    final isFailed = item['status'] == 'failed';

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: isLive
              ? Colors.red.shade100
              : Colors.purple.shade100,
          child: Icon(
            isLive ? Icons.live_tv : Icons.movie,
            color: isLive ? Colors.red : Colors.purple,
          ),
        ),
        title: Text(item['title'] as String),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(item['subtitle'] as String),
            const SizedBox(height: 4),
            Row(
              children: [
                Icon(
                  isFailed ? Icons.error_outline : Icons.check_circle,
                  size: 14,
                  color: isFailed ? Colors.red : Colors.green,
                ),
                const SizedBox(width: 4),
                Text(
                  item['date'] as String,
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ],
            ),
          ],
        ),
        trailing: PopupMenuButton(
          itemBuilder: (context) => [
            if (isLive)
              const PopupMenuItem(
                value: 'go_live',
                child: Text('Go Live Again'),
              ),
            const PopupMenuItem(
              value: 'delete',
              child: Text('Delete'),
            ),
          ],
          onSelected: (value) {
            if (value == 'delete') {
              setState(() => _historyItems.remove(item));
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Item deleted')),
              );
            }
          },
        ),
        isThreeLine: true,
      ),
    );
  }
}
