#!/usr/bin/env bash
# Runs the lesson's calibration test against a throwaway copy of the module. See ../README.md.
#
# Usage: reference/calibrate.sh <record|scaffold|solved|naive> [comma-separated challenge ids]
set -u

mode="${1:?Usage: calibrate.sh <record|scaffold|solved|naive> [ids]}"
export AKITLAB_CAL="$mode"
if [ -n "${2:-}" ]; then
  export AKITLAB_CAL_ONLY="$2"
fi

reference="$(cd "$(dirname "$0")" && pwd)"
module="$reference/../../../modules/advantagekit-challenges"
work="$reference/work"

rm -rf "$work"
mkdir -p "$work"
(cd "$module" && tar --exclude=./build --exclude=./logs --exclude=./.gradle --exclude=./dashboard -cf - .) |
  (cd "$work" && tar -xf -)

if [ "$mode" = "solved" ] || [ "$mode" = "naive" ]; then
  cp -r "$reference/solutions/." "$work/src/main/java/frc/robot/challenges/"
fi
if [ "$mode" = "naive" ]; then
  cp -r "$reference/naive/." "$work/src/main/java/frc/robot/challenges/"
fi
mkdir -p "$work/src/test/java/frc/robot/lab"
cp "$reference/CalibrationTest.java" "$work/src/test/java/frc/robot/lab/"

cd "$work" || exit 1
chmod +x gradlew
# shellcheck disable=SC2086 # GRADLE_ARGS is a list of arguments.
./gradlew test ${GRADLE_ARGS:-} --console=plain -i --rerun-tasks >calibration-output.txt 2>&1
code=$?
grep -E "^ *(OK|NOPE|pass|FAIL|ALL OK|PROBLEMS|ERROR|TIMED OUT|wrote|replay error)|error:|BUILD " calibration-output.txt

if [ "$mode" = "record" ] && [ "$code" -eq 0 ]; then
  cp src/main/deploy/match-logs/*.wpilog "$module/src/main/deploy/match-logs/"
  echo "Copied the new logs into the module."
fi
exit "$code"
