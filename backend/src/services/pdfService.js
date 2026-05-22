const PDFDocument = require('pdfkit');
const fs = require('fs');
const path = require('path');

/**
 * Genera el PDF de la Licencia de Funcionamiento
 * Formato basado en el modelo oficial de la Municipalidad de Trujillo
 */
const generarLicenciaPDF = async (datos, outputPath) => {
  return new Promise((resolve, reject) => {
    const doc = new PDFDocument({
      size: 'A4',
      margins: { top: 40, bottom: 40, left: 50, right: 50 }
    });

    const stream = fs.createWriteStream(outputPath);
    doc.pipe(stream);

    const W = 595.28;
    const ancho = W - 100;

    // ── BORDE EXTERIOR ──────────────────────────────────────────
    doc.rect(30, 30, W - 60, 780).lineWidth(3).strokeColor('#003580').stroke();
    doc.rect(35, 35, W - 70, 770).lineWidth(1).strokeColor('#c8a900').stroke();

    // ── ESCUDO (si existe el archivo) ───────────────────────────
    const escudoPath = path.join(__dirname, '../../assets/escudo-trujillo.png');
    if (fs.existsSync(escudoPath)) {
      doc.image(escudoPath, 50, 45, { width: 80, height: 80 });
    }

    // ── ENCABEZADO ───────────────────────────────────────────────
    doc
      .font('Helvetica-Bold')
      .fontSize(9)
      .fillColor('#003580')
      .text('MUNICIPALIDAD PROVINCIAL DE TRUJILLO', 140, 48, { width: ancho - 80, align: 'center' })
      .text('GERENCIA DE DESARROLLO URBANO', 140, 60, { width: ancho - 80, align: 'center' })
      .text('SUB GERENCIA DE AUTORIZACIONES MUNICIPALES', 140, 72, { width: ancho - 80, align: 'center' });

    // ── DATOS SUPERIORES DERECHA ──────────────────────────────────
    doc
      .font('Helvetica')
      .fontSize(8)
      .fillColor('#000')
      .text(`NRO LICENCIA  :  ${datos.numeroLicencia}`, W - 200, 50)
      .text(`EXPEDIENTE     :  ${datos.numeroExpediente}`, W - 200, 62)
      .text(`FECHA INGRESO :  ${formatFecha(datos.fechaIngreso)}`, W - 200, 74)
      .text(`CODIGO PREDIO  :  ${datos.codigoPredio || '—'}`, W - 200, 86);

    // ── LÍNEA SEPARADORA ─────────────────────────────────────────
    doc.moveTo(50, 140).lineTo(W - 50, 140).lineWidth(2).strokeColor('#003580').stroke();

    // ── TÍTULO PRINCIPAL ─────────────────────────────────────────
    doc
      .font('Helvetica-Bold')
      .fontSize(22)
      .fillColor('#003580')
      .text('LICENCIA MUNICIPAL DE', 50, 155, { width: ancho, align: 'center' })
      .text('FUNCIONAMIENTO', 50, 183, { width: ancho, align: 'center' });

    // ── TEXTO LEGAL ───────────────────────────────────────────────
    doc
      .font('Helvetica')
      .fontSize(8.5)
      .fillColor('#000')
      .text(
        'Habiendo cumplido con los requisitos establecidos, y en aplicación a lo previsto en la Ley Marco de Licencia ' +
        'de Funcionamiento N° 28976, la Ordenanza Municipal vigente y el artículo 79°, numeral 3 de la Ley N° 27972, ' +
        'Ley Orgánica de Municipalidades, se concede el presente:',
        50, 220, { width: ancho, align: 'justify', lineGap: 2 }
      );

    // ── DATOS DEL NEGOCIO ─────────────────────────────────────────
    const campoY = 275;
    const colLabel = 50;
    const colValor = 190;
    const espacioFila = 22;

    const campos = [
      ['A', datos.razonSocial],
      ['Nombre Comercial', datos.nombreComercial || datos.razonSocial],
      ['Dirección', datos.domicilioFiscal],
      ['R.U.C.', datos.ruc + (datos.areaM2 ? `            Área :  ${datos.areaM2} M²` : '')],
      ['Giro del Negocio', datos.rubro],
      ['Resolución', datos.numeroResolucion],
    ];

    campos.forEach(([label, valor], i) => {
      const y = campoY + i * espacioFila;
      doc
        .font('Helvetica-Bold').fontSize(9).fillColor('#000')
        .text(label, colLabel, y);
      doc
        .font('Helvetica').fontSize(9).fillColor('#000')
        .text(`:  ${valor}`, colValor, y, { width: ancho - 150 });
    });

    // ── LÍNEA SEPARADORA ─────────────────────────────────────────
    const lineaY = campoY + campos.length * espacioFila + 10;
    doc.moveTo(50, lineaY).lineTo(W - 50, lineaY).lineWidth(1).strokeColor('#ccc').stroke();

    // ── LUGAR Y FECHA ─────────────────────────────────────────────
    const fechaEmision = new Date(datos.fechaEmision);
    const dia = fechaEmision.getDate();
    const mes = fechaEmision.toLocaleString('es-PE', { month: 'long' });
    const anio = fechaEmision.getFullYear();

    doc
      .font('Helvetica')
      .fontSize(10)
      .fillColor('#000')
      .text(`Trujillo, ${dia} de ${mes} del ${anio}`, 50, lineaY + 20, { width: ancho, align: 'center' });

    // ── VIGENCIA ──────────────────────────────────────────────────
    doc
      .font('Helvetica-Bold')
      .fontSize(9)
      .fillColor('#003580')
      .text(`VIGENCIA: ${formatFecha(datos.fechaEmision)} al ${formatFecha(datos.fechaVencimiento)}`,
        50, lineaY + 40, { width: ancho, align: 'center' });

    // ── FIRMA ─────────────────────────────────────────────────────
    const firmaY = lineaY + 100;
    doc.moveTo(W / 2 - 80, firmaY).lineTo(W / 2 + 80, firmaY).lineWidth(1).strokeColor('#000').stroke();
    doc
      .font('Helvetica-Bold').fontSize(9).fillColor('#000')
      .text('SUB GERENTE DE AUTORIZACIONES MUNICIPALES', 50, firmaY + 5, { width: ancho, align: 'center' });

    // ── CÓDIGO DE VERIFICACIÓN ────────────────────────────────────
    doc
      .font('Helvetica')
      .fontSize(7.5)
      .fillColor('#555')
      .text(
        `Código de verificación: ${datos.codigoVerificacion}  |  ` +
        `Verifique en: ${process.env.FRONTEND_URL || 'https://licencias.trujillo.gob.pe'}/verificar/${datos.codigoVerificacion}`,
        50, firmaY + 60, { width: ancho, align: 'center' }
      );

    // ── PIE ───────────────────────────────────────────────────────
    doc
      .font('Helvetica')
      .fontSize(7)
      .fillColor('#333')
      .text(
        'EL PRESENTE DEBE MANTENERSE EN LUGAR VISIBLE. EN CASO DE CESE DE ACTIVIDADES DEBE COMUNICARSE A LA MUNICIPALIDAD.',
        50, firmaY + 80, { width: ancho, align: 'center' }
      );

    doc.end();

    stream.on('finish', () => resolve(outputPath));
    stream.on('error', reject);
  });
};

const formatFecha = (fecha) => {
  if (!fecha) return '—';
  const d = new Date(fecha);
  return d.toLocaleDateString('es-PE', { day: '2-digit', month: '2-digit', year: 'numeric' });
};

module.exports = { generarLicenciaPDF };
