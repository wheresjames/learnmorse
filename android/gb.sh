#!/bin/bash

#--------------------------------------------------------------------
# Created by Robert Umbehant
#--------------------------------------------------------------------
# Install sdkman
#--------------------------------------------------------------------
#
# $ curl -s "https://get.sdkman.io" | bash
#
#   Add to .bashrc or run in each terminal
# $ source ~/.sdkman/bin/sdkman-init.sh
#
#   Upgrade sdkman
# $ sdk selfupdate force
#
#--------------------------------------------------------------------
# Install gradle
#--------------------------------------------------------------------
#
# $ sdk install gradle
#
#--------------------------------------------------------------------
# Run gradle
#--------------------------------------------------------------------
#
#   Get a list of tasks
# $ gradle tasks
#
#   Build
# $ gradle :project:build
#
#   Run
# $ gradle :project:run
#
#   Install debug android apk on first emulator
# $ gradle installDebug
#
#   For global build, add following to ~/.gradle/init.gradle
# + gradle.projectsLoaded {
# +     rootProject.allprojects {
# +         buildDir = "/path/to/build/${rootProject.name}/${project.name}"
# +     }
# + }
#
# ./gradlew --refresh-dependencies clean build
#
#--------------------------------------------------------------------
# Install JDK
#--------------------------------------------------------------------
#
# https://openjdk.java.net/install/
#
# $ export JDK_HOME=~/Java/jdk-11.0.6+10
# $ export JAVA_HOME=~/Java/jdk-11.0.6+10
#
#--------------------------------------------------------------------
# Android
#--------------------------------------------------------------------
#
#   Minimal Android project
# https://stackoverflow.com/questions/47167769/hello-world-android-app-with-as-few-files-as-possible-no-ide-and-text-editor/47251607#47251607
#
#--------------------------------------------------------------------
# Example script use
#--------------------------------------------------------------------
#
#   Build and Run Project
# $ cd ./test-project
# $ gb.sh local-build-run
#
#   Sign apk
# $ gb.sh sign-signapk key-name key-password
#
#   Build Android
# $ ./gb.sh anroid-build
#
#   Install Android apk
#   ./gb.sh android-install <device-id>
#
#   Build, release, sign
#   ./gb.sh android-build-release-sign-signaab <keyname> <keypassword>
#
#--------------------------------------------------------------------
#
# Generate privacy policy and TOS
#
# https://app-privacy-policy-generator.firebaseapp.com/
#
#--------------------------------------------------------------------


# Usage
USAGE="USAGE: gb.sh <cmd>"

#--------------------------------------------------------------------
# Functions
#--------------------------------------------------------------------

isCmd()
{
    # - separator
    FOUND=$(echo "\-$COMMANDLIST-" | grep -o "\-${1}-")
    if [[ ! -z $FOUND ]]; then return 0; fi

    # , separator
    FOUND=$(echo ",$COMMANDLIST," | grep -o ",${1},")
    if [[ ! -z $FOUND ]]; then return 0; fi
    return -1
}

findInStr()
{
    FIND_LIST=$1
    FIND_EXISTS=$(echo $FIND_LIST | grep -o $2)
    if [[ ! -z $FIND_EXISTS ]]; then return 0; fi
    return -1
}

findIn()
{
    findInStr "$($1 2>&1)" $2
    return $?
}

isInstalled()
{
    if ! findIn "which $1" "$1"; then return -1; fi
    return 0
}

showInfo()
{
	if [[ 0 -lt ${#@} ]]; then
		echo -e "[\e[1;37mINFO\e[1;0m] \e[1;37m${@}\e[1;0m"
	fi
}

showWarning()
{
	if [[ 0 -lt ${#@} ]]; then
		echo -e "[\e[1;33mWARNING\e[1;0m] \e[1;33m${@}\e[1;0m"
	fi
}

exitWithError()
{
	if [[ 0 -lt ${#@} ]]; then
        echo
        echo "--------------------------------------------------------------------"
		echo -e "[\e[1;31mERROR\e[1;0m] \e[1;31m${@}\e[1;0m"
        echo "--------------------------------------------------------------------"
        echo
	fi

	exit -1
}

exitOnError()
{
	if [[ 0 -eq $? ]]; then return 0; fi
    exitWithError $@
}

warnOnError()
{
	if [[ 0 -eq $? ]]; then return 0; fi
    showWarning $@
}

doIf()
{
    findInStr "$($1 2>&1)" $2
    if [[ 0 -ne $? ]]; then return 0; fi
    if [[ ! -z $4 ]]; then $4 | $3; else $3; fi
}

doIfNot()
{
    findInStr "$($1 2>&1)" $2
    if [[ 0 -eq $? ]]; then return 0; fi
    if [[ ! -z $4 ]]; then $4 | $3; else $3; fi
}

findFile()
{
    # Try to find the apk file or root
    if [ -z $1 ]; then
        FINDFILE="$GRADLE_OUTPUT"
    else
        if [ -d $1 ]; then
            FINDFILE="${1}"
        else
            FINDFILE="${1}"
        fi
    fi

    FINDTMPL=$2
    if [ -z $FINDTMPL ]; then
        exitWithError "Find template not specified"
    fi

    echo "FINDFILE= $FINDFILE, FINDTMPL= $FINDTMPL"

    # Do we need to search for an apk file?
    if [ -d $FINDFILE ]; then
        FINDFILE=$(find $FINDFILE | grep "$FINDTMPL" | head -1)
    fi

    # Make sure apk file exists
    if [ ! -f "$FINDFILE" ]; then
        FINDFILE=
    fi
}

isOnline()
{
    wget -q --tries=1 --timeout=8 --spider http://google.com
    return $?
}

#--------------------------------------------------------------------
# Command line
#--------------------------------------------------------------------

COMMANDLIST=$1
if [ -z $COMMANDLIST ]; then
    echo $USAGE
    exit 0
fi


#--------------------------------------------------------------------
# Config
#--------------------------------------------------------------------

SCRIPT_NAME=$(basename $0)
SCRIPT_PATH=$(dirname "$(readlink -f "$0")")
WORKING_PATH=$(pwd)

CURRENT_DATE=$(date '+%Y-%m-%d')
CURRENT_TIME=$(date '+%Y-%m-%d-%H-%M-%S')

# Gradle
if [ -z "$GRADLE" ]; then
    GRADLE="./gradlew"
fi
GRADLE_PROJECT=
#if [ $SCRIPT_PATH != $WORKING_PATH ]; then
    GRADLE_PROJECT=$(basename $WORKING_PATH)
#fi
if [ -z $GRADLE_BUILD ]; then
    GRADLE_BUILD='.'
fi
if [ -z $GRADLE_PROJECT ]; then
    GRADLE_OUTPUT="$GRADLE_BUILD"
else
    GRADLE_OUTPUT="$GRADLE_BUILD/$GRADLE_PROJECT"
fi
if [ -z "$GRADLE_USER_HOME" ]; then
    # Keep Gradle cache inside workspace to avoid sandbox permission errors
    export GRADLE_USER_HOME="$WORKING_PATH/.gradle"
fi
mkdir -p "$GRADLE_USER_HOME"
ONLINE_STATUS=1
if ! isOnline; then
    ONLINE_STATUS=0
fi
GRADLE_WRAPPER_PROPERTIES="$SCRIPT_PATH/gradle/wrapper/gradle-wrapper.properties"
WRAPPER_DIST_FILE=
if [ -f "$GRADLE_WRAPPER_PROPERTIES" ]; then
    WRAPPER_DIST_URL=$(grep '^distributionUrl=' "$GRADLE_WRAPPER_PROPERTIES" | cut -d= -f2-)
    WRAPPER_DIST_FILE=$(basename "$WRAPPER_DIST_URL")
fi
WRAPPER_DIST_AVAILABLE=0
if [ ! -z "$WRAPPER_DIST_FILE" ]; then
    WRAPPER_DIST_DIR_NAME=${WRAPPER_DIST_FILE%.zip}
    WRAPPER_DIST_CACHE="$GRADLE_USER_HOME/wrapper/dists/$WRAPPER_DIST_DIR_NAME"
    if [ -d "$WRAPPER_DIST_CACHE" ]; then
        WRAPPER_DIST_MATCH=$(find "$WRAPPER_DIST_CACHE" -name "$WRAPPER_DIST_FILE" -print -quit 2>/dev/null)
        if [ ! -z "$WRAPPER_DIST_MATCH" ]; then
            WRAPPER_DIST_AVAILABLE=1
        fi
    fi
fi
if [ $ONLINE_STATUS -eq 0 ] && [ $WRAPPER_DIST_AVAILABLE -eq 0 ] && [ ! -z "$GRADLE_HOME" ] && [ -x "$GRADLE_HOME/bin/gradle" ]; then
    showWarning "Offline, falling back to GRADLE_HOME/bin/gradle"
    GRADLE="$GRADLE_HOME/bin/gradle"
fi

# Android SDK
if [ -z $ANDROID_SDK_ROOT ]; then
    ADBEXEC="adb"
    EMUEXEC="emulator"
    ZIPALIGN="emulator"
    showWarning "ANDROID_SDK_ROOT is not set"
else
    ADBEXEC="${ANDROID_SDK_ROOT}/platform-tools/adb"
    EMUEXEC="${ANDROID_SDK_ROOT}/emulator/emulator"
    ZIPALIGN=$(find $ANDROID_SDK_ROOT | grep "zipalign" | head -1)
    APKSIGNER=$(find $ANDROID_SDK_ROOT/build-tools -name "apksigner" | sort -V | tail -1)
fi
APKDBG="\-debug\.apk"
APKREL="\-release-unsigned\.apk"
ABBDBG="debug/android-debug.aab"
ABBREL="release/android-release.aab"
EMULATORVER="Pixel8Pro"
COMPANYID=$ANDROID_COMPANYID
if [ -z "$COMPANYID" ]; then
    COMPANYID="learnmorse"
fi


# Java SDK
if [ -z $JDK_HOME ]; then
    export JDK_HOME=$JAVA_HOME
fi

# Find Java exe
if [ ! -z $JDK_HOME ]; then
    JAVA="${JDK_HOME}/bin/java"
    KEYTOOL="${JDK_HOME}/bin/keytool"
    JARSIGNER="${JDK_HOME}/bin/jarsigner"
fi
if [ ! -f $JAVA ]; then
    JAVA="java"
    KEYTOOL="keytool"
    JARSIGNER="jarsigner"
fi

if [ -z $JAVA ]; then
    exitWithError "JAVA_HOME or JDK_HOME not set"
fi

if [ ! -f $JAVA ]; then
    exitWithError "Java executable not found : $JAVA"
fi

# Where does release ouptput go
RELEASE_PATH="$WORKING_PATH/release"

echo "------------------------- Configuration ----------------------------"
echo "- COMPANY ID     : $COMPANYID"
echo "- COMMAND LINE   : $SCRIPT_NAME $@"
echo "- COMMANDS       : $COMMANDLIST"
echo "- SCRIPT         : $SCRIPT_NAME"
echo "- SCRIPT PATH    : $SCRIPT_PATH"
echo "- WORKING PATH   : $WORKING_PATH"
echo "- RELEASE PATH   : $RELEASE_PATH"
echo "- GRADLE         : $GRADLE"
echo "- GRADLE PROJECT : $GRADLE_PROJECT"
echo "- GRADLE HOME    : $GRADLE_HOME"
echo "- GRADLE BUILD   : $GRADLE_BUILD"
echo "- GRADLE OUTPUT  : $GRADLE_OUTPUT"
echo "- ANDROID SDK    : $ANDROID_SDK_ROOT"
echo "- ADB            : $ADBEXEC"
echo "- EMULATOR       : $EMUEXEC"
echo "- EMULATOR VER   : $EMULATORVER"
echo "- ZIPALIGN       : $ZIPALIGN"
echo "- JAVA SDK       : $JDK_HOME"
echo "- JAVA HOME      : $JAVA_HOME"
echo "- JAVA EXEC      : $JAVA"
echo "- KEY TOOL       : $KEYTOOL"
echo "- JAR SIGNER     : $JARSIGNER"
echo "--------------------------------------------------------------------"
echo

if [ $ONLINE_STATUS -eq 0 ]; then
    showWarning "You do not appear to be online"
fi

#--------------------------------------------------------------------
# Defines
#--------------------------------------------------------------------

#--------------------------------------------------------------------
# Install tools
if isCmd "installgradle"; then

    # Install sdkman
    curl -s "https://get.sdkman.io" | bash
    exitOnError "SDKMAN install failed"

    # Setup SDKMAN environment variables
    source ~/.sdkman/bin/sdkman-init.sh
    exitOnError "SDKMAN initialization failed"

    # Upgrade sdkman if needed
    sdk selfupdate

    # Install gradle
    sdk install gradle


    # ./gradlew wrapper --gradle-version=8.13 --distribution-type=bin

    exit 0
fi


#--------------------------------------------------------------------
# Create new project
if isCmd "create"; then

    echo "--------------------------------------------------------------------"
    echo "- Creating project"
    echo "--------------------------------------------------------------------"
    echo "- https://docs.gradle.org/current/userguide/build_init_plugin.html"
    echo "--------------------------------------------------------------------"


    if [ -z $2 ]; then
        echo "${USAGE} <directory> <project-type>"
        exitWithError "Project directory not specified"
    fi

    if [ -z $3 ]; then
        echo "${USAGE} <directory> <project-type>"
        echo
        echo "Examples project types"
        echo "   basic, kotlin-application, cpp-application, cpp-library, ..."
        exitWithError "Project type not specified"
    fi

    DIR="${2}"
    if [ -d $DIR ]; then
        exitWithError "Directory already exists : $DIR"
    fi

    PROJECTTYPE=$3

    mkdir -p $DIR
    cd $DIR

    gradle init --type=${PROJECTTYPE}
    exitOnError "gradle init failed"

    cd $WORKING_PATH

    exit 0
fi

#--------------------------------------------------------------------
# Android app
if isCmd "android"; then

    USAGE="$USAGE <project>"
    #PROJECT=$2
    PROJECT="android"

    # Must have company info
    if [ -z $COMPANYID ]; then
        exitWithError "ANDROID_COMPANYID environment variable not defined"
    fi

    if [ "." = "$PROJECT" ]; then
        PROJECT=
    elif [ ! -z $PROJECT ]; then
        PROJECT="$PROJECT"
        echo "PROJECT : $PROJECT"
    fi

    # Application log
    if isCmd "log"; then
        $ADBEXEC logcat -c
        $ADBEXEC logcat -s "$GRADLE_PROJECT"
        exit 0
    fi

    # Build
    if isCmd "build"; then
        echo "Building..."
        $GRADLE ":${PROJECT}:build"
        echo "Creating bundle..."
        $GRADLE ":${PROJECT}:bundle"
        exitOnError "gradle build failed"
    fi

    # List devices
    if isCmd "restart"; then
        $ADBEXEC kill-server
        $ADBEXEC start-server
        $ADBEXEC devices
    fi

    # List devices
    if isCmd "devices"; then

        echo "List adb devices..."
        $ADBEXEC devices

    fi

    if isCmd "bumpversion"; then

        BUILDFILE="$WORKING_PATH/app/build.gradle.kts"
        CURRENT_VERSION_CODE=$(grep "versionCode" "$BUILDFILE" | grep -Eo "[0-9]+")
        if [ -z $CURRENT_VERSION_CODE ]; then
            exitWithError "Failed to get current version code from build.gradle.kts"
        fi
        NEXT_VERSION_CODE=$(($CURRENT_VERSION_CODE + 1))
        echo "Bumping version code from $CURRENT_VERSION_CODE to $NEXT_VERSION_CODE"

        CURRENT_VERSION=$(grep "versionName" "$BUILDFILE" | grep -Eo "[0-9]+\.[0-9]+(\.[0-9]+)?")
        if [ -z $CURRENT_VERSION ]; then
            exitWithError "Failed to get current version from build.gradle.kts"
        fi

        CURRENT_VERSION_MAJOR=$(echo $CURRENT_VERSION | awk -F. '{print $1}')
        CURRENT_VERSION_MINOR=$(echo $CURRENT_VERSION | awk -F. '{print $2}')
        NEXT_VERSION="$CURRENT_VERSION_MAJOR.$CURRENT_VERSION_MINOR.$NEXT_VERSION_CODE"
        echo "Bumping version from $CURRENT_VERSION to $NEXT_VERSION"

        # Update build.gradle.kts (KTS uses = "..." assignment syntax)
        sed -i "s/versionCode = $CURRENT_VERSION_CODE/versionCode = $NEXT_VERSION_CODE/" "$BUILDFILE"
        sed -i "s/versionName = \"$CURRENT_VERSION\"/versionName = \"$NEXT_VERSION\"/" "$BUILDFILE"

    fi

    # Install apk file into emulator
    if isCmd "install"; then

        DEVICE="$2"
        APPNAME="$3"
        findFile "$PROJECT" "$APKDBG"
        APKFILE=$FINDFILE

        # Make sure apk file exists
        if [ ! -f "$APKFILE" ]; then
            echo "${USAGE} <deviceid> <apk-file | project-root-dir>"
            echo
            exitWithError "APK file not found : $APKFILE"
        fi

        if [ "." = "$APPNAME" ]; then
            APPNAME=
        fi
        if [ -z $APPNAME ]; then
            if [ ! -z $GRADLE_PROJECT ]; then
                APPNAME="com.${COMPANYID}.${GRADLE_PROJECT}"
            fi
        fi
        if [ ! -z $APPNAME ]; then
            echo "Uninstalling : $APPNAME"
            $ADBEXEC -s $DEVICE uninstall "$APPNAME"
        fi

        echo "Installing : $DEV <- $APKFILE"
        $ADBEXEC -s $DEVICE install "$APKFILE"
        exitOnError "Install failed"

    fi

    # Run in the emulator
    if isCmd "run"; then

        echo "Running..."

        # Find the apk file
        findFile "app" "$APKDBG"
        APKFILE=$FINDFILE

        # Make sure apk file exists
        if [ ! -f "$APKFILE" ]; then
            echo "${USAGE} <apk-file | project-root-dir>"
            echo
            exitWithError "APK file not found : $APKFILE"
        fi

        echo "APK FILE : $APKFILE"

        # Attempt to grab a device
        DEVICE=$($ADBEXEC devices | grep -Eo emulator\-.*$ | head -1 | grep -Eo [a-z]+\-[0-9]+)

        # Start the emulator if we didn't get a device
        if [ -z $DEVICE ]; then

            showWarning "No running emulators found, starting a new emulator..."
            echo "Running: $EMUEXEC -avd $EMULATORVER -netdelay none -netspeed full -gpu swiftshader_indirect"
            nohup $EMUEXEC -avd $EMULATORVER -netdelay none -netspeed full -gpu swiftshader_indirect &
            sleep 3

            echo "Waiting for emulator to start..."
            while ! findIn "$ADBEXEC devices" "emulator-"; do
                printf .
                sleep 1
            done
            echo

            # Try again for device name
            DEVICE=$($ADBEXEC devices | grep -Eo emulator\-.*$ | grep -Eo [a-z]+\-[0-9]+)

            if [ -z $DEVICE ]; then
                exitWithError "Failed to get device name"
            fi

            # Uninstall the old app
            if [ ! -z $GRADLE_PROJECT ]; then
                APPNAME="com.${COMPANYID}.${GRADLE_PROJECT}"
                echo "Uninstalling : $APPNAME"
                $ADBEXEC -s $DEVICE uninstall "$APPNAME"
            fi

            echo "Installing : $DEVICE <- $APKFILE"

            retry=0
            until [ $retry -gt 10 ]; do

                if [ $retry -gt 0 ]; then
                    echo "Attempt $retry failed : Retrying in 30 seconds..."
                    sleep 30
                fi
                ((retry++))

                $ADBEXEC -s $DEVICE install $APKFILE
               	if [[ 0 -eq $? ]]; then
                   retry=999
                fi

            done

        else
            echo "Installing : $DEVICE <- $APKFILE"
            $ADBEXEC -s $DEVICE install $APKFILE
            exitOnError "adb install failed"
        fi
    fi

    # Start emulator
    if isCmd "emulator"; then

        echo "Starting emulator..."

        # Start emulator
        $EMUEXEC -avd $EMULATORVER -netdelay none -netspeed full &
        exitOnError "launch emulator failed"

    fi

    if isCmd "release"; then

        # Find the apk file
        findFile "$PROJECT" "$APKREL"
        APKFILE=$FINDFILE
        if [ ! -f "$APKFILE" ]; then
            echo "${USAGE} [apk-file | project-root-dir]"
            echo
            exitWithError "APK file not found : $APKFILE"
        fi

        echo "APKFILE = $APKFILE"

        APKRELEASE="$RELEASE_PATH/$COMPANYID-$GRADLE_PROJECT-$CURRENT_TIME.apk"

        if isCmd "signapk"; then

            echo "!!! Not copying, will be done during signing"

        else

            # Ensure release directory
            mkdir -p "$RELEASE_PATH"
            cp $APKFILE $APKRELEASE

            echo "APK Release : $APKRELEASE"

        fi
    fi

    # Copy cache directory to device or emulator
    if isCmd "cache"; then

        DEVICE="$2"
        if [ -z "$DEVICE" ]; then
            exitWithError "Device ID not specified. Usage: ./gb.sh android-build-install-cache <device-id>"
        fi

        CACHE_SRC="$WORKING_PATH/cache"
        if [ ! -d "$CACHE_SRC" ]; then
            exitWithError "Cache directory not found: $CACHE_SRC"
        fi

        DEVICE_CACHE_PATH="/sdcard/Android/data/com.playthroughapps.bookscan/files/cache"
        echo "Pushing cache to $DEVICE : $DEVICE_CACHE_PATH"
        $ADBEXEC -s $DEVICE shell mkdir -p "$DEVICE_CACHE_PATH"
        exitOnError "Failed to create cache directory on device"
        $ADBEXEC -s $DEVICE push "$CACHE_SRC/." "$DEVICE_CACHE_PATH/"
        exitOnError "Failed to push cache to device"
        echo "Cache pushed successfully"

    fi

fi

#--------------------------------------------------------------------
# Local app
if isCmd "local"; then

    # Build
    if isCmd "build"; then

        echo "Building..."

        GRADLECMD="desktop:build"
        if [ ! -z $2 ]; then
            GRADLECMD=":$2:${GRADLECMD}"
        fi

        echo "--------------------------------------------------------------------"
        echo "- $GRADLE $GRADLECMD"
        echo "--------------------------------------------------------------------"

        $GRADLE $GRADLECMD
        exitOnError "$GRADLE $GRADLECMD failed"
    fi

    if isCmd "run"; then

        echo "Running..."

        GRADLECMD="desktop:run"
        if [ ! -z $2 ]; then
            GRADLECMD=":$2:${GRADLECMD}"
        fi

        echo "--------------------------------------------------------------------"
        echo "- $GRADLE $GRADLECMD"
        echo "--------------------------------------------------------------------"

        $GRADLE $GRADLECMD
        exitOnError "$GRADLE $GRADLECMD failed"
    fi

fi

if isCmd "sign"; then

    if isCmd "genkey"; then

        KEYNAME=$2
        if [ -z "$KEYNAME" ]; then
            echo "${USAGE} <key-name>"
            echo
            exitWithError "Key name not specified"
        fi

        KEYSTORE="$WORKING_PATH/keys/$KEYNAME.keystore"

        mkdir -p "$WORKING_PATH/keys"

        # Generate key
        $KEYTOOL -genkey -v -keystore $KEYSTORE -alias $KEYNAME -keyalg RSA -keysize 2048 -validity 10000
    fi

    if isCmd "export"; then

        KEYNAME=$2
        if [ -z "$KEYNAME" ]; then
            echo "${USAGE} <key-name>"
            echo
            exitWithError "Key name not specified"
        fi

        # Setup tool file name
        PEPEXEC="$SCRIPT_PATH/tools/pepk.jar"
        PEPLINK="https://www.gstatic.com/play-apps-publisher-rapid/signing-tool/prod/pepk.jar"

        # Download the tool if we don't have it
        if [ ! -f $PEPEXEC ]; then
            echo "Downloading GDX project setup tool..."
            echo
            curl -L $PEPLINK -o $PEPEXEC
            exitOnError "Failed to download GDX Setup tool"
        fi

        KEYSTORE="$WORKING_PATH/keys/$KEYNAME.keystore"
        if [ ! -f $KEYSTORE ]; then
            exitWithError "Invalid keystore : $KEYSTORE"
        fi

        PLAYKEYFILE="$WORKING_PATH/keys/playstore.key"
        if [ ! -f $PLAYKEYFILE ]; then
            exitWithError "Invalid playstore keystore : $PLAYKEYFILE"
        fi
        PLAYKEY=`cat $PLAYKEYFILE`
        if [ -z $PLAYKEY ]; then
            exitWithError "Empty playstore keystore : $PLAYKEYFILE"
        fi

        $JAVA -jar $PEPEXEC  --keystore=$KEYSTORE \
                             --alias=$KEYNAME \
                             --output="$WORKING_PATH/keys/$KEYNAME-playstorekey.zip" \
                             --encryptionkey=$PLAYKEY \
                             --include-cert

        PLAYSTOREPEM="$WORKING_PATH/keys/$KEYNAME-playstore-cert.pem"
        $KEYTOOL -export -rfc -alias $KEYNAME -file "$PLAYSTOREPEM" -keystore "$KEYSTORE"

    fi

    if isCmd "signapk"; then

        KEYNAME=$2
        if [ -z "$KEYNAME" ]; then
            echo "${USAGE} <key-name> <key-password> [apk-file | project-root-dir]"
            echo
            exitWithError "Key name not specified"
        fi

        KEYPASSWORD=$3
        if [ -z "$KEYPASSWORD" ]; then
            echo "${USAGE} <key-name> <key-password> [apk-file | project-root-dir]"
            echo
            exitWithError "Key password not specified"
        fi

        # Find the apk file
        findFile "android" "$APKREL"
        APKFILE=$FINDFILE
        if [ ! -f "$APKFILE" ]; then
            echo "${USAGE} <key-name> <key-password> [apk-file | project-root-dir]"
            echo
            exitWithError "APK file not found : $APKFILE"
        fi

        echo "APKFILE = $APKFILE"

        KEYSTORE="$WORKING_PATH/keys/$KEYNAME.keystore"
        APK_UNALIGNED="$RELEASE_PATH/$COMPANYID-$GRADLE_PROJECT-$CURRENT_TIME-unaligned.apk"
        APK_ALIGNED="$RELEASE_PATH/$COMPANYID-$GRADLE_PROJECT-$CURRENT_TIME.apk"

        # Ensure release directory
        mkdir -p "$RELEASE_PATH"
        cp $APKFILE $APK_UNALIGNED

        # zipalign BEFORE signing (required by apksigner)
        echo "Aligning apk : $APK_ALIGNED"
        $ZIPALIGN -f -v 4 $APK_UNALIGNED $APK_ALIGNED
        exitOnError "APK Alignment failed"

        # Sign with apksigner to produce v2/v3 signature accepted by Android
        echo "Signing apk : $APK_ALIGNED"
        $APKSIGNER sign --ks $KEYSTORE --ks-pass pass:$KEYPASSWORD --ks-key-alias $KEYNAME $APK_ALIGNED
        exitOnError "APK Signing failed"

        rm $APK_UNALIGNED

    fi

    if isCmd "signaab"; then

        KEYNAME=$2
        if [ -z "$KEYNAME" ]; then
            echo "${USAGE} <key-name> <key-password> [aab-file | project-root-dir]"
            echo
            exitWithError "Key name not specified"
        fi

        KEYPASSWORD=$3
        if [ -z "$KEYPASSWORD" ]; then
            echo "${USAGE} <key-name> <key-password> [aab-file | project-root-dir]"
            echo
            exitWithError "Key password not specified"
        fi

        # Find the aab file
        findFile "android" "$ABBREL"
        AABFILE=$FINDFILE
        if [ ! -f "$AABFILE" ]; then
            echo "${USAGE} <key-name> <key-password> [aab-file | project-root-dir]"
            echo
            exitWithError "AAB file not found : $AABFILE"
        fi

        echo "AABFILE = $AABFILE"

        KEYSTORE="$WORKING_PATH/keys/$KEYNAME.keystore"
        AABOUT="$RELEASE_PATH/$COMPANYID-$GRADLE_PROJECT-$CURRENT_TIME.aab"

        # Ensure release directory
        mkdir -p "$RELEASE_PATH"
        cp $AABFILE $AABOUT

        echo "Signing aab : $AABOUT"
        $JARSIGNER -sigalg SHA256withRSA -digestalg SHA-256 -keystore $KEYSTORE -storepass $KEYPASSWORD $AABOUT $KEYNAME
        exitOnError "AAB Signing failed"

    fi

fi

echo "--------------------------------------------------------------------"
echo
