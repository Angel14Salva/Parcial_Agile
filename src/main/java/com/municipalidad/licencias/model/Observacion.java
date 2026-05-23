package com.municipalidad.licencias.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "observaciones")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Observacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspeccion_id", nullable = false)
    private Inspeccion inspeccion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Enums.TipoObservacion tipo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    // Solo si tipo == DOCUMENTAL: URL del archivo corregido
    private String documentoCorregidoUrl;

    private boolean subsanada = false;
    private LocalDateTime fechaSubsanacion;

    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() {
        this.creadoEn = LocalDateTime.now();
    }
}
