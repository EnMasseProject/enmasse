#!/bin/sh
NAME=$1

FILENAME=`basename $NAME`

if [ -f $FILENAME ]
then
    echo "$FILENAME already downloaded, skipping"
else
    wget $NAME
fi
