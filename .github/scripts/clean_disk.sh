#!/bin/bash
set -e

#Remove github thrash

echo "=============================================================================="
echo "Freeing up disk space on CI system"
echo "=============================================================================="
sudo rm -rf "/usr/local/share/boost"
sudo rm -rf "$AGENT_TOOLSDIRECTORY"
echo "Listing 100 largest packages"
dpkg-query -Wf '${Installed-Size}\t${Package}\n' | sort -n | tail -n 100
df -h
echo "Removing large packages"
sudo apt-get remove -y '^ghc-8.*'
sudo apt-get remove -y '^dotnet-.*'
sudo apt-get remove -y '^llvm-.*'
sudo apt-get remove -y 'php.*'
sudo apt-get remove -y azure-cli google-cloud-sdk hhvm google-chrome-stable firefox powershell mono-devel monodoc-http
sudo apt-get autoremove -y
sudo apt-get clean -y
df -h

echo "Removing large directories"
# deleting 15GB
rm -rf /usr/share/dotnet/

df -h
