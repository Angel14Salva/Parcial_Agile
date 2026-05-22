import { useAuth } from '../../hooks/useAuth';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import { Building2, LogOut, FileText, PlusCircle, Home, Users, ClipboardList } from 'lucide-react';

const menuPorRol = {
  negocio: [
    { to: '/mis-solicitudes', icon: FileText, label: 'Mis solicitudes' },
    { to: '/nueva-solicitud', icon: PlusCircle, label: 'Nueva solicitud' },
    { to: '/mis-licencias', icon: FileText, label: 'Mis licencias' },
  ],
  inspector: [
    { to: '/inspector', icon: ClipboardList, label: 'Mis inspecciones' },
    { to: '/supervisiones', icon: ClipboardList, label: 'Supervisiones' },
  ],
  municipalidad: [
    { to: '/municipalidad', icon: Home, label: 'Dashboard' },
  ],
};

export default function Layout({ children }) {
  const { usuario, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const menu = menuPorRol[usuario?.rol] || [];

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      {/* Topbar */}
      <header className="bg-blue-900 text-white shadow-lg z-10">
        <div className="max-w-7xl mx-auto px-4 py-3 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Building2 className="w-7 h-7 text-yellow-400" />
            <div>
              <p className="font-bold text-sm leading-none">Municipalidad Provincial de Trujillo</p>
              <p className="text-xs text-blue-300">Sistema de Licencias de Funcionamiento</p>
            </div>
          </div>
          <div className="flex items-center gap-4">
            <span className="text-sm text-blue-200 hidden sm:block">{usuario?.email}</span>
            <span className="text-xs bg-yellow-500 text-blue-900 font-bold px-2 py-0.5 rounded capitalize">
              {usuario?.rol}
            </span>
            <button onClick={handleLogout}
              className="flex items-center gap-1 text-sm text-blue-300 hover:text-white transition-colors">
              <LogOut className="w-4 h-4" />
              <span className="hidden sm:block">Salir</span>
            </button>
          </div>
        </div>
      </header>

      <div className="flex flex-1">
        {/* Sidebar */}
        <aside className="w-56 bg-white border-r border-gray-200 shadow-sm hidden md:block">
          <nav className="p-4 space-y-1">
            {menu.map(({ to, icon: Icon, label }) => (
              <Link key={to} to={to}
                className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors
                  ${location.pathname === to
                    ? 'bg-blue-50 text-blue-800'
                    : 'text-gray-600 hover:bg-gray-50 hover:text-blue-800'
                  }`}>
                <Icon className="w-4 h-4" />
                {label}
              </Link>
            ))}
          </nav>
        </aside>

        {/* Content */}
        <main className="flex-1 p-6 max-w-5xl w-full mx-auto">
          {children}
        </main>
      </div>

      <footer className="bg-white border-t border-gray-200 py-3 text-center text-xs text-gray-400">
        Municipalidad Provincial de Trujillo — Sistema de Licencias de Funcionamiento © {new Date().getFullYear()}
      </footer>
    </div>
  );
}
