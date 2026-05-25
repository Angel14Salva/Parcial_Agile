package com.municipalidad.licencias.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "licencias")
public class Licencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String numeroLicencia;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private Solicitud solicitud;

    @Column(nullable = false)
    private LocalDate fechaEmision;

    @Column(nullable = false)
    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Enums.EstadoLicencia estado = Enums.EstadoLicencia.VIGENTE;

    private String motivoRevocacion;
    private LocalDateTime fechaRevocacion;
    private LocalDateTime creadoEn;

    @OneToMany(mappedBy = "licencia", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Renovacion> renovaciones = new ArrayList<>();

    @OneToMany(mappedBy = "licencia", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Inspeccion> inspeccionesOficio = new ArrayList<>();

    @PrePersist
    void prePersist() { this.creadoEn = LocalDateTime.now(); }

    public Licencia() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String numeroLicencia;
        private Solicitud solicitud;
        private LocalDate fechaEmision;
        private LocalDate fechaVencimiento;
        private Enums.EstadoLicencia estado = Enums.EstadoLicencia.VIGENTE;

        public Builder numeroLicencia(String v)   { this.numeroLicencia = v; return this; }
        public Builder solicitud(Solicitud v)      { this.solicitud = v; return this; }
        public Builder fechaEmision(LocalDate v)   { this.fechaEmision = v; return this; }
        public Builder fechaVencimiento(LocalDate v){ this.fechaVencimiento = v; return this; }
        public Builder estado(Enums.EstadoLicencia v) { this.estado = v; return this; }

        public Licencia build() {
            Licencia l = new Licencia();
            l.numeroLicencia = this.numeroLicencia;
            l.solicitud = this.solicitud;
            l.fechaEmision = this.fechaEmision;
            l.fechaVencimiento = this.fechaVencimiento;
            l.estado = this.estado;
            return l;
        }
    }

    public Long getId()                       { return id; }
    public String getNumeroLicencia()         { return numeroLicencia; }
    public Solicitud getSolicitud()           { return solicitud; }
    public LocalDate getFechaEmision()        { return fechaEmision; }
    public LocalDate getFechaVencimiento()    { return fechaVencimiento; }
    public Enums.EstadoLicencia getEstado()   { return estado; }
    public String getMotivoRevocacion()       { return motivoRevocacion; }
    public LocalDateTime getFechaRevocacion() { return fechaRevocacion; }

    public void setEstado(Enums.EstadoLicencia v)    { this.estado = v; }
    public void setFechaVencimiento(LocalDate v)     { this.fechaVencimiento = v; }
    public void setMotivoRevocacion(String v)        { this.motivoRevocacion = v; }
    public void setFechaRevocacion(LocalDateTime v)  { this.fechaRevocacion = v; }
}
