#!/usr/bin/env bash
# wait-for-grid.sh
set -e
cmd="$@"
while ! curl -sSL "http://localhost:4444/wd/hub/status" 2>&1 \
        | jq -r '.value.ready' 2>&1 | grep "true" >/dev/null; do
    echo 'Waiting for the Grid'
    sleep 1
done
>&2 echo "Selenium Grid is up - executing tests"
exec $cmd

java -Djava.security.egd=file:/dev/./urandom -jar -Dserver.port=8080 /app.jar