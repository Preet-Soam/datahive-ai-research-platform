#!/usr/bin/env bash
set -Eeuo pipefail

APP_NAME="datahive"
PROJECT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
WAR_FILE="$PROJECT_DIR/target/${APP_NAME}.war"
TOMCAT_DIR="${CATALINA_HOME:-${TOMCAT_HOME:-}}"

fail() {
    printf 'Error: %s\n' "$*" >&2
    exit 1
}

if [[ -z "$TOMCAT_DIR" ]]; then
    fail 'Set CATALINA_HOME or TOMCAT_HOME to your Tomcat 10.1 installation folder.'
fi

# Convert a Windows-style Tomcat path when the script is run from Git Bash.
if command -v cygpath >/dev/null 2>&1 && [[ "$TOMCAT_DIR" =~ ^[A-Za-z]:[\\/].* ]]; then
    TOMCAT_DIR="$(cygpath -u "$TOMCAT_DIR")"
fi

# Tomcat needs JAVA_HOME even when java.exe is already on PATH.
if [[ -z "${JAVA_HOME:-}" ]] && [[ -x "/c/Program Files/Java/jdk-27/bin/java.exe" ]]; then
    export JAVA_HOME="/c/Program Files/Java/jdk-27"
fi
[[ -n "${JAVA_HOME:-}" ]] || fail 'Set JAVA_HOME to your JDK folder before starting Tomcat.'

[[ -d "$TOMCAT_DIR" ]] || fail "Tomcat folder not found: $TOMCAT_DIR"
[[ -d "$TOMCAT_DIR/webapps" ]] || fail "Tomcat webapps folder not found under: $TOMCAT_DIR"

tomcat_action() {
    local action="$1"
    local catalina_script="$TOMCAT_DIR/bin/catalina.sh"

    if [[ -f "$catalina_script" ]]; then
        bash "$catalina_script" "$action"
        return
    fi

    # Git Bash fallback for a Windows Tomcat distribution that has .bat scripts only.
    local batch_file
    if [[ "$action" == "start" ]]; then
        batch_file="$TOMCAT_DIR/bin/startup.bat"
    else
        batch_file="$TOMCAT_DIR/bin/shutdown.bat"
    fi

    if [[ -f "$batch_file" ]] && command -v cmd.exe >/dev/null 2>&1 && command -v cygpath >/dev/null 2>&1; then
        local windows_batch
        windows_batch="$(cygpath -aw "$batch_file")"
        MSYS_NO_PATHCONV=1 cmd.exe //d //c "call \"$windows_batch\""
        return
    fi

    fail "Could not find a usable Tomcat start/stop script under $TOMCAT_DIR/bin"
}

start_app() {
    command -v mvn >/dev/null 2>&1 || fail 'Maven was not found. Install Maven and make sure mvn is on PATH.'

    printf 'Building the DataHive web app...\n'
    (cd "$PROJECT_DIR" && mvn -DskipTests package)
    [[ -f "$WAR_FILE" ]] || fail "Build finished but WAR was not created: $WAR_FILE"

    printf 'Deploying %s to Tomcat...\n' "$APP_NAME"
    cp "$WAR_FILE" "$TOMCAT_DIR/webapps/${APP_NAME}.war"

    printf 'Starting Tomcat...\n'
    tomcat_action start
    printf 'DataHive is starting at http://localhost:8080/%s/\n' "$APP_NAME"
}

stop_app() {
    printf 'Stopping Tomcat...\n'
    tomcat_action stop
    printf 'Tomcat stop requested.\n'
}

case "${1:-}" in
    start)
        start_app
        ;;
    stop)
        stop_app
        ;;
    *)
        printf 'Usage: bash manage-datahive.sh {start|stop}\n' >&2
        printf 'Set CATALINA_HOME to your Tomcat 10.1 folder first.\n' >&2
        exit 2
        ;;
esac
