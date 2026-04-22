FROM node:20-alpine AS development-dependencies-env
COPY . /app
WORKDIR /app
RUN npm ci

FROM node:20-alpine AS production-dependencies-env
COPY ./package.json package-lock.json /app/
WORKDIR /app
RUN npm ci --omit=dev

FROM node:20-alpine AS build-env
COPY . /app/
COPY --from=development-dependencies-env /app/node_modules /app/node_modules
WORKDIR /app

# Build-time config (baked into the static bundle).
# These are used by import.meta.env.* on the client.
ARG VITE_API_BASE_URL
ARG VITE_SSH_HOST
ARG VITE_SSH_USER
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_SSH_HOST=$VITE_SSH_HOST
ENV VITE_SSH_USER=$VITE_SSH_USER

RUN npm run build

FROM node:20-alpine
COPY ./package.json package-lock.json /app/
COPY --from=production-dependencies-env /app/node_modules /app/node_modules
COPY --from=build-env /app/build /app/build
WORKDIR /app
CMD ["npm", "run", "start"]