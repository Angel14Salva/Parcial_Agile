import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import api from '../services/api';
import toast from 'react-hot-toast';
import { Building2, Mail, Lock, Eye, EyeOff } from 'lucide-react';

export default function Registro() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [form, setForm] = useState({ email: '', password: '', confirmPassword: '' });
  const [showPass, setShowPass] = useState(false);
  const [cargando, setCargando] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (form.password !== form.confirmPassword) { toast.error('Las contraseñas no coinciden'); return; }
    if (form.password.length < 8) { toast.error('La contraseña debe tener al menos 8 caracteres'); return; }
    setCargando(true);
    try {
      await api.post('/auth/registro', form);
      await login(form.email, form.password);
      toast.success('Cuenta creada exitosamente');
      navigate('/nueva-solicitud');
    } catch (err) {
      toast.error(err.response?.data?.error || 'Error al registrarse');
    } finally {
      setCargando(false);
    }
  };

  const f = (field) => ({ value: form[field], onChange: (e) => setForm(p => ({ ...p, [field]: e.target.value })) });
  const inputClass = "w-full pl-10 pr-10 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none text-sm";

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-900 via-blue-800 to-yellow-600 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md p-8">
        <div className="text-center mb-8">
          <Building2 className="w-12 h-12 text-blue-800 mx-auto mb-3" />
          <h1 className="text-2xl font-bold text-blue-900">Crear cuenta</h1>
          <p className="text-gray-500 text-sm mt-1">Municipalidad Provincial de Trujillo</p>
          <p className="text-gray-400 text-xs mt-1">Para negocios que desean tramitar su licencia</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Correo electrónico</label>
            <div className="relative">
              <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input type="email" required placeholder="correo@ejemplo.com" className={inputClass} {...f('email')} />
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Contraseña</label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input type={showPass ? 'text' : 'password'} required placeholder="Mínimo 8 caracteres" className={inputClass} {...f('password')} />
              <button type="button" onClick={() => setShowPass(!showPass)} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400">
                {showPass ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Confirmar contraseña</label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input type={showPass ? 'text' : 'password'} required placeholder="Repite la contraseña" className={inputClass} {...f('confirmPassword')} />
            </div>
          </div>

          <button type="submit" disabled={cargando}
            className="w-full bg-blue-800 hover:bg-blue-900 text-white font-semibold py-3 rounded-lg transition-colors disabled:opacity-60">
            {cargando ? 'Creando cuenta...' : 'Crear cuenta'}
          </button>
        </form>

        <p className="text-center text-sm text-gray-500 mt-6">
          ¿Ya tienes cuenta?{' '}
          <Link to="/login" className="text-blue-700 hover:underline font-medium">Inicia sesión</Link>
        </p>
      </div>
    </div>
  );
}
