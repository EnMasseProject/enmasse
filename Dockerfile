FROM rhscl/nodejs-4-rhel7

RUN ["/bin/bash", "-c", "npm install debug && npm install bluebird && npm install rhea"]

ADD subserv.tgz /opt/app-root/src/
EXPOSE 5672
CMD /bin/bash -c 'node subserv.js'
#
#CMD ["node", "/usr/local/subserv/subserv.js"]
