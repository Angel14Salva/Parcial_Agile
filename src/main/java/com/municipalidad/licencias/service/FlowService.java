package com.municipalidad.licencias.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class FlowService {

    private static final Logger log = LoggerFactory.getLogger(FlowService.class);

    @Value("${flow.api.key}")
    private String apiKey;

    @Value("${flow.secret.key}")
    private String secretKey;

    @Value("${flow.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public record OrdenFlow(String url, String token, String flowOrder) {}

    public OrdenFlow crearOrden(Long solicitudId, String email,
                                String nombrePagador, double monto,
                                String urlRetorno, String urlConfirmacion) {
        try {
            Map<String, String> params = new TreeMap<>();
            params.put("apiKey", apiKey);
            params.put("commerceOrder", "SOL-" + solicitudId);
            params.put("subject", "Licencia de Funcionamiento - Solicitud #" + solicitudId);
            params.put("currency", "PEN");
            params.put("amount", String.valueOf((int) monto));
            params.put("email", email);
            params.put("payerName", nombrePagador);
            params.put("urlConfirmation", urlConfirmacion);
            params.put("urlReturn", urlRetorno);

            String firma = firmar(params);
            params.put("s", firma);

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            params.forEach(formData::add);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            ResponseEntity<String> resp = restTemplate.exchange(
                apiUrl + "/payment/create",
                HttpMethod.POST,
                new HttpEntity<>(formData, headers),
                String.class
            );

            JsonNode json = mapper.readTree(resp.getBody());
            String token = json.path("token").asText();
            String flowOrderNum = json.path("flowOrder").asText();
            String urlPago = json.path("url").asText() + "?token=" + token;

            log.info("Orden Flow creada: {} para solicitud {}", flowOrderNum, solicitudId);
            return new OrdenFlow(urlPago, token, flowOrderNum);

        } catch (Exception e) {
            log.error("Error creando orden Flow: {}", e.getMessage());
            throw new RuntimeException("Error al crear la orden de pago: " + e.getMessage());
        }
    }

    public JsonNode verificarPago(String token) {
        try {
            Map<String, String> params = new TreeMap<>();
            params.put("apiKey", apiKey);
            params.put("token", token);
            String firma = firmar(params);

            String url = apiUrl + "/payment/getStatus?apiKey=" + apiKey +
                         "&token=" + token + "&s=" + firma;

            ResponseEntity<String> resp = restTemplate.exchange(
                url, HttpMethod.GET, null, String.class);
            return mapper.readTree(resp.getBody());

        } catch (Exception e) {
            log.error("Error verificando pago Flow: {}", e.getMessage());
            return null;
        }
    }

    public String firmar(Map<String, String> params) throws Exception {
        StringBuilder sb = new StringBuilder();
        new TreeMap<>(params).forEach((k, v) -> sb.append(k).append(v));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(sb.toString().getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) hex.append(String.format("%02x", b));
        return hex.toString();
    }
}
