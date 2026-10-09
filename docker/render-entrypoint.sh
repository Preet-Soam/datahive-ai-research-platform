#!/bin/sh
set -eu

port="${PORT:-10000}"
case "$port" in
    ''|*[!0-9]*)
        echo "PORT must be a number; received: $port" >&2
        exit 1
        ;;
esac

server_config="${CATALINA_HOME:-/usr/local/tomcat}/conf/server.xml"
if [ ! -f "$server_config" ]; then
    echo "Tomcat server configuration was not found: $server_config" >&2
    exit 1
fi

sed -i "s/port=\"8080\"/port=\"$port\"/" "$server_config"
exec "${CATALINA_HOME:-/usr/local/tomcat}/bin/catalina.sh" run
