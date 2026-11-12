import AppRoutes from './routes/AppRoutes';
import SesionProvider from './features/sesion/SesionProvider';

export default function App() {
  return (
    <SesionProvider>
      <AppRoutes />
    </SesionProvider>
  );
}
