FROM rhscl/nodejs-4-rhel7

RUN ["/bin/bash", "-c", "npm install debug && npm install rhea"]

COPY ragent.js future.js router.js kube_utils.js utils.js /opt/app-root/src/
EXPOSE 55672

CMD ["/bin/bash", "-c", "node ragent.js"]
