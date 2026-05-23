package com.municipalidad.licencias.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "renovaciones")
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
    void prePersist() { this.fechaPago = LocalDateTime.now(); }

    public Renovacion() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Licencia licencia;
        private BigDecimal montoPagado;
        private String referenciaPago;
        private LocalDate nuevaFechaVencimiento;

        public Builder licencia(Licencia v)               { this.licencia = v; return this; }
        public Builder montoPagado(BigDecimal v)          { this.montoPagado = v; return this; }
        public Builder referenciaPago(String v)           { this.referenciaPago = v; return this; }
        public Builder nuevaFechaVencimiento(LocalDate v) { this.nuevaFechaVencimiento = v; return this; }

        public Renovacion build() {
            Renovacion r = new Renovacion();
            r.licencia = this.licencia;
            r.montoPagado = this.montoPagado;
            r.referenciaPago = this.referenciaPago;
            r.nuevaFechaVencimiento = this.nuevaFechaVencimiento;
            return r;
        }
    }

    public Long getId()                      { return id; }
    public Licencia getLicencia()            { return licencia; }
    public BigDecimal getMontoPagado()       { return montoPagado; }
    public String getReferenciaPago()        { return referenciaPago; }
    public LocalDate getNuevaFechaVencimiento() { return nuevaFechaVencimiento; }
}
