const axios = require('axios');

const APIS_TOKEN = process.env.APIS_NET_PE_TOKEN || 'sk_15754.TlOTUeK0utOb5ZcrmNy1DOrnMXo8p9Yk';

const consultarRUC = async (ruc) => {
  try {
    if (!/^\d{11}$/.test(ruc)) {
      return { valido: false, error: 'RUC debe tener 11 dígitos' };
    }

    const response = await axios.get(
      'https://api.apis.net.pe/v2/sunat/ruc?numero=' + ruc,
      {
        headers: { Authorization: 'Bearer ' + APIS_TOKEN },
        timeout: 10000
      }
    );

    const data = response.data;

    if (!data || !data.razonSocial) {
      return { valido: false, error: 'RUC no encontrado en SUNAT' };
    }

    if (data.estado && data.estado !== 'ACTIVO') {
      return { valido: false, error: 'RUC con estado irregular en SUNAT: ' + data.estado };
    }

    if (data.condicion && data.condicion !== 'HABIDO') {
      return { valido: false, error: 'RUC con condicion irregular en SUNAT: ' + data.condicion };
    }

    return {
      valido: true,
      datos: {
        ruc: data.ruc || ruc,
        razonSocial: data.razonSocial,
        domicilioFiscal: data.direccion || data.domicilioFiscal || '',
        estado: data.estado,
        condicion: data.condicion,
        tipoContribuyente: data.tipoContribuyente
      }
    };
  } catch (err) {
    console.error('SUNAT API error:', err.message);
    if (err.response && err.response.status === 404) {
      return { valido: false, error: 'RUC no encontrado en SUNAT' };
    }
    return {
      valido: true,
      advertencia: 'No se pudo verificar en tiempo real con SUNAT. Intente nuevamente.',
      datos: { ruc, razonSocial: null, domicilioFiscal: null }
    };
  }
};

const consultarDNI = async (dni) => {
  try {
    if (!/^\d{8}$/.test(dni)) {
      return { valido: false, error: 'DNI debe tener 8 dígitos' };
    }

    const response = await axios.get(
      'https://api.apis.net.pe/v2/reniec/dni?numero=' + dni,
      {
        headers: { Authorization: 'Bearer ' + APIS_TOKEN },
        timeout: 10000
      }
    );

    const data = response.data;
    if (!data || !data.nombres) {
      return { valido: false, error: 'DNI no encontrado en RENIEC' };
    }

    return {
      valido: true,
      datos: {
        dni,
        nombres: data.nombres,
        apellidoPaterno: data.apellidoPaterno,
        apellidoMaterno: data.apellidoMaterno,
        nombreCompleto: data.nombres + ' ' + data.apellidoPaterno + ' ' + data.apellidoMaterno
      }
    };
  } catch (err) {
    console.error('RENIEC API error:', err.message);
    if (err.response && err.response.status === 404) {
      return { valido: false, error: 'DNI no encontrado en RENIEC' };
    }
    return { valido: true, advertencia: 'No se pudo verificar DNI en tiempo real.', datos: { dni } };
  }
};

module.exports = { consultarRUC, consultarDNI };
