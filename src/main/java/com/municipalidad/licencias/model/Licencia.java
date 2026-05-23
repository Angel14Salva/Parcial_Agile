package com.municipalidad.licencias.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "licencias")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Licencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String numeroLicencia;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private Solicitud solicitud;

    @Column(nullable = false)
    private LocalDate fechaEmision;

    @Column(nullable = false)
    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Enums.EstadoLicencia estado = Enums.EstadoLicencia.VIGENTE;

    // Revocación
    private String motivoRevocacion;
    private LocalDateTime fechaRevocacion;

    // Historial de renovaciones
    @OneToMany(mappedBy = "licencia", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Renovacion> renovaciones = new ArrayList<>();

    // Inspecciones de oficio
    @OneToMany(mappedBy = "licencia", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Inspeccion> inspeccionesOficio = new ArrayList<>();

    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() {
        this.creadoEn = LocalDateTime.now();
    }
}
