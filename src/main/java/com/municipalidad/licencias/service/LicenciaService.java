package com.municipalidad.licencias.service;

import com.municipalidad.licencias.model.*;
import com.municipalidad.licencias.repository.*;
import com.municipalidad.licencias.util.PdfLicenciaUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class LicenciaService {

    private final LicenciaRepository  licenciaRepo;
    private final SolicitudRepository solicitudRepo;
    private final RenovacionRepository renovacionRepo;
    private final com.municipalidad.licencias.repository.MultaRepository multaRepo;
    private final NotificacionService notificacionService;

    @Value("${app.licencia.vigencia-dias}")   private int vigenciaDias;
    @Value("${app.pago.renovacion}")          private BigDecimal montoPagoRenovacion;
    @Value("${app.licencia.alerta-dias-1}")   private int alertaDias1;
    @Value("${app.licencia.alerta-dias-2}")   private int alertaDias2;

    public LicenciaService(LicenciaRepository licenciaRepo,
                           SolicitudRepository solicitudRepo,
                           RenovacionRepository renovacionRepo,
                           com.municipalidad.licencias.repository.MultaRepository multaRepo,
                           NotificacionService notificacionService) {
        this.licenciaRepo        = licenciaRepo;
        this.solicitudRepo       = solicitudRepo;
        this.renovacionRepo      = renovacionRepo;
        this.multaRepo           = multaRepo;
        this.notificacionService = notificacionService;
    }

    @Transactional
    public Licencia emitirLicencia(Solicitud solicitud) {
        // RNF01: No emitir licencia si tiene multas pendientes
        long multasPendientes = multaRepo.findByLicenciaOrderByCreadoEnDesc(
            licenciaRepo.findBySolicitud(solicitud).orElse(null) != null ?
            licenciaRepo.findBySolicitud(solicitud).get() : null) != null ?
            multaRepo.findByLicenciaOrderByCreadoEnDesc(
                licenciaRepo.findBySolicitud(solicitud).orElse(null))
                .stream()
                .filter(m -> m.getEstado() == com.municipalidad.licencias.model.Multa.EstadoMulta.PENDIENTE)
                .count() : 0;
        if (multasPendientes > 0) {
            throw new IllegalStateException("No se puede emitir la licencia: el negocio tiene " + multasPendientes + " multa(s) pendiente(s) de pago.");
        }
        if (solicitud.getEstado() != Enums.EstadoTramite.APROBADO)
            throw new IllegalStateException("Solo se puede emitir licencia para trámites aprobados.");
        LocalDate hoy = LocalDate.now();
        Licencia licencia = Licencia.builder()
            .numeroLicencia(generarNumero(solicitud))
            .solicitud(solicitud)
            .fechaEmision(hoy)
            .fechaVencimiento(hoy.plusDays(vigenciaDias))
            .estado(Enums.EstadoLicencia.VIGENTE)
            .build();
        return licenciaRepo.save(licencia);
    }

    @Transactional
    public Licencia renovar(Long licenciaId, String referenciaPago) {
        Licencia licencia = obtenerPorId(licenciaId);
        if (licencia.getEstado() == Enums.EstadoLicencia.REVOCADA)
            throw new IllegalStateException("No se puede renovar una licencia revocada.");
        LocalDate nuevaFecha = licencia.getFechaVencimiento().plusDays(vigenciaDias);
        renovacionRepo.save(Renovacion.builder()
            .licencia(licencia).montoPagado(montoPagoRenovacion)
            .referenciaPago(referenciaPago).nuevaFechaVencimiento(nuevaFecha).build());
        licencia.setFechaVencimiento(nuevaFecha);
        licencia.setEstado(Enums.EstadoLicencia.VIGENTE);
        return licenciaRepo.save(licencia);
    }

    @Transactional
    public Licencia revocar(Long licenciaId, String motivo) {
        Licencia licencia = obtenerPorId(licenciaId);
        licencia.setEstado(Enums.EstadoLicencia.REVOCADA);
        licencia.setMotivoRevocacion(motivo);
        licencia.setFechaRevocacion(LocalDateTime.now());
        return licenciaRepo.save(licencia);
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void procesarVencimientos() {
        LocalDate hoy = LocalDate.now();
        List<Licencia> expiradas = licenciaRepo.findByEstado(Enums.EstadoLicencia.VIGENTE)
            .stream().filter(l -> l.getFechaVencimiento().isBefore(hoy)).toList();
        expiradas.forEach(l -> l.setEstado(Enums.EstadoLicencia.EXPIRADA));
        licenciaRepo.saveAll(expiradas);

        List<Licencia> porVencer = licenciaRepo.findLicenciasProximasAVencer(hoy.plusDays(alertaDias1));
        porVencer.stream().filter(l -> l.getEstado() == Enums.EstadoLicencia.VIGENTE)
            .forEach(l -> {
                l.setEstado(Enums.EstadoLicencia.POR_VENCER);
                // Notificar al negocio sobre vencimiento próximo
                notificacionService.crear(
                    l.getSolicitud().getUsuario(),
                    "Tu licencia está por vencer",
                    "La licencia " + l.getNumeroLicencia() + " vence el " +
                    l.getFechaVencimiento().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                    ". Renuévala para continuar operando.",
                    "/solicitud/" + l.getSolicitud().getId() + "/detalle"
                );
            });
        licenciaRepo.saveAll(porVencer);
    }

    public byte[] generarPdf(Long licenciaId) {
        Licencia licencia = obtenerPorId(licenciaId);
        if (licencia.getEstado() == Enums.EstadoLicencia.EXPIRADA ||
            licencia.getEstado() == Enums.EstadoLicencia.REVOCADA)
            throw new IllegalStateException("No se puede descargar una licencia " +
                licencia.getEstado().name().toLowerCase() + ".");
        return PdfLicenciaUtil.generar(licencia);
    }

    public Licencia obtenerPorId(Long id) {
        return licenciaRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Licencia no encontrada: " + id));
    }

    public Licencia obtenerPorSolicitud(Solicitud solicitud) {
        return licenciaRepo.findBySolicitud(solicitud).orElse(null);
    }

    public long contarMultasPendientes(Long licenciaId) {
        try {
            return multaRepo.findByLicenciaOrderByCreadoEnDesc(obtenerPorId(licenciaId))
                .stream()
                .filter(m -> m.getEstado() == com.municipalidad.licencias.model.Multa.EstadoMulta.PENDIENTE)
                .count();
        } catch (Exception e) { return 0; }
    }

    public java.util.List<Licencia> obtenerLicenciasRevocadas() {
        return licenciaRepo.findByEstado(Enums.EstadoLicencia.REVOCADA);
    }

    public java.util.List<Licencia> obtenerLicenciasVigentes() {
        return licenciaRepo.findByEstado(Enums.EstadoLicencia.VIGENTE);
    }

    private String generarNumero(Solicitud solicitud) {
        String anio = DateTimeFormatter.ofPattern("yyyy").format(LocalDate.now());
        return "LIC-" + anio + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
