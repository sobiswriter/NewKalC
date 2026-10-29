FROM node:22-slim
WORKDIR /app
COPY package.json index.mjs ./
USER node
ENV NODE_ENV=production
CMD ["node", "index.mjs"]
