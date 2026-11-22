import { Routes, Route, Navigate } from 'react-router-dom';
import CustomerLayout from '../layouts/CustomerLayout';
import AdminLayout from '../layouts/AdminLayout';
import CustomerLoginPage from '../pages/auth/CustomerLoginPage';
import AdminLoginPage from '../pages/auth/AdminLoginPage';
import CustomerDashboard from '../pages/customer/CustomerDashboard';
import AdminDashboard from '../pages/admin/AdminDashboard';
import PlaceholderPage from '../components/PlaceholderPage';

/**
 * Central routing configuration for Banking Transaction Processing System.
 */
export default function AppRoutes() {
  return (
    <Routes>
      {/* Root redirects to Customer Login */}
      <Route path="/" element={<Navigate to="/customer/login" replace />} />

      {/* Authentication Routes (UI Prototypes) */}
      <Route path="/customer/login" element={<CustomerLoginPage />} />
      <Route path="/admin/login" element={<AdminLoginPage />} />

      {/* Customer Portal Nested Routes */}
      <Route path="/customer" element={<CustomerLayout />}>
        <Route index element={<Navigate to="/customer/dashboard" replace />} />
        <Route path="dashboard" element={<CustomerDashboard />} />
        <Route
          path="accounts"
          element={
            <PlaceholderPage
              title="My Accounts"
              description="Manage multiple checking, savings, and fixed-deposit accounts."
              backUrl="/customer/dashboard"
            />
          }
        />
        <Route
          path="transactions"
          element={
            <PlaceholderPage
              title="Transactions History"
              description="Review past credit and debit transactions, filter by date, and download statements."
              backUrl="/customer/dashboard"
            />
          }
        />
        <Route
          path="transfer"
          element={
            <PlaceholderPage
              title="Fund Transfer"
              description="Send funds to registered beneficiaries, wire transfers, or schedule recurring payments."
              backUrl="/customer/dashboard"
            />
          }
        />
        <Route
          path="profile"
          element={
            <PlaceholderPage
              title="Customer Profile & Settings"
              description="Update personal contact info, view security preferences, and manage credentials."
              backUrl="/customer/dashboard"
            />
          }
        />
      </Route>

      {/* Admin Console Nested Routes */}
      <Route path="/admin" element={<AdminLayout />}>
        <Route index element={<Navigate to="/admin/dashboard" replace />} />
        <Route path="dashboard" element={<AdminDashboard />} />
        <Route
          path="customers"
          element={
            <PlaceholderPage
              title="Customer Management"
              description="Directory of all banking clients, KYC verification, and account onboarding."
              backUrl="/admin/dashboard"
            />
          }
        />
        <Route
          path="accounts"
          element={
            <PlaceholderPage
              title="Accounts Registry"
              description="System-wide account directory, balance adjustments, freeze, and close controls."
              backUrl="/admin/dashboard"
            />
          }
        />
        <Route
          path="transactions"
          element={
            <PlaceholderPage
              title="Global Transaction Log"
              description="Real-time transaction stream and audit logs across the financial institution."
              backUrl="/admin/dashboard"
            />
          }
        />
        <Route
          path="failed-transactions"
          element={
            <PlaceholderPage
              title="Failed & Disputed Transactions"
              description="Investigate rejected payments, insufficient balance alerts, and system exceptions."
              backUrl="/admin/dashboard"
            />
          }
        />
        <Route
          path="reports"
          element={
            <PlaceholderPage
              title="Financial Reports & Auditing"
              description="Daily settlement totals, reconciliation reports, and regulatory audit exports."
              backUrl="/admin/dashboard"
            />
          }
        />
      </Route>

      {/* Catch-all redirect */}
      <Route path="*" element={<Navigate to="/customer/login" replace />} />
    </Routes>
  );
}
