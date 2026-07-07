# CONFIGURACION_STRIPE.md — Configuración Técnica de la Integración

Este documento detalla la configuración técnica del backend y del frontend para la integración oficial de la pasarela de pagos y suscripciones de Stripe en modo Test.

---

## 1. Dependencias del Backend
En el archivo `pom.xml` de Spring Boot, se importó el SDK oficial para Java (versión `28.0.0`):

```xml
<dependency>
    <groupId>com.stripe</groupId>
    <artifactId>stripe-java</artifactId>
    <version>28.0.0</version>
</dependency>
```

---

## 2. Configuración en `application.properties`
Las credenciales de Stripe se inyectan dinámicamente mediante las siguientes propiedades:

*   `stripe.api.key`: La clave secreta privada (`sk_test_...`) usada para autenticar y firmar peticiones contra la API REST de Stripe.
*   `stripe.webhook.secret`: El secreto de firma de webhook (`whsec_...`) usado para verificar criptográficamente que los eventos recibidos provienen genuinamente de Stripe.

---

## 3. Inyección y Flujo de Inicialización
La clave de API se inyectan de forma segura a través de una clase de configuración:

```java
@Configuration
public class StripeConfig {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }
}
```

Al arrancar la aplicación, `@PostConstruct` asigna de forma estática `Stripe.apiKey`, autenticando globalmente el SDK para cualquier transacción (creación de Checkouts y sesiones de Customer Portal).
