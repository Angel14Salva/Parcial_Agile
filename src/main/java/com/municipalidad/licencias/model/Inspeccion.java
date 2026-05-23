package com.municipalidad.licencias.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inspecciones")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Inspeccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private Solicitud solicitud;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspector_id")
    private Usuario inspector;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Enums.TipoInspeccion tipo;

    @Column(nullable = false)
    private LocalDate fechaProgramada;

    private LocalDateTime fechaRealizada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Enums.ResultadoInspeccion resultado = Enums.ResultadoInspeccion.PENDIENTE;

    private String notasInspector;

    // Solo para inspecciones de oficio (licencias ya emitidas)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "licencia_id")
    private Licencia licencia;

    private boolean licenciaVisible;

    @OneToMany(mappedBy = "inspeccion", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Observacion> observaciones = new ArrayList<>();

    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() {
        this.creadoEn = LocalDateTime.now();
    }
}
