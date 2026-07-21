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
                                                            BigDecimal importeTotal, BigDecimal montoRecibido,
                                                            String concepto, Usuario cajero) {
        if (montoRecibido.compareTo(importeTotal) < 0)
            throw new IllegalArgumentException("El monto recibido es menor al monto a pagar (S/ " + importeTotal + ").");

        BigDecimal vuelto = montoRecibido.subtract(importeTotal).setScale(2, RoundingMode.HALF_UP);
        BigDecimal[] valores = calcularValorVentaEIgv(importeTotal);

        FacturaCaja factura = construirBase(ruc, razonSocial, direccion, concepto, importeTotal, valores, cajero, Enums.MetodoPago.EFECTIVO);
        factura.setMontoRecibido(montoRecibido);
        factura.setVuelto(vuelto);
        factura.setEstado(Enums.EstadoFactura.PAGADA);
        factura = facturaRepo.save(factura);
        factura.setNumeroOperacion("EFECTIVO-" + factura.getId() + "-" + System.currentTimeMillis());
        return facturaRepo.save(factura);
    }

    @Transactional
    public synchronized FacturaCaja crearFacturaPendienteQR(String ruc, String razonSocial, String direccion,
                                                             BigDecimal importeTotal, String concepto, Usuario cajero) {
        BigDecimal[] valores = calcularValorVentaEIgv(importeTotal);
        FacturaCaja factura = construirBase(ruc, razonSocial, direccion, concepto, importeTotal, valores, cajero, Enums.MetodoPago.QR);
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

    private FacturaCaja construirBase(String ruc, String razonSocial, String direccion, String concepto,
                                       BigDecimal importeTotal, BigDecimal[] valores, Usuario cajero,
                                       Enums.MetodoPago metodoPago) {
        return FacturaCaja.builder()
            .numero(facturaRepo.obtenerUltimoNumero() + 1)
            .rucCliente(ruc)
            .razonSocialCliente(razonSocial)
            .direccionCliente(direccion)
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
