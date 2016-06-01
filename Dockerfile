FROM node:0.12
RUN npm install debug && npm install rhea
COPY ragent.js /usr/sbin/ragent.js
COPY future.js /usr/sbin/future.js
COPY router.js /usr/sbin/router.js
COPY kube_utils.js /usr/sbin/kube_utils.js
COPY utils.js /usr/sbin/utils.js
EXPOSE 55672
CMD ["node", "/usr/sbin/ragent.js"]