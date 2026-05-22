import { useState, useEffect } from 'react';
import Layout from '../../components/shared/Layout';
import api from '../../services/api';
import toast from 'react-hot-toast';
import { FileText, Download, RefreshCw, CreditCard, CheckCircle, XCircle } from 'lucide-react';

export default function MisLicencias() {
  const [licencias, setLicencias] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [modalRenovar, setModalRenovar] = useState(null);
  const [pagoForm, setPagoForm] = useState({ numeroTarjeta: '', titular: '', mesAnio: '', cvv: '' });
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    api.get('/renovaciones/mis-licencias')
      .then(r => setLicencias(r.data))
      .catch(() => toast.error('Error al cargar licencias'))
      .finally(() => setCargando(false));
  }, []);

  const descargar = async (solicitudId) => {
    try {
      const r = await api.get('/licencias/' + solicitudId + '/descargar', { responseType: 'blob' });
      const url = URL.createObjectURL(new Blob([r.data], { type: 'application/pdf' }));
      const a = document.createElement('a'); a.href = url;
      a.download = 'licencia.pdf'; a.click(); URL.revokeObjectURL(url);
    } catch { toast.error('Error al descargar'); }
  };

  const renovar = async () => {
    const { numeroTarjeta, titular, mesAnio, cvv } = pagoForm;
    if (!numeroTarjeta || !titular || !mesAnio || !cvv) { toast.error('Completa los datos de pago'); return; }
    setEnviando(true);
    try {
      const { data } = await api.post('/renovaciones/' + modalRenovar.id + '/renovar', pagoForm);
      toast.success(data.mensaje);
      setModalRenovar(null);
      setPagoForm({ numeroTarjeta: '', titular: '', mesAnio: '', cvv: '' });
      const r = await api.get('/renovaciones/mis-licencias');
      setLicencias(r.data);
    } catch (err) {
      toast.error(err.response?.data?.error || 'Error al renovar');
    } finally { setEnviando(false); }
  };

  const hoy = new Date();
  const inputClass = 'w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-blue-500';

  return (
    <Layout>
      <h1 className="text-2xl font-bold text-blue-900 mb-6">Mis Licencias</h1>
      {cargando ? (
        <div className="flex justify-center py-16"><div className="animate-spin h-8 w-8 border-4 border-blue-600 border-t-transparent rounded-full"/></div>
      ) : licencias.length === 0 ? (
        <div className="bg-white rounded-xl border border-gray-200 p-12 text-center text-gray-400">
          <FileText className="w-12 h-12 mx-auto mb-4 text-gray-300"/>
          <p>No tienes licencias emitidas aún</p>
        </div>
      ) : (
        <div className="space-y-4">
          {licencias.map(lic => {
            const vencida = new Date(lic.fecha_vencimiento) < hoy;
            const porVencer = !vencida && (new Date(lic.fecha_vencimiento) - hoy) < 30 * 24 * 60 * 60 * 1000;
            return (
              <div key={lic.id} className="bg-white rounded-xl border border-gray-200 p-5 shadow-sm">
                <div className="flex items-start justify-between gap-4">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="font-semibold text-gray-800">{lic.razon_social}</span>
                      {lic.activa && !vencida ? (
                        <span className="text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded-full font-medium flex items-center gap-1">
                          <CheckCircle className="w-3 h-3"/> Vigente
                        </span>
                      ) : (
                        <span className="text-xs bg-red-100 text-red-700 px-2 py-0.5 rounded-full font-medium flex items-center gap-1">
                          <XCircle className="w-3 h-3"/> {lic.activa ? 'Vencida' : 'Revocada'}
                        </span>
                      )}
                      {porVencer && <span className="text-xs bg-yellow-100 text-yellow-700 px-2 py-0.5 rounded-full">Por vencer</span>}
                    </div>
                    <p className="text-sm text-gray-500">N° Licencia: {lic.numero_licencia} · RUC: {lic.ruc}</p>
                    <p className="text-sm text-gray-500">Vigencia: {new Date(lic.fecha_emision).toLocaleDateString('es-PE')} al {new Date(lic.fecha_vencimiento).toLocaleDateString('es-PE')}</p>
                  </div>
                  <div className="flex gap-2 flex-wrap justify-end">
                    <button onClick={() => descargar(lic.solicitud_id)}
                      className="flex items-center gap-1 bg-blue-800 hover:bg-blue-900 text-white px-3 py-2 rounded-lg text-sm font-medium">
                      <Download className="w-4 h-4"/> Descargar
                    </button>
                    {(vencida || porVencer) && lic.activa && (
                      <button onClick={() => setModalRenovar(lic)}
                        className="flex items-center gap-1 bg-yellow-500 hover:bg-yellow-600 text-blue-900 px-3 py-2 rounded-lg text-sm font-bold">
                        <RefreshCw className="w-4 h-4"/> Renovar
                      </button>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {modalRenovar && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-md p-6">
            <h3 className="text-lg font-bold text-gray-800 mb-1">Renovar Licencia</h3>
            <p className="text-sm text-gray-500 mb-4">{modalRenovar.razon_social}</p>
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 flex justify-between mb-4">
              <span className="text-sm text-blue-800">Derecho de renovación</span>
              <span className="font-bold text-blue-900">S/ 180.00</span>
            </div>
            <div className="space-y-3">
              <div>
                <label className="block text-xs font-medium text-gray-600 mb-1">Número de tarjeta</label>
                <input type="text" maxLength={19} placeholder="0000 0000 0000 0000"
                  value={pagoForm.numeroTarjeta}
                  onChange={e => { const v = e.target.value.replace(/D/g,'').slice(0,16); const fmt = v.match(/.{1,4}/g)?.join(' ')||v; setPagoForm(f=>({...f,numeroTarjeta:fmt})); }}
                  className={inputClass + ' font-mono'}/>
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-600 mb-1">Titular</label>
                <input type="text" placeholder="NOMBRE APELLIDO" value={pagoForm.titular}
                  onChange={e => setPagoForm(f=>({...f,titular:e.target.value.toUpperCase()}))}
                  className={inputClass}/>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-gray-600 mb-1">Vencimiento</label>
                  <input type="text" maxLength={5} placeholder="MM/AA" value={pagoForm.mesAnio}
                    onChange={e => { let v=e.target.value.replace(/D/g,'').slice(0,4); if(v.length>2) v=v.slice(0,2)+'/'+v.slice(2); setPagoForm(f=>({...f,mesAnio:v})); }}
                    className={inputClass + ' font-mono'}/>
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-600 mb-1">CVV</label>
                  <input type="password" maxLength={4} placeholder="•••" value={pagoForm.cvv}
                    onChange={e => setPagoForm(f=>({...f,cvv:e.target.value.replace(/D/g,'').slice(0,4)}))}
                    className={inputClass + ' font-mono'}/>
                </div>
              </div>
            </div>
            <div className="flex gap-3 mt-4">
              <button onClick={() => setModalRenovar(null)}
                className="flex-1 py-2.5 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50">
                Cancelar
              </button>
              <button onClick={renovar} disabled={enviando}
                className="flex-1 flex items-center justify-center gap-2 py-2.5 bg-yellow-500 hover:bg-yellow-600 text-blue-900 rounded-lg text-sm font-bold disabled:opacity-60">
                <CreditCard className="w-4 h-4"/> {enviando ? 'Procesando...' : 'Pagar S/ 180.00'}
              </button>
            </div>
          </div>
        </div>
      )}
    </Layout>
  );
}
