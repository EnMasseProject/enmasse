#!/bin/bash
set -e

echo -n "Verify qdrouterd-base version alignment... "

PATHSPEC='./* :!.travis/**'
EXP='/qdrouterd-base:([^\\"]+)'

NUM=$(git grep -E -h -e '/qdrouterd-base:([^\\"]+)' -- $PATHSPEC | cat - | sed -E 's/.*qdrouterd-base:(.*)/\1/g'  | sort -u | wc -l)

test "$NUM" -eq 1 || {
	echo "FAILED"
	echo "Versions misaligned:" >&2
	echo "========================================" >&2
	git grep -E -e '/qdrouterd-base:([^\\"]+)' -- $PATHSPEC | cat -
	echo "========================================" >&2
	exit 1
}

echo "OK"
