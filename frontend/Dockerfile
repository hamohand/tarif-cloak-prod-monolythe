# ==================================
# ÉTAPE 1: Construire l'application Angular
# ==================================
FROM node:20-alpine AS build
WORKDIR /app
# Copier les fichiers de dépendances et les installer
COPY package.json package-lock.json ./
RUN npm install
# Copier le reste du code source
COPY . .

# Accepter les variables de configuration comme arguments de build
ARG KEYCLOAK_EXTERNAL_URL
ARG KEYCLOAK_REALM
ARG KEYCLOAK_FRONTEND_CLIENT
ARG API_URL
ARG MARKET_VERSION=DEFAULT

# Les rendre disponibles comme variables d'environnement pour le script generate-env.js
ENV KEYCLOAK_EXTERNAL_URL=${KEYCLOAK_EXTERNAL_URL}
ENV KEYCLOAK_REALM=${KEYCLOAK_REALM}
ENV KEYCLOAK_FRONTEND_CLIENT=${KEYCLOAK_FRONTEND_CLIENT}
ENV API_URL=${API_URL}
ENV MARKET_VERSION=${MARKET_VERSION}

# Construire l'application en mode production
# Le résultat sera dans /app/dist/frontend... (ou un nom similaire)
RUN npm run build

# ==================================
# ÉTAPE 2: Servir l'application avec Nginx
# ==================================
FROM nginx:stable-alpine

# Accepter un argument pour le fichier de configuration Nginx
ARG NGINX_CONF=nginx-dev.conf

# Copier les fichiers statiques construits depuis l'étape précédente
# Assurez-vous que 'dist/frontend' correspond au chemin de sortie de votre build
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

# --- CORRECTION DES PERMISSIONS ---
# S'assurer que l'utilisateur nginx peut lire les fichiers du site
RUN chmod -R 755 /usr/share/nginx/html

# Copier notre configuration de proxy Nginx personnalisée
COPY ${NGINX_CONF} /etc/nginx/conf.d/default.conf

# Exposer le port 80 du conteneur
EXPOSE 80
