# TextEZ – Mobile Text Editor

TextEZ is a lightweight Android text editor developed in **Kotlin** as part of the **IS2205 Mobile Application Development Mini Project**.

The application allows users to create, edit, save, organize, and version-control text documents directly on an Android device. It also supports automatic syntax highlighting for Kotlin and Markdown files, crash recovery, and document version management.

---

## Features

### File Management
- Create new text files
- Open existing files
- Save files
- Save As
- Delete files
- Recent Files list

### Text Editing
- Undo
- Redo
- Search
- Replace

### Smart Editor
- Automatic Kotlin syntax highlighting
- Automatic Markdown syntax highlighting
- Automatic language detection while typing
- Plain text editing
- Read Only mode

### Data Protection
- Auto Save
- Crash Recovery
- UTF-8 file support

### Version Control
- Create document versions
- View Version History
- Preview previous versions
- Compare versions
- Restore previous versions
- Automatic backup before rollback
- Delta storage using **java-diff-utils**

### Version Control System
- Git
- GitHub Repository
- Feature Branch workflow
- Release Tags

---

# Technologies Used

- Kotlin
- Android SDK
- Android Studio
- Gradle
- SharedPreferences
- Internal Storage
- java-diff-utils
- Git
- GitHub

---

# Project Structure

```
TextEZ
│
├── activities
│   ├── MainActivity
│   ├── EditorActivity
│   ├── OpenFileActivity
│   ├── VersionHistoryActivity
│   ├── VersionPreviewActivity
│   └── VersionCompareActivity
│
├── adapters
│
├── managers
│   ├── LanguageDetector
│   ├── KotlinSyntaxHighlighter
│   ├── MarkdownSyntaxHighlighter
│   ├── LineDiffManager
│   └── VersionManager
│
├── storage
│   ├── AutoSaveManager
│   └── RecentFilesManager
│
├── models
│   └── Version
│
└── res
```

---

# Installation

Clone the repository

```bash
git clone https://github.com/Himansha23/TextEZ.git
```

Open the project using **Android Studio**.

Build the project.

Run the application on:

- Android Emulator
- Physical Android Device (Android 7.0+)

---

# How to Use

### Create File

- Tap **New File**
- Start typing

### Save

- Press **Save**
- Enter file name

### Search

- Tap **Search**
- Enter keyword

### Replace

- Tap **Replace**
- Replace one or all occurrences

### Version Control

- Press **Create Version**
- View **History**
- Preview
- Compare
- Restore

### Delete File

- Press **Delete**
- Confirm deletion

---

# Version Control Workflow

```
main
│
├── feature/editor
├── feature/search
├── feature/version-control
└── release/v2.1
```

---

# Future Improvements

- Dark Mode
- Line Numbers
- Multiple Tabs
- Code Folding
- Themes
- Export as PDF
- Cloud Synchronization
- Plugin Support

---

# Author

**R A H N Wijesekara - 24021202**
**W S C de Silva - 24020222**
**H S R Perera - 24020796**

IS2205 – Mobile Application Development

University of Colombo School of Computing

---

# License

This project was developed for educational purposes as part of the IS2205 Mobile Application Development Mini Project.
