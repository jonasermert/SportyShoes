#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
mvn clean
(cd frontend && npm ci && npm run build)
mkdir -p target/classes/static
cp -R frontend/dist/. target/classes/static/
mvn package
