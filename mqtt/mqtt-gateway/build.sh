#!/bin/sh
VERSION=$1
fail=0

if [ -n "${VERSION}" ]
then
    mvn versions:set -DgenerateBackupPoms=false -DnewVersion=${VERSION}
fi

docker pull ppatierno/qdrouterd:0.8.0-repo || fail=1
docker run -d --name qdrouterd -p 5672:5672 -p 55673:55673 -v $PWD/src/test/resources:/conf ppatierno/qdrouterd:0.8.0-repo qdrouterd --conf /conf/qdrouterd.conf || fail=1

trap "docker stop qdrouterd; docker rm qdrouterd" EXIT

mvn test package -B || fail=1

exit $fail
