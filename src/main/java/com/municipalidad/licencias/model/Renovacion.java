package com.municipalidad.licencias.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "renovaciones")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Renovacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "licencia_id", nullable = false)
    private Licencia licencia;

    @Column(nullable = false)
    private BigDecimal montoPagado;

    @Column(nullable = false)
    private String referenciaPago;

    @Column(nullable = false)
    private LocalDate nuevaFechaVencimiento;

    private LocalDateTime fechaPago;

    @PrePersist
    void prePersist() {
        this.fechaPago = LocalDateTime.now();
    }
}
