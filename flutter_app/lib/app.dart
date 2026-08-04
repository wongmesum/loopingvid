import 'package:flutter/material.dart';
import 'package:loopingvid/core/theme/app_theme.dart';
import 'package:loopingvid/core/navigation/app_router.dart';

class LoopingVidApp extends StatelessWidget {
  const LoopingVidApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'LoopingVid',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.lightTheme,
      darkTheme: AppTheme.darkTheme,
      themeMode: ThemeMode.system,
      home: const AppRouter(),
    );
  }
}
