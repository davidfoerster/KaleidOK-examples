#!/bin/sh
set -eu
case "$0" in
  */*)
    SCRIPT_PATH="$0";;
  *)
    SCRIPT_PATH="`command -v "$0"`";;
esac
SCRIPT_PATH="`readlink -e -- "$SCRIPT_PATH"`"

cd "${SCRIPT_PATH%/*}"
exec java -jar kaleidoscope.jar "$@"
