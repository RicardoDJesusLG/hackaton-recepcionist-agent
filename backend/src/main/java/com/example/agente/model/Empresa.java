package com.example.agente.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "empresas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "whatsapp_phone_id", unique = true, nullable = false, length = 50)
    private String whatsappPhoneId;

    @Column(name = "whatsapp_token", columnDefinition = "TEXT")
    private String whatsappToken;

    @Column(name = "direccion", columnDefinition = "TEXT")
    private String direccion;

    @Column(name = "descripcion_negocio", columnDefinition = "TEXT")
    private String descripcionNegocio;

    @Column(name = "suscripcion_activa", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean suscripcionActiva = false;



    @Column(name = "plan_suscripcion", nullable = false, columnDefinition = "varchar(50) default 'BASIC'")
    @Builder.Default
    private String planSuscripcion = "BASIC";

    @Column(name = "telefono_contacto", length = 20)
    private String telefonoContacto;

    @Column(name = "maps_link", columnDefinition = "TEXT")
    private String mapsLink;

    @Column(name = "requiere_nombre", nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private Boolean requiereNombre = true;

    @Column(name = "requiere_telefono", nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private Boolean requiereTelefono = true;

    @Column(name = "requiere_correo", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean requiereCorreo = false;

    @Column(name = "url_menu_imagen", columnDefinition = "TEXT")
    private String urlMenuImagen;

    @Column(name = "activar_envio_menu", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean activarEnvioMenu = false;

    @Column(name = "envio_menu_inmediato", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean envioMenuInmediato = false;

    @Column(name = "stripe_customer_id", length = 255)
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id", length = 255)
    private String stripeSubscriptionId;

    @Column(name = "fecha_inicio_suscripcion")
    private LocalDateTime fechaInicioSuscripcion;

    @Column(name = "fecha_fin_suscripcion")
    private LocalDateTime fechaFinSuscripcion;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;
}
