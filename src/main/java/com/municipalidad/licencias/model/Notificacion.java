package com.municipalidad.licencias.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Column(nullable = false)
    private boolean leida = false;

    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() { this.creadoEn = LocalDateTime.now(); }

    public Notificacion() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Usuario usuario;
        private String titulo, mensaje;

        public Builder usuario(Usuario v) { this.usuario = v; return this; }
        public Builder titulo(String v)   { this.titulo = v; return this; }
        public Builder mensaje(String v)  { this.mensaje = v; return this; }

        public Notificacion build() {
            Notificacion n = new Notificacion();
            n.usuario = this.usuario;
            n.titulo  = this.titulo;
            n.mensaje = this.mensaje;
            return n;
        }
    }

    public Long getId()                  { return id; }
    public Usuario getUsuario()          { return usuario; }
    public String getTitulo()            { return titulo; }
    public String getMensaje()           { return mensaje; }
    public boolean isLeida()             { return leida; }
    public LocalDateTime getCreadoEn()   { return creadoEn; }
    public void setLeida(boolean v)      { this.leida = v; }
}
