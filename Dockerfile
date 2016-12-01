FROM node:0.12
# RUN npm install debug && npm install bluebird && npm install rhea
RUN npm install debug && npm install bluebird && npm install https://github.com/grs/rhea
ADD subserv.tgz /usr/local/subserv
EXPOSE 5672
CMD ["node", "/usr/local/subserv/subserv.js"]