import 'package:file_picker/file_picker.dart';

/// Service for picking files from device storage
class FilePickerService {
  static final FilePickerService _instance = FilePickerService._internal();
  factory FilePickerService() => _instance;
  FilePickerService._internal();

  /// Pick a video file. Returns file path or null if cancelled.
  Future<String?> pickVideo() async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.video,
      allowMultiple: false,
    );
    return result?.files.single.path;
  }

  /// Pick an audio file. Returns file path or null if cancelled.
  Future<String?> pickAudio() async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.audio,
      allowMultiple: false,
    );
    return result?.files.single.path;
  }

  /// Pick an image file. Returns file path or null if cancelled.
  Future<String?> pickImage() async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.image,
      allowMultiple: false,
    );
    return result?.files.single.path;
  }

  /// Pick any media file (video, audio, image). Returns file path or null.
  Future<String?> pickMedia() async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.media,
      allowMultiple: false,
    );
    return result?.files.single.path;
  }

  /// Pick multiple video files. Returns list of paths.
  Future<List<String>> pickMultipleVideos() async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.video,
      allowMultiple: true,
    );
    if (result == null) return [];
    return result.files
        .where((f) => f.path != null)
        .map((f) => f.path!)
        .toList();
  }

  /// Pick a directory. Returns path or null.
  Future<String?> pickDirectory() async {
    return await FilePicker.platform.getDirectoryPath();
  }
}
