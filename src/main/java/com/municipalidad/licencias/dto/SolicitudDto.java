package com.municipalidad.licencias.dto;

import com.municipalidad.licencias.model.Enums;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.List;

// ── Solicitud ─────────────────────────────────────────────────────────────────
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SolicitudDto {

    @NotBlank(message = "La razón social es obligatoria.")
    @Size(min = 3, max = 200, message = "La razón social debe tener entre 3 y 200 caracteres.")
    private String razonSocial;

    @NotBlank(message = "El domicilio fiscal es obligatorio.")
    @Size(min = 5, message = "El domicilio fiscal debe tener al menos 5 caracteres.")
    private String domicilioFiscal;

    @NotBlank(message = "El rubro es obligatorio.")
    private String rubro;
}
