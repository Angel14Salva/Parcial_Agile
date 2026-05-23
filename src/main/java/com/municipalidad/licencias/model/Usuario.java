package com.municipalidad.licencias.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Enums.Rol rol;

    private String nombreCompleto;

    @Column(nullable = false)
    private boolean activo = true;

    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() { this.creadoEn = LocalDateTime.now(); }

    public Usuario() {}

    // Builder estático
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String username, password, email, nombreCompleto;
        private Enums.Rol rol;
        private boolean activo = true;

        public Builder username(String v)       { this.username = v; return this; }
        public Builder password(String v)       { this.password = v; return this; }
        public Builder email(String v)          { this.email = v; return this; }
        public Builder nombreCompleto(String v) { this.nombreCompleto = v; return this; }
        public Builder rol(Enums.Rol v)         { this.rol = v; return this; }
        public Builder activo(boolean v)        { this.activo = v; return this; }

        public Usuario build() {
            Usuario u = new Usuario();
            u.username = this.username;
            u.password = this.password;
            u.email = this.email;
            u.nombreCompleto = this.nombreCompleto;
            u.rol = this.rol;
            u.activo = this.activo;
            return u;
        }
    }

    public Long getId()                  { return id; }
    public String getUsername()          { return username; }
    public String getPassword()          { return password; }
    public String getEmail()             { return email; }
    public Enums.Rol getRol()            { return rol; }
    public String getNombreCompleto()    { return nombreCompleto; }
    public boolean isActivo()            { return activo; }
    public LocalDateTime getCreadoEn()   { return creadoEn; }
    public void setUsername(String v)    { this.username = v; }
    public void setPassword(String v)    { this.password = v; }
    public void setEmail(String v)       { this.email = v; }
    public void setRol(Enums.Rol v)      { this.rol = v; }
    public void setNombreCompleto(String v){ this.nombreCompleto = v; }
    public void setActivo(boolean v)     { this.activo = v; }
}
