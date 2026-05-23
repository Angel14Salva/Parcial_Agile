package com.municipalidad.licencias.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "solicitudes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Datos del negocio
    @Column(nullable = false)
    private String razonSocial;

    @Column(nullable = false)
    private String domicilioFiscal;

    @Column(nullable = false)
    private String rubro;

    // Solicitante
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // Documentos
    private String planoUrl;

    // Pago
    private BigDecimal montoPagado;
    private LocalDateTime fechaPago;
    private String referenciaPago;

    // Estado del trámite
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Enums.EstadoTramite estado;

    // Validación SUNAT
    private boolean validadoSunat = false;
    private String motivoRechazoSunat;

    // Timestamps
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;

    // Relaciones
    @OneToMany(mappedBy = "solicitud", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Inspeccion> inspecciones = new ArrayList<>();

    @OneToOne(mappedBy = "solicitud", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Licencia licencia;

    @PrePersist
    void prePersist() {
        this.creadoEn = LocalDateTime.now();
        this.actualizadoEn = LocalDateTime.now();
        if (this.estado == null) this.estado = Enums.EstadoTramite.BORRADOR;
    }

    @PreUpdate
    void preUpdate() {
        this.actualizadoEn = LocalDateTime.now();
    }
}
