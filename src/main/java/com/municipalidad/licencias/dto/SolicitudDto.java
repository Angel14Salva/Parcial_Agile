package com.municipalidad.licencias.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SolicitudDto {

    @NotBlank(message = "La razón social es obligatoria.")
    @Size(min = 3, max = 200, message = "La razón social debe tener entre 3 y 200 caracteres.")
    private String razonSocial;

    @NotBlank(message = "El domicilio fiscal es obligatorio.")
    @Size(min = 5, message = "El domicilio fiscal debe tener al menos 5 caracteres.")
    private String domicilioFiscal;

    @NotBlank(message = "El rubro es obligatorio.")
    private String rubro;

    public SolicitudDto() {}

    public String getRazonSocial()         { return razonSocial; }
    public String getDomicilioFiscal()     { return domicilioFiscal; }
    public String getRubro()               { return rubro; }
    public void setRazonSocial(String v)   { this.razonSocial = v; }
    public void setDomicilioFiscal(String v){ this.domicilioFiscal = v; }
    public void setRubro(String v)         { this.rubro = v; }
}
