package com.municipalidad.licencias.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "facturas_caja")
public class FacturaCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String serie = "E001";

    @Column(nullable = false)
    private Long numero;

    @Column(nullable = false)
    private LocalDate fechaEmision;

    @Column(nullable = false)
    private String rucCliente;

    @Column(nullable = false)
    private String razonSocialCliente;

    private String direccionCliente;

    private String emailCliente;

    @Column(nullable = false)
    private String concepto;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valorVenta;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal igv;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal importeTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Enums.MetodoPago metodoPago;

    @Column(precision = 10, scale = 2)
    private BigDecimal montoRecibido;

    @Column(precision = 10, scale = 2)
    private BigDecimal vuelto;

    private String numeroOperacion;

    private String flowToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Enums.EstadoFactura estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cajero_id", nullable = false)
    private Usuario cajero;

    private Long solicitudId;

    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() {
        this.creadoEn = LocalDateTime.now();
        if (this.fechaEmision == null) this.fechaEmision = LocalDate.now();
        if (this.estado == null) this.estado = Enums.EstadoFactura.PENDIENTE;
    }

    public FacturaCaja() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String rucCliente, razonSocialCliente, direccionCliente, emailCliente, concepto;
        private BigDecimal valorVenta, igv, importeTotal, montoRecibido, vuelto;
        private Enums.MetodoPago metodoPago;
        private Enums.EstadoFactura estado;
        private Usuario cajero;
        private Long numero;

        public Builder rucCliente(String v)         { this.rucCliente = v; return this; }
        public Builder razonSocialCliente(String v) { this.razonSocialCliente = v; return this; }
        public Builder direccionCliente(String v)   { this.direccionCliente = v; return this; }
        public Builder emailCliente(String v)       { this.emailCliente = v; return this; }
        public Builder concepto(String v)           { this.concepto = v; return this; }
        public Builder valorVenta(BigDecimal v)     { this.valorVenta = v; return this; }
        public Builder igv(BigDecimal v)            { this.igv = v; return this; }
        public Builder importeTotal(BigDecimal v)   { this.importeTotal = v; return this; }
        public Builder metodoPago(Enums.MetodoPago v) { this.metodoPago = v; return this; }
        public Builder montoRecibido(BigDecimal v)  { this.montoRecibido = v; return this; }
        public Builder vuelto(BigDecimal v)         { this.vuelto = v; return this; }
        public Builder estado(Enums.EstadoFactura v){ this.estado = v; return this; }
        public Builder cajero(Usuario v)            { this.cajero = v; return this; }
        public Builder numero(Long v)               { this.numero = v; return this; }

        public FacturaCaja build() {
            FacturaCaja f = new FacturaCaja();
            f.rucCliente         = this.rucCliente;
            f.razonSocialCliente = this.razonSocialCliente;
            f.direccionCliente   = this.direccionCliente;
            f.emailCliente       = this.emailCliente;
            f.concepto            = this.concepto;
            f.valorVenta          = this.valorVenta;
            f.igv                 = this.igv;
            f.importeTotal        = this.importeTotal;
            f.metodoPago          = this.metodoPago;
            f.montoRecibido       = this.montoRecibido;
            f.vuelto              = this.vuelto;
            f.estado              = this.estado;
            f.cajero              = this.cajero;
            f.numero              = this.numero;
            return f;
        }
    }

    // Getters
    public Long getId()                          { return id; }
    public String getSerie()                     { return serie; }
    public Long getNumero()                      { return numero; }
    public String getNumeroFormateado()          { return serie + "-" + String.format("%07d", numero); }
    public LocalDate getFechaEmision()           { return fechaEmision; }
    public String getRucCliente()                { return rucCliente; }
    public String getRazonSocialCliente()        { return razonSocialCliente; }
    public String getDireccionCliente()          { return direccionCliente; }
    public String getEmailCliente()              { return emailCliente; }
    public String getConcepto()                  { return concepto; }
    public BigDecimal getValorVenta()            { return valorVenta; }
    public BigDecimal getIgv()                   { return igv; }
    public BigDecimal getImporteTotal()          { return importeTotal; }
    public Enums.MetodoPago getMetodoPago()      { return metodoPago; }
    public BigDecimal getMontoRecibido()         { return montoRecibido; }
    public BigDecimal getVuelto()                { return vuelto; }
    public String getNumeroOperacion()           { return numeroOperacion; }
    public String getFlowToken()                 { return flowToken; }
    public Enums.EstadoFactura getEstado()       { return estado; }
    public Usuario getCajero()                   { return cajero; }
    public Long getSolicitudId()                 { return solicitudId; }
    public LocalDateTime getCreadoEn()           { return creadoEn; }

    // Setters
    public void setNumero(Long v)                { this.numero = v; }
    public void setNumeroOperacion(String v)     { this.numeroOperacion = v; }
    public void setFlowToken(String v)           { this.flowToken = v; }
    public void setEstado(Enums.EstadoFactura v) { this.estado = v; }
    public void setSolicitudId(Long v)           { this.solicitudId = v; }
    public void setMontoRecibido(BigDecimal v)   { this.montoRecibido = v; }
    public void setVuelto(BigDecimal v)          { this.vuelto = v; }
}
