SUBDIRS=activemq

all: $(SUBDIRS)

$(SUBDIRS):
	$(MAKE) -C $@ all

.PHONY: all $(SUBDIRS)
