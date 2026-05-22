import { useAuth } from '../hooks/useAuth';
import { useNavigate } from 'react-router-dom';
import { useEffect } from 'react';

export default function Dashboard() {
  const { usuario } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (!usuario) return;
    if (usuario.rol === 'negocio') navigate('/mis-solicitudes', { replace: true });
    if (usuario.rol === 'inspector') navigate('/inspector', { replace: true });
    if (usuario.rol === 'municipalidad') navigate('/municipalidad', { replace: true });
  }, [usuario]);

  return null;
}
