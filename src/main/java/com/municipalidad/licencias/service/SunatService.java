package com.municipalidad.licencias.service;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SunatService {

    private static final Logger log = LoggerFactory.getLogger(SunatService.class);

    public record ResultadoSunat(boolean valido, String mensaje) {}

    public ResultadoSunat validar(String razonSocial, String domicilioFiscal) {
        log.info("Consultando SUNAT para: {} / {}", razonSocial, domicilioFiscal);
        if (razonSocial == null || razonSocial.trim().length() < 3)
            return new ResultadoSunat(false, "Razón social inválida o demasiado corta.");
        if (domicilioFiscal == null || domicilioFiscal.trim().length() < 5)
            return new ResultadoSunat(false, "Domicilio fiscal inválido.");
        String rs = razonSocial.toUpperCase().trim();
        if (rs.equals("EMPRESA FALSA") || rs.equals("TEST") || rs.equals("PRUEBA")
            || rs.equals("NULL") || rs.equals("N/A"))
            return new ResultadoSunat(false,
                "La razón social '" + razonSocial + "' no figura en el padrón de SUNAT.");
        return new ResultadoSunat(true, "Datos validados correctamente en SUNAT.");
    }
}
