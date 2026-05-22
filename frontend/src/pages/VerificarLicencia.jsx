import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import api from '../services/api';
import { Building2, CheckCircle, XCircle, Search } from 'lucide-react';

export default function VerificarLicencia() {
  const { codigo } = useParams();
  const [licencia, setLicencia] = useState(null);
  const [error, setError] = useState(null);
  const [cargando, setCargando] = useState(false);
  const [inputCodigo, setInputCodigo] = useState(codigo !== 'codigo' ? codigo : '');

  const verificar = async (cod) => {
    if (!cod) return;
    setCargando(true);
    setError(null);
    try {
      const { data } = await api.get(`/verificar/${cod}`);
      setLicencia(data);
    } catch (err) {
      setError('Licencia no encontrada o código inválido');
      setLicencia(null);
    } finally {
      setCargando(false);
    }
  };

  useEffect(() => {
    if (codigo && codigo !== 'codigo') verificar(codigo);
  }, []);

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-900 via-blue-800 to-yellow-600 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg p-8">
        <div className="text-center mb-6">
          <Building2 className="w-10 h-10 text-blue-800 mx-auto mb-2" />
          <h1 className="text-xl font-bold text-blue-900">Verificar Licencia</h1>
          <p className="text-sm text-gray-500">Municipalidad Provincial de Trujillo</p>
        </div>

        <div className="flex gap-2 mb-6">
          <input type="text" value={inputCodigo}
            onChange={e => setInputCodigo(e.target.value.toUpperCase())}
            onKeyDown={e => e.key === 'Enter' && verificar(inputCodigo)}
            placeholder="Código de verificación"
            className="flex-1 px-3 py-2.5 border border-gray-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-blue-500 font-mono"
          />
          <button onClick={() => verificar(inputCodigo)} disabled={cargando}
            className="flex items-center gap-2 bg-blue-800 hover:bg-blue-900 text-white px-4 py-2.5 rounded-lg text-sm font-medium disabled:opacity-60">
            <Search className="w-4 h-4" />
          </button>
        </div>

        {cargando && <div className="flex justify-center py-8"><div className="animate-spin h-8 w-8 border-4 border-blue-600 border-t-transparent rounded-full" /></div>}

        {error && (
          <div className="flex items-center gap-3 bg-red-50 border border-red-200 rounded-lg p-4 text-red-700">
            <XCircle className="w-5 h-5 shrink-0" />
            <p className="text-sm">{error}</p>
          </div>
        )}

        {licencia && (
          <div className={`rounded-xl border-2 p-5 ${licencia.valida ? 'border-green-400 bg-green-50' : 'border-red-400 bg-red-50'}`}>
            <div className="flex items-center gap-2 mb-4">
              {licencia.valida
                ? <CheckCircle className="w-6 h-6 text-green-600" />
                : <XCircle className="w-6 h-6 text-red-600" />
              }
              <span className={`font-bold text-lg ${licencia.valida ? 'text-green-700' : 'text-red-700'}`}>
                {licencia.estado}
              </span>
            </div>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-500">N° Licencia</span>
                <span className="font-mono font-bold">{licencia.numeroLicencia}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Razón Social</span>
                <span className="font-medium text-right max-w-xs">{licencia.razonSocial}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">RUC</span>
                <span className="font-mono">{licencia.ruc}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Dirección</span>
                <span className="text-right max-w-xs">{licencia.domicilio}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Giro</span>
                <span>{licencia.rubro}</span>
              </div>
              <hr className="my-2" />
              <div className="flex justify-between">
                <span className="text-gray-500">Emisión</span>
                <span>{new Date(licencia.fechaEmision).toLocaleDateString('es-PE')}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Vencimiento</span>
                <span>{new Date(licencia.fechaVencimiento).toLocaleDateString('es-PE')}</span>
              </div>
            </div>
          </div>
        )}

        <div className="mt-6 text-center">
          <Link to="/login" className="text-xs text-gray-400 hover:text-gray-600">
            ← Ir al sistema
          </Link>
        </div>
      </div>
    </div>
  );
}
