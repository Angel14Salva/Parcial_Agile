package com.municipalidad.licencias.dto;

import com.municipalidad.licencias.model.Enums;
import jakarta.validation.constraints.NotNull;

public class InspeccionOficioDto {

    @NotNull(message = "Debe seleccionar un resultado.")
    private Enums.ResultadoInspeccion resultado;

    private String notas;
    private boolean licenciaVisible;
    private String descripcionInfraccion;

    public InspeccionOficioDto() {}

    public Enums.ResultadoInspeccion getResultado()    { return resultado; }
    public String getNotas()                           { return notas; }
    public boolean isLicenciaVisible()                 { return licenciaVisible; }
    public String getDescripcionInfraccion()           { return descripcionInfraccion; }
    public void setResultado(Enums.ResultadoInspeccion v) { this.resultado = v; }
    public void setNotas(String v)                     { this.notas = v; }
    public void setLicenciaVisible(boolean v)          { this.licenciaVisible = v; }
    public void setDescripcionInfraccion(String v)     { this.descripcionInfraccion = v; }
}
