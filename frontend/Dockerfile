# syntax=docker/dockerfile:1

# Build: install exactly the locked packages and build the static site.
FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci
COPY . .
RUN npm run build

# Serve: unprivileged nginx (runs as a non-root user on port 8080) with the built files. It
# forwards /api to the backend, so the browser sees one origin and the session and CSRF cookies
# work without cross-origin setup. BACKEND_URL is filled into the config at start-up.
FROM nginxinc/nginx-unprivileged:1.31-alpine
ENV BACKEND_URL=http://backend:8080
COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 8080
