package com.municipalidad.licencias.service;

import com.municipalidad.licencias.dto.SolicitudDto;
import com.municipalidad.licencias.model.*;
import com.municipalidad.licencias.repository.SolicitudRepository;
import com.municipalidad.licencias.repository.UsuarioRepository;
import com.municipalidad.licencias.service.SunatService.ResultadoSunat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SolicitudService {

    private final SolicitudRepository solicitudRepo;
    private final UsuarioRepository usuarioRepo;
    private final SunatService sunatService;
    private final InspeccionService inspeccionService;
    private final com.municipalidad.licencias.service.CloudinaryService cloudinaryService;
    private final com.municipalidad.licencias.service.EmailService emailService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.pago.tramite}")
    private BigDecimal montoPagoTramite;

    public SolicitudService(SolicitudRepository solicitudRepo,
                            UsuarioRepository usuarioRepo,
                            SunatService sunatService,
                            InspeccionService inspeccionService,
                            com.municipalidad.licencias.service.CloudinaryService cloudinaryService,
                            com.municipalidad.licencias.service.EmailService emailService) {
        this.solicitudRepo     = solicitudRepo;
        this.usuarioRepo       = usuarioRepo;
        this.sunatService      = sunatService;
        this.inspeccionService = inspeccionService;
        this.cloudinaryService = cloudinaryService;
        this.emailService      = emailService;
    }

    @Transactional
    public Solicitud crearBorrador(SolicitudDto dto, Usuario usuario) {
        if (dto.getRuc() == null || !dto.getRuc().startsWith("20"))
            throw new IllegalArgumentException(
                "Solo se aceptan RUC de empresas (persona jurídica, inician con 20).");
        // Eliminar borradores previos del usuario publico (sin pago completado)
        solicitudRepo.findByUsuario(usuario).stream()
            .filter(s -> s.getEstado() == Enums.EstadoTramite.BORRADOR)
            .forEach(solicitudRepo::delete);
        // RF15: Si tiene licencia vigente con misma dirección → bloquear, pedir renovación
        String direccionNueva = dto.getDireccionEstablecimiento() != null ?
            dto.getDireccionEstablecimiento().trim().toLowerCase() : "";
        for (Solicitud solicitudExistente : solicitudRepo.findByUsuario(usuario)) {
            if (solicitudExistente.getLicencia() != null &&
                solicitudExistente.getLicencia().getEstado() == Enums.EstadoLicencia.VIGENTE) {
                String direccionExistente = solicitudExistente.getDireccionEstablecimiento() != null ?
                    solicitudExistente.getDireccionEstablecimiento().trim().toLowerCase() : "";
                if (!direccionNueva.isEmpty() && direccionNueva.equals(direccionExistente)) {
                    throw new IllegalStateException(
                        "Ya tienes una licencia vigente para este local. " +
                        "Si deseas continuar operando, debes renovar tu licencia existente. " +
                        "Solo puedes iniciar un nuevo trámite si cambias de local.");
                }
            }
        }
        Solicitud s = Solicitud.builder()
            .razonSocial(dto.getRazonSocial().trim())
            .domicilioFiscal(dto.getDomicilioFiscal().trim())
            .rubro(dto.getRubro().trim())
            .ruc(dto.getRuc())
            .dni(dto.getDni())
            .telefono(dto.getTelefono())
            .correoElectronico(dto.getCorreoElectronico())
            .nombreRepresentante(dto.getNombreRepresentante())
            .dniRepresentante(dto.getDniRepresentante())
            .partidaSunarp(dto.getPartidaSunarp())
            .nombreComercial(dto.getNombreComercial())
            .direccionEstablecimiento(dto.getDireccionEstablecimiento())
            .horarioAtencion(dto.getHorarioAtencion())
            .areaTotalM2(dto.getAreaTotalM2())
            .numEstacionamientos(dto.getNumEstacionamientos())
            .modalidadTramite(dto.getModalidadTramite())
            .observacionesSolicitante(dto.getObservacionesSolicitante())
            .usuario(usuario)
            .estado(Enums.EstadoTramite.BORRADOR)
            .build();
        s.setDistrito(dto.getDistrito());
        s.setDistrito(dto.getDistrito());
        s.setDistrito(dto.getDistrito());
        s.setDistrito(dto.getDistrito());
        // Generar código de seguimiento único
        String prefijo = s.getDistrito() != null ?
            s.getDistrito().name().substring(0,3) : "TRJ";
        String codigo = prefijo + "-" + java.time.Year.now().getValue() + "-" +
            java.util.UUID.randomUUID().toString().substring(0,6).toUpperCase();
        s.setCodigoSeguimiento(codigo);
        return solicitudRepo.save(s);
    }

    @Transactional
    public void guardarReferencia(Long solicitudId, String token) {
        Solicitud s = obtenerPorId(solicitudId);
        s.setReferenciaPago(token);
        solicitudRepo.save(s);
    }

    @Transactional
    public void cargarFirma(Long solicitudId, MultipartFile archivo) throws IOException {
        Solicitud s = obtenerPorId(solicitudId);
        if (archivo == null || archivo.isEmpty()) return;
        String url = cloudinaryService.subirArchivo(archivo, "firmas");
        s.setFirmaUrl(url);
        solicitudRepo.save(s);
    }

    @Transactional
    public void cargarPlano(Long solicitudId, MultipartFile archivo) throws IOException {
        Solicitud s = obtenerPorId(solicitudId);
        validarArchivo(archivo);
        String url = cloudinaryService.subirArchivo(archivo, "planos");
        s.setPlanoUrl(url);
        solicitudRepo.save(s);
    }

    @Transactional
    public Solicitud enviarConPago(Long solicitudId, String referenciaPago) {
        Solicitud s = obtenerPorId(solicitudId);
        if (s.getPlanoUrl() == null || s.getPlanoUrl().isBlank())
            throw new IllegalStateException("Debe cargar el plano antes de pagar.");

        ResultadoSunat resultado = sunatService.validar(s.getRazonSocial(), s.getDomicilioFiscal());
        if (!resultado.valido()) {
            s.setEstado(Enums.EstadoTramite.PENDIENTE_VALIDACION);
            s.setValidadoSunat(false);
            s.setMotivoRechazoSunat(resultado.mensaje());
            solicitudRepo.save(s);
            throw new IllegalArgumentException("SUNAT: " + resultado.mensaje());
        }

        s.setValidadoSunat(true);
        s.setMontoPagado(montoPagoTramite);
        s.setFechaPago(LocalDateTime.now());
        s.setReferenciaPago(referenciaPago);
        s.setEstado(Enums.EstadoTramite.ADMITIDO);
        solicitudRepo.save(s);
        inspeccionService.programarPrimeraInspeccion(s);
        // Enviar email con código de seguimiento
        if (s.getCorreoElectronico() != null && s.getCodigoSeguimiento() != null) {
            emailService.enviarCodigoSeguimiento(
                s.getCorreoElectronico(),
                s.getRazonSocial(),
                s.getCodigoSeguimiento(),
                s.getDistrito() != null ? s.getDistrito().name() : "TRUJILLO"
            );
        }
        return s;
    }

    /**
     * RF-CAJERO: Registro presencial de una solicitud en ventanilla.
     * El cajero ingresa los datos del negocio, la documentación (plano) y cobra
     * el pago directamente (no pasa por Flow). El trámite queda ADMITIDO de inmediato
     * y se programa la primera inspección automáticamente.
     */
    @Transactional
    public Solicitud registrarPresencial(SolicitudDto dto, MultipartFile plano,
                                         MultipartFile firma, Usuario cajero, String referenciaPagoExterna) {
        if (dto.getRuc() == null || !dto.getRuc().startsWith("20"))
            throw new IllegalArgumentException(
                "Solo se aceptan RUC de empresas (persona jurídica, inician con 20).");
        if (plano == null || plano.isEmpty())
            throw new IllegalArgumentException("El plano del local es obligatorio.");
        validarArchivo(plano);

        // RF15: mismo control de licencia vigente en la misma dirección
        String direccionNueva = dto.getDireccionEstablecimiento() != null ?
            dto.getDireccionEstablecimiento().trim().toLowerCase() : "";
        if (!direccionNueva.isEmpty()) {
            for (Solicitud existente : solicitudRepo.findAll()) {
                if (existente.getLicencia() != null &&
                    existente.getLicencia().getEstado() == Enums.EstadoLicencia.VIGENTE) {
                    String direccionExistente = existente.getDireccionEstablecimiento() != null ?
                        existente.getDireccionEstablecimiento().trim().toLowerCase() : "";
                    if (direccionNueva.equals(direccionExistente)) {
                        throw new IllegalStateException(
                            "Ya existe una licencia vigente para esa dirección. " +
                            "Debe tramitarse como renovación, no como solicitud nueva.");
                    }
                }
            }
        }

        ResultadoSunat resultado = sunatService.validar(dto.getRazonSocial(), dto.getDomicilioFiscal());
        if (!resultado.valido())
            throw new IllegalArgumentException("SUNAT: " + resultado.mensaje());

        // El titular queda como el usuario "publico" para que pueda consultarse por código+DNI
        Usuario titular = usuarioRepo.findByUsername("publico")
            .orElseThrow(() -> new IllegalStateException("Usuario público del sistema no configurado."));

        Solicitud s = Solicitud.builder()
            .razonSocial(dto.getRazonSocial().trim())
            .domicilioFiscal(dto.getDomicilioFiscal().trim())
            .rubro(dto.getRubro().trim())
            .ruc(dto.getRuc())
            .dni(dto.getDni())
            .telefono(dto.getTelefono())
            .correoElectronico(dto.getCorreoElectronico())
            .nombreRepresentante(dto.getNombreRepresentante())
            .dniRepresentante(dto.getDniRepresentante())
            .partidaSunarp(dto.getPartidaSunarp())
            .nombreComercial(dto.getNombreComercial())
            .direccionEstablecimiento(dto.getDireccionEstablecimiento())
            .horarioAtencion(dto.getHorarioAtencion())
            .areaTotalM2(dto.getAreaTotalM2())
            .numEstacionamientos(dto.getNumEstacionamientos())
            .modalidadTramite(dto.getModalidadTramite())
            .observacionesSolicitante(dto.getObservacionesSolicitante())
            .usuario(titular)
            .estado(Enums.EstadoTramite.ADMITIDO)
            .build();
        s.setDistrito(dto.getDistrito());
        s.setCanal(Enums.CanalTramite.PRESENCIAL);
        s.setCajero(cajero);

        String prefijo = s.getDistrito() != null ? s.getDistrito().name().substring(0, 3) : "TRJ";
        s.setCodigoSeguimiento(prefijo + "-" + java.time.Year.now().getValue() + "-" +
            UUID.randomUUID().toString().substring(0, 6).toUpperCase());

        try {
            String urlPlano = cloudinaryService.subirArchivo(plano, "planos");
            s.setPlanoUrl(urlPlano);
            if (firma != null && !firma.isEmpty()) {
                s.setFirmaUrl(cloudinaryService.subirArchivo(firma, "firmas"));
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al subir el plano: " + e.getMessage(), e);
        }

        s.setValidadoSunat(true);
        s.setMontoPagado(montoPagoTramite);
        s.setFechaPago(LocalDateTime.now());
        s.setReferenciaPago(
            (referenciaPagoExterna != null && !referenciaPagoExterna.isBlank())
                ? referenciaPagoExterna.trim()
                : "CAJA-" + cajero.getUsername().toUpperCase() + "-" + System.currentTimeMillis());

        solicitudRepo.save(s);
        inspeccionService.programarPrimeraInspeccion(s);

        if (s.getCorreoElectronico() != null && !s.getCorreoElectronico().isBlank()) {
            emailService.enviarCodigoSeguimiento(
                s.getCorreoElectronico(), s.getRazonSocial(), s.getCodigoSeguimiento(),
                s.getDistrito() != null ? s.getDistrito().name() : "TRUJILLO");
        }
        return s;
    }

    public Solicitud obtenerPorId(Long id) {
        return solicitudRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada: " + id));
    }

    public List<Solicitud> obtenerPorUsuario(Usuario usuario) {
        return solicitudRepo.findByUsuario(usuario);
    }

    public List<Solicitud> obtenerTramitesActivos() {
        return solicitudRepo.findTramitesActivos();
    }

    private void validarArchivo(MultipartFile archivo) {
        if (archivo.isEmpty()) throw new IllegalArgumentException("El archivo está vacío.");
        String nombre = archivo.getOriginalFilename();
        if (nombre == null) throw new IllegalArgumentException("Nombre de archivo inválido.");
        String ext = nombre.toLowerCase();
        if (!ext.endsWith(".pdf") && !ext.endsWith(".jpg")
            && !ext.endsWith(".jpeg") && !ext.endsWith(".png"))
            throw new IllegalArgumentException("Formato no permitido. Use PDF, JPG o PNG.");
        if (archivo.getSize() > 10 * 1024 * 1024)
            throw new IllegalArgumentException("El archivo supera el límite de 10 MB.");
    }
}
