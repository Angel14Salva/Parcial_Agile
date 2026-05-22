import { useState, useEffect } from 'react';
import Layout from '../../components/shared/Layout';
import api from '../../services/api';
import toast from 'react-hot-toast';
import { MapPin, Calendar, CheckCircle, AlertTriangle, ExternalLink } from 'lucide-react';

export default function PanelInspector() {
  const [inspecciones, setInspecciones] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [modalAbierto, setModalAbierto] = useState(null);
  const [form, setForm] = useState({ resultado: '', observaciones: '', requiereDocs: false });
  const [enviando, setEnviando] = useState(false);

  const cargar = () => {
    api.get('/inspector/mis-inspecciones')
      .then(r => setInspecciones(r.data))
      .catch(() => toast.error('Error al cargar inspecciones'))
      .finally(() => setCargando(false));
  };

  useEffect(() => { cargar(); }, []);

  const registrar = async (inspeccionId) => {
    if (!form.resultado) { toast.error('Selecciona un resultado'); return; }
    setEnviando(true);
    try {
      const { data } = await api.post(`/inspector/inspecciones/${inspeccionId}/resultado`, form);
      toast.success(data.mensaje);
      setModalAbierto(null);
      setForm({ resultado: '', observaciones: '', requiereDocs: false });
      cargar();
    } catch (err) {
      toast.error(err.response?.data?.error || 'Error al registrar');
    } finally {
      setEnviando(false);
    }
  };

  const pendientes = inspecciones.filter(i => i.resultado === 'pendiente');
  const realizadas = inspecciones.filter(i => i.resultado !== 'pendiente');

  return (
    <Layout>
      <h1 className="text-2xl font-bold text-blue-900 mb-6">Panel del Inspector</h1>

      {cargando ? (
        <div className="flex justify-center py-16">
          <div className="animate-spin h-8 w-8 border-4 border-blue-600 border-t-transparent rounded-full" />
        </div>
      ) : (
        <div className="space-y-8">
          {/* Pendientes */}
          <section>
            <h2 className="text-base font-semibold text-gray-700 mb-3">
              Inspecciones Pendientes ({pendientes.length})
            </h2>
            {pendientes.length === 0 ? (
              <div className="bg-white rounded-xl border border-gray-200 p-8 text-center text-gray-400 text-sm">
                No tienes inspecciones pendientes
              </div>
            ) : (
              <div className="space-y-3">
                {pendientes.map(insp => (
                  <div key={insp.id} className="bg-white rounded-xl border border-blue-200 p-5 shadow-sm">
                    <div className="flex items-start justify-between gap-4">
                      <div className="flex-1">
                        <div className="flex items-center gap-2 mb-1">
                          <span className="text-xs bg-purple-100 text-purple-700 font-medium px-2 py-0.5 rounded-full">
                            Visita N°{insp.numero_inspeccion}
                          </span>
                          <span className="font-semibold text-gray-800">{insp.razon_social}</span>
                        </div>
                        <p className="text-sm text-gray-500">RUC: {insp.ruc} · Exp: {insp.numero_expediente}</p>
                        <div className="flex items-center gap-1 text-sm text-gray-500 mt-1">
                          <MapPin className="w-3.5 h-3.5" />
                          {insp.domicilio_fiscal}
                        </div>
                        <div className="flex items-center gap-1 text-sm text-gray-500 mt-1">
                          <Calendar className="w-3.5 h-3.5" />
                          Fecha: {new Date(insp.fecha_programada).toLocaleDateString('es-PE')}
                          {insp.hora_programada && ` a las ${insp.hora_programada}`}
                        </div>
                        <p className="text-sm text-gray-500">Rubro: {insp.rubro} · Área: {insp.area_m2 || '—'} m²</p>
                        {insp.plano_url && (
                          <a href={insp.plano_url} target="_blank" rel="noreferrer"
                            className="inline-flex items-center gap-1 text-xs text-blue-600 hover:underline mt-1">
                            <ExternalLink className="w-3 h-3" /> Ver plano del local
                          </a>
                        )}
                      </div>
                      <button
                        onClick={() => setModalAbierto(insp)}
                        className="flex items-center gap-2 bg-blue-800 hover:bg-blue-900 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors whitespace-nowrap">
                        Registrar resultado
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>

          {/* Realizadas */}
          {realizadas.length > 0 && (
            <section>
              <h2 className="text-base font-semibold text-gray-700 mb-3">Historial ({realizadas.length})</h2>
              <div className="space-y-2">
                {realizadas.map(insp => (
                  <div key={insp.id} className="bg-white rounded-lg border border-gray-200 p-4 flex items-center justify-between">
                    <div>
                      <p className="font-medium text-gray-700 text-sm">{insp.razon_social}</p>
                      <p className="text-xs text-gray-400">Exp: {insp.numero_expediente} · Visita N°{insp.numero_inspeccion}</p>
                    </div>
                    <span className={`text-xs font-medium px-2 py-0.5 rounded-full
                      ${insp.resultado === 'conforme' ? 'bg-green-100 text-green-700' : 'bg-orange-100 text-orange-700'}`}>
                      {insp.resultado === 'conforme' ? '✓ Conforme' : '⚠ Observado'}
                    </span>
                  </div>
                ))}
              </div>
            </section>
          )}
        </div>
      )}

      {/* Modal de resultado */}
      {modalAbierto && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-md p-6">
            <h3 className="text-lg font-bold text-gray-800 mb-1">Registrar Resultado</h3>
            <p className="text-sm text-gray-500 mb-5">{modalAbierto.razon_social} — Visita N°{modalAbierto.numero_inspeccion}</p>

            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <button
                  onClick={() => setForm(f => ({ ...f, resultado: 'conforme' }))}
                  className={`flex items-center justify-center gap-2 py-3 rounded-lg border-2 text-sm font-medium transition-colors
                    ${form.resultado === 'conforme' ? 'border-green-500 bg-green-50 text-green-700' : 'border-gray-200 text-gray-600 hover:border-green-300'}`}>
                  <CheckCircle className="w-4 h-4" /> Conforme
                </button>
                <button
                  onClick={() => setForm(f => ({ ...f, resultado: 'observado' }))}
                  className={`flex items-center justify-center gap-2 py-3 rounded-lg border-2 text-sm font-medium transition-colors
                    ${form.resultado === 'observado' ? 'border-orange-500 bg-orange-50 text-orange-700' : 'border-gray-200 text-gray-600 hover:border-orange-300'}`}>
                  <AlertTriangle className="w-4 h-4" /> Con Observaciones
                </button>
              </div>

              {form.resultado === 'observado' && (
                <>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Descripción de observaciones *</label>
                    <textarea rows={4}
                      value={form.observaciones}
                      onChange={e => setForm(f => ({ ...f, observaciones: e.target.value }))}
                      placeholder="Describe detalladamente las observaciones encontradas..."
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 outline-none resize-none"
                    />
                  </div>
                  <label className="flex items-center gap-3 cursor-pointer">
                    <input type="checkbox"
                      checked={form.requiereDocs}
                      onChange={e => setForm(f => ({ ...f, requiereDocs: e.target.checked }))}
                      className="w-4 h-4 accent-blue-700"
                    />
                    <span className="text-sm text-gray-700">¿Requiere subir documentación corregida?</span>
                  </label>
                </>
              )}

              <div className="flex gap-3 pt-2">
                <button onClick={() => { setModalAbierto(null); setForm({ resultado: '', observaciones: '', requiereDocs: false }); }}
                  className="flex-1 py-2.5 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50">
                  Cancelar
                </button>
                <button onClick={() => registrar(modalAbierto.id)} disabled={enviando || !form.resultado}
                  className="flex-1 py-2.5 bg-blue-800 hover:bg-blue-900 text-white rounded-lg text-sm font-medium disabled:opacity-60 transition-colors">
                  {enviando ? 'Guardando...' : 'Guardar Resultado'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </Layout>
  );
}
