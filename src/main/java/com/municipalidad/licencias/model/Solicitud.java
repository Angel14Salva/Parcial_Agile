package com.municipalidad.licencias.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "solicitudes")
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Sección II: Datos del solicitante ─────────────────────────────────────
    @Column(nullable = false)
    private String razonSocial;

    @Column(nullable = false)
    private String domicilioFiscal;

    @Enumerated(jakarta.persistence.EnumType.STRING)
    private Enums.Distrito distrito;
    private String codigoSeguimiento;
    private String ruc;
    private String dni;
    private String telefono;
    private String correoElectronico;

    // ── Sección III: Representante legal ──────────────────────────────────────
    private String nombreRepresentante;
    private String dniRepresentante;
    private String partidaSunarp;

    // ── Sección IV: Datos del establecimiento ─────────────────────────────────
    @Column(nullable = false)
    private String rubro;

    private String nombreComercial;
    private String direccionEstablecimiento;
    private String horarioAtencion;
    private Double areaTotalM2;
    private Integer numEstacionamientos;

    // ── Modalidad ─────────────────────────────────────────────────────────────
    private String modalidadTramite = "NUEVA";

    // ── Documentos ────────────────────────────────────────────────────────────
    private String planoUrl;
    private String firmaUrl;

    // ── Observaciones del solicitante ─────────────────────────────────────────
    @Column(columnDefinition = "TEXT")
    private String observacionesSolicitante;

    // ── Pago ──────────────────────────────────────────────────────────────────
    private BigDecimal montoPagado;
    private LocalDateTime fechaPago;
    private String referenciaPago;

    // ── Estado y validaciones ─────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Enums.EstadoTramite estado;

    private boolean validadoSunat = false;
    private String motivoRechazoSunat;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @OneToMany(mappedBy = "solicitud", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
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
    void preUpdate() { this.actualizadoEn = LocalDateTime.now(); }

    public Solicitud() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String razonSocial, domicilioFiscal, rubro, ruc, dni;
        private String telefono, correoElectronico, nombreRepresentante, dniRepresentante;
        private String nombreComercial, direccionEstablecimiento, horarioAtencion;
        private String modalidadTramite, observacionesSolicitante, partidaSunarp;
        private Double areaTotalM2;
        private Integer numEstacionamientos;
        private Usuario usuario;
        private Enums.EstadoTramite estado;

        public Builder razonSocial(String v)              { this.razonSocial = v; return this; }
        public Builder domicilioFiscal(String v)          { this.domicilioFiscal = v; return this; }
        public Builder rubro(String v)                    { this.rubro = v; return this; }
        public Builder ruc(String v)                      { this.ruc = v; return this; }
        public Builder dni(String v)                      { this.dni = v; return this; }
        public Builder telefono(String v)                 { this.telefono = v; return this; }
        public Builder correoElectronico(String v)        { this.correoElectronico = v; return this; }
        public Builder nombreRepresentante(String v)      { this.nombreRepresentante = v; return this; }
        public Builder dniRepresentante(String v)         { this.dniRepresentante = v; return this; }
        public Builder partidaSunarp(String v)            { this.partidaSunarp = v; return this; }
        public Builder nombreComercial(String v)          { this.nombreComercial = v; return this; }
        public Builder direccionEstablecimiento(String v) { this.direccionEstablecimiento = v; return this; }
        public Builder horarioAtencion(String v)          { this.horarioAtencion = v; return this; }
        public Builder areaTotalM2(Double v)              { this.areaTotalM2 = v; return this; }
        public Builder numEstacionamientos(Integer v)     { this.numEstacionamientos = v; return this; }
        public Builder modalidadTramite(String v)         { this.modalidadTramite = v; return this; }
        public Builder observacionesSolicitante(String v) { this.observacionesSolicitante = v; return this; }
        public Builder usuario(Usuario v)                 { this.usuario = v; return this; }
        public Builder estado(Enums.EstadoTramite v)      { this.estado = v; return this; }

        public Solicitud build() {
            Solicitud s = new Solicitud();
            s.razonSocial             = this.razonSocial;
            s.domicilioFiscal         = this.domicilioFiscal;
            s.rubro                   = this.rubro;
            s.ruc                     = this.ruc;
            s.dni                     = this.dni;
            s.telefono                = this.telefono;
            s.correoElectronico       = this.correoElectronico;
            s.nombreRepresentante     = this.nombreRepresentante;
            s.dniRepresentante        = this.dniRepresentante;
            s.partidaSunarp           = this.partidaSunarp;
            s.nombreComercial         = this.nombreComercial;
            s.direccionEstablecimiento= this.direccionEstablecimiento;
            s.horarioAtencion         = this.horarioAtencion;
            s.areaTotalM2             = this.areaTotalM2;
            s.numEstacionamientos     = this.numEstacionamientos;
            s.modalidadTramite        = this.modalidadTramite != null ? this.modalidadTramite : "NUEVA";
            s.observacionesSolicitante= this.observacionesSolicitante;
            s.usuario                 = this.usuario;
            s.estado                  = this.estado;
            return s;
        }
    }

    // Getters
    public Long getId()                          { return id; }
    public String getRazonSocial()               { return razonSocial; }
    public String getDomicilioFiscal()           { return domicilioFiscal; }
    public String getRubro()                     { return rubro; }
    public com.municipalidad.licencias.model.Enums.Distrito getDistrito() { return distrito; }
    public void setDistrito(com.municipalidad.licencias.model.Enums.Distrito v) { this.distrito = v; }
    public String getCodigoSeguimiento() { return codigoSeguimiento; }
    public void setCodigoSeguimiento(String v) { this.codigoSeguimiento = v; }
    public String getRuc()                       { return ruc; }
    public String getDni()                       { return dni; }
    public String getTelefono()                  { return telefono; }
    public String getCorreoElectronico()         { return correoElectronico; }
    public String getNombreRepresentante()       { return nombreRepresentante; }
    public String getDniRepresentante()          { return dniRepresentante; }
    public String getPartidaSunarp()             { return partidaSunarp; }
    public String getNombreComercial()           { return nombreComercial; }
    public String getDireccionEstablecimiento()  { return direccionEstablecimiento; }
    public String getHorarioAtencion()           { return horarioAtencion; }
    public Double getAreaTotalM2()               { return areaTotalM2; }
    public Integer getNumEstacionamientos()      { return numEstacionamientos; }
    public String getModalidadTramite()          { return modalidadTramite; }
    public String getObservacionesSolicitante()  { return observacionesSolicitante; }
    public String getPlanoUrl()                  { return planoUrl; }
    public String getFirmaUrl()                  { return firmaUrl; }
    public BigDecimal getMontoPagado()           { return montoPagado; }
    public LocalDateTime getFechaPago()          { return fechaPago; }
    public String getReferenciaPago()            { return referenciaPago; }
    public Enums.EstadoTramite getEstado()       { return estado; }
    public boolean isValidadoSunat()             { return validadoSunat; }
    public String getMotivoRechazoSunat()        { return motivoRechazoSunat; }
    public LocalDateTime getCreadoEn()           { return creadoEn; }
    public Usuario getUsuario()                  { return usuario; }
    public List<Inspeccion> getInspecciones()    { return inspecciones; }
    public Licencia getLicencia()                { return licencia; }

    // Setters
    public void setPlanoUrl(String v)             { this.planoUrl = v; }
    public void setFirmaUrl(String v)             { this.firmaUrl = v; }
    public void setMontoPagado(BigDecimal v)      { this.montoPagado = v; }
    public void setFechaPago(LocalDateTime v)     { this.fechaPago = v; }
    public void setReferenciaPago(String v)       { this.referenciaPago = v; }
    public void setEstado(Enums.EstadoTramite v)  { this.estado = v; }
    public void setValidadoSunat(boolean v)       { this.validadoSunat = v; }
    public void setMotivoRechazoSunat(String v)   { this.motivoRechazoSunat = v; }
    public void setRuc(String v)                  { this.ruc = v; }
    public void setDni(String v)                  { this.dni = v; }
    public void setTelefono(String v)             { this.telefono = v; }
    public void setCorreoElectronico(String v)    { this.correoElectronico = v; }
    public void setNombreRepresentante(String v)  { this.nombreRepresentante = v; }
    public void setDniRepresentante(String v)     { this.dniRepresentante = v; }
    public void setPartidaSunarp(String v)        { this.partidaSunarp = v; }
    public void setNombreComercial(String v)      { this.nombreComercial = v; }
    public void setDireccionEstablecimiento(String v) { this.direccionEstablecimiento = v; }
    public void setHorarioAtencion(String v)      { this.horarioAtencion = v; }
    public void setAreaTotalM2(Double v)          { this.areaTotalM2 = v; }
    public void setNumEstacionamientos(Integer v) { this.numEstacionamientos = v; }
    public void setModalidadTramite(String v)     { this.modalidadTramite = v; }
    public void setObservacionesSolicitante(String v) { this.observacionesSolicitante = v; }
}
