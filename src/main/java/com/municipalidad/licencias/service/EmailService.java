package com.municipalidad.licencias.service;

import com.resend.*;
import com.resend.services.emails.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Value("${resend.api-key}")
    private String apiKey;

    @Value("${resend.from:onboarding@resend.dev}")
    private String fromEmail;

    public void enviarCodigoSeguimiento(String destinatario, String nombreNegocio,
                                        String codigo, String distrito) {
        try {
            Resend resend = new Resend(apiKey);
            String html = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto">
                  <div style="background:#1D3557;color:#fff;padding:24px;border-radius:8px 8px 0 0;text-align:center">
                    <h2 style="margin:0">🏛️ Municipalidad de Trujillo</h2>
                    <p style="margin:8px 0 0;opacity:.8">Licencias de Funcionamiento</p>
                  </div>
                  <div style="background:#f8f9fa;padding:24px;border-radius:0 0 8px 8px">
                    <h3 style="color:#1D3557">¡Solicitud recibida!</h3>
                    <p>Hola, tu solicitud de licencia para <strong>%s</strong> en el distrito de <strong>%s</strong> ha sido registrada exitosamente.</p>
                    <div style="background:#fff;border:2px solid #1D3557;border-radius:8px;padding:20px;text-align:center;margin:20px 0">
                      <p style="margin:0;color:#666;font-size:14px">Tu código de seguimiento es:</p>
                      <h1 style="margin:8px 0;color:#1D3557;font-size:32px;letter-spacing:4px">%s</h1>
                      <p style="margin:0;color:#666;font-size:12px">Guarda este código — lo necesitarás para consultar el estado de tu trámite</p>
                    </div>
                    <p>Consulta el estado de tu trámite en cualquier momento en:</p>
                    <a href="https://parcial-agile.onrender.com/seguimiento"
                       style="display:inline-block;background:#1D3557;color:#fff;padding:12px 24px;border-radius:6px;text-decoration:none;font-weight:bold">
                      Ver estado del trámite →
                    </a>
                    <hr style="margin:24px 0;border:none;border-top:1px solid #dee2e6"/>
                    <p style="color:#999;font-size:12px;margin:0">
                      Este es un correo automático. Si tienes dudas, acércate a la Municipalidad de Trujillo.
                    </p>
                  </div>
                </div>
                """.formatted(nombreNegocio, distrito.replace("_"," "), codigo);

            CreateEmailOptions params = CreateEmailOptions.builder()
                .from(fromEmail)
                .to(destinatario)
                .subject("Código de seguimiento - Licencia de Funcionamiento | " + codigo)
                .html(html)
                .build();

            resend.emails().send(params);
        } catch (Exception e) {
            // Log error pero no fallar el flujo principal
            System.err.println("Error enviando email: " + e.getMessage());
        }
    }

    public void enviarLicencia(String destinatario, String nombreNegocio,
                               String numeroLicencia, String fechaVencimiento,
                               String codigoSeguimiento) {
        try {
            Resend resend = new Resend(apiKey);
            String html = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto">
                  <div style="background:#1D3557;color:#fff;padding:24px;border-radius:8px 8px 0 0;text-align:center">
                    <h2 style="margin:0">🏛️ Municipalidad de Trujillo</h2>
                    <p style="margin:8px 0 0;opacity:.8">Licencias de Funcionamiento</p>
                  </div>
                  <div style="background:#f8f9fa;padding:24px;border-radius:0 0 8px 8px">
                    <h3 style="color:#198754">✅ ¡Tu licencia ha sido emitida!</h3>
                    <p>Estimado representante de <strong>%s</strong>, nos complace informarle que su Licencia de Funcionamiento ha sido aprobada y emitida.</p>
                    <div style="background:#fff;border:2px solid #198754;border-radius:8px;padding:20px;text-align:center;margin:20px 0">
                      <p style="margin:0;color:#666;font-size:14px">N° de Licencia</p>
                      <h2 style="margin:8px 0;color:#198754">%s</h2>
                      <p style="margin:0;color:#666;font-size:14px">Válida hasta: <strong>%s</strong></p>
                    </div>
                    <p>Puede descargar su licencia en formato PDF ingresando a:</p>
                    <a href="https://parcial-agile.onrender.com/seguimiento"
                       style="display:inline-block;background:#198754;color:#fff;padding:12px 24px;border-radius:6px;text-decoration:none;font-weight:bold">
                      Descargar licencia PDF →
                    </a>
                    <p style="margin-top:16px;color:#666;font-size:13px">Use su código de seguimiento: <strong>%s</strong></p>
                    <hr style="margin:24px 0;border:none;border-top:1px solid #dee2e6"/>
                    <p style="color:#999;font-size:12px;margin:0">Municipalidad Provincial de Trujillo — Subgerencia de Licencias y Comercialización</p>
                  </div>
                </div>
                """.formatted(nombreNegocio, numeroLicencia, fechaVencimiento, codigoSeguimiento);

            CreateEmailOptions params = CreateEmailOptions.builder()
                .from(fromEmail)
                .to(destinatario)
                .subject("🎉 Licencia de Funcionamiento emitida - " + numeroLicencia)
                .html(html)
                .build();
            resend.emails().send(params);
        } catch (Exception e) {
            System.err.println("Error enviando email licencia: " + e.getMessage());
        }
    }

    public void enviarActualizacion(String destinatario, String nombreNegocio,
                                     String codigo, String estado, String detalle) {
        try {
            Resend resend = new Resend(apiKey);
            String html = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto">
                  <div style="background:#1D3557;color:#fff;padding:24px;border-radius:8px 8px 0 0;text-align:center">
                    <h2 style="margin:0">🏛️ Municipalidad de Trujillo</h2>
                  </div>
                  <div style="background:#f8f9fa;padding:24px;border-radius:0 0 8px 8px">
                    <h3 style="color:#1D3557">Actualización de tu trámite</h3>
                    <p>Hola, tu solicitud <strong>%s</strong> para <strong>%s</strong> ha sido actualizada.</p>
                    <div style="background:#fff;border-left:4px solid #1D3557;padding:16px;margin:16px 0;border-radius:0 8px 8px 0">
                      <strong>Estado actual:</strong> %s<br/>
                      <span style="color:#666">%s</span>
                    </div>
                    <a href="https://parcial-agile.onrender.com/seguimiento?codigo=%s"
                       style="display:inline-block;background:#1D3557;color:#fff;padding:12px 24px;border-radius:6px;text-decoration:none;font-weight:bold">
                      Ver detalle →
                    </a>
                  </div>
                </div>
                """.formatted(codigo, nombreNegocio, estado, detalle, codigo);

            CreateEmailOptions params = CreateEmailOptions.builder()
                .from(fromEmail)
                .to(destinatario)
                .subject("Actualización de trámite " + codigo)
                .html(html)
                .build();

            resend.emails().send(params);
        } catch (Exception e) {
            System.err.println("Error enviando email actualización: " + e.getMessage());
        }
    }
}
