package com.municipalidad.licencias.service;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock del servicio SUNAT.
 * En producción real se reemplazaría por una llamada HTTP a la API de SUNAT.
 * Por ahora simula la validación con reglas básicas.
 */
@Service
public class SunatService {

    private static final Logger log = LoggerFactory.getLogger(SunatService.class);

    public record ResultadoSunat(boolean valido, String mensaje) {}

    /**
     * Valida que el negocio exista y tenga datos coherentes en SUNAT.
     * Mock: acepta todo excepto razones sociales claramente falsas.
     */
    public ResultadoSunat validar(String razonSocial, String domicilioFiscal) {
        log.info("Consultando SUNAT para: {} / {}", razonSocial, domicilioFiscal);

        // Regla de negocio: no se permiten datos vacíos o falsos evidentes
        if (razonSocial == null || razonSocial.trim().length() < 3) {
            return new ResultadoSunat(false, "Razón social inválida o demasiado corta.");
        }
        if (domicilioFiscal == null || domicilioFiscal.trim().length() < 5) {
            return new ResultadoSunat(false, "Domicilio fiscal inválido.");
        }

        // Simular negocios que no existen en SUNAT
        String rsUpper = razonSocial.toUpperCase().trim();
        if (rsUpper.equals("EMPRESA FALSA") ||
            rsUpper.equals("TEST") ||
            rsUpper.equals("PRUEBA") ||
            rsUpper.equals("NULL") ||
            rsUpper.equals("N/A")) {
            return new ResultadoSunat(false,
                "La razón social '" + razonSocial + "' no figura en el padrón de SUNAT.");
        }

        // Mock: el resto se considera válido
        log.info("SUNAT: validación exitosa para {}", razonSocial);
        return new ResultadoSunat(true, "Datos validados correctamente en SUNAT.");
    }
}
