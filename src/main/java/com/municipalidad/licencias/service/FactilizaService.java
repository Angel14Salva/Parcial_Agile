package com.municipalidad.licencias.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class FactilizaService {

    private static final Logger log = LoggerFactory.getLogger(FactilizaService.class);
    private static final String BASE_URL = "https://api.factiliza.com/v1";

    @Value("${factiliza.api.token}")
    private String token;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public record DatosRuc(
        String razonSocial,
        String domicilioFiscal,
        String estado,
        String condicion
    ) {}

    public record DatosRepresentante(
        String numeroDocumento,
        String nombre,
        String cargo
    ) {}

    public record DatosDni(
        String nombreCompleto,
        String nombres,
        String apellidoPaterno,
        String apellidoMaterno
    ) {}

    public record ResultadoValidacion(
        boolean valido,
        String mensaje,
        DatosRuc datosRuc,
        DatosDni datosDni
    ) {}

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    public DatosRuc consultarRuc(String ruc) {
        try {
            String url = BASE_URL + "/ruc/info/" + ruc;
            ResponseEntity<String> resp = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers()), String.class);
            JsonNode data = mapper.readTree(resp.getBody()).path("data");
            return new DatosRuc(
                data.path("nombre_o_razon_social").asText(),
                data.path("direccion_completa").asText(),
                data.path("estado").asText(),
                data.path("condicion").asText()
            );
        } catch (Exception e) {
            log.error("Error consultando RUC {}: {}", ruc, e.getMessage());
            return null;
        }
    }

    public DatosRepresentante consultarRepresentante(String ruc) {
        try {
            String url = BASE_URL + "/ruc/representante/" + ruc;
            ResponseEntity<String> resp = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers()), String.class);
            JsonNode data = mapper.readTree(resp.getBody()).path("data");
            if (data.isArray() && data.size() > 0) {
                JsonNode rep = data.get(0);
                return new DatosRepresentante(
                    rep.path("numero_de_documento").asText(),
                    rep.path("nombre").asText(),
                    rep.path("cargo").asText()
                );
            }
            return null;
        } catch (Exception e) {
            log.error("Error consultando representante RUC {}: {}", ruc, e.getMessage());
            return null;
        }
    }

    public DatosDni consultarDni(String dni) {
        try {
            String url = BASE_URL + "/dni/info/" + dni;
            ResponseEntity<String> resp = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers()), String.class);
            JsonNode data = mapper.readTree(resp.getBody()).path("data");
            return new DatosDni(
                data.path("nombre_completo").asText(),
                data.path("nombres").asText(),
                data.path("apellido_paterno").asText(),
                data.path("apellido_materno").asText()
            );
        } catch (Exception e) {
            log.error("Error consultando DNI {}: {}", dni, e.getMessage());
            return null;
        }
    }

    /**
     * Valida que el DNI sea representante legal del RUC.
     * La API devuelve el número enmascarado (1*****72), así que
     * comparamos inicio y fin del DNI con el patrón.
     */
    public ResultadoValidacion validarRucYDni(String ruc, String dni) {
        // 1. Validar formato
        if (ruc == null || !ruc.matches("\\d{11}"))
            return new ResultadoValidacion(false, "El RUC debe tener 11 dígitos.", null, null);
        if (dni == null || !dni.matches("\\d{8}"))
            return new ResultadoValidacion(false, "El DNI debe tener 8 dígitos.", null, null);

        // 2. Consultar RUC
        DatosRuc datosRuc = consultarRuc(ruc);
        if (datosRuc == null)
            return new ResultadoValidacion(false, "No se encontró el RUC en SUNAT.", null, null);
        if (!datosRuc.condicion().equalsIgnoreCase("HABIDO"))
            return new ResultadoValidacion(false,
                "El RUC tiene condición: " + datosRuc.condicion() + ". Solo se aceptan contribuyentes HABIDOS.", null, null);

        // 3. Consultar representante
        DatosRepresentante rep = consultarRepresentante(ruc);
        if (rep == null)
            return new ResultadoValidacion(false,
                "No se encontró representante legal para el RUC ingresado.", datosRuc, null);

        // 4. Validar que el DNI coincida (comparando caracteres no enmascarados)
        String dniEnmascarado = rep.numeroDocumento(); // ej: "1*****72"
        boolean coincide = validarDniConMascara(dni, dniEnmascarado);
        if (!coincide)
            return new ResultadoValidacion(false,
                "El DNI ingresado no corresponde al representante legal del RUC. " +
                "El representante registrado es: " + rep.nombre() + " (" + rep.cargo() + ").",
                datosRuc, null);

        // 5. Consultar DNI para obtener nombre completo
        DatosDni datosDni = consultarDni(dni);

        return new ResultadoValidacion(true,
            "Validación exitosa. Representante legal confirmado.", datosRuc, datosDni);
    }

    private boolean validarDniConMascara(String dni, String mascara) {
        if (mascara == null || dni.length() != mascara.length()) return false;
        for (int i = 0; i < mascara.length(); i++) {
            char m = mascara.charAt(i);
            if (m != '*' && m != dni.charAt(i)) return false;
        }
        return true;
    }
}
