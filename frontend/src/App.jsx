import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider, useAuth } from './hooks/useAuth';

// Pages
import Login from './pages/Login';
import Registro from './pages/Registro';
import Dashboard from './pages/Dashboard';
import NuevaSolicitud from './pages/negocio/NuevaSolicitud';
import MisSolicitudes from './pages/negocio/MisSolicitudes';
import PanelInspector from './pages/inspector/PanelInspector';
import PanelMunicipalidad from './pages/municipalidad/PanelMunicipalidad';
import VerificarLicencia from './pages/VerificarLicencia';

const ProtectedRoute = ({ children, roles }) => {
  const { usuario, cargando } = useAuth();
  if (cargando) return <div className="flex items-center justify-center min-h-screen"><div className="animate-spin h-8 w-8 border-4 border-blue-600 border-t-transparent rounded-full"/></div>;
  if (!usuario) return <Navigate to="/login" replace />;
  if (roles && !roles.includes(usuario.rol)) return <Navigate to="/dashboard" replace />;
  return children;
};

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Toaster position="top-right" toastOptions={{ duration: 4000 }} />
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/registro" element={<Registro />} />
          <Route path="/verificar/:codigo" element={<VerificarLicencia />} />

          <Route path="/dashboard" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />

          {/* Negocio */}
          <Route path="/nueva-solicitud" element={<ProtectedRoute roles={['negocio']}><NuevaSolicitud /></ProtectedRoute>} />
          <Route path="/mis-solicitudes" element={<ProtectedRoute roles={['negocio']}><MisSolicitudes /></ProtectedRoute>} />

          {/* Inspector */}
          <Route path="/inspector" element={<ProtectedRoute roles={['inspector']}><PanelInspector /></ProtectedRoute>} />

          {/* Municipalidad */}
          <Route path="/municipalidad" element={<ProtectedRoute roles={['municipalidad']}><PanelMunicipalidad /></ProtectedRoute>} />

          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
