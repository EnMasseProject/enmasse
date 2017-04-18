TAG?=latest

ifdef TRAVIS_TAG
	TAG=$(TRAVIS_TAG)
endif

all:
	tar -czf console-$(TAG).tar.gz bin lib www package.json

clean:
	rm -rf console-$(TAG).tar.gz
