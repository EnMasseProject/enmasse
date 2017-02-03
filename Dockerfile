FROM node:6-alpine
RUN apk add --no-cache git
RUN npm install debug && npm install bluebird && npm install rhea
ADD subserv.tgz /usr/local/subserv
EXPOSE 5672
CMD ["node", "/usr/local/subserv/subserv.js"]
