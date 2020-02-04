#!/bin/bash
set -e
echo "Collecting test reports"
mkdir -p artifacts/test-reports
cp -r /tmp/testlogs artifacts
for i in `find . -name "TEST-*.xml"`
do
    cp ${i} artifacts/test-reports
done
echo $(sudo journalctl -xe) > /artifacts/test-reports/journal_dump.txt
zip -r test-logs.zip artifacts
