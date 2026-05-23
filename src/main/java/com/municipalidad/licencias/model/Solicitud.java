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

    @Column(nullable = false)
    private String razonSocial;

    @Column(nullable = false)
    private String domicilioFiscal;

    @Column(nullable = false)
    private String rubro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    private String planoUrl;
    private BigDecimal montoPagado;
    private LocalDateTime fechaPago;
    private String referenciaPago;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Enums.EstadoTramite estado;

    private boolean validadoSunat = false;
    private String motivoRechazoSunat;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;

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
        private String razonSocial, domicilioFiscal, rubro;
        private Usuario usuario;
        private Enums.EstadoTramite estado;

        public Builder razonSocial(String v)    { this.razonSocial = v; return this; }
        public Builder domicilioFiscal(String v){ this.domicilioFiscal = v; return this; }
        public Builder rubro(String v)          { this.rubro = v; return this; }
        public Builder usuario(Usuario v)       { this.usuario = v; return this; }
        public Builder estado(Enums.EstadoTramite v) { this.estado = v; return this; }

        public Solicitud build() {
            Solicitud s = new Solicitud();
            s.razonSocial = this.razonSocial;
            s.domicilioFiscal = this.domicilioFiscal;
            s.rubro = this.rubro;
            s.usuario = this.usuario;
            s.estado = this.estado;
            return s;
        }
    }

    public Long getId()                          { return id; }
    public String getRazonSocial()               { return razonSocial; }
    public String getDomicilioFiscal()           { return domicilioFiscal; }
    public String getRubro()                     { return rubro; }
    public Usuario getUsuario()                  { return usuario; }
    public String getPlanoUrl()                  { return planoUrl; }
    public BigDecimal getMontoPagado()           { return montoPagado; }
    public LocalDateTime getFechaPago()          { return fechaPago; }
    public String getReferenciaPago()            { return referenciaPago; }
    public Enums.EstadoTramite getEstado()       { return estado; }
    public boolean isValidadoSunat()             { return validadoSunat; }
    public String getMotivoRechazoSunat()        { return motivoRechazoSunat; }
    public LocalDateTime getCreadoEn()           { return creadoEn; }
    public List<Inspeccion> getInspecciones()    { return inspecciones; }
    public Licencia getLicencia()                { return licencia; }

    public void setPlanoUrl(String v)            { this.planoUrl = v; }
    public void setMontoPagado(BigDecimal v)     { this.montoPagado = v; }
    public void setFechaPago(LocalDateTime v)    { this.fechaPago = v; }
    public void setReferenciaPago(String v)      { this.referenciaPago = v; }
    public void setEstado(Enums.EstadoTramite v) { this.estado = v; }
    public void setValidadoSunat(boolean v)      { this.validadoSunat = v; }
    public void setMotivoRechazoSunat(String v)  { this.motivoRechazoSunat = v; }
}
