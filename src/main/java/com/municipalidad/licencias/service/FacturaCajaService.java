package com.municipalidad.licencias.service;

import com.municipalidad.licencias.model.Enums;
import com.municipalidad.licencias.model.FacturaCaja;
import com.municipalidad.licencias.model.Usuario;
import com.municipalidad.licencias.repository.FacturaCajaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FacturaCajaService {

    private static final BigDecimal TASA_IGV = new BigDecimal("0.18");
    private static final BigDecimal UNO_MAS_IGV = new BigDecimal("1.18");

    private final FacturaCajaRepository facturaRepo;

    public FacturaCajaService(FacturaCajaRepository facturaRepo) {
        this.facturaRepo = facturaRepo;
    }

    @Transactional
    public synchronized FacturaCaja generarFacturaEfectivo(String ruc, String razonSocial, String direccion,
                                                            String email, BigDecimal importeTotal,
                                                            BigDecimal montoRecibido,
                                                            String concepto, Usuario cajero) {
        if (montoRecibido.compareTo(importeTotal) < 0)
            throw new IllegalArgumentException("El monto recibido es menor al monto a pagar (S/ " + importeTotal + ").");

        BigDecimal vuelto = montoRecibido.subtract(importeTotal).setScale(2, RoundingMode.HALF_UP);
        BigDecimal[] valores = calcularValorVentaEIgv(importeTotal);

        FacturaCaja factura = construirBase(ruc, razonSocial, direccion, email, concepto, importeTotal, valores, cajero, Enums.MetodoPago.EFECTIVO);
        factura.setMontoRecibido(montoRecibido);
        factura.setVuelto(vuelto);
        factura.setEstado(Enums.EstadoFactura.PAGADA);
        factura = facturaRepo.save(factura);
        factura.setNumeroOperacion("EFECTIVO-" + factura.getId() + "-" + System.currentTimeMillis());
        return facturaRepo.save(factura);
    }

    @Transactional
    public synchronized FacturaCaja crearFacturaPendienteQR(String ruc, String razonSocial, String direccion,
                                                             String email, BigDecimal importeTotal,
                                                             String concepto, Usuario cajero) {
        BigDecimal[] valores = calcularValorVentaEIgv(importeTotal);
        FacturaCaja factura = construirBase(ruc, razonSocial, direccion, email, concepto, importeTotal, valores, cajero, Enums.MetodoPago.QR);
        factura.setEstado(Enums.EstadoFactura.PENDIENTE);
        return facturaRepo.save(factura);
    }

    @Transactional
    public FacturaCaja confirmarPagoQR(Long facturaId, String flowToken) {
        FacturaCaja factura = obtenerPorId(facturaId);
        if (factura.getEstado() != Enums.EstadoFactura.PAGADA) {
            factura.setEstado(Enums.EstadoFactura.PAGADA);
            factura.setFlowToken(flowToken);
            factura.setNumeroOperacion("QR-" + facturaId + "-" + System.currentTimeMillis());
            facturaRepo.save(factura);
        }
        return factura;
    }

    // ── Pago dividido en varias partes (efectivo y/o QR) ────────────────────────

    @Transactional
    public synchronized FacturaCaja crearParteQR(String ruc, String razonSocial, String direccion, String email,
                                                  BigDecimal montoParte, String grupoPago, Usuario cajero) {
        if (montoParte.compareTo(new BigDecimal("2.00")) < 0)
            throw new IllegalArgumentException("La pasarela de pago no acepta montos menores a S/ 2.00.");
        BigDecimal[] valores = calcularValorVentaEIgv(montoParte);
        FacturaCaja factura = construirBase(ruc, razonSocial, direccion, email,
            "Derecho de tramite - Licencia de Funcionamiento (parte)", montoParte, valores, cajero, Enums.MetodoPago.QR);
        factura.setEstado(Enums.EstadoFactura.PENDIENTE);
        factura.setGrupoPago(grupoPago);
        return facturaRepo.save(factura);
    }

    @Transactional
    public synchronized FacturaCaja crearParteEfectivo(String ruc, String razonSocial, String direccion, String email,
                                                        BigDecimal montoParte, BigDecimal montoRecibido,
                                                        String grupoPago, Usuario cajero) {
        if (montoRecibido.compareTo(montoParte) < 0)
            throw new IllegalArgumentException("El monto recibido es menor al monto de esta parte (S/ " + montoParte + ").");
        BigDecimal vuelto = montoRecibido.subtract(montoParte).setScale(2, RoundingMode.HALF_UP);
        BigDecimal[] valores = calcularValorVentaEIgv(montoParte);
        FacturaCaja factura = construirBase(ruc, razonSocial, direccion, email,
            "Derecho de tramite - Licencia de Funcionamiento (parte)", montoParte, valores, cajero, Enums.MetodoPago.EFECTIVO);
        factura.setMontoRecibido(montoRecibido);
        factura.setVuelto(vuelto);
        factura.setEstado(Enums.EstadoFactura.PAGADA);
        factura.setGrupoPago(grupoPago);
        factura = facturaRepo.save(factura);
        factura.setNumeroOperacion("EFECTIVO-" + factura.getId() + "-" + System.currentTimeMillis());
        return facturaRepo.save(factura);
    }

    public java.util.List<FacturaCaja> obtenerPorGrupo(String grupoPago) {
        return facturaRepo.findByGrupoPago(grupoPago);
    }

    /**
     * Junta las partes ya pagadas de un grupo en una factura consolidada (MIXTO) que
     * representa el monto total, para poder emitir un solo comprobante y continuar el
     * registro del tramite. Las partes originales NO se tocan ni se borran: cada una
     * sigue contando por separado (EFECTIVO/QR) para el arqueo de caja.
     */
    @Transactional
    public synchronized FacturaCaja consolidarGrupo(String grupoPago, BigDecimal totalEsperado) {
        java.util.List<FacturaCaja> partes = facturaRepo.findByGrupoPago(grupoPago);
        if (partes.isEmpty())
            throw new IllegalArgumentException("No se encontraron pagos para este grupo.");
        for (FacturaCaja p : partes) {
            if (p.getEstado() != Enums.EstadoFactura.PAGADA)
                throw new IllegalStateException("Aún hay partes de este pago sin confirmar.");
        }
        BigDecimal suma = partes.stream().map(FacturaCaja::getImporteTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (suma.compareTo(totalEsperado) != 0)
            throw new IllegalStateException(
                "La suma de las partes (S/ " + suma + ") no coincide con el monto del trámite (S/ " + totalEsperado + ").");
        BigDecimal recibido = partes.stream()
            .map(f -> f.getMontoRecibido() != null ? f.getMontoRecibido() : f.getImporteTotal())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal vuelto = partes.stream()
            .map(f -> f.getVuelto() != null ? f.getVuelto() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        FacturaCaja base = partes.get(0);
        BigDecimal[] valores = calcularValorVentaEIgv(suma);
        FacturaCaja consolidada = construirBase(base.getRucCliente(), base.getRazonSocialCliente(),
            base.getDireccionCliente(), base.getEmailCliente(),
            "Derecho de tramite - Licencia de Funcionamiento (pago dividido)",
            suma, valores, base.getCajero(), Enums.MetodoPago.MIXTO);
        consolidada.setMontoRecibido(recibido);
        consolidada.setVuelto(vuelto);
        consolidada.setEstado(Enums.EstadoFactura.PAGADA);
        consolidada.setGrupoPago(grupoPago);
        consolidada = facturaRepo.save(consolidada);
        consolidada.setNumeroOperacion("MIXTO-" + consolidada.getId() + "-" + System.currentTimeMillis());
        return facturaRepo.save(consolidada);
    }

    private FacturaCaja construirBase(String ruc, String razonSocial, String direccion, String email, String concepto,
                                       BigDecimal importeTotal, BigDecimal[] valores, Usuario cajero,
                                       Enums.MetodoPago metodoPago) {
        return FacturaCaja.builder()
            .numero(facturaRepo.obtenerUltimoNumero() + 1)
            .rucCliente(ruc)
            .razonSocialCliente(razonSocial)
            .direccionCliente(direccion)
            .emailCliente(email)
            .concepto(concepto)
            .valorVenta(valores[0])
            .igv(valores[1])
            .importeTotal(importeTotal)
            .metodoPago(metodoPago)
            .cajero(cajero)
            .build();
    }

    /** Retorna [valorVenta, igv] dado un importe total que ya incluye IGV (18%). */
    private BigDecimal[] calcularValorVentaEIgv(BigDecimal importeTotal) {
        BigDecimal valorVenta = importeTotal.divide(UNO_MAS_IGV, 2, RoundingMode.HALF_UP);
        BigDecimal igv = importeTotal.subtract(valorVenta);
        return new BigDecimal[]{valorVenta, igv};
    }

    public FacturaCaja obtenerPorId(Long id) {
        return facturaRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada: " + id));
    }

    public FacturaCaja obtenerPorNumeroOperacion(String numeroOperacion) {
        return facturaRepo.findByNumeroOperacion(numeroOperacion)
            .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada para la operación: " + numeroOperacion));
    }

    @Transactional
    public void asociarASolicitud(Long facturaId, Long solicitudId) {
        FacturaCaja f = obtenerPorId(facturaId);
        f.setSolicitudId(solicitudId);
        facturaRepo.save(f);
    }
}
