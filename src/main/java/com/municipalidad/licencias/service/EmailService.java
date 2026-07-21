package com.municipalidad.licencias.service;

import com.resend.*;
import com.resend.services.emails.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${resend.api-key}")
    private String apiKey;

    @Value("${resend.from:onboarding@resend.dev}")
    private String fromEmail;

    @jakarta.annotation.PostConstruct
    void verificarConfiguracion() {
        if (apiKey == null || apiKey.isBlank()) {
            log.error("resend.api-key esta vacio: ningun correo se podra enviar hasta que se configure.");
        } else {
            log.info("EmailService configurado. from={} apiKey(len)={}", fromEmail, apiKey.length());
        }
    }

    public void enviarComprobanteYCodigo(String destinatario, String razonSocial, String ruc,
                                        String codigoSeguimiento, String numeroFactura,
                                        String concepto, String total, String metodoPago,
                                        String numeroOperacion, String distrito) {
        try {
            Resend resend = new Resend(apiKey);
            String html = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto">
                  <div style="background:#1D3557;color:#fff;padding:24px;border-radius:8px 8px 0 0;text-align:center">
                    <h2 style="margin:0">🏛️ Municipalidad Provincial de Trujillo</h2>
                    <p style="margin:8px 0 0;opacity:.8">Comprobante de Pago y Código de Seguimiento</p>
                  </div>
                  <div style="background:#f8f9fa;padding:24px;border-radius:0 0 8px 8px">
                    <h3 style="color:#1D3557;margin-top:0">¡Solicitud y Pago Registrados!</h3>
                    <p>Estimado(a) representante de <strong>%s</strong> (RUC: <strong>%s</strong>):</p>
                    <p>Se ha registrado exitosamente su trámite de Licencia de Funcionamiento en el distrito de <strong>%s</strong>.</p>
                    
                    <div style="background:#fff;border:2px solid #1D3557;border-radius:8px;padding:20px;text-align:center;margin:20px 0">
                      <p style="margin:0;color:#666;font-size:14px">Su Código de Seguimiento es:</p>
                      <h1 style="margin:8px 0;color:#1D3557;font-size:32px;letter-spacing:4px">%s</h1>
                      <p style="margin:0;color:#666;font-size:12px">Guarde este código para consultar el estado de su trámite e inspección</p>
                    </div>

                    <div style="background:#fff;border:1px solid #dee2e6;border-radius:8px;padding:16px;margin:20px 0">
                      <h4 style="margin:0 0 12px;color:#1D3557;border-bottom:1px solid #eee;padding-bottom:8px">📄 Detalle del Comprobante de Pago</h4>
                      <table style="width:100%%;font-size:14px;border-collapse:collapse">
                        <tr><td style="padding:4px 0;color:#666">N° Comprobante:</td><td style="padding:4px 0;font-weight:bold;text-align:right">%s</td></tr>
                        <tr><td style="padding:4px 0;color:#666">Concepto:</td><td style="padding:4px 0;text-align:right">%s</td></tr>
                        <tr><td style="padding:4px 0;color:#666">Método de Pago:</td><td style="padding:4px 0;text-align:right">%s</td></tr>
                        <tr><td style="padding:4px 0;color:#666">N° Operación:</td><td style="padding:4px 0;text-align:right">%s</td></tr>
                        <tr style="border-top:1px solid #eee"><td style="padding:8px 0 0;font-weight:bold;color:#1D3557">Monto Total:</td><td style="padding:8px 0 0;font-weight:bold;color:#198754;font-size:16px;text-align:right">S/ %s</td></tr>
                      </table>
                    </div>

                    <div style="text-align:center;margin-top:24px">
                      <a href="https://parcial-agile.onrender.com/seguimiento?codigo=%s"
                         style="display:inline-block;background:#1D3557;color:#fff;padding:12px 28px;border-radius:6px;text-decoration:none;font-weight:bold">
                        Consultar Estado del Trámite →
                      </a>
                    </div>

                    <hr style="margin:24px 0;border:none;border-top:1px solid #dee2e6"/>
                    <p style="color:#999;font-size:12px;margin:0;text-align:center">
                      Municipalidad Provincial de Trujillo — Subgerencia de Licencias y Comercialización
                    </p>
                  </div>
                </div>
                """.formatted(razonSocial, ruc, distrito.replace("_"," "), codigoSeguimiento,
                              numeroFactura, concepto, metodoPago, numeroOperacion, total, codigoSeguimiento);

            CreateEmailOptions params = CreateEmailOptions.builder()
                .from(fromEmail)
                .to(destinatario)
                .subject("📄 Comprobante de Pago y Código de Seguimiento - " + codigoSeguimiento)
                .html(html)
                .build();

            resend.emails().send(params);
        } catch (Exception e) {
            log.error("Error enviando email comprobante/seguimiento a {}", destinatario, e);
        }
    }

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
            log.error("Error enviando email de código de seguimiento a {}", destinatario, e);
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
            log.error("Error enviando email de licencia a {}", destinatario, e);
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
            log.error("Error enviando email de actualización a {}", destinatario, e);
        }
    }

    public void enviarComprobantePago(String destinatario, String razonSocial, String ruc,
                                      String numeroFactura, String concepto, String total,
                                      String metodoPago, String numeroOperacion) {
        try {
            Resend resend = new Resend(apiKey);
            String html = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto">
                  <div style="background:#1D3557;color:#fff;padding:24px;border-radius:8px 8px 0 0;text-align:center">
                    <h2 style="margin:0">🏛️ Municipalidad Provincial de Trujillo</h2>
                    <p style="margin:8px 0 0;opacity:.8">Comprobante de Pago Emitido</p>
                  </div>
                  <div style="background:#f8f9fa;padding:24px;border-radius:0 0 8px 8px">
                    <h3 style="color:#1D3557;margin-top:0">¡Pago Confirmado Exitosamente!</h3>
                    <p>Estimado(a) representante de <strong>%s</strong> (RUC: <strong>%s</strong>):</p>
                    <p>Se ha registrado exitosamente el pago de su derecho de trámite.</p>

                    <div style="background:#fff;border:1px solid #dee2e6;border-radius:8px;padding:16px;margin:20px 0">
                      <h4 style="margin:0 0 12px;color:#1D3557;border-bottom:1px solid #eee;padding-bottom:8px">📄 Detalle del Comprobante de Pago</h4>
                      <table style="width:100%%;font-size:14px;border-collapse:collapse">
                        <tr><td style="padding:4px 0;color:#666">N° Comprobante:</td><td style="padding:4px 0;font-weight:bold;text-align:right">%s</td></tr>
                        <tr><td style="padding:4px 0;color:#666">Concepto:</td><td style="padding:4px 0;text-align:right">%s</td></tr>
                        <tr><td style="padding:4px 0;color:#666">Método de Pago:</td><td style="padding:4px 0;text-align:right">%s</td></tr>
                        <tr><td style="padding:4px 0;color:#666">N° Operación:</td><td style="padding:4px 0;text-align:right">%s</td></tr>
                        <tr style="border-top:1px solid #eee"><td style="padding:8px 0 0;font-weight:bold;color:#1D3557">Monto Total:</td><td style="padding:8px 0 0;font-weight:bold;color:#198754;font-size:16px;text-align:right">S/ %s</td></tr>
                      </table>
                    </div>

                    <p style="font-size:14px;color:#555;text-align:center;margin-top:20px;">
                      Recuerde continuar con el formulario de registro del trámite para completar su solicitud.
                    </p>

                    <hr style="margin:24px 0;border:none;border-top:1px solid #dee2e6"/>
                    <p style="color:#999;font-size:12px;margin:0;text-align:center">
                      Municipalidad Provincial de Trujillo — Subgerencia de Licencias y Comercialización
                    </p>
                  </div>
                </div>
                """.formatted(razonSocial, ruc, numeroFactura, concepto, metodoPago, numeroOperacion, total);

            CreateEmailOptions params = CreateEmailOptions.builder()
                .from(fromEmail)
                .to(destinatario)
                .subject("📄 Comprobante de Pago - " + numeroFactura)
                .html(html)
                .build();

            resend.emails().send(params);
        } catch (Exception e) {
            log.error("Error enviando email comprobante de pago a {}", destinatario, e);
        }
    }
}
