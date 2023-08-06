$ErrorActionPreference = "Stop"

Write-Host "Setting up Environment Variables..."
$user = [ordered]@{
    # just for easy navigation
    HOME = "%USERPROFILE%"
    # base for programming stuff
    PROG_HOME="P:"
    PROG_HOME_LARGE="Z:"
    # needed for my gradle plugin
    RELEASE_HOME = "%PROG_HOME%\repos\release"
    # command line utility collection
    TOOLS_HOME = "%PROG_HOME%\tools\misc"
    GIT_HOME = "%PROG_HOME%\tools\vcs\git"
    BUNCH_HOME = "%PROG_HOME%\tools\build\bunch-0.9.0"

    # Conventional settings for Android Studio and other tools.
    # https://developer.android.com/studio/command-line/variables#android_verbose
    ANDROID_HOME = "%PROG_HOME_LARGE%\tools\sdk\android"
    ANDROID_AVD_HOME = "%ANDROID_HOME%\.android\avd"
    ANDROID_USER_HOME = "%ANDROID_HOME%\.android\user"
    ANDROID_EMULATOR_HOME = "%ANDROID_HOME%\.android\emulator"
    ANDROID_PREFS_ROOT = "%ANDROID_HOME%\.android\settings"
    # DO NOT SET THIS, it messes up AVD creation in latest AS (EE).
    #ANDROID_SDK_HOME = "%ANDROID_HOME%\.android\legacy"
    ANDROID_NDK_HOME = "%ANDROID_HOME%\ndk\25.1.8937393"

    BACKUP_DIR = "G:\My Drive\Safe"

    # com.android.prefs.AndroidLocationsException: Several environment variables and/or system properties contain different paths to the Android Preferences folder.
    # Please correct and use only one way to inject the preference location.
    #
    # - ANDROID_SDK_HOME(environment variable): Z:\tools\sdk\android\.android\legacy
    # - ANDROID_USER_HOME(environment variable): Z:\tools\sdk\android\.android\user
    #
    # It is recommended to use ANDROID_USER_HOME as other methods are deprecated
    #     at com.android.prefs.PathLocator.singlePathOf(AbstractAndroidLocations.kt:310)
    #     at com.android.prefs.AbstractAndroidLocations.computeAndroidFolder(AbstractAndroidLocations.kt:144)
    #     at com.android.prefs.AbstractAndroidLocations.getPrefsLocation(AbstractAndroidLocations.kt:75)
    #     at com.android.tools.idea.welcome.config.GlobalInstallerData.readProperties(InstallerData.kt:75)
    #     at com.android.tools.idea.welcome.config.GlobalInstallerData.parse(InstallerData.kt:103)
    #     at com.android.tools.idea.welcome.config.GlobalInstallerData.<clinit>(InstallerData.kt:95)
    #     at com.android.tools.idea.welcome.wizard.AndroidStudioWelcomeScreenProvider$Companion.isHandoff(AndroidStudioWelcomeScreenProvider.kt:90)
    #     at com.android.tools.idea.welcome.wizard.AndroidStudioWelcomeScreenProvider$Companion.getWizardMode(AndroidStudioWelcomeScreenProvider.kt:79)
    #     at com.android.tools.idea.welcome.wizard.AndroidStudioWelcomeScreenProvider.isAvailable(AndroidStudioWelcomeScreenProvider.kt:56)
    #     at com.android.tools.idea.welcome.wizard.FirstRunWizardFrameProvider.lambda$createFrame$0(FirstRunWizardFrameProvider.java:39)

    GCLOUD_HOME = "%PROG_HOME%\tools\sdk\google-cloud-sdk"
    # set up a Gradle for global things
    GRADLE7_HOME = "%PROG_HOME%\tools\build\gradle\gradle-7.6-all\9f832ih6bniajn45pbmqhk2cw\gradle-7.6"
    GRADLE8_HOME = "%PROG_HOME%\tools\build\gradle\gradle-8.0.1-all\aro4hu1c3oeioove7l0i4i14o\gradle-8.0.1"
    GRADLE_HOME = "%GRADLE7_HOME%"
    # Global Gradle arguments to be passed to `gradle`/`gradlew`
    #-PSNAPSHOT_REPOSITORY_URL=file://p:\projects\contrib\github-glide-m2 -PRELEASE_REPOSITORY_URL=file://p:\projects\contrib\github-glide-m2
    GRADLE_ARGS = "--stacktrace"
    # Global Gradle JVM options, JAVA_OPTS would also work, but that probably conflicts with other programs (maven?)
    GRADLE_OPTS = "-Xmx512M -Djdk.tls.client.protocols=TLSv1.2"
    # relocate Gradle directory from ~/.gradle
    GRADLE_USER_HOME = "%PROG_HOME_LARGE%\caches\gradle"
    HEROKU_HOME = "%PROG_HOME%\tools\vcs\heroku"
    MVN_HOME = "%PROG_HOME%\tools\build\apache-maven-3.8.6"
    # NodeJS / NPM / NVM
    NODE_HOME = "%ProgramFiles%\nodejs"
    NPM_CONFIG_CACHE = "%PROG_HOME%\caches\npm"
    NPM_CONFIG_USERCONFIG = "%PROG_HOME%\config\npm\user.npmrc"
    NVM_HOME = "%PROG_HOME%\tools\lang\nvm-1.1.5"
    NVM_SYMLINK = "%NODE_HOME%"
    NODE_ENV = "development"

    JAVA5_HOME = "%PROG_HOME%\tools\lang\java-1.5.0_22-x64-jdk"
    JAVA6_HOME = "%PROG_HOME%\tools\lang\java-1.6.0_45-x64-jdk"
    JAVA7_HOME = "%PROG_HOME%\tools\lang\java-1.7.0_80-x64-jdk"
    JAVA8_HOME = "%PROG_HOME%\tools\lang\java-1.8.0_201-x64-jdk"
    JAVA8_HOME_32 = "%PROG_HOME%\tools\lang\java-1.8.0_121-x86-jdk"
    JAVA9_HOME = "%PROG_HOME%\tools\lang\java-9.0.1_11-x64-jdk"
    JAVA10_HOME = "%PROG_HOME%\tools\lang\java-10.0.2-x64-jdk"
    # BEWARE: after 11 Oracle JDKs are commercially licenced, not free
    JAVA11_HOME = "%PROG_HOME%\tools\lang\java-11.0.2-x64-openjdk"
    # 17+ is fine licence-wise
    JAVA17_HOME = "%PROG_HOME%\tools\lang\java-17.0.5-x64-jdk"
    JAVA19_HOME = "%PROG_HOME%\tools\lang\java-19.0.1-x64-jdk"
    JAVA20_HOME = "%PROG_HOME%\tools\lang\java-20.0.2-x64-jdk"
    # for gradlew, mvn
    JAVA_HOME = "%JAVA17_HOME%"
    # Global Java settings for random `java` executions
    # (unofficial version: _JAVA_OPTIONS, but that doesn't allow -Xmx1G to be used from command line)
    # -Xmx to minimize 1/4 memory usage (8G out of 32G)
    # G1 GC allows heap shrinking
    # PrintCommandLineFlags to see what's being used really
    JAVA_TOOL_OPTIONS="-Xms32M -Xmx256M -XX:+UseG1GC -D-XX:+PrintCommandLineFlags"
    # for kotlin GitHub project
    JDK_16 = "%JAVA6_HOME%"
    JDK_17 = "%JAVA7_HOME%"
    JDK_18 = "%JAVA8_HOME%"
    JDK_9 = "%JAVA9_HOME%"

    KOTLIN_HOME = "%PROG_HOME%\tools\lang\kotlin-1.7.10"
    KOTLIN_MAIN_KTS_COMPILED_SCRIPTS_CACHE_DIR = "%PROG_HOME_LARGE%\caches\kotlin\main.kts.compiled.cache"
    KONAN_DATA_DIR = "Z:\caches\kotlin\konan"
    RUBY_HOME = "%PROG_HOME%\tools\lang\ruby-2.5.3-x64-msys64"
    # likely Ruby 2.6.x default, use for previous versions
    RUBYOPT = "-Eutf-8"

    FONTFORGE_HOME = "%PROG_HOME%\tools\other\FontForge-mingw-w64-i686-333856-r2"
    WOFF2_HOME = "%PROG_HOME%\tools\misc\woff2-1.0.3-SNAPSHOT"
    #SCALA_HOME = "%PROG_HOME%\tools\lang\scala-2.11.7"
    PYTHON2_HOME = "%PROG_HOME%\tools\lang\python-2.7.10-amd64"
    PYTHON3_HOME = "%PROG_HOME%\tools\lang\python-3.7.2.post1-embed-amd64"
    PYTHON_HOME = "%PYTHON3_HOME%"

    PERL5_HOME = "%PROG_HOME%\tools\lang\perl-5.14.2.1402-x64"
    PERL_HOME= "%PERL5_HOME%"

    #REDIS_HOME = "%PROG_HOME%\tools\data\Redis-x64-2.8.2400"
    NEO4J_HOME = "%PROG_HOME%\tools\data\neo4j-community-3.5.8"

    XDG_CACHE_HOME = "%PROG_HOME%\caches\intellij\plugin-development"
    IDEA_HOME="%PROG_HOME%\tools\ide\idea"
    IDEA_JDK="%PROG_HOME%\tools\ide\idea\jbr"
    IDEA_PROPERTIES="%PROG_HOME%\config\intellij\idea\idea.properties"
    IDEA_VM_OPTIONS="%PROG_HOME%\config\intellij\idea\idea64.vmoptions"
    STUDIO_JDK="%PROG_HOME%\tools\ide\android-studio\jbr"
    STUDIO_PROPERTIES="%PROG_HOME%\config\intellij\android\studio.properties"
    STUDIO_VM_OPTIONS="%PROG_HOME%\config\intellij\android\studio64.vmoptions"

    # %USERPROFILE%\AppData\Local\Microsoft\WindowsApps
}
$paths = @(
        "%SystemRoot%\System32",
        # java, javac, javaw, javap, javah
        "%JAVA_HOME%\bin",
        # kotlin, kotlinc
        "%KOTLIN_HOME%\bin",
        "%TOOLS_HOME%",
        # nvm
        "%NVM_HOME%",
        # npm, node (and whatever else is `npm install -g/--global`)
        "%NODE_HOME%"
        # ruby, gem, bundle, fontcustom, jekyll, scss
        "%RUBY_HOME%\bin",
        # fontforge for fontcustom
        "%FONTFORGE_HOME%\bin",
        # woff2_compress for fontcustom
        "%WOFF2_HOME%",
        # pyhton is needed for fontcustom
        "%PYTHON_HOME%",
        # perl
        "%PERL_HOME%\bin",
        # git
        "%GIT_HOME%\bin",
        # gitk
        "%GIT_HOME%\cmd",
        "%BUNCH_HOME%\bin",
        # Android
        "%TOOLS_HOME%\android",
        # apkanalyzer, avdmanager, lint, retrace, sdkmanager
        "%ANDROID_HOME%\cmdline-tools\latest\bin",
        # emulator
        "%ANDROID_HOME%\emulator",
        # emulator, monitor, android
        "%ANDROID_HOME%\tools",
        # uiautomatorviewer, avdmanager, lint, monkeyrunner, sdkmanager, uiautomatorviewer
        "%ANDROID_HOME%\tools\bin",
        # adb, fastboot, hprof-conv, sqlite3
        "%ANDROID_HOME%\platform-tools",
        # d8, aapt, aapt2, aidl, dexdump, zipalign, apksigner
        "%ANDROID_HOME%\build-tools\33.0.1",
        # Clouds
        "%GCLOUD_HOME%\bin",
        "%HEROKU_HOME%\bin",
        # gradle
        "%GRADLE_HOME%\bin",
        # idea (careful, defines format.bat and other junk as well)
        "%IDEA_HOME%\bin"
)
$user['PATH'] = $paths -join ';'

$DONT_VALIDATE = @('PATH', 'RUBYOPT', '_JAVA_OPTIONS', 'JAVA_TOOL_OPTIONS', 'GRADLE_OPTS', 'GRADLE_ARGS', 'NODE_ENV')
foreach ($v in $user.GetEnumerator()) {
    $value = [System.Environment]::ExpandEnvironmentVariables($v.Value)
    # TODO check if value contains %, because then I messed something up
    Write-Host "User\$($v.Name) = $($v.Value) -> $value"
    if ($DONT_VALIDATE -NotContains $v.Name -And -Not (Test-Path "$value")) {
        Write-Error "$($v.Name) does not point to an existing file system location: $($v.Value)"
    }
    if ($v.Name -eq "PATH") {
        $paths | ForEach-Object -Process {
            $path = [System.Environment]::ExpandEnvironmentVariables($_)
            if (-Not (Test-Path "$path")) {
                Write-Error "PATH part '$_' does not point to an existing file system location: $path"
            }
        }
    }
    Set-Item "env:$($v.Name)" $value
    [Environment]::SetEnvironmentVariable($v.Name, $value, "User") # or Machine
}
Write-Host "Done with Environment Variables."
