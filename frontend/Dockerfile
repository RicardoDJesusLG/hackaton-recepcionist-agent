# Etapa 1: Compilar la aplicación de Angular
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build -- --configuration=production

# Etapa 2: Servir con Nginx
FROM nginx:1.25-alpine
# Copiar los archivos compilados al directorio de Nginx (dist/frontend/browser en Angular 17/18)
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
# Copiar plantilla de configuración de Nginx (se compilará automáticamente con el PORT de Cloud Run)
COPY nginx.conf /etc/nginx/templates/default.conf.template
EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]
