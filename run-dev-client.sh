#!/usr/bin/env bash
# Oathfall — launch a development RuneLite client with the plugin loaded.
# First run downloads Gradle and the RuneLite client; later runs are fast.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"

echo
echo "  OATHFALL — development client"
echo "  ------------------------------------------------------------"
command -v java >/dev/null 2>&1 || {
	echo "  [x] Java is not on your PATH. RuneLite needs JDK 11:"
	echo "      https://adoptium.net/temurin/releases/"
	exit 1
}
echo "  Java:   $(java -version 2>&1 | head -1)"
echo "  Plugin: $(pwd)"
echo
echo "  Building and launching. The game window opens when it is ready."
echo
exec ./gradlew run --console=plain
