FROM node:6-alpine
RUN npm install debug && npm install rhea
COPY ragent.js future.js router.js kube_utils.js utils.js /usr/sbin/
EXPOSE 55672
CMD ["node", "/usr/sbin/ragent.js"]
