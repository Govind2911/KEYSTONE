import { Navigate, Route, Routes } from 'react-router-dom';
import { homeFor, useAuth } from './auth';
import type { Role } from './types';
import { Spinner } from './ui';
import Layout from './Layout';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Board from './pages/Board';
import WorkOrders from './pages/WorkOrders';
import WorkOrderForm from './pages/WorkOrderForm';
import WorkOrderDetail from './pages/WorkOrderDetail';
import { CustomerDetail, Customers } from './pages/Customers';
import Parts from './pages/Parts';
import Users from './pages/Users';
import Audit from './pages/Audit';

function Guard({ roles, children }: { roles?: Role[]; children: JSX.Element }) {
  const { user } = useAuth();
  if (roles && user && !roles.includes(user.role)) return <Navigate to={homeFor(user)} replace />;
  return children;
}

export default function App() {
  const { user, ready } = useAuth();
  if (!ready) return <Spinner />;
  if (!user) return <Routes><Route path="*" element={<Login />} /></Routes>;
  const staff: Role[] = ['MANAGER', 'DISPATCHER'];
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/dashboard" element={<Guard roles={['MANAGER']}><Dashboard /></Guard>} />
        <Route path="/board" element={<Guard roles={staff}><Board /></Guard>} />
        <Route path="/work-orders" element={<WorkOrders />} />
        <Route path="/work-orders/new" element={<Guard roles={['MANAGER', 'DISPATCHER', 'CUSTOMER']}><WorkOrderForm /></Guard>} />
        <Route path="/work-orders/:id" element={<WorkOrderDetail />} />
        <Route path="/customers" element={<Guard roles={staff}><Customers /></Guard>} />
        <Route path="/customers/:id" element={<Guard roles={staff}><CustomerDetail /></Guard>} />
        <Route path="/parts" element={<Guard roles={['MANAGER']}><Parts /></Guard>} />
        <Route path="/users" element={<Guard roles={['MANAGER']}><Users /></Guard>} />
        <Route path="/audit" element={<Guard roles={['MANAGER']}><Audit /></Guard>} />
        <Route path="*" element={<Navigate to={homeFor(user)} replace />} />
      </Route>
    </Routes>
  );
}
