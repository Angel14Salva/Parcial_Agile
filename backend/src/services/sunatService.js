const axios = require('axios');

/**
 * Consulta datos de un RUC en SUNAT
 * Usa API pública de consulta (sin convenio para sandbox académico)
 * En producción real: reemplazar con API oficial SUNAT con convenio
 */
const consultarRUC = async (ruc) => {
  try {
    // Validación formato RUC peruano
    if (!/^\d{11}$/.test(ruc)) {
      return { valido: false, error: 'RUC debe tener 11 dígitos' };
    }

    // Dígito verificador RUC
    if (!validarDigitoRUC(ruc)) {
      return { valido: false, error: 'RUC no válido (dígito verificador incorrecto)' };
    }

    // Consulta API pública
    const response = await axios.get(
      `https://api.sunat.cloud/v1/contribuyente/contribuyentes/${ruc}/verificar`,
      {
        headers: { Authorization: `Bearer ${process.env.SUNAT_API_TOKEN || ''}` },
        timeout: 8000
      }
    );

    const data = response.data;

    if (!data || data.estado === 'NO HABIDO' || data.condicion === 'NO ACTIVO') {
      return {
        valido: false,
        error: `RUC con estado irregular en SUNAT: ${data.estado || 'Inactivo'}`
      };
    }

    return {
      valido: true,
      datos: {
        ruc: data.ruc || ruc,
        razonSocial: data.razonSocial || data.nombre,
        domicilioFiscal: data.direccion || data.domicilioFiscal,
        estado: data.estado,
        condicion: data.condicion,
        tipoContribuyente: data.tipoContribuyente
      }
    };
  } catch (err) {
    // Si la API pública falla, validamos localmente el formato
    console.error('SUNAT API error:', err.message);

    // Fallback: validación de formato solamente
    return {
      valido: true,
      advertencia: 'No se pudo verificar en tiempo real con SUNAT. Validación de formato OK.',
      datos: { ruc, razonSocial: null, domicilioFiscal: null }
    };
  }
};

/**
 * Algoritmo oficial de dígito verificador RUC Perú
 */
const validarDigitoRUC = (ruc) => {
  const factores = [5, 4, 3, 2, 7, 6, 5, 4, 3, 2];
  let suma = 0;
  for (let i = 0; i < 10; i++) {
    suma += parseInt(ruc[i]) * factores[i];
  }
  const resto = suma % 11;
  const digitoCalculado = resto < 2 ? resto : 11 - resto;
  return digitoCalculado === parseInt(ruc[10]);
};

module.exports = { consultarRUC };
