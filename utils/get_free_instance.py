#!/usr/bin/env python

import fcntl, glob, os, sys

def is_free(dirname):
    lockfile = dirname + "/lock/cli.lock"
    deletefile = dirname + "/enmasse-deleted"
    if os.path.isdir(dirname):
        if os.path.isfile(deletefile):
            return False

        if os.path.isfile(lockfile):
            try:
                f = open(lockfile, 'w+')
                fcntl.lockf(f, fcntl.LOCK_EX | fcntl.LOCK_NB)
                fcntl.lockf(f, fcntl.LOCK_UN)
                return True
            except Exception as e:
                return False
    else:
        return True

if len(sys.argv) > 1:
    basedir = sys.argv[1]
else:
    basedir = "/var/run/artemis"
if len(sys.argv) > 2:
    basename = sys.argv[2]
else:
    basename = "artemis"
if len(sys.argv) > 3:
    preferred = sys.argv[3]
else:
    preferred = None
n = 1
done = False
instancedir = ""

# first try to lock any matching directory
for f in glob.glob(basedir + basename + '_*'):
    if is_free(f):
        instancedir = f
        done = True

# then, if unsuccessful, try a directory using the hostname
if not done and preferred:
    name = basedir + basename + '_' + preferred
    if is_free(name):
        instancedir = name
        done = True

# then, if still unsuccessful, create a new directory with the lowest unused ordinal
while not done:
    name = "%s_%i" % (basename, n)
    instancedir = basedir + name
    if is_free(instancedir):
        done = True
    else:
        n = n + 1

print instancedir
