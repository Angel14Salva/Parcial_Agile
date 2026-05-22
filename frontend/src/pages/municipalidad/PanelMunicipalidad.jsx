import { useState, useEffect } from 'react';
import Layout from '../../components/shared/Layout';
import api from '../../services/api';
import toast from 'react-hot-toast';
import { Users, FileText, CheckCircle, XCircle, DollarSign, Clock, Calendar } from 'lucide-react';

const ESTADOS_LABEL = {
  pendiente_pago: 'Pdte. pago', documentos_pendientes: 'Docs. pdte.',
  en_validacion: 'En validación', inspeccion_programada: 'Insp. programada',
  observado: 'Observado', segunda_inspeccion_programada: '2da insp.',
  aprobado: 'Aprobado', denegado: 'Denegado',
};
const ESTADOS_COLOR = {
  pendiente_pago: 'bg-gray-100 text-gray-600', documentos_pendientes: 'bg-yellow-100 text-yellow-700',
  en_validacion: 'bg-blue-100 text-blue-700', inspeccion_programada: 'bg-purple-100 text-purple-700',
  observado: 'bg-orange-100 text-orange-700', segunda_inspeccion_programada: 'bg-purple-100 text-purple-700',
  aprobado: 'bg-green-100 text-green-700', denegado: 'bg-red-100 text-red-700',
};

export default function PanelMunicipalidad() {
  const [stats, setStats] = useState(null);
  const [solicitudes, setSolicitudes] = useState([]);
  const [filtroEstado, setFiltroEstado] = useState('');
  const [inspectores, setInspectores] = useState([]);
  const [modalSol, setModalSol] = useState(null);
  const [formInsp, setFormInsp] = useState({ inspectorId: '', fechaProgramada: '', horaProgramada: '' });
  const [enviando, setEnviando] = useState(false);
  const [tabActiva, setTabActiva] = useState('dashboard');
  const [historialSupervisiones, setHistorialSupervisiones] = useState([]);

  useEffect(() => {
    if (tabActiva === 'supervisiones') {
      api.get('/supervisiones/historial').then(r => setHistorialSupervisiones(r.data)).catch(() => {});
    }
  }, [tabActiva]);

  const cargar = () => {
    api.get('/municipalidad/stats').then(r => setStats(r.data)).catch(() => {});
    api.get(`/municipalidad/solicitudes${filtroEstado ? `?estado=${filtroEstado}` : ''}`)
      .then(r => setSolicitudes(r.data)).catch(() => {});
    api.get('/municipalidad/inspectores').then(r => setInspectores(r.data)).catch(() => {});
  };

  useEffect(() => { cargar(); }, [filtroEstado]);

  const programarInspeccion = async () => {
    if (!formInsp.inspectorId || !formInsp.fechaProgramada) {
      toast.error('Selecciona inspector y fecha'); return;
    }
    setEnviando(true);
    try {
      const { data } = await api.post(`/municipalidad/solicitudes/${modalSol.id}/programar-inspeccion`, formInsp);
      toast.success(data.mensaje);
      setModalSol(null);
      setFormInsp({ inspectorId: '', fechaProgramada: '', horaProgramada: '' });
      cargar();
    } catch (err) {
      toast.error(err.response?.data?.error || 'Error al programar');
    } finally {
      setEnviando(false);
    }
  };

  const solProgramables = solicitudes.filter(s =>
    ['en_validacion', 'observado', 'pendiente_inspeccion'].includes(s.estado)
  );

  return (
    <Layout>
      <h1 className="text-2xl font-bold text-blue-900 mb-6">Panel Municipal — Trujillo</h1>

      {/* Tabs */}
      <div className="flex gap-2 mb-6 border-b border-gray-200">
        {[['dashboard', 'Dashboard'], ['solicitudes', 'Solicitudes'], ['programar', 'Programar Inspecciones'], ['supervisiones', 'Supervisiones']].map(([key, label]) => (
          <button key={key} onClick={() => setTabActiva(key)}
            className={`pb-3 px-1 text-sm font-medium border-b-2 transition-colors
              ${tabActiva === key ? 'border-blue-700 text-blue-800' : 'border-transparent text-gray-500 hover:text-gray-700'}`}>
            {label}
          </button>
        ))}
      </div>

      {/* DASHBOARD */}
      {tabActiva === 'dashboard' && stats && (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
          {[
            { label: 'Total trámites', valor: stats.total, icon: FileText, color: 'text-blue-700' },
            { label: 'En validación', valor: stats.en_validacion, icon: Clock, color: 'text-blue-600' },
            { label: 'Inspecciones prog.', valor: stats.inspecciones_programadas, icon: Calendar, color: 'text-purple-600' },
            { label: 'Con observaciones', valor: stats.observados, icon: Clock, color: 'text-orange-600' },
            { label: 'Aprobados', valor: stats.aprobados, icon: CheckCircle, color: 'text-green-600' },
            { label: 'Denegados', valor: stats.denegados, icon: XCircle, color: 'text-red-600' },
            { label: 'Ingresos totales', valor: `S/ ${Number(stats.ingresos_total).toFixed(2)}`, icon: DollarSign, color: 'text-green-700' },
            { label: 'Pdte. de pago', valor: stats.pendientes_pago, icon: Clock, color: 'text-gray-500' },
          ].map(({ label, valor, icon: Icon, color }) => (
            <div key={label} className="bg-white rounded-xl border border-gray-200 p-5 shadow-sm">
              <div className="flex items-center justify-between mb-3">
                <span className="text-xs text-gray-500 font-medium">{label}</span>
                <Icon className={`w-5 h-5 ${color}`} />
              </div>
              <p className={`text-2xl font-bold ${color}`}>{valor}</p>
            </div>
          ))}
        </div>
      )}

      {/* SOLICITUDES */}
      {tabActiva === 'solicitudes' && (
        <div className="space-y-4">
          <div className="flex gap-3 items-center">
            <select value={filtroEstado} onChange={e => setFiltroEstado(e.target.value)}
              className="px-3 py-2 border border-gray-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-blue-500">
              <option value="">Todos los estados</option>
              {Object.entries(ESTADOS_LABEL).map(([k, v]) => (
                <option key={k} value={k}>{v}</option>
              ))}
            </select>
            <span className="text-sm text-gray-500">{solicitudes.length} resultado(s)</span>
          </div>
          <div className="space-y-3">
            {solicitudes.map(sol => (
              <div key={sol.id} className="bg-white rounded-xl border border-gray-200 p-4 shadow-sm">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <span className="font-semibold text-gray-800 text-sm">{sol.razon_social}</span>
                      <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${ESTADOS_COLOR[sol.estado] || 'bg-gray-100 text-gray-600'}`}>
                        {ESTADOS_LABEL[sol.estado] || sol.estado}
                      </span>
                    </div>
                    <p className="text-xs text-gray-500">RUC: {sol.ruc} · Exp: {sol.numero_expediente}</p>
                    <p className="text-xs text-gray-500">Rep: {sol.rep_nombre} · Tel: {sol.rep_telefono || '—'}</p>
                    <p className="text-xs text-gray-400 mt-1">{new Date(sol.fecha_solicitud || sol.creado_en).toLocaleDateString('es-PE')}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* SUPERVISIONES */}
      {tabActiva === 'supervisiones' && (
        <div className="space-y-3">
          <p className="text-sm text-gray-500">Historial de supervisiones realizadas ({historialSupervisiones.length})</p>
          {historialSupervisiones.length === 0 ? (
            <div className="bg-white rounded-xl border border-gray-200 p-8 text-center text-gray-400 text-sm">No hay supervisiones registradas</div>
          ) : historialSupervisiones.map(sv => (
            <div key={sv.id} className="bg-white rounded-xl border border-gray-200 p-4 shadow-sm">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="font-semibold text-gray-800 text-sm">{sv.razon_social}</p>
                  <p className="text-xs text-gray-500">Licencia: {sv.numero_licencia} · Inspector: {sv.inspector_email}</p>
                  <p className="text-xs text-gray-400">{new Date(sv.creado_en).toLocaleDateString('es-PE')}</p>
                  {sv.observaciones && <p className="text-xs text-gray-600 mt-1">{sv.observaciones}</p>}
                  {sv.multa_aplicada && <p className="text-xs text-red-600 font-medium mt-1">Multa: S/ {sv.multa_monto}</p>}
                </div>
                <div className="flex flex-col items-end gap-1">
                  <span className={"text-xs font-medium px-2 py-0.5 rounded-full " + (sv.resultado === 'conforme' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700')}>
                    {sv.resultado === 'conforme' ? 'Conforme' : 'Infraccion'}
                  </span>
                  {sv.licencia_revocada && <span className="text-xs bg-red-200 text-red-800 px-2 py-0.5 rounded-full font-bold">Revocada</span>}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* PROGRAMAR */}
      {tabActiva === 'programar' && (
        <div className="space-y-3">
          <p className="text-sm text-gray-500">
            Solicitudes listas para programar inspección ({solProgramables.length})
          </p>
          {solProgramables.length === 0 ? (
            <div className="bg-white rounded-xl border border-gray-200 p-8 text-center text-gray-400 text-sm">
              No hay solicitudes pendientes de programar inspección
            </div>
          ) : solProgramables.map(sol => (
            <div key={sol.id} className="bg-white rounded-xl border border-blue-200 p-4 shadow-sm flex items-center justify-between gap-4">
              <div>
                <p className="font-semibold text-gray-800 text-sm">{sol.razon_social}</p>
                <p className="text-xs text-gray-500">Exp: {sol.numero_expediente} · {sol.domicilio_fiscal}</p>
                <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${ESTADOS_COLOR[sol.estado]}`}>
                  {ESTADOS_LABEL[sol.estado]}
                </span>
              </div>
              <button onClick={() => setModalSol(sol)}
                className="flex items-center gap-2 bg-blue-800 hover:bg-blue-900 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors whitespace-nowrap">
                <Calendar className="w-4 h-4" /> Programar
              </button>
            </div>
          ))}
        </div>
      )}

      {/* Modal programar inspección */}
      {modalSol && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-md p-6">
            <h3 className="text-lg font-bold text-gray-800 mb-1">Programar Inspección</h3>
            <p className="text-sm text-gray-500 mb-5">{modalSol.razon_social}</p>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Inspector *</label>
                <select value={formInsp.inspectorId}
                  onChange={e => setFormInsp(f => ({ ...f, inspectorId: e.target.value }))}
                  className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-blue-500">
                  <option value="">Seleccionar inspector</option>
                  {inspectores.map(i => (
                    <option key={i.id} value={i.id}>{i.email}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Fecha de inspección *</label>
                <input type="date" value={formInsp.fechaProgramada}
                  min={new Date().toISOString().split('T')[0]}
                  onChange={e => setFormInsp(f => ({ ...f, fechaProgramada: e.target.value }))}
                  className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Hora (opcional)</label>
                <input type="time" value={formInsp.horaProgramada}
                  onChange={e => setFormInsp(f => ({ ...f, horaProgramada: e.target.value }))}
                  className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div className="flex gap-3 pt-2">
                <button onClick={() => setModalSol(null)}
                  className="flex-1 py-2.5 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50">
                  Cancelar
                </button>
                <button onClick={programarInspeccion} disabled={enviando}
                  className="flex-1 py-2.5 bg-blue-800 hover:bg-blue-900 text-white rounded-lg text-sm font-medium disabled:opacity-60 transition-colors">
                  {enviando ? 'Guardando...' : 'Programar'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </Layout>
  );
}
