import { useState, useEffect } from 'react';
import Layout from '../../components/shared/Layout';
import api from '../../services/api';
import toast from 'react-hot-toast';
import { MapPin, AlertTriangle, CheckCircle, ShieldX } from 'lucide-react';

export default function Supervisiones() {
  const [licencias, setLicencias] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [modal, setModal] = useState(null);
  const [form, setForm] = useState({ resultado: '', observaciones: '', multaAplicada: false, multaMonto: '', revocarLicencia: false });
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    api.get('/supervisiones/licencias-activas')
      .then(r => setLicencias(r.data))
      .catch(() => toast.error('Error al cargar licencias'))
      .finally(() => setCargando(false));
  }, []);

  const registrar = async () => {
    if (!form.resultado) { toast.error('Selecciona un resultado'); return; }
    if (form.resultado === 'infraccion' && !form.observaciones) { toast.error('Describe la infraccion'); return; }
    setEnviando(true);
    try {
      const { data } = await api.post('/supervisiones/' + modal.id + '/registrar', {
        ...form, multaMonto: form.multaMonto ? parseFloat(form.multaMonto) : null
      });
      toast.success(data.mensaje);
      setModal(null);
      setForm({ resultado: '', observaciones: '', multaAplicada: false, multaMonto: '', revocarLicencia: false });
      const r = await api.get('/supervisiones/licencias-activas');
      setLicencias(r.data);
    } catch (err) {
      toast.error(err.response?.data?.error || 'Error');
    } finally { setEnviando(false); }
  };

  return (
    <Layout>
      <h1 className="text-2xl font-bold text-blue-900 mb-2">Supervisiones</h1>
      <p className="text-sm text-gray-500 mb-6">Realiza inspecciones sorpresa a negocios con licencia activa</p>
      {cargando ? (
        <div className="flex justify-center py-16"><div className="animate-spin h-8 w-8 border-4 border-blue-600 border-t-transparent rounded-full"/></div>
      ) : (
        <div className="space-y-3">
          {licencias.map(lic => (
            <div key={lic.id} className="bg-white rounded-xl border border-gray-200 p-4 shadow-sm flex items-center justify-between gap-4">
              <div className="flex-1">
                <p className="font-semibold text-gray-800 text-sm">{lic.razon_social}</p>
                <p className="text-xs text-gray-500">RUC: {lic.ruc} · Licencia: {lic.numero_licencia}</p>
                <div className="flex items-center gap-1 text-xs text-gray-500 mt-0.5">
                  <MapPin className="w-3 h-3"/>{lic.domicilio_fiscal}
                </div>
                <p className="text-xs text-gray-400">Vence: {new Date(lic.fecha_vencimiento).toLocaleDateString('es-PE')}</p>
              </div>
              <button onClick={() => setModal(lic)}
                className="flex items-center gap-2 bg-blue-800 hover:bg-blue-900 text-white px-4 py-2 rounded-lg text-sm font-medium whitespace-nowrap">
                Supervisar
              </button>
            </div>
          ))}
        </div>
      )}

      {modal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-md p-6 max-h-screen overflow-y-auto">
            <h3 className="text-lg font-bold text-gray-800 mb-1">Registrar Supervision</h3>
            <p className="text-sm text-gray-500 mb-4">{modal.razon_social} — {modal.domicilio_fiscal}</p>
            <div className="grid grid-cols-2 gap-3 mb-4">
              <button onClick={() => setForm(f=>({...f, resultado:'conforme', revocarLicencia:false}))}
                className={'flex items-center justify-center gap-2 py-3 rounded-lg border-2 text-sm font-medium transition-colors ' + (form.resultado==='conforme' ? 'border-green-500 bg-green-50 text-green-700' : 'border-gray-200 text-gray-600')}>
                <CheckCircle className="w-4 h-4"/> Conforme
              </button>
              <button onClick={() => setForm(f=>({...f, resultado:'infraccion'}))}
                className={'flex items-center justify-center gap-2 py-3 rounded-lg border-2 text-sm font-medium transition-colors ' + (form.resultado==='infraccion' ? 'border-red-500 bg-red-50 text-red-700' : 'border-gray-200 text-gray-600')}>
                <AlertTriangle className="w-4 h-4"/> Infraccion
              </button>
            </div>
            {form.resultado === 'infraccion' && (
              <div className="space-y-3">
                <div>
                  <label className="block text-xs font-medium text-gray-600 mb-1">Descripcion de la infraccion</label>
                  <textarea rows={3} value={form.observaciones}
                    onChange={e => setForm(f=>({...f,observaciones:e.target.value}))}
                    placeholder="Describe detalladamente..."
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-blue-500 resize-none"/>
                </div>
                <label className="flex items-center gap-3 cursor-pointer">
                  <input type="checkbox" checked={form.multaAplicada}
                    onChange={e => setForm(f=>({...f,multaAplicada:e.target.checked}))}
                    className="w-4 h-4 accent-blue-700"/>
                  <span className="text-sm text-gray-700">Aplicar multa</span>
                </label>
                {form.multaAplicada && (
                  <div>
                    <label className="block text-xs font-medium text-gray-600 mb-1">Monto de multa (S/)</label>
                    <input type="number" min="0" value={form.multaMonto}
                      onChange={e => setForm(f=>({...f,multaMonto:e.target.value}))}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-blue-500"/>
                  </div>
                )}
                <label className="flex items-center gap-3 cursor-pointer bg-red-50 border border-red-200 rounded-lg p-3">
                  <input type="checkbox" checked={form.revocarLicencia}
                    onChange={e => setForm(f=>({...f,revocarLicencia:e.target.checked}))}
                    className="w-4 h-4 accent-red-600"/>
                  <div>
                    <p className="text-sm font-medium text-red-700 flex items-center gap-1">
                      <ShieldX className="w-4 h-4"/> Revocar licencia
                    </p>
                    <p className="text-xs text-red-500">Esta accion no se puede deshacer</p>
                  </div>
                </label>
              </div>
            )}
            <div className="flex gap-3 mt-4">
              <button onClick={() => { setModal(null); setForm({ resultado:'', observaciones:'', multaAplicada:false, multaMonto:'', revocarLicencia:false }); }}
                className="flex-1 py-2.5 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50">
                Cancelar
              </button>
              <button onClick={registrar} disabled={enviando || !form.resultado}
                className="flex-1 py-2.5 bg-blue-800 hover:bg-blue-900 text-white rounded-lg text-sm font-medium disabled:opacity-60">
                {enviando ? 'Guardando...' : 'Guardar'}
              </button>
            </div>
          </div>
        </div>
      )}
    </Layout>
  );
}
