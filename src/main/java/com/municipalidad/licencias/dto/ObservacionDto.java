package com.municipalidad.licencias.dto;

import com.municipalidad.licencias.model.Enums;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ObservacionDto {

    @NotNull(message = "El tipo de observación es obligatorio.")
    private Enums.TipoObservacion tipo;

    @NotBlank(message = "La descripción es obligatoria.")
    private String descripcion;
}
