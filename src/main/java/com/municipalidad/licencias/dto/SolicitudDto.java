package com.municipalidad.licencias.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SolicitudDto {

    // Sección II
    @NotBlank(message = "La razón social es obligatoria.")
    @Size(min = 3, max = 200)
    private String razonSocial;

    @NotBlank(message = "El domicilio fiscal es obligatorio.")
    private String domicilioFiscal;

    private String ruc;
    private String dni;
    private String telefono;
    private String correoElectronico;

    // Sección III
    private String nombreRepresentante;
    private String dniRepresentante;
    private String partidaSunarp;

    // Sección IV
    @NotBlank(message = "El rubro es obligatorio.")
    private String rubro;

    private String nombreComercial;
    private String direccionEstablecimiento;
    private String horarioAtencion;
    private Double areaTotalM2;
    private Integer numEstacionamientos;

    // Modalidad y observaciones
    private String modalidadTramite = "NUEVA";
    private String observacionesSolicitante;

    public SolicitudDto() {}

    public String getRazonSocial()                   { return razonSocial; }
    public String getDomicilioFiscal()               { return domicilioFiscal; }
    public String getRubro()                         { return rubro; }
    public String getRuc()                           { return ruc; }
    public String getDni()                           { return dni; }
    public String getTelefono()                      { return telefono; }
    public String getCorreoElectronico()             { return correoElectronico; }
    public String getNombreRepresentante()           { return nombreRepresentante; }
    public String getDniRepresentante()              { return dniRepresentante; }
    public String getPartidaSunarp()                 { return partidaSunarp; }
    public String getNombreComercial()               { return nombreComercial; }
    public String getDireccionEstablecimiento()      { return direccionEstablecimiento; }
    public String getHorarioAtencion()               { return horarioAtencion; }
    public Double getAreaTotalM2()                   { return areaTotalM2; }
    public Integer getNumEstacionamientos()          { return numEstacionamientos; }
    public String getModalidadTramite()              { return modalidadTramite; }
    public String getObservacionesSolicitante()      { return observacionesSolicitante; }

    public void setRazonSocial(String v)             { this.razonSocial = v; }
    public void setDomicilioFiscal(String v)         { this.domicilioFiscal = v; }
    public void setRubro(String v)                   { this.rubro = v; }
    public void setRuc(String v)                     { this.ruc = v; }
    public void setDni(String v)                     { this.dni = v; }
    public void setTelefono(String v)                { this.telefono = v; }
    public void setCorreoElectronico(String v)       { this.correoElectronico = v; }
    public void setNombreRepresentante(String v)     { this.nombreRepresentante = v; }
    public void setDniRepresentante(String v)        { this.dniRepresentante = v; }
    public void setPartidaSunarp(String v)           { this.partidaSunarp = v; }
    public void setNombreComercial(String v)         { this.nombreComercial = v; }
    public void setDireccionEstablecimiento(String v){ this.direccionEstablecimiento = v; }
    public void setHorarioAtencion(String v)         { this.horarioAtencion = v; }
    public void setAreaTotalM2(Double v)             { this.areaTotalM2 = v; }
    public void setNumEstacionamientos(Integer v)    { this.numEstacionamientos = v; }
    public void setModalidadTramite(String v)        { this.modalidadTramite = v; }
    public void setObservacionesSolicitante(String v){ this.observacionesSolicitante = v; }
}
