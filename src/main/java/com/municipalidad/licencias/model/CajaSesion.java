package com.municipalidad.licencias.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "caja_sesiones")
public class CajaSesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cajero_id", nullable = false)
    private Usuario cajero;

    @Column(precision = 10, scale = 2)
    private BigDecimal montoApertura;

    @Column(nullable = false)
    private LocalDateTime fechaApertura;

    private BigDecimal montoCierreContado;
    private BigDecimal montoCierreEsperado;
    private BigDecimal diferencia;
    private LocalDateTime fechaCierre;

    @Column(columnDefinition = "TEXT")
    private String observacionesCajero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Enums.EstadoSesionCaja estado = Enums.EstadoSesionCaja.PENDIENTE_APERTURA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revisado_por_id")
    private Usuario revisadoPor;

    private LocalDateTime fechaRevision;

    @Column(columnDefinition = "TEXT")
    private String comentarioAdmin;

    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() {
        this.creadoEn = LocalDateTime.now();
        if (this.fechaApertura == null) this.fechaApertura = this.creadoEn;
    }

    public CajaSesion() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Usuario cajero;
        private BigDecimal montoApertura;

        public Builder cajero(Usuario v)         { this.cajero = v; return this; }
        public Builder montoApertura(BigDecimal v) { this.montoApertura = v; return this; }

        public CajaSesion build() {
            CajaSesion s = new CajaSesion();
            s.cajero = this.cajero;
            s.montoApertura = this.montoApertura;
            return s;
        }
    }

    public Long getId()                          { return id; }
    public Usuario getCajero()                    { return cajero; }
    public BigDecimal getMontoApertura()          { return montoApertura; }
    public void setMontoApertura(BigDecimal v)    { this.montoApertura = v; }
    public LocalDateTime getFechaApertura()       { return fechaApertura; }
    public void setFechaApertura(LocalDateTime v) { this.fechaApertura = v; }
    public BigDecimal getMontoCierreContado()     { return montoCierreContado; }
    public void setMontoCierreContado(BigDecimal v) { this.montoCierreContado = v; }
    public BigDecimal getMontoCierreEsperado()    { return montoCierreEsperado; }
    public void setMontoCierreEsperado(BigDecimal v) { this.montoCierreEsperado = v; }
    public BigDecimal getDiferencia()             { return diferencia; }
    public void setDiferencia(BigDecimal v)       { this.diferencia = v; }
    public LocalDateTime getFechaCierre()         { return fechaCierre; }
    public void setFechaCierre(LocalDateTime v)   { this.fechaCierre = v; }
    public String getObservacionesCajero()        { return observacionesCajero; }
    public void setObservacionesCajero(String v)  { this.observacionesCajero = v; }
    public Enums.EstadoSesionCaja getEstado()     { return estado; }
    public void setEstado(Enums.EstadoSesionCaja v) { this.estado = v; }
    public Usuario getRevisadoPor()               { return revisadoPor; }
    public void setRevisadoPor(Usuario v)         { this.revisadoPor = v; }
    public LocalDateTime getFechaRevision()       { return fechaRevision; }
    public void setFechaRevision(LocalDateTime v) { this.fechaRevision = v; }
    public String getComentarioAdmin()            { return comentarioAdmin; }
    public void setComentarioAdmin(String v)      { this.comentarioAdmin = v; }
    public LocalDateTime getCreadoEn()            { return creadoEn; }
}
