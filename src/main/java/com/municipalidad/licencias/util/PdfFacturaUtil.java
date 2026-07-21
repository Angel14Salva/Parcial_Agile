package com.municipalidad.licencias.util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import com.municipalidad.licencias.model.Enums;
import com.municipalidad.licencias.model.FacturaCaja;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

public class PdfFacturaUtil {

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static byte[] generar(FacturaCaja factura, String montoEnLetras) {
        try {
            Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font fEmpresa = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Font fDatos   = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
            Font fTitulo  = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Font fSerie   = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Font fLabel   = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
            Font fValor   = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
            Font fThead   = new Font(Font.FontFamily.HELVETICA, 8.5f, Font.BOLD);
            Font fBold    = new Font(Font.FontFamily.HELVETICA, 9.5f, Font.BOLD);
            Font fSmall   = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);

            // ── Encabezado ───────────────────────────────────────────────────
            PdfPTable encabezado = new PdfPTable(2);
            encabezado.setWidthPercentage(100);
            encabezado.setWidths(new float[]{60, 40});

            PdfPCell celdaEmpresa = new PdfPCell();
            celdaEmpresa.setBorder(Rectangle.NO_BORDER);
            celdaEmpresa.addElement(new Paragraph("MUNICIPALIDAD PROVINCIAL DE TRUJILLO", fEmpresa));
            celdaEmpresa.addElement(new Paragraph("JR. ALMAGRO NRO. 525", fDatos));
            celdaEmpresa.addElement(new Paragraph("TRUJILLO - TRUJILLO - LA LIBERTAD", fDatos));
            encabezado.addCell(celdaEmpresa);

            PdfPTable cajaFactura = new PdfPTable(1);
            cajaFactura.addCell(celdaCentrada(new Phrase("FACTURA ELECTRONICA", fTitulo)));
            cajaFactura.addCell(celdaCentrada(new Phrase("RUC: 20175639391", fDatos)));
            cajaFactura.addCell(celdaCentrada(new Phrase(factura.getNumeroFormateado(), fSerie)));
            PdfPCell celdaCaja = new PdfPCell(cajaFactura);
            celdaCaja.setBorder(Rectangle.BOX);
            celdaCaja.setPadding(8);
            encabezado.addCell(celdaCaja);
            doc.add(encabezado);

            LineSeparator linea = new LineSeparator();
            linea.setLineWidth(1.2f);
            doc.add(new Chunk(linea));
            doc.add(new Paragraph(" ", new Font(Font.FontFamily.HELVETICA, 6)));

            // ── Datos del trámite ────────────────────────────────────────────
            PdfPTable datos = new PdfPTable(2);
            datos.setWidthPercentage(100);
            datos.setWidths(new float[]{30, 70});
            agregarFila(datos, "Fecha de Emisión", factura.getFechaEmision().format(FMT_FECHA), fLabel, fValor);
            agregarFila(datos, "Señor(es)", factura.getRazonSocialCliente(), fLabel, fValor);
            agregarFila(datos, "RUC", factura.getRucCliente(), fLabel, fValor);
            agregarFila(datos, "Dirección del Cliente",
                factura.getDireccionCliente() != null ? factura.getDireccionCliente() : "-", fLabel, fValor);
            agregarFila(datos, "Tipo de Moneda", "SOLES", fLabel, fValor);
            agregarFila(datos, "Forma de Pago",
                factura.getMetodoPago() == Enums.MetodoPago.EFECTIVO ? "EFECTIVO" : "MEDIO ELECTRONICO (QR)",
                fLabel, fValor);
            doc.add(datos);
            doc.add(new Paragraph(" ", new Font(Font.FontFamily.HELVETICA, 6)));

            // ── Item ─────────────────────────────────────────────────────────
            PdfPTable items = new PdfPTable(5);
            items.setWidthPercentage(100);
            items.setWidths(new float[]{10, 15, 18, 42, 15});
            for (String h : new String[]{"Cantidad", "Unidad Medida", "Código", "Descripción", "Valor Unitario"}) {
                PdfPCell c = new PdfPCell(new Phrase(h, fThead));
                c.setGrayFill(0.94f);
                c.setPadding(4);
                items.addCell(c);
            }
            items.addCell(celda("1.00", fValor, Element.ALIGN_RIGHT));
            items.addCell(celda("UNIDAD", fValor, Element.ALIGN_LEFT));
            items.addCell(celda("TRAM-LIC-FUNC", fValor, Element.ALIGN_LEFT));
            items.addCell(celda(factura.getConcepto(), fValor, Element.ALIGN_LEFT));
            items.addCell(celda(formatoMonto(factura.getValorVenta()), fValor, Element.ALIGN_RIGHT));
            doc.add(items);
            doc.add(new Paragraph(" ", new Font(Font.FontFamily.HELVETICA, 8)));

            // ── Zona inferior: izquierda (pago) + derecha (totales) ─────────
            PdfPTable zonaInferior = new PdfPTable(2);
            zonaInferior.setWidthPercentage(100);
            zonaInferior.setWidths(new float[]{45, 55});

            PdfPCell zonaIzq = new PdfPCell();
            zonaIzq.setBorder(Rectangle.NO_BORDER);
            if (factura.getMetodoPago() == Enums.MetodoPago.EFECTIVO) {
                zonaIzq.addElement(new Paragraph("Monto recibido: S/ " + formatoMonto(factura.getMontoRecibido()), fValor));
                zonaIzq.addElement(new Paragraph("Vuelto: S/ " + formatoMonto(factura.getVuelto()), fValor));
            } else {
                zonaIzq.addElement(new Paragraph("N.° de operación: " + factura.getNumeroOperacion(), fValor));
            }
            zonaInferior.addCell(zonaIzq);

            PdfPTable totales = new PdfPTable(2);
            totales.setWidths(new float[]{60, 40});
            agregarTotal(totales, "Sub Total Ventas", factura.getValorVenta(), fLabel, fValor, false);
            agregarTotal(totales, "Anticipos", java.math.BigDecimal.ZERO, fLabel, fValor, false);
            agregarTotal(totales, "Descuentos", java.math.BigDecimal.ZERO, fLabel, fValor, false);
            agregarTotal(totales, "Valor Venta", factura.getValorVenta(), fLabel, fValor, false);
            agregarTotal(totales, "ISC", java.math.BigDecimal.ZERO, fLabel, fValor, false);
            agregarTotal(totales, "IGV", factura.getIgv(), fLabel, fValor, false);
            agregarTotal(totales, "Otros Cargos", java.math.BigDecimal.ZERO, fLabel, fValor, false);
            agregarTotal(totales, "Otros Tributos", java.math.BigDecimal.ZERO, fLabel, fValor, false);
            agregarTotal(totales, "Importe Total", factura.getImporteTotal(), fBold, fBold, true);
            PdfPCell celdaTotales = new PdfPCell(totales);
            celdaTotales.setBorder(Rectangle.NO_BORDER);
            zonaInferior.addCell(celdaTotales);
            doc.add(zonaInferior);

            doc.add(new Paragraph(" ", new Font(Font.FontFamily.HELVETICA, 6)));
            doc.add(new Paragraph(montoEnLetras, fBold));
            doc.add(new Paragraph(" ", new Font(Font.FontFamily.HELVETICA, 10)));

            PdfPTable notaPie = new PdfPTable(1);
            notaPie.setWidthPercentage(100);
            PdfPCell notaCelda = new PdfPCell(new Phrase(
                "Esta es una representación impresa de la factura electrónica, generada en el Sistema de " +
                "Licencias de la Municipalidad Provincial de Trujillo.", fSmall));
            notaCelda.setBorder(Rectangle.BOX);
            notaCelda.setPadding(8);
            notaCelda.setHorizontalAlignment(Element.ALIGN_CENTER);
            notaPie.addCell(notaCelda);
            doc.add(notaPie);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF de factura: " + e.getMessage(), e);
        }
    }

    private static void agregarFila(PdfPTable t, String label, String valor, Font fL, Font fV) {
        PdfPCell cL = new PdfPCell(new Phrase(label, fL));
        cL.setBorder(Rectangle.NO_BORDER); cL.setPaddingBottom(3);
        PdfPCell cV = new PdfPCell(new Phrase(valor != null ? valor : "-", fV));
        cV.setBorder(Rectangle.NO_BORDER); cV.setPaddingBottom(3);
        t.addCell(cL); t.addCell(cV);
    }

    private static void agregarTotal(PdfPTable t, String label, java.math.BigDecimal valor, Font fL, Font fV, boolean destacado) {
        PdfPCell cL = new PdfPCell(new Phrase(label, fL));
        cL.setPadding(4);
        if (destacado) cL.setGrayFill(0.93f);
        PdfPCell cV = new PdfPCell(new Phrase("S/. " + formatoMonto(valor), fV));
        cV.setPadding(4);
        cV.setHorizontalAlignment(Element.ALIGN_RIGHT);
        if (destacado) cV.setGrayFill(0.93f);
        t.addCell(cL); t.addCell(cV);
    }

    private static PdfPCell celdaCentrada(Phrase phrase) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPaddingTop(2); cell.setPaddingBottom(2);
        return cell;
    }

    private static PdfPCell celda(String texto, Font f, int alineacion) {
        PdfPCell cell = new PdfPCell(new Phrase(texto != null ? texto : "-", f));
        cell.setPadding(4);
        cell.setHorizontalAlignment(alineacion);
        return cell;
    }

    private static String formatoMonto(java.math.BigDecimal valor) {
        return valor != null ? valor.setScale(2, java.math.RoundingMode.HALF_UP).toString() : "0.00";
    }
}
