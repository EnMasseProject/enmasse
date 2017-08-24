#! /usr/bin/env ruby

require 'yaml'
require 'json'

def main
  file = ARGV[0]
  outputFile = ARGV[1]
  output = File.open(outputFile, "w")

  content = JSON::load( File.open(file) ).to_yaml
  output << content
end


main()
