package com.municipalidad.licencias.dto;

import com.municipalidad.licencias.model.Enums;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InspeccionOficioDto {

    @NotNull(message = "Debe seleccionar un resultado.")
    private Enums.ResultadoInspeccion resultado;

    private String notas;
    private boolean licenciaVisible;
    private String descripcionInfraccion;
}
