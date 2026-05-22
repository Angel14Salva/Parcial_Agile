import { createContext, useContext, useState, useEffect } from 'react';
import api from '../services/api';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [usuario, setUsuario] = useState(null);
  const [cargando, setCargando] = useState(true);

  useEffect(() => {
    const stored = localStorage.getItem('usuario');
    const token = localStorage.getItem('token');
    if (stored && token) {
      // Verificar si el token sigue válido con el backend
      fetch((import.meta.env.VITE_API_URL || '/api') + '/auth/perfil', {
        headers: { Authorization: 'Bearer ' + token }
      })
        .then(res => {
          if (res.ok) {
            setUsuario(JSON.parse(stored));
          } else {
            localStorage.removeItem('token');
            localStorage.removeItem('usuario');
          }
          setCargando(false);
        })
        .catch(() => {
          // Si falla la red, mantener sesión para no bloquear al usuario
          setUsuario(JSON.parse(stored));
          setCargando(false);
        });
    } else {
      setCargando(false);
    }
  }, []);

  const login = async (email, password) => {
    const { data } = await api.post('/auth/login', { email, password });
    localStorage.setItem('token', data.token);
    localStorage.setItem('usuario', JSON.stringify(data.usuario));
    setUsuario(data.usuario);
    return data.usuario;
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('usuario');
    setUsuario(null);
  };

  return (
    <AuthContext.Provider value={{ usuario, login, logout, cargando }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
