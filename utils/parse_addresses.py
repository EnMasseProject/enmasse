#!/usr/bin/env python

import json
import sys

if len(sys.argv) < 2:
    print "usage: " + sys.argv[0] + " <json>"
    sys.exit(1)

data = json.loads(sys.argv[1])

for addr in data:
    print addr
