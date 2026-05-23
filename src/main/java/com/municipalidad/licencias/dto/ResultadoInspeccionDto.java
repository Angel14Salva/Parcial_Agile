package com.municipalidad.licencias.dto;

import com.municipalidad.licencias.model.Enums;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class ResultadoInspeccionDto {

    @NotNull(message = "Debe seleccionar un resultado.")
    private Enums.ResultadoInspeccion resultado;

    private String notas;
    private List<ObservacionDto> observaciones;

    public ResultadoInspeccionDto() {}

    public Enums.ResultadoInspeccion getResultado()    { return resultado; }
    public String getNotas()                           { return notas; }
    public List<ObservacionDto> getObservaciones()     { return observaciones; }
    public void setResultado(Enums.ResultadoInspeccion v) { this.resultado = v; }
    public void setNotas(String v)                     { this.notas = v; }
    public void setObservaciones(List<ObservacionDto> v) { this.observaciones = v; }
}
