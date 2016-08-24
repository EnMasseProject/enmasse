FROM node:0.12
RUN npm install debug && npm install rhea && npm install bluebird
ADD subserv.tgz /usr/local/subserv
EXPOSE 5672
CMD ["node", "/usr/local/subserv/subserv.js"]