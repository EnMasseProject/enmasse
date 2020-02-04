#!/bin/bash
set -e
echo "Collecting test reports"
echo $(journalctl -xe) > /tmp/testlogs/journal_dump.txt
mkdir -p artifacts/test-reports
cp -r /tmp/testlogs artifacts
for i in `find . -name "TEST-*.xml"`
do
    cp ${i} artifacts/test-reports
done
zip -r test-logs.zip artifacts
