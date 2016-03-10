#!/usr/bin/env python

import fcntl, os, sys

if len(sys.argv) > 1:
    basedir = sys.argv[1]
else:
    basedir = "var/run/qpidd/"
if len(sys.argv) > 2:
    basename = sys.argv[2]
else:
    basename = "qpidd"
n = 1
done = False
name = ""

while not done:
    name = "%s_%i" % (basename, n)
    datadir = basedir + name
    lockfile = datadir + "/lock"
    if os.path.isdir(datadir) and os.path.isfile(lockfile):
        try:
            f = open(lockfile, 'w+')
            fcntl.lockf(f, fcntl.LOCK_EX | fcntl.LOCK_NB)
            fcntl.lockf(f, fcntl.LOCK_UN)
            done = True
        except Exception as e:
            n = n + 1
    else:
        done = True
        
print name
