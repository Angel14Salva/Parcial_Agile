package com.municipalidad.licencias.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inspecciones")
public class Inspeccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_id")
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
    private Enums.ResultadoInspeccion resultado = Enums.ResultadoInspeccion.PENDIENTE;

    private String notasInspector;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "licencia_id")
    private Licencia licencia;

    private boolean licenciaVisible;

    @OneToMany(mappedBy = "inspeccion", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Observacion> observaciones = new ArrayList<>();

    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() { this.creadoEn = LocalDateTime.now(); }

    public Inspeccion() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Solicitud solicitud;
        private Usuario inspector;
        private Enums.TipoInspeccion tipo;
        private LocalDate fechaProgramada;
        private Enums.ResultadoInspeccion resultado = Enums.ResultadoInspeccion.PENDIENTE;
        private Licencia licencia;
        private LocalDateTime fechaRealizada;
        private String notasInspector;
        private boolean licenciaVisible;

        public Builder solicitud(Solicitud v)    { this.solicitud = v; return this; }
        public Builder inspector(Usuario v)      { this.inspector = v; return this; }
        public Builder tipo(Enums.TipoInspeccion v) { this.tipo = v; return this; }
        public Builder fechaProgramada(LocalDate v)  { this.fechaProgramada = v; return this; }
        public Builder resultado(Enums.ResultadoInspeccion v) { this.resultado = v; return this; }
        public Builder licencia(Licencia v)      { this.licencia = v; return this; }
        public Builder fechaRealizada(LocalDateTime v) { this.fechaRealizada = v; return this; }
        public Builder notasInspector(String v)  { this.notasInspector = v; return this; }
        public Builder licenciaVisible(boolean v){ this.licenciaVisible = v; return this; }

        public Inspeccion build() {
            Inspeccion i = new Inspeccion();
            i.solicitud = this.solicitud;
            i.inspector = this.inspector;
            i.tipo = this.tipo;
            i.fechaProgramada = this.fechaProgramada;
            i.resultado = this.resultado;
            i.licencia = this.licencia;
            i.fechaRealizada = this.fechaRealizada;
            i.notasInspector = this.notasInspector;
            i.licenciaVisible = this.licenciaVisible;
            return i;
        }
    }

    public Long getId()                              { return id; }
    public Solicitud getSolicitud()                  { return solicitud; }
    public Usuario getInspector()                    { return inspector; }
    public Enums.TipoInspeccion getTipo()            { return tipo; }
    public LocalDate getFechaProgramada()            { return fechaProgramada; }
    public void setFechaProgramada(LocalDate v)      { this.fechaProgramada = v; }
    public LocalDateTime getFechaRealizada()         { return fechaRealizada; }
    public Enums.ResultadoInspeccion getResultado()  { return resultado; }
    public String getNotasInspector()                { return notasInspector; }
    public Licencia getLicencia()                    { return licencia; }
    public boolean isLicenciaVisible()               { return licenciaVisible; }
    public List<Observacion> getObservaciones()      { return observaciones; }

    public void setResultado(Enums.ResultadoInspeccion v) { this.resultado = v; }
    public void setFechaRealizada(LocalDateTime v)   { this.fechaRealizada = v; }
    public void setNotasInspector(String v)          { this.notasInspector = v; }
    public void setLicenciaVisible(boolean v)        { this.licenciaVisible = v; }
}
