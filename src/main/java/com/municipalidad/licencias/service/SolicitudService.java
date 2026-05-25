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

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.pago.tramite}")
    private BigDecimal montoPagoTramite;

    public SolicitudService(SolicitudRepository solicitudRepo,
                            UsuarioRepository usuarioRepo,
                            SunatService sunatService,
                            InspeccionService inspeccionService,
                            com.municipalidad.licencias.service.CloudinaryService cloudinaryService) {
        this.solicitudRepo     = solicitudRepo;
        this.usuarioRepo       = usuarioRepo;
        this.sunatService      = sunatService;
        this.inspeccionService = inspeccionService;
        this.cloudinaryService = cloudinaryService;
    }

    @Transactional
    public Solicitud crearBorrador(SolicitudDto dto, Usuario usuario) {
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
