const axios = require('axios');

const CULQI_URL = 'https://api.culqi.com/v2';

/**
 * Procesa un cargo con Culqi
 * token: generado por Culqi.js en el frontend
 */
const procesarPago = async ({ token, monto = 18000, email, descripcion }) => {
  try {
    const response = await axios.post(
      `${CULQI_URL}/charges`,
      {
        amount: monto, // en céntimos: 18000 = S/180.00
        currency_code: 'PEN',
        email,
        source_id: token,
        description: descripcion || 'Derecho de trámite - Licencia de Funcionamiento',
        metadata: {
          municipalidad: 'Trujillo',
          concepto: 'Licencia de Funcionamiento'
        }
      },
      {
        headers: {
          Authorization: `Bearer ${process.env.CULQI_SECRET_KEY}`,
          'Content-Type': 'application/json'
        },
        timeout: 15000
      }
    );

    return {
      exitoso: true,
      chargeId: response.data.id,
      monto: response.data.amount,
      estado: response.data.outcome?.type
    };
  } catch (err) {
    const culqiError = err.response?.data;
    console.error('Culqi error:', culqiError || err.message);
    return {
      exitoso: false,
      error: culqiError?.user_message || 'Error al procesar el pago. Intenta nuevamente.',
      codigo: culqiError?.code
    };
  }
};

/**
 * Verifica el estado de un cargo existente
 */
const verificarCargo = async (chargeId) => {
  try {
    const response = await axios.get(`${CULQI_URL}/charges/${chargeId}`, {
      headers: { Authorization: `Bearer ${process.env.CULQI_SECRET_KEY}` }
    });
    return { exitoso: true, cargo: response.data };
  } catch (err) {
    return { exitoso: false, error: 'No se pudo verificar el cargo' };
  }
};

module.exports = { procesarPago, verificarCargo };
