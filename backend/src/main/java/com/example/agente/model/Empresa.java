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

    @Column(name = "suscripcion_activa", nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private Boolean suscripcionActiva = true;

    @Column(name = "promocion_activa", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean promocionActiva = false;

    @Column(name = "promocion_descripcion", columnDefinition = "TEXT")
    private String promocionDescripcion;

    @Column(name = "telefono_contacto", length = 20)
    private String telefonoContacto;

    @Column(name = "maps_link", columnDefinition = "TEXT")
    private String mapsLink;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;
}
