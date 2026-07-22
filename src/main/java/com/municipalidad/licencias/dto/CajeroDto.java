package com.municipalidad.licencias.dto;

import jakarta.validation.constraints.*;

public class CajeroDto {

    @NotBlank(message = "El nombre completo es obligatorio.")
    private String nombreCompleto;

    private String dni;

    @NotBlank(message = "El correo es obligatorio.")
    @Email(message = "El correo no es válido.")
    private String email;

    @NotBlank(message = "El usuario es obligatorio.")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria.")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres.")
    private String password;

    private String telefono;

    public CajeroDto() {}

    public String getNombreCompleto()       { return nombreCompleto; }
    public void setNombreCompleto(String v) { this.nombreCompleto = v; }
    public String getDni()                  { return dni; }
    public void setDni(String v)            { this.dni = v; }
    public String getEmail()                { return email; }
    public void setEmail(String v)          { this.email = v; }
    public String getUsername()             { return username; }
    public void setUsername(String v)       { this.username = v; }
    public String getPassword()             { return password; }
    public void setPassword(String v)       { this.password = v; }
    public String getTelefono()             { return telefono; }
    public void setTelefono(String v)       { this.telefono = v; }
}
