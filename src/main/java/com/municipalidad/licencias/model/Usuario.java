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
    private String dni;
    private String telefono;
    private String cargo;
    @Enumerated(jakarta.persistence.EnumType.STRING)
    private Enums.Distrito distrito;
    private String numeroColegiatura;
    private java.time.LocalDate fechaNombramiento;
    // Campos legales adicionales
    private String regimentLaboral;          // CAS, 276, 728, Servicio Civil
    private String resolucionDesignacion;    // N° resolución de designación
    private java.time.LocalDate fechaResolucion;
    private String codigoPlaza;             // Código de plaza presupuestal
    private String especialidad;            // Derecho Administrativo, Gestión Pública, etc.
    private String codigoFiscalizador;      // Solo para fiscalizadores
    private String certificacionFiscalizacion; // Certificación de capacitación

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
    public String getDni()                   { return dni; }
    public String getTelefono()              { return telefono; }
    public Enums.Distrito getDistrito()      { return distrito; }
    public void setDistrito(Enums.Distrito v){ this.distrito = v; }
    public String getCargo()                 { return cargo; }
    public String getNumeroColegiatura()     { return numeroColegiatura; }
    public java.time.LocalDate getFechaNombramiento() { return fechaNombramiento; }
    public void setDni(String v)             { this.dni = v; }
    public void setTelefono(String v)        { this.telefono = v; }
    public void setCargo(String v)           { this.cargo = v; }
    public void setNumeroColegiatura(String v){ this.numeroColegiatura = v; }
    public void setFechaNombramiento(java.time.LocalDate v) { this.fechaNombramiento = v; }
    public String getRegimentLaboral()          { return regimentLaboral; }
    public void setRegimentLaboral(String v)    { this.regimentLaboral = v; }
    public String getResolucionDesignacion()    { return resolucionDesignacion; }
    public void setResolucionDesignacion(String v) { this.resolucionDesignacion = v; }
    public java.time.LocalDate getFechaResolucion() { return fechaResolucion; }
    public void setFechaResolucion(java.time.LocalDate v) { this.fechaResolucion = v; }
    public String getCodigoPlaza()              { return codigoPlaza; }
    public void setCodigoPlaza(String v)        { this.codigoPlaza = v; }
    public String getEspecialidad()             { return especialidad; }
    public void setEspecialidad(String v)       { this.especialidad = v; }
    public String getCodigoFiscalizador()       { return codigoFiscalizador; }
    public void setCodigoFiscalizador(String v) { this.codigoFiscalizador = v; }
    public String getCertificacionFiscalizacion() { return certificacionFiscalizacion; }
    public void setCertificacionFiscalizacion(String v) { this.certificacionFiscalizacion = v; }
    public void setActivo(boolean v)     { this.activo = v; }
}
