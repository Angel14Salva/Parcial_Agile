package com.municipalidad.licencias.dto;

import jakarta.validation.constraints.*;

public class InspectorDto {

    @NotBlank(message = "El nombre completo es obligatorio.")
    private String nombreCompleto;

    @NotBlank(message = "El DNI es obligatorio.")
    @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos.")
    private String dni;

    @NotBlank(message = "El correo es obligatorio.")
    @Email(message = "El correo no es válido.")
    private String email;

    @NotBlank(message = "El usuario es obligatorio.")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria.")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres.")
    private String password;

    @NotBlank(message = "El teléfono es obligatorio.")
    @Pattern(regexp = "\\d{9}", message = "El teléfono debe tener 9 dígitos.")
    private String telefono;

    private String cargo = "Inspector Municipal";
    private String numeroColegiatura;
    private java.time.LocalDate fechaNombramiento;
    // Campos legales
    private String rol = "INSPECTOR";
    private String distrito;
    private String regimentLaboral;
    private String resolucionDesignacion;
    private java.time.LocalDate fechaResolucion;
    private String codigoPlaza;
    private String especialidad;
    private String codigoFiscalizador;
    private String certificacionFiscalizacion;

    public InspectorDto() {}

    public String getNombreCompleto()    { return nombreCompleto; }
    public String getDni()               { return dni; }
    public String getEmail()             { return email; }
    public String getUsername()          { return username; }
    public String getPassword()          { return password; }
    public String getTelefono()          { return telefono; }
    public String getCargo()             { return cargo; }
    public String getNumeroColegiatura() { return numeroColegiatura; }
    public java.time.LocalDate getFechaNombramiento() { return fechaNombramiento; }

    public void setNombreCompleto(String v)    { this.nombreCompleto = v; }
    public void setDni(String v)               { this.dni = v; }
    public void setEmail(String v)             { this.email = v; }
    public void setUsername(String v)          { this.username = v; }
    public void setPassword(String v)          { this.password = v; }
    public void setTelefono(String v)          { this.telefono = v; }
    public void setCargo(String v)             { this.cargo = v; }
    public void setNumeroColegiatura(String v) { this.numeroColegiatura = v; }
    public void setFechaNombramiento(java.time.LocalDate v) { this.fechaNombramiento = v; }
    public String getRol()                      { return rol; }
    public void setRol(String v)                { this.rol = v; }
    public String getDistrito()                 { return distrito; }
    public void setDistrito(String v)           { this.distrito = v; }
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
}
