import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import Layout from '../../components/shared/Layout';
import api from '../../services/api';
import { FileText, PlusCircle, Download, Clock, CheckCircle, XCircle, AlertCircle, Upload } from 'lucide-react';

const ESTADOS = {
  pendiente_pago: { label: 'Pendiente de pago', color: 'bg-gray-100 text-gray-700', icon: Clock },
  documentos_pendientes: { label: 'Subir documentos', color: 'bg-yellow-100 text-yellow-700', icon: AlertCircle },
  en_validacion: { label: 'En validación', color: 'bg-blue-100 text-blue-700', icon: Clock },
  pendiente_inspeccion: { label: 'Pendiente de inspección', color: 'bg-blue-100 text-blue-700', icon: Clock },
  inspeccion_programada: { label: 'Inspección programada', color: 'bg-purple-100 text-purple-700', icon: Clock },
  observado: { label: 'Con observaciones', color: 'bg-orange-100 text-orange-700', icon: AlertCircle },
  segunda_inspeccion_programada: { label: '2da inspección programada', color: 'bg-purple-100 text-purple-700', icon: Clock },
  aprobado: { label: 'Aprobado ✓', color: 'bg-green-100 text-green-700', icon: CheckCircle },
  denegado: { label: 'Denegado', color: 'bg-red-100 text-red-700', icon: XCircle },
};

export default function MisSolicitudes() {
  const [solicitudes, setSolicitudes] = useState([]);
  const [subiendoDocs, setSubiendoDocs] = useState(null);
  const [archivoCorregido, setArchivoCorregido] = useState(null);
  const [cargando, setCargando] = useState(true);

  useEffect(() => {
    api.get('/solicitudes/mis-solicitudes')
      .then(r => setSolicitudes(r.data))
      .catch(() => {})
      .finally(() => setCargando(false));
  }, []);

  const subirDocsCorregidos = async (solicitudId) => {
    if (!archivoCorregido) { toast.error('Selecciona un archivo'); return; }
    const fd = new FormData();
    fd.append('plano', archivoCorregido);
    try {
      await api.post('/solicitudes/' + solicitudId + '/subir-docs-corregidos', fd, { headers: { 'Content-Type': 'multipart/form-data' } });
      toast.success('Documentos corregidos enviados correctamente');
      setSubiendoDocs(null);
      setArchivoCorregido(null);
      const r = await api.get('/solicitudes/mis-solicitudes');
      setSolicitudes(r.data);
    } catch (err) { toast.error(err.response?.data?.error || 'Error al subir'); }
  };

  const descargarLicencia = async (id) => {
    try {
      const response = await api.get(`/licencias/${id}/descargar`, { responseType: 'blob' });
      const url = URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
      const a = document.createElement('a');
      a.href = url;
      a.download = `licencia_funcionamiento.pdf`;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      alert('Error al descargar la licencia');
    }
  };

  return (
    <Layout>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-blue-900">Mis Solicitudes</h1>
        <Link to="/nueva-solicitud"
          className="flex items-center gap-2 bg-blue-800 hover:bg-blue-900 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors">
          <PlusCircle className="w-4 h-4" />
          Nueva solicitud
        </Link>
      </div>

      {cargando ? (
        <div className="flex justify-center py-16">
          <div className="animate-spin h-8 w-8 border-4 border-blue-600 border-t-transparent rounded-full" />
        </div>
      ) : solicitudes.length === 0 ? (
        <div className="bg-white rounded-xl border border-gray-200 p-12 text-center">
          <FileText className="w-12 h-12 text-gray-300 mx-auto mb-4" />
          <p className="text-gray-500 mb-4">No tienes solicitudes registradas</p>
          <Link to="/nueva-solicitud"
            className="inline-flex items-center gap-2 bg-blue-800 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-900 transition-colors">
            <PlusCircle className="w-4 h-4" /> Iniciar trámite
          </Link>
        </div>
      ) : (
        <div className="space-y-4">
          {solicitudes.map(sol => {
            const estado = ESTADOS[sol.estado] || { label: sol.estado, color: 'bg-gray-100 text-gray-700' };
            return (
              <div key={sol.id} className="bg-white rounded-xl border border-gray-200 p-5 shadow-sm">
                <div className="flex items-start justify-between gap-4">
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-1">
                      <h3 className="font-semibold text-gray-800">{sol.razon_social}</h3>
                      <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${estado.color}`}>
                        {estado.label}
                      </span>
                    </div>
                    <p className="text-sm text-gray-500">RUC: {sol.ruc} · Exp: {sol.numero_expediente}</p>
                    <p className="text-sm text-gray-500">Rubro: {sol.rubro}</p>
                    <p className="text-xs text-gray-400 mt-1">
                      Registrado: {new Date(sol.fecha_solicitud).toLocaleDateString('es-PE')}
                    </p>
                  </div>
                  {sol.estado === 'aprobado' && (
                    <button onClick={() => descargarLicencia(sol.id)}
                      className="flex items-center gap-2 bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors whitespace-nowrap">
                      <Download className="w-4 h-4" />
                      Descargar Licencia
                    </button>
                  )}
                  {sol.estado === 'pendiente_pago' && (
                    <Link to={`/nueva-solicitud`}
                      className="text-sm text-blue-700 hover:underline whitespace-nowrap">
                      Completar pago →
                    </Link>
                  )}
                </div>

                {sol.estado === 'observado' && (
                  <div className="mt-3 bg-orange-50 border border-orange-200 rounded-lg p-3 text-sm text-orange-800">
                    <p className="font-medium mb-2">Tu solicitud tiene observaciones. Realiza las correcciones y sube los documentos corregidos si aplica.</p>
                    {subiendoDocs === sol.id ? (
                      <div className="space-y-2">
                        <label className="flex items-center gap-2 cursor-pointer bg-white border border-orange-300 rounded-lg p-2">
                          <Upload className="w-4 h-4 text-orange-600"/>
                          <span className="text-xs">{archivoCorregido ? archivoCorregido.name : 'Seleccionar documento corregido'}</span>
                          <input type="file" accept=".pdf,.jpg,.jpeg,.png" className="hidden" onChange={e => setArchivoCorregido(e.target.files[0])}/>
                        </label>
                        <div className="flex gap-2">
                          <button onClick={() => subirDocsCorregidos(sol.id)} className="text-xs bg-orange-600 text-white px-3 py-1.5 rounded-lg hover:bg-orange-700">Enviar</button>
                          <button onClick={() => { setSubiendoDocs(null); setArchivoCorregido(null); }} className="text-xs border border-orange-300 px-3 py-1.5 rounded-lg">Cancelar</button>
                        </div>
                      </div>
                    ) : (
                      <button onClick={() => setSubiendoDocs(sol.id)} className="text-xs bg-orange-600 text-white px-3 py-1.5 rounded-lg hover:bg-orange-700 flex items-center gap-1">
                        <Upload className="w-3 h-3"/> Subir documentos corregidos
                      </button>
                    )}
                  </div>
                )}
                {sol.estado === 'denegado' && (
                  <div className="mt-3 bg-red-50 border border-red-200 rounded-lg p-3 text-sm text-red-800">
                    <p className="font-medium">Tu solicitud fue denegada.</p>
                    <p>Las observaciones no fueron levantadas en la segunda inspección. Puedes iniciar un nuevo trámite.</p>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </Layout>
  );
}
