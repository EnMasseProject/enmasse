#!/bin/bash

function install_firefox_driver {
    wget https://github.com/mozilla/geckodriver/releases/download/v0.19.1/geckodriver-v0.19.1-linux64.tar.gz
    tar -xvf geckodriver-v0.19.1-linux64.tar.gz
}


function install_chrome_driver {
    wget https://chromedriver.storage.googleapis.com/2.33/chromedriver_linux64.zip
    unzip chromedriver_linux64.zip
}


CURDIR=`readlink -f \`dirname $0\``
rm -rf ${CURDIR}/../web_driver
mkdir ${CURDIR}/../web_driver
ACTUAL_DIR=`pwd`
cd ${CURDIR}/../web_driver
install_firefox_driver
install_chrome_driver
cd $ACTUAL_DIR
