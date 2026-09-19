# General Preferences

- Always keep responses brief and to the point.
- When generating documentation, summaries, or artifacts, always include a `TL;DR` section at the top.
- **Release Notes**: Whenever updating or creating release notes, always check the format of previous releases (e.g., v1.0.0) via GitHub CLI (`gh release view`). Check the diff from the previous version (`git log <prev>..<current>`) and create a compact, well-formatted summary of changes following the existing structure.
- **Environment & Tooling**:
  - `JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"`
  - `ANDROID_HOME="$HOME/Library/Android/sdk"`
  - For Gradle/Java/Android commands, always set: `export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" PATH="$JAVA_HOME/bin:$PATH"`
