import { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from './useAuth';
import toast from 'react-hot-toast';

const TIMEOUT_MS = 10 * 60 * 1000; // 10 minutos

export const useInactividad = () => {
  const { usuario, logout } = useAuth();
  const navigate = useNavigate();
  const timerRef = useRef(null);

  const resetTimer = () => {
    if (timerRef.current) clearTimeout(timerRef.current);
    timerRef.current = setTimeout(() => {
      logout();
      toast('Sesion cerrada por inactividad', { icon: 'ℹ️' });
      navigate('/login');
    }, TIMEOUT_MS);
  };

  useEffect(() => {
    if (!usuario) return;
    const eventos = ['mousedown', 'mousemove', 'keydown', 'scroll', 'touchstart', 'click'];
    eventos.forEach(e => window.addEventListener(e, resetTimer));
    resetTimer();
    return () => {
      eventos.forEach(e => window.removeEventListener(e, resetTimer));
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, [usuario]);
};
