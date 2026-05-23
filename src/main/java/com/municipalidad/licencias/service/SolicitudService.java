package com.municipalidad.licencias.service;

import com.municipalidad.licencias.dto.SolicitudDto;
import com.municipalidad.licencias.model.*;
import com.municipalidad.licencias.repository.Repositories.*;
import com.municipalidad.licencias.service.SunatService.ResultadoSunat;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class SolicitudService {

    private final SolicitudRepository solicitudRepo;
    private final UsuarioRepository usuarioRepo;
    private final SunatService sunatService;
    private final InspeccionService inspeccionService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.pago.tramite}")
    private BigDecimal montoPagoTramite;

    // ── E1-US01: Registrar solicitud ──────────────────────────────────────────
    @Transactional
    public Solicitud crearBorrador(SolicitudDto dto, Usuario usuario) {
        Solicitud s = Solicitud.builder()
            .razonSocial(dto.getRazonSocial().trim())
            .domicilioFiscal(dto.getDomicilioFiscal().trim())
            .rubro(dto.getRubro().trim())
            .usuario(usuario)
            .estado(Enums.EstadoTramite.BORRADOR)
            .build();
        return solicitudRepo.save(s);
    }

    // ── E1-US03: Cargar plano ─────────────────────────────────────────────────
    @Transactional
    public void cargarPlano(Long solicitudId, MultipartFile archivo) throws IOException {
        Solicitud s = obtenerPorId(solicitudId);
        validarArchivo(archivo);

        String nombre = UUID.randomUUID() + "_" + archivo.getOriginalFilename();
        Path destino = Paths.get(uploadDir).resolve(nombre);
        Files.createDirectories(destino.getParent());
        Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

        s.setPlanoUrl(nombre);
        solicitudRepo.save(s);
    }

    // ── E1-US02 + E1-US04: Validar SUNAT y registrar pago ────────────────────
    @Transactional
    public Solicitud enviarConPago(Long solicitudId, String referenciaPago) {
        Solicitud s = obtenerPorId(solicitudId);

        if (s.getPlanoUrl() == null || s.getPlanoUrl().isBlank()) {
            throw new IllegalStateException("Debe cargar el plano antes de pagar.");
        }

        // Validación SUNAT (E1-US02)
        ResultadoSunat resultado = sunatService.validar(s.getRazonSocial(), s.getDomicilioFiscal());
        if (!resultado.valido()) {
            s.setEstado(Enums.EstadoTramite.PENDIENTE_VALIDACION);
            s.setValidadoSunat(false);
            s.setMotivoRechazoSunat(resultado.mensaje());
            solicitudRepo.save(s);
            throw new IllegalArgumentException("SUNAT: " + resultado.mensaje());
        }

        // Registrar pago (E1-US04)
        s.setValidadoSunat(true);
        s.setMontoPagado(montoPagoTramite);
        s.setFechaPago(LocalDateTime.now());
        s.setReferenciaPago(referenciaPago);
        s.setEstado(Enums.EstadoTramite.ADMITIDO);
        solicitudRepo.save(s);

        // Programar 1.ª inspección automáticamente (E2-US01)
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
            && !ext.endsWith(".jpeg") && !ext.endsWith(".png")) {
            throw new IllegalArgumentException("Formato no permitido. Use PDF, JPG o PNG.");
        }
        if (archivo.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("El archivo supera el límite de 10 MB.");
        }
    }
}
