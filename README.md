# TextEZ

<p align="center">
  <b>Mobile Text Editor with Incremental Version Control</b>
</p>

## Project Information

**University:** University of Colombo School of Computing  
**Course:** IS2205 – Mobile Application Design and Development  
**Project Type:** Mini Project  
**Application:** TextEZ  
**Platform:** Android  
**Language:** Kotlin  

TextEZ is a mobile text editor developed for Android. The application provides standard text-editing and file-management functions together with Kotlin and Markdown syntax highlighting, autosave and recovery, read-only protection, and an incremental document version-control mechanism.

The version-control functionality is implemented inside the TextEZ application and allows users to create, preview, compare, and restore previous versions of a document.

---

## Group Members

| Student | Student ID |
|---|---|
| RAHN Wijesekara | 24021202 |
| WSC de Silva | 24020222 |
| HSR Perera | 24020796 |

---

## Main Features

### File Management

- Create new text documents
- Open existing documents from Android storage
- Save documents
- Save As support
- Delete documents
- Recent file management
- UTF-8 file handling
- Android Storage Access Framework integration

### Text Editing

- Multi-line text editing
- Undo
- Redo
- Search
- Replace One
- Replace All
- Line count
- Character count
- Read-only mode

### Syntax Highlighting

TextEZ supports syntax highlighting for:

- Kotlin (`.kt`)
- Markdown (`.md`)

The application can also detect Kotlin and Markdown content while the user is typing an unsaved document.

### Autosave and Recovery

TextEZ provides a recovery mechanism for unsaved work.

When a document contains unsaved changes, recovery information is periodically stored in the application cache. If unsaved recovery data is detected when TextEZ is opened again, the user can choose to:

- Restore the unsaved content
- Discard the recovery data

### Incremental Version Control

TextEZ includes an in-app document version-control system.

The first version of a document is stored as a complete **base version**. Subsequent versions are stored as **delta patches** rather than complete duplicate documents.

```text
Version 1 -> Base Snapshot
Version 2 -> Delta Patch
Version 3 -> Delta Patch
Version 4 -> Delta Patch
```

The version-control system supports:

- Named document versions
- Version numbering
- Version timestamps
- Base and delta storage
- Version history
- Version reconstruction
- Version preview
- Line-based comparison
- Rollback to previous versions
- Automatic backup before rollback

TextEZ uses `java-diff-utils` to generate and apply unified line-based patches.

---

## Version Comparison

TextEZ can compare a selected historical version with the current document.

The comparison output uses the following notation:

```diff
  Unchanged line
- Removed line
+ Added line
```

This allows users to identify document changes without manually comparing complete files.

---

## Document Version Reconstruction

Delta versions are reconstructed by starting with the base version and sequentially applying the required patches.

```text
Base Version
     |
     v
Apply Delta 2
     |
     v
Apply Delta 3
     |
     v
Reconstructed Version
```

This approach reduces unnecessary duplication while maintaining access to previous document states.

---

## Storage Architecture

TextEZ uses several Android storage mechanisms for different purposes.

### User Documents

User documents are accessed using the Android Storage Access Framework and `ContentResolver`.

This allows users to create and open documents through Android's system document interface.

### Version Storage

Document version information is maintained separately by TextEZ.

Version storage contains:

- Base snapshot files
- Delta patch files
- Version metadata

### Recovery Storage

Temporary recovery information is stored in the application's cache.

### SharedPreferences

SharedPreferences are used for lightweight application state such as:

- Recent file references
- Read-only document state

---

## Project Structure

The main project components are organized approximately as follows:

```text
com.example.textez
|
+-- activities
|   +-- MainActivity
|   +-- EditorActivity
|   +-- OpenFileActivity
|   +-- VersionHistoryActivity
|
+-- adapters
|   +-- VersionAdapter
|
+-- managers
|   +-- LanguageDetector
|   +-- KotlinSyntaxHighlighter
|   +-- MarkdownSyntaxHighlighter
|   +-- LineDiffManager
|   +-- VersionManager
|
+-- models
|   +-- Version
|
+-- storage
    +-- AutoSaveManager
    +-- RecentFilesManager
```

---

## Technologies Used

- Kotlin
- Android SDK
- Android Studio
- XML layouts
- AndroidX
- Material Components
- RecyclerView
- SharedPreferences
- Android Storage Access Framework
- `java-diff-utils`
- Gradle

### Android Configuration

```text
minSdk: 24
targetSdk: 36
compileSdk: 37
Java compatibility: Java 11
```

---

## Application Workflow

```text
                TextEZ
                   |
          +--------+--------+
          |                 |
      New File          Open File
          |                 |
          +--------+--------+
                   |
                Editor
                   |
     +-------------+-------------+
     |             |             |
 File Editing    Syntax       Version
 Operations    Highlighting    Control
     |             |             |
 Save/Open     Kotlin/MD     Base + Delta
 Search        Detection     Compare
 Replace       Highlight     Restore
 Undo/Redo                  Version History
```

---

## APK

The compiled TextEZ Android application is available in the repository:

```text
apk/TextEZ-v1.0.apk
```

### Installation

1. Download `TextEZ-v1.0.apk`.
2. Transfer it to an Android device if necessary.
3. Allow installation from the selected source if Android requests permission.
4. Open the APK.
5. Install TextEZ.

> The APK was generated from the Android project for project demonstration and assessment purposes.

---

## Build from Source

### Requirements

- Android Studio
- Android SDK
- JDK compatible with the project configuration

### Steps

1. Clone the repository:

```bash
git clone https://github.com/Himansha23/TextEZ.git
```

2. Open the cloned project in Android Studio.

3. Allow Gradle to synchronize the project dependencies.

4. Select an Android emulator or connected Android device.

5. Build and run the application.

A debug APK can also be generated using:

```bash
./gradlew assembleDebug
```

The generated APK is normally available at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## Team Contributions

### HSR Perera – 24020796

**Core Editor and File Management**

Main implementation areas:

- New File
- Open File
- Save
- Save As
- Delete
- Undo
- Redo
- Search
- Replace

### WSC de Silva – 24020222

**Syntax Highlighting and Recovery**

Main implementation areas:

- Kotlin syntax highlighting
- Markdown syntax highlighting
- Automatic language detection
- Autosave
- Unsaved-work recovery

### RAHN Wijesekara – 24021202

**Incremental Version Control**

Main implementation areas:

- Version creation
- Base version storage
- Delta tracking
- Patch generation
- Version reconstruction
- Version history
- Version preview
- Version comparison
- Rollback

Integration and final application testing were carried out collaboratively by the group.

---

## Technical Documentation

The project technical report describes the implementation of:

- File and document storage
- Syntax highlighting
- Autosave and recovery
- Incremental delta tracking
- Version storage
- Version reconstruction
- Version preview
- Version comparison
- Version rollback

**Technical Report:** Add final report link here

---

## Application Demonstration

A recorded demonstration accompanies the project submission.

All three group members participate in the demonstration and explain the features related to their individual contributions.

**Demonstration Video:** Add final video link here

---

## Repository

**GitHub Repository:**  
https://github.com/Himansha23/TextEZ

---

## Academic Project

TextEZ was developed as a group mini project for:

**IS2205 – Mobile Application Design and Development**  
**University of Colombo School of Computing**

The application was developed for academic assessment and demonstration purposes.
