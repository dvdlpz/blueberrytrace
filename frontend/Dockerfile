# syntax=docker/dockerfile:1

FROM node:22-alpine AS build
WORKDIR /workspace

COPY package.json package-lock.json ./
COPY frontend/package.json ./frontend/package.json
RUN npm ci --workspace frontend

COPY frontend ./frontend
RUN npm run frontend:build

FROM nginx:1.30.3-alpine AS runtime
COPY --from=build /workspace/frontend/dist /usr/share/nginx/html

EXPOSE 80 443
