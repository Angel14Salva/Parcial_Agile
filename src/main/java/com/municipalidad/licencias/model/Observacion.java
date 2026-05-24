package com.municipalidad.licencias.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "observaciones")
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

    private String documentoCorregidoUrl;
    private boolean subsanada = false;
    private LocalDateTime fechaSubsanacion;
    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() { this.creadoEn = LocalDateTime.now(); }

    public Observacion() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Inspeccion inspeccion;
        private Enums.TipoObservacion tipo;
        private String descripcion;

        public Builder inspeccion(Inspeccion v)      { this.inspeccion = v; return this; }
        public Builder tipo(Enums.TipoObservacion v) { this.tipo = v; return this; }
        public Builder descripcion(String v)         { this.descripcion = v; return this; }

        public Observacion build() {
            Observacion o = new Observacion();
            o.inspeccion = this.inspeccion;
            o.tipo = this.tipo;
            o.descripcion = this.descripcion;
            return o;
        }
    }

    public Long getId()                        { return id; }
    public Inspeccion getInspeccion()          { return inspeccion; }
    public Enums.TipoObservacion getTipo()     { return tipo; }
    public String getDescripcion()             { return descripcion; }
    public boolean isSubsanada()               { return subsanada; }
    public void setSubsanada(boolean v)        { this.subsanada = v; }
    public LocalDateTime getFechaSubsanacion() { return fechaSubsanacion; }
    public void setFechaSubsanacion(LocalDateTime v) { this.fechaSubsanacion = v; }
    public void setDocumentoCorregidoUrl(String v)   { this.documentoCorregidoUrl = v; }
}
