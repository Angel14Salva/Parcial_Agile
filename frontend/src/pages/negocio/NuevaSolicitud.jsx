import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Layout from '../../components/shared/Layout';
import api from '../../services/api';
import toast from 'react-hot-toast';
import { CheckCircle, AlertCircle, Upload, CreditCard } from 'lucide-react';

const PASOS = ['Datos del Negocio', 'Validación SUNAT', 'Documentos', 'Pago'];

export default function NuevaSolicitud() {
  const navigate = useNavigate();
  const [paso, setPaso] = useState(0);
  const [cargando, setCargando] = useState(false);
  const [solicitudId, setSolicitudId] = useState(null);
  const [sunatData, setSunatData] = useState(null);
  const [planoFile, setPlanoFile] = useState(null);
  const [pagoForm, setPagoForm] = useState({ numeroTarjeta: '', titular: '', mesAnio: '', cvv: '' });

  const [form, setForm] = useState({
    ruc: '', razonSocial: '', nombreComercial: '', domicilioFiscal: '',
    rubro: '', areaM2: '', repNombre: '', repDni: '', repTelefono: ''
  });

  // Paso 0: Validar RUC en SUNAT
  const validarRUC = async () => {
    if (!/^\d{11}$/.test(form.ruc)) {
      toast.error('El RUC debe tener 11 dígitos');
      return;
    }
    setCargando(true);
    try {
      const { data } = await api.post('/solicitudes/validar-ruc', { ruc: form.ruc });
      if (!data.valido) { toast.error(data.error); return; }
      setSunatData(data.datos);
      if (data.datos?.razonSocial) setForm(f => ({ ...f, razonSocial: data.datos.razonSocial }));
      if (data.datos?.domicilioFiscal) setForm(f => ({ ...f, domicilioFiscal: data.datos.domicilioFiscal }));
      setPaso(1);
      if (data.advertencia) toast(data.advertencia, { icon: '⚠️' });
      else toast.success('RUC validado en SUNAT');
    } catch (err) {
      toast.error('Error al validar RUC');
    } finally {
      setCargando(false);
    }
  };

  // Paso 1: Registrar solicitud
  const registrar = async () => {
    if (!form.razonSocial || !form.domicilioFiscal || !form.rubro || !form.repNombre || !form.repDni) {
      toast.error('Completa todos los campos obligatorios');
      return;
    }
    setCargando(true);
    try {
      const { data } = await api.post('/solicitudes/registrar', form);
      setSolicitudId(data.solicitud.id);
      setPaso(2);
      toast.success('Solicitud registrada. Sube tu plano.');
    } catch (err) {
      toast.error(err.response?.data?.error || 'Error al registrar');
    } finally {
      setCargando(false);
    }
  };

  // Paso 2: Subir plano
  const subirPlano = async () => {
    if (!planoFile) { toast.error('Selecciona el plano del local'); return; }
    setCargando(true);
    try {
      const fd = new FormData();
      fd.append('plano', planoFile);
      await api.post(`/solicitudes/${solicitudId}/subir-plano`, fd, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      setPaso(3);
      toast.success('Plano subido correctamente');
    } catch (err) {
      toast.error(err.response?.data?.error || 'Error al subir plano');
    } finally {
      setCargando(false);
    }
  };

  // Paso 3: Pago simulado
  const pagar = async () => {
    const { numeroTarjeta, titular, mesAnio, cvv } = pagoForm;
    if (!numeroTarjeta || !titular || !mesAnio || !cvv) {
      toast.error('Completa todos los datos de la tarjeta');
      return;
    }
    setCargando(true);
    try {
      await api.post(`/solicitudes/${solicitudId}/pagar`, pagoForm);
      toast.success('¡Pago realizado! Tu trámite está en proceso.');
      navigate('/mis-solicitudes');
    } catch (err) {
      toast.error(err.response?.data?.error || 'Error en el pago');
    } finally {
      setCargando(false);
    }
  };

  const f = (field) => ({
    value: form[field],
    onChange: (e) => setForm(prev => ({ ...prev, [field]: e.target.value }))
  });

  const inputClass = "w-full px-3 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none text-sm";
  const labelClass = "block text-sm font-medium text-gray-700 mb-1";

  return (
    <Layout>
      <div className="max-w-2xl mx-auto">
        <h1 className="text-2xl font-bold text-blue-900 mb-6">Nueva Solicitud de Licencia</h1>

        {/* Stepper */}
        <div className="flex items-center mb-8">
          {PASOS.map((p, i) => (
            <div key={i} className="flex items-center flex-1">
              <div className={`flex items-center justify-center w-8 h-8 rounded-full text-sm font-bold
                ${i < paso ? 'bg-green-500 text-white' : i === paso ? 'bg-blue-800 text-white' : 'bg-gray-200 text-gray-500'}`}>
                {i < paso ? <CheckCircle className="w-4 h-4" /> : i + 1}
              </div>
              <span className={`ml-2 text-xs font-medium hidden sm:block ${i === paso ? 'text-blue-800' : 'text-gray-400'}`}>
                {p}
              </span>
              {i < PASOS.length - 1 && <div className={`flex-1 h-0.5 mx-2 ${i < paso ? 'bg-green-400' : 'bg-gray-200'}`} />}
            </div>
          ))}
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">

          {/* PASO 0: RUC */}
          {paso === 0 && (
            <div className="space-y-4">
              <h2 className="text-lg font-semibold text-gray-800">Ingresa tu RUC</h2>
              <p className="text-sm text-gray-500">Verificaremos tu registro en SUNAT antes de continuar.</p>
              <div>
                <label className={labelClass}>RUC <span className="text-red-500">*</span></label>
                <input type="text" maxLength={11} placeholder="20xxxxxxxxx" className={inputClass} {...f('ruc')} />
              </div>
              <button onClick={validarRUC} disabled={cargando}
                className="w-full bg-blue-800 hover:bg-blue-900 text-white font-semibold py-3 rounded-lg transition-colors disabled:opacity-60">
                {cargando ? 'Verificando en SUNAT...' : 'Validar RUC'}
              </button>
            </div>
          )}

          {/* PASO 1: Datos del negocio */}
          {paso === 1 && (
            <div className="space-y-4">
              <h2 className="text-lg font-semibold text-gray-800">Datos del Negocio</h2>
              {sunatData?.advertencia && (
                <div className="flex gap-2 bg-yellow-50 border border-yellow-200 rounded-lg p-3 text-sm text-yellow-800">
                  <AlertCircle className="w-4 h-4 mt-0.5 shrink-0" />
                  {sunatData.advertencia}
                </div>
              )}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="sm:col-span-2">
                  <label className={labelClass}>Razón Social <span className="text-red-500">*</span></label>
                  <input type="text" className={inputClass} {...f('razonSocial')} />
                </div>
                <div className="sm:col-span-2">
                  <label className={labelClass}>Nombre Comercial</label>
                  <input type="text" className={inputClass} {...f('nombreComercial')} />
                </div>
                <div className="sm:col-span-2">
                  <label className={labelClass}>Domicilio Fiscal <span className="text-red-500">*</span></label>
                  <input type="text" className={inputClass} {...f('domicilioFiscal')} />
                </div>
                <div className="sm:col-span-2">
                  <label className={labelClass}>Giro / Rubro del Negocio <span className="text-red-500">*</span></label>
                  <input type="text" placeholder="Ej: Bodega y abarrotes" className={inputClass} {...f('rubro')} />
                </div>
                <div>
                  <label className={labelClass}>Área del local (m²)</label>
                  <input type="number" min="1" className={inputClass} {...f('areaM2')} />
                </div>
              </div>

              <hr className="my-4" />
              <h3 className="font-semibold text-gray-700 text-sm">Representante Legal</h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="sm:col-span-2">
                  <label className={labelClass}>Nombre completo <span className="text-red-500">*</span></label>
                  <input type="text" className={inputClass} {...f('repNombre')} />
                </div>
                <div>
                  <label className={labelClass}>DNI <span className="text-red-500">*</span></label>
                  <input type="text" maxLength={8} className={inputClass} {...f('repDni')} />
                </div>
                <div>
                  <label className={labelClass}>Teléfono</label>
                  <input type="tel" className={inputClass} {...f('repTelefono')} />
                </div>
              </div>

              <button onClick={registrar} disabled={cargando}
                className="w-full bg-blue-800 hover:bg-blue-900 text-white font-semibold py-3 rounded-lg transition-colors disabled:opacity-60">
                {cargando ? 'Registrando...' : 'Continuar'}
              </button>
            </div>
          )}

          {/* PASO 2: Plano */}
          {paso === 2 && (
            <div className="space-y-4">
              <h2 className="text-lg font-semibold text-gray-800">Plano del Local</h2>
              <p className="text-sm text-gray-500">Sube el plano de tu local en PDF, JPG o PNG (máx. 10 MB).</p>
              <label className="flex flex-col items-center justify-center border-2 border-dashed border-blue-300 rounded-lg p-8 cursor-pointer hover:bg-blue-50 transition-colors">
                <Upload className="w-8 h-8 text-blue-400 mb-2" />
                <span className="text-sm text-blue-700 font-medium">
                  {planoFile ? planoFile.name : 'Haz clic para seleccionar el plano'}
                </span>
                <input type="file" accept=".pdf,.jpg,.jpeg,.png" className="hidden"
                  onChange={e => setPlanoFile(e.target.files[0])} />
              </label>
              <button onClick={subirPlano} disabled={cargando || !planoFile}
                className="w-full bg-blue-800 hover:bg-blue-900 text-white font-semibold py-3 rounded-lg transition-colors disabled:opacity-60">
                {cargando ? 'Subiendo...' : 'Subir Plano y Continuar'}
              </button>
            </div>
          )}

          {/* PASO 3: Pago */}
          {paso === 3 && (
            <div className="space-y-4">
              <h2 className="text-lg font-semibold text-gray-800">Pago de Derecho de Trámite</h2>

              <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 flex justify-between items-center">
                <span className="text-sm text-blue-800">Licencia de Funcionamiento</span>
                <span className="text-xl font-bold text-blue-900">S/ 180.00</span>
              </div>

              {/* Formulario de tarjeta */}
              <div className="border border-gray-200 rounded-xl p-5 space-y-4">
                <div className="flex items-center gap-2 mb-1">
                  <CreditCard className="w-4 h-4 text-gray-500" />
                  <span className="text-sm font-medium text-gray-700">Datos de pago</span>
                </div>

                <div>
                  <label className="block text-xs font-medium text-gray-600 mb-1">Número de tarjeta</label>
                  <input
                    type="text" maxLength={19} placeholder="0000 0000 0000 0000"
                    value={pagoForm.numeroTarjeta}
                    onChange={e => {
                      const v = e.target.value.replace(/\D/g,'').slice(0,16);
                      const fmt = v.match(/.{1,4}/g)?.join(' ') || v;
                      setPagoForm(f => ({...f, numeroTarjeta: fmt}));
                    }}
                    className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm font-mono outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-600 mb-1">Titular de la tarjeta</label>
                  <input
                    type="text" placeholder="NOMBRE APELLIDO"
                    value={pagoForm.titular}
                    onChange={e => setPagoForm(f => ({...f, titular: e.target.value.toUpperCase()}))}
                    className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-medium text-gray-600 mb-1">Vencimiento (MM/AA)</label>
                    <input
                      type="text" maxLength={5} placeholder="MM/AA"
                      value={pagoForm.mesAnio}
                      onChange={e => {
                        let v = e.target.value.replace(/\D/g,'').slice(0,4);
                        if (v.length > 2) v = v.slice(0,2) + '/' + v.slice(2);
                        setPagoForm(f => ({...f, mesAnio: v}));
                      }}
                      className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm font-mono outline-none focus:ring-2 focus:ring-blue-500"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-gray-600 mb-1">CVV</label>
                    <input
                      type="password" maxLength={4} placeholder="•••"
                      value={pagoForm.cvv}
                      onChange={e => setPagoForm(f => ({...f, cvv: e.target.value.replace(/\D/g,'').slice(0,4)}))}
                      className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm font-mono outline-none focus:ring-2 focus:ring-blue-500"
                    />
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-2 text-xs text-gray-400">
                <span>🔒</span>
                <span>Pago procesado de forma segura. Tus datos no se almacenan.</span>
              </div>

              <button onClick={pagar} disabled={cargando}
                className="w-full flex items-center justify-center gap-2 bg-yellow-500 hover:bg-yellow-600 text-blue-900 font-bold py-3 rounded-lg transition-colors disabled:opacity-60">
                <CreditCard className="w-5 h-5" />
                {cargando ? 'Procesando pago...' : 'Pagar S/ 180.00'}
              </button>
            </div>
          )}
        </div>
      </div>
    </Layout>
  );
}
