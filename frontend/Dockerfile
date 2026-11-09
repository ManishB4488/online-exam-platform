# ---- Stage 1: build the React app ----
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# ---- Stage 2: serve the built files with nginx ----
# The "slim" image has only the parts nginx needs for a React app,
# so it has far fewer packages that can have security problems
FROM nginx:stable-alpine-slim
# Install the newest security patches of the operating system packages
RUN apk upgrade --no-cache
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80