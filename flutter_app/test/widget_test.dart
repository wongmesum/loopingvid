import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:loopingvid/core/theme/app_theme.dart';

void main() {
  group('AppTheme', () {
    test('light theme uses Material 3', () {
      final theme = AppTheme.lightTheme;
      expect(theme.useMaterial3, isTrue);
      expect(theme.brightness, equals(Brightness.light));
    });

    test('dark theme uses Material 3', () {
      final theme = AppTheme.darkTheme;
      expect(theme.useMaterial3, isTrue);
      expect(theme.brightness, equals(Brightness.dark));
    });

    test('light theme has proper color scheme', () {
      final theme = AppTheme.lightTheme;
      expect(theme.colorScheme, isNotNull);
      expect(theme.colorScheme.brightness, equals(Brightness.light));
    });

    test('dark theme has proper color scheme', () {
      final theme = AppTheme.darkTheme;
      expect(theme.colorScheme, isNotNull);
      expect(theme.colorScheme.brightness, equals(Brightness.dark));
    });

    test('card theme has rounded corners', () {
      final theme = AppTheme.lightTheme;
      final cardShape = theme.cardTheme.shape as RoundedRectangleBorder;
      final radius =
          (cardShape.borderRadius as BorderRadius).topLeft.x;
      expect(radius, equals(16.0));
    });

    test('appBar is centered', () {
      final theme = AppTheme.lightTheme;
      expect(theme.appBarTheme.centerTitle, isTrue);
    });
  });

  group('Navigation Widget', () {
    testWidgets('MaterialApp renders without error',
        (WidgetTester tester) async {
      // Test a minimal MaterialApp with our theme to ensure it builds
      await tester.pumpWidget(
        MaterialApp(
          theme: AppTheme.lightTheme,
          home: const Scaffold(
            body: Center(child: Text('LoopingVid')),
          ),
        ),
      );

      expect(find.text('LoopingVid'), findsOneWidget);
    });

    testWidgets('NavigationBar renders 5 destinations',
        (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          theme: AppTheme.lightTheme,
          home: Scaffold(
            bottomNavigationBar: NavigationBar(
              selectedIndex: 0,
              onDestinationSelected: (_) {},
              destinations: const [
                NavigationDestination(
                    icon: Icon(Icons.loop), label: 'Loop'),
                NavigationDestination(
                    icon: Icon(Icons.equalizer), label: 'Mastering'),
                NavigationDestination(
                    icon: Icon(Icons.movie_edit), label: 'Editor'),
                NavigationDestination(
                    icon: Icon(Icons.live_tv), label: 'Live'),
                NavigationDestination(
                    icon: Icon(Icons.history), label: 'History'),
              ],
            ),
          ),
        ),
      );

      expect(find.text('Loop'), findsOneWidget);
      expect(find.text('Mastering'), findsOneWidget);
      expect(find.text('Editor'), findsOneWidget);
      expect(find.text('Live'), findsOneWidget);
      expect(find.text('History'), findsOneWidget);
    });

    testWidgets('Settings icon renders in AppBar',
        (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          theme: AppTheme.lightTheme,
          home: Scaffold(
            appBar: AppBar(
              title: const Text('Loop Studio'),
              actions: [
                IconButton(
                  icon: const Icon(Icons.settings),
                  onPressed: () {},
                ),
              ],
            ),
          ),
        ),
      );

      expect(find.byIcon(Icons.settings), findsOneWidget);
      expect(find.text('Loop Studio'), findsOneWidget);
    });

    testWidgets('NavigationBar responds to selection',
        (WidgetTester tester) async {
      int selectedIndex = 0;

      await tester.pumpWidget(
        MaterialApp(
          theme: AppTheme.lightTheme,
          home: StatefulBuilder(
            builder: (context, setState) {
              return Scaffold(
                body: Center(
                    child: Text('Screen $selectedIndex')),
                bottomNavigationBar: NavigationBar(
                  selectedIndex: selectedIndex,
                  onDestinationSelected: (i) {
                    setState(() => selectedIndex = i);
                  },
                  destinations: const [
                    NavigationDestination(
                        icon: Icon(Icons.loop), label: 'Loop'),
                    NavigationDestination(
                        icon: Icon(Icons.equalizer),
                        label: 'Mastering'),
                    NavigationDestination(
                        icon: Icon(Icons.movie_edit),
                        label: 'Editor'),
                    NavigationDestination(
                        icon: Icon(Icons.live_tv), label: 'Live'),
                    NavigationDestination(
                        icon: Icon(Icons.history),
                        label: 'History'),
                  ],
                ),
              );
            },
          ),
        ),
      );

      expect(find.text('Screen 0'), findsOneWidget);

      await tester.tap(find.text('Mastering'));
      await tester.pumpAndSettle();
      expect(find.text('Screen 1'), findsOneWidget);

      await tester.tap(find.text('Editor'));
      await tester.pumpAndSettle();
      expect(find.text('Screen 2'), findsOneWidget);

      await tester.tap(find.text('Live'));
      await tester.pumpAndSettle();
      expect(find.text('Screen 3'), findsOneWidget);

      await tester.tap(find.text('History'));
      await tester.pumpAndSettle();
      expect(find.text('Screen 4'), findsOneWidget);
    });
  });
}
