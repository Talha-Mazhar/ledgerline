# syntax=docker/dockerfile:1

# Build: install exactly the locked packages and build the static site.
FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci
COPY . .
RUN npm run build

# Serve: nginx with the built files. It also forwards /api to the backend, so the browser sees one
# origin and the session and CSRF cookies work without any cross-origin setup.
FROM nginx:1.29-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
