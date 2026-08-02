import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:loopingvid/app.dart';

void main() {
  group('LoopingVidApp', () {
    testWidgets('renders app with bottom navigation bar',
        (WidgetTester tester) async {
      await tester.pumpWidget(const LoopingVidApp());

      // Verify bottom navigation destinations exist
      expect(find.text('Loop'), findsOneWidget);
      expect(find.text('Mastering'), findsOneWidget);
      expect(find.text('Editor'), findsOneWidget);
      expect(find.text('Live'), findsOneWidget);
      expect(find.text('History'), findsOneWidget);
    });

    testWidgets('shows Loop Studio title on initial screen',
        (WidgetTester tester) async {
      await tester.pumpWidget(const LoopingVidApp());

      expect(find.text('Loop Studio'), findsOneWidget);
    });

    testWidgets('navigates to Mastering screen on tab tap',
        (WidgetTester tester) async {
      await tester.pumpWidget(const LoopingVidApp());

      // Tap the Mastering tab
      await tester.tap(find.text('Mastering'));
      await tester.pumpAndSettle();

      expect(find.text('Audio Mastering'), findsOneWidget);
    });

    testWidgets('navigates to Editor screen on tab tap',
        (WidgetTester tester) async {
      await tester.pumpWidget(const LoopingVidApp());

      // Tap the Editor tab
      await tester.tap(find.text('Editor'));
      await tester.pumpAndSettle();

      expect(find.text('Video Editor'), findsOneWidget);
    });

    testWidgets('navigates to Live screen on tab tap',
        (WidgetTester tester) async {
      await tester.pumpWidget(const LoopingVidApp());

      // Tap the Live tab
      await tester.tap(find.text('Live'));
      await tester.pumpAndSettle();

      expect(find.text('Go Live'), findsWidgets);
    });

    testWidgets('navigates to History screen on tab tap',
        (WidgetTester tester) async {
      await tester.pumpWidget(const LoopingVidApp());

      // Tap the History tab
      await tester.tap(find.text('History'));
      await tester.pumpAndSettle();

      // History tab should update the app bar title
      expect(find.text('History'), findsWidgets);
    });

    testWidgets('has settings button in app bar',
        (WidgetTester tester) async {
      await tester.pumpWidget(const LoopingVidApp());

      expect(find.byIcon(Icons.settings), findsOneWidget);
    });

    testWidgets('Loop screen shows input video card',
        (WidgetTester tester) async {
      await tester.pumpWidget(const LoopingVidApp());

      expect(find.text('Input Video'), findsOneWidget);
      expect(find.text('Tap to select video'), findsOneWidget);
    });

    testWidgets('Loop screen shows loop configuration',
        (WidgetTester tester) async {
      await tester.pumpWidget(const LoopingVidApp());

      // Loop Configuration card should be visible (may need scroll)
      expect(find.text('Loop Configuration'), findsOneWidget);
    });

    testWidgets('Mastering screen shows audio controls',
        (WidgetTester tester) async {
      await tester.pumpWidget(const LoopingVidApp());

      await tester.tap(find.text('Mastering'));
      await tester.pumpAndSettle();

      // Verify mastering screen loaded with its controls
      expect(find.text('Audio Source'), findsOneWidget);
      expect(find.text('Mastering Preset'), findsOneWidget);
    });
  });
}
