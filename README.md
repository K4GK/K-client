# PvP Client (Minecraft 1.21.1, Fabric)

## 1. Build the mod (needs JDK 21)
    cd mod
    gradle wrapper        # one time, needs Gradle installed (or copy a wrapper from any Fabric template)
    ./gradlew build       # Windows: gradlew.bat build
The jar appears in mod/build/libs/pvpclient-1.0.0.jar

## 2. Run the launcher
Run Minecraft 1.21.1 once in the official launcher, then:
    python launcher/launcher.py
Click 1 (installs Fabric + Fabric API), 2 (pick the jar), 3 (add your texture pack zips).
Open the official Minecraft Launcher, choose the "PvP Client" profile, Play.

## 3. In game
Press Right Shift: toggle packs, Apply, save/load a "pvp" preset.

Note: written without being able to compile here. If the build reports a method name
mismatch, paste the error and it is a quick fix.
