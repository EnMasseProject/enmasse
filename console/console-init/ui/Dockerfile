FROM node:10.16.3

# install yarn
RUN npm install -g yarn

# make the 'app' folder the current working directory
WORKDIR /app

# copy package.json
COPY package.json ./

# install project dependencies
RUN yarn install

# copy project files and folders to the current working directory (i.e. 'app' folder)
COPY . .

# run the app

EXPOSE 3000
CMD ["yarn", "start"]