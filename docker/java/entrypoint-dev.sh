#!/bin/sh

# Poll for source file changes and recompile
# This works reliably across Windows/Docker bind mounts
watch_and_compile() {
    LAST_HASH=""
    while true; do
        # Get hash of all Java files
        CURRENT_HASH=$(find /workspace/src -name "*.java" -exec md5sum {} \; 2>/dev/null | sort | md5sum)
        if [ "$CURRENT_HASH" != "$LAST_HASH" ] && [ -n "$LAST_HASH" ]; then
            echo "[entrypoint] Source change detected, recompiling..."
            ./gradlew classes -x test --quiet 2>/dev/null
        fi
        LAST_HASH="$CURRENT_HASH"
        sleep 2
    done
}

# Start polling watcher in background
watch_and_compile &

# Run the app (DevTools will restart when classes change)
./gradlew bootRun
