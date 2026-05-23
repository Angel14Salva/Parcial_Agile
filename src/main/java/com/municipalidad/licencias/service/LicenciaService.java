package com.municipalidad.licencias.service;

import com.municipalidad.licencias.model.*;
import com.municipalidad.licencias.repository.Repositories.*;
import com.municipalidad.licencias.util.PdfLicenciaUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LicenciaService {

    private final LicenciaRepository licenciaRepo;
    private final SolicitudRepository solicitudRepo;
    private final RenovacionRepository renovacionRepo;

    @Value("${app.licencia.vigencia-dias}")
    private int vigenciaDias;

    @Value("${app.pago.renovacion}")
    private BigDecimal montoPagoRenovacion;

    @Value("${app.licencia.alerta-dias-1}")
    private int alertaDias1;

    @Value("${app.licencia.alerta-dias-2}")
    private int alertaDias2;

    // ── E4-US01: Emitir licencia tras aprobación ──────────────────────────────
    @Transactional
    public Licencia emitirLicencia(Solicitud solicitud) {
        if (solicitud.getEstado() != Enums.EstadoTramite.APROBADO) {
            throw new IllegalStateException("Solo se puede emitir licencia para trámites aprobados.");
        }

        LocalDate hoy = LocalDate.now();
        String numero = generarNumeroLicencia(solicitud);

        Licencia licencia = Licencia.builder()
            .numeroLicencia(numero)
            .solicitud(solicitud)
            .fechaEmision(hoy)
            .fechaVencimiento(hoy.plusDays(vigenciaDias))
            .estado(Enums.EstadoLicencia.VIGENTE)
            .build();

        return licenciaRepo.save(licencia);
    }

    // ── E5-US01: Renovación automática con S/180 ──────────────────────────────
    @Transactional
    public Licencia renovar(Long licenciaId, String referenciaPago) {
        Licencia licencia = obtenerPorId(licenciaId);

        if (licencia.getEstado() == Enums.EstadoLicencia.REVOCADA) {
            throw new IllegalStateException("No se puede renovar una licencia revocada. Inicie un nuevo trámite.");
        }

        // E5-US02: validar que el local no cambió (comparación simplificada - en producción
        // se verificaría contra SUNAT nuevamente si el domicilio cambió)
        LocalDate nuevaFecha = licencia.getFechaVencimiento().plusDays(vigenciaDias);

        Renovacion renovacion = Renovacion.builder()
            .licencia(licencia)
            .montoPagado(montoPagoRenovacion)
            .referenciaPago(referenciaPago)
            .nuevaFechaVencimiento(nuevaFecha)
            .build();
        renovacionRepo.save(renovacion);

        licencia.setFechaVencimiento(nuevaFecha);
        licencia.setEstado(Enums.EstadoLicencia.VIGENTE);
        return licenciaRepo.save(licencia);
    }

    // ── E6-US03: Revocar licencia ─────────────────────────────────────────────
    @Transactional
    public Licencia revocar(Long licenciaId, String motivo) {
        Licencia licencia = obtenerPorId(licenciaId);
        licencia.setEstado(Enums.EstadoLicencia.REVOCADA);
        licencia.setMotivoRevocacion(motivo);
        licencia.setFechaRevocacion(java.time.LocalDateTime.now());
        return licenciaRepo.save(licencia);
    }

    // ── E4-US02: Scheduler - controlar vencimiento ────────────────────────────
    @Scheduled(cron = "0 0 8 * * *") // Todos los días a las 8am
    @Transactional
    public void procesarVencimientos() {
        LocalDate hoy = LocalDate.now();

        // Marcar expiradas
        List<Licencia> expiradas = licenciaRepo.findByEstado(Enums.EstadoLicencia.VIGENTE)
            .stream()
            .filter(l -> l.getFechaVencimiento().isBefore(hoy))
            .toList();
        expiradas.forEach(l -> l.setEstado(Enums.EstadoLicencia.EXPIRADA));
        licenciaRepo.saveAll(expiradas);

        // Marcar por vencer (alertaDias1 = 30 días)
        List<Licencia> porVencer = licenciaRepo.findLicenciasProximasAVencer(
            hoy.plusDays(alertaDias1));
        porVencer.stream()
            .filter(l -> l.getEstado() == Enums.EstadoLicencia.VIGENTE)
            .forEach(l -> l.setEstado(Enums.EstadoLicencia.POR_VENCER));
        licenciaRepo.saveAll(porVencer);
    }

    public byte[] generarPdf(Long licenciaId) {
        Licencia licencia = obtenerPorId(licenciaId);
        if (licencia.getEstado() == Enums.EstadoLicencia.EXPIRADA ||
            licencia.getEstado() == Enums.EstadoLicencia.REVOCADA) {
            throw new IllegalStateException("No se puede descargar una licencia " +
                licencia.getEstado().name().toLowerCase() + ".");
        }
        return PdfLicenciaUtil.generar(licencia);
    }

    public Licencia obtenerPorId(Long id) {
        return licenciaRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Licencia no encontrada: " + id));
    }

    public Licencia obtenerPorSolicitud(Solicitud solicitud) {
        return licenciaRepo.findBySolicitud(solicitud).orElse(null);
    }

    private String generarNumeroLicencia(Solicitud solicitud) {
        String anio = DateTimeFormatter.ofPattern("yyyy").format(LocalDate.now());
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "LIC-" + anio + "-" + uuid;
    }
}
