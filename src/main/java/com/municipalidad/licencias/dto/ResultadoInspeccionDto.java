package com.municipalidad.licencias.dto;

import com.municipalidad.licencias.model.Enums;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResultadoInspeccionDto {

    @NotNull(message = "Debe seleccionar un resultado.")
    private Enums.ResultadoInspeccion resultado;

    private String notas;

    // Solo si resultado == CON_OBSERVACIONES
    private List<ObservacionDto> observaciones;
}
