#!/usr/bin/env python

import yaml
import json
import sys
import os

input_file = sys.argv[1]
output_dir = sys.argv[2]

base = os.path.basename(input_file)
output_file = base[0:len(base) - 5] + ".yaml"

print "Converting", input_file, "to", os.path.join(output_dir, output_file)
output = yaml.dump(yaml.load(json.dumps(json.loads(open(input_file).read()))), default_flow_style=False)
f = open(os.path.join(output_dir, output_file), 'w')
f.write(output)
f.close()
