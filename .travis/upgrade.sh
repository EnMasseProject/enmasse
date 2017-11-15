#!/bin/sh
#sudo do-release-upgrade -f DistUpgradeViewNonInteractive
#sudo apt-get -qq update
#sudo apt-get install -y python-apt autoconf pkg-config e2fslibs-dev libblkid-dev zlib1g-dev liblzo2-dev asciidoc

# # dash? wtf is dash? UGH! use a real shell
#  sudo rm /bin/sh
#  sudo ln -s  /bin/bash /bin/sh
#
#  # install devmapper from scratch
#  cd $HOME
#  git clone http://sourceware.org/git/lvm2.git
#  cd lvm2
#  ./configure
#  sudo make install_device-mapper
#  cd ..
#
#  #  build btrfs from scratch
#  git clone https://github.com/kdave/btrfs-progs.git
#  cd btrfs-progs
#  ./autogen.sh
#  ./configure
#  make
#  sudo make install
#  cd $TRAVIS_BUILD_DIR
#
#  # now install deps
#  wget -O /tmp/glide.tar.gz $GLIDE_TARBALL
#  tar xfv /tmp/glide.tar.gz -C /tmp
#  sudo mv $(find /tmp -name "glide") /usr/bin
#
#  # install golint
#  go get -u github.com/golang/lint/golint
#
#  # install goveralls for coveralls integration
#  go get github.com/mattn/goveralls
#
#elif [[ "$action" == "before_script" ]]; then
#  echo "================================="
#  echo "          Before Script          "
#  echo "================================="
#  sudo ufw disable
