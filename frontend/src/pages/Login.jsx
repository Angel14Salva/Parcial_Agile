import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import toast from 'react-hot-toast';
import { Building2, Lock, Mail, Eye, EyeOff } from 'lucide-react';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: '', password: '' });
  const [showPass, setShowPass] = useState(false);
  const [cargando, setCargando] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setCargando(true);
    try {
      const usuario = await login(form.email, form.password);
      toast.success(`Bienvenido/a`);
      navigate('/dashboard');
    } catch (err) {
      toast.error(err.response?.data?.error || 'Error al iniciar sesión');
    } finally {
      setCargando(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-900 via-blue-800 to-yellow-600 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md p-8">

        {/* Logo y encabezado */}
        <div className="text-center mb-8">
          <div className="flex items-center justify-center mb-3">
            <Building2 className="w-12 h-12 text-blue-800" />
          </div>
          <h1 className="text-2xl font-bold text-blue-900">Municipalidad Provincial</h1>
          <h2 className="text-xl font-bold text-blue-800">de Trujillo</h2>
          <p className="text-gray-500 text-sm mt-2">Sistema de Licencias de Funcionamiento</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Correo electrónico</label>
            <div className="relative">
              <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input
                type="email"
                required
                value={form.email}
                onChange={e => setForm({ ...form, email: e.target.value })}
                placeholder="correo@ejemplo.com"
                className="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none text-sm"
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Contraseña</label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input
                type={showPass ? 'text' : 'password'}
                required
                value={form.password}
                onChange={e => setForm({ ...form, password: e.target.value })}
                placeholder="••••••••"
                className="w-full pl-10 pr-10 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none text-sm"
              />
              <button type="button" onClick={() => setShowPass(!showPass)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600">
                {showPass ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
          </div>

          <button
            type="submit"
            disabled={cargando}
            className="w-full bg-blue-800 hover:bg-blue-900 text-white font-semibold py-3 rounded-lg transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
          >
            {cargando ? 'Ingresando...' : 'Ingresar'}
          </button>
        </form>

        <div className="mt-6 text-center">
          <p className="text-sm text-gray-500">
            ¿Eres un negocio y no tienes cuenta?{' '}
            <Link to="/registro" className="text-blue-700 hover:underline font-medium">
              Regístrate aquí
            </Link>
          </p>
        </div>

        <div className="mt-4 text-center">
          <Link to="/verificar/codigo" className="text-xs text-gray-400 hover:text-gray-600">
            Verificar una licencia emitida
          </Link>
        </div>

        {/* Pie institucional */}
        <div className="mt-8 pt-4 border-t border-gray-100 text-center">
          <p className="text-xs text-gray-400">
            Municipalidad Provincial de Trujillo<br />
            Gerencia de Desarrollo Urbano — SGAM
          </p>
        </div>
      </div>
    </div>
  );
}
