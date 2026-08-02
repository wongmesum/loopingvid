import 'package:flutter/material.dart';
import 'package:loopingvid/features/loop/loop_screen.dart';
import 'package:loopingvid/features/mastering/mastering_screen.dart';
import 'package:loopingvid/features/editor/editor_screen.dart';
import 'package:loopingvid/features/live/live_screen.dart';
import 'package:loopingvid/features/history/history_screen.dart';
import 'package:loopingvid/features/settings/settings_screen.dart';

class AppRouter extends StatefulWidget {
  const AppRouter({super.key});

  @override
  State<AppRouter> createState() => _AppRouterState();
}

class _AppRouterState extends State<AppRouter> {
  int _currentIndex = 0;

  final List<Widget> _screens = const [
    LoopScreen(),
    MasteringScreen(),
    EditorScreen(),
    LiveScreen(),
    HistoryScreen(),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_getTitle()),
        actions: [
          IconButton(
            icon: const Icon(Icons.settings),
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (context) => const SettingsScreen(),
                ),
              );
            },
          ),
        ],
      ),
      body: AnimatedSwitcher(
        duration: const Duration(milliseconds: 300),
        child: _screens[_currentIndex],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _currentIndex,
        onDestinationSelected: (index) {
          setState(() {
            _currentIndex = index;
          });
        },
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.loop),
            selectedIcon: Icon(Icons.loop, color: Colors.deepPurple),
            label: 'Loop',
          ),
          NavigationDestination(
            icon: Icon(Icons.equalizer),
            selectedIcon: Icon(Icons.equalizer, color: Colors.deepPurple),
            label: 'Mastering',
          ),
          NavigationDestination(
            icon: Icon(Icons.movie_edit),
            selectedIcon: Icon(Icons.movie_edit, color: Colors.deepPurple),
            label: 'Editor',
          ),
          NavigationDestination(
            icon: Icon(Icons.live_tv),
            selectedIcon: Icon(Icons.live_tv, color: Colors.deepPurple),
            label: 'Live',
          ),
          NavigationDestination(
            icon: Icon(Icons.history),
            selectedIcon: Icon(Icons.history, color: Colors.deepPurple),
            label: 'History',
          ),
        ],
      ),
    );
  }

  String _getTitle() {
    switch (_currentIndex) {
      case 0:
        return 'Loop Studio';
      case 1:
        return 'Audio Mastering';
      case 2:
        return 'Video Editor';
      case 3:
        return 'Go Live';
      case 4:
        return 'History';
      default:
        return 'LoopingVid';
    }
  }
}
