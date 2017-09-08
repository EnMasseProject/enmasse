#! /usr/bin/env ruby

require 'yaml'
require 'json'

def main
  file = ARGV[0]
  outputDir = ARGV[1]
  base = File.basename(file, ".json")
  ext = File.extname(file)
  output = File.open("#{outputDir}/#{base}.yaml", "w")

  content = JSON::load( File.open(file) ).to_yaml
  output << content
end


main()
