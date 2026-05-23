package com.municipalidad.licencias.dto;

import com.municipalidad.licencias.model.Enums;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ObservacionDto {

    @NotNull(message = "El tipo de observación es obligatorio.")
    private Enums.TipoObservacion tipo;

    @NotBlank(message = "La descripción es obligatoria.")
    private String descripcion;

    public ObservacionDto() {}

    public Enums.TipoObservacion getTipo()  { return tipo; }
    public String getDescripcion()          { return descripcion; }
    public void setTipo(Enums.TipoObservacion v) { this.tipo = v; }
    public void setDescripcion(String v)    { this.descripcion = v; }
}
