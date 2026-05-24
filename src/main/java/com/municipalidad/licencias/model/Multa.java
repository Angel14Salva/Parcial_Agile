package com.municipalidad.licencias.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "multas")
public class Multa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "licencia_id", nullable = false)
    private Licencia licencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspector_id", nullable = false)
    private Usuario inspector;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoMulta estado = EstadoMulta.PENDIENTE;

    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() { this.creadoEn = LocalDateTime.now(); }

    public enum EstadoMulta { PENDIENTE, PAGADA, APELADA }

    public Multa() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Licencia licencia;
        private Usuario inspector;
        private String descripcion;
        private BigDecimal monto;

        public Builder licencia(Licencia v)    { this.licencia = v; return this; }
        public Builder inspector(Usuario v)    { this.inspector = v; return this; }
        public Builder descripcion(String v)   { this.descripcion = v; return this; }
        public Builder monto(BigDecimal v)     { this.monto = v; return this; }

        public Multa build() {
            Multa m = new Multa();
            m.licencia    = this.licencia;
            m.inspector   = this.inspector;
            m.descripcion = this.descripcion;
            m.monto       = this.monto;
            return m;
        }
    }

    public Long getId()                { return id; }
    public Licencia getLicencia()      { return licencia; }
    public Usuario getInspector()      { return inspector; }
    public String getDescripcion()     { return descripcion; }
    public BigDecimal getMonto()       { return monto; }
    public EstadoMulta getEstado()     { return estado; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setEstado(EstadoMulta v) { this.estado = v; }
}
