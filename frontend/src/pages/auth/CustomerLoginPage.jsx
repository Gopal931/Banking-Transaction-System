import { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  TextField,
  Button,
  Alert,
  Divider,
  CircularProgress,
  IconButton,
  Tooltip,
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import AccountBalanceIcon from '@mui/icons-material/AccountBalance';
import RefreshIcon from '@mui/icons-material/Refresh';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorOutlinedIcon from '@mui/icons-material/ErrorOutlined';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import { getHealthStatus } from '../../services/api';

export default function CustomerLoginPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('alex.morgan@example.com');
  const [password, setPassword] = useState('password123');

  // Backend connection test state (Task 9)
  const [healthStatus, setHealthStatus] = useState(null);
  const [healthLoading, setHealthLoading] = useState(true);
  const [healthError, setHealthError] = useState(null);

  const testBackendConnection = () => {
    setHealthLoading(true);
    setHealthError(null);
    getHealthStatus()
      .then((data) => {
        setHealthStatus(data);
        setHealthError(null);
      })
      .catch((err) => {
        setHealthError(err.message || 'Unable to reach backend');
        setHealthStatus(null);
      })
      .finally(() => {
        setHealthLoading(false);
      });
  };

  useEffect(() => {
    let isMounted = true;
    getHealthStatus()
      .then((data) => {
        if (isMounted) {
          setHealthStatus(data);
          setHealthError(null);
          setHealthLoading(false);
        }
      })
      .catch((err) => {
        if (isMounted) {
          setHealthError(err.message || 'Unable to reach backend');
          setHealthStatus(null);
          setHealthLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, []);

  const handleLoginSubmit = (e) => {
    e.preventDefault();
    // UI Demonstration only - no real authentication
    navigate('/customer/dashboard');
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        bgcolor: 'background.default',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        p: 2,
      }}
    >
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 3 }}>
        <AccountBalanceIcon sx={{ fontSize: 36, color: 'primary.main' }} />
        <Typography variant="h5" sx={{ fontWeight: 700, color: 'primary.main' }}>
          NewGen Banking
        </Typography>
      </Box>

      <Card sx={{ maxWidth: 460, width: '100%', p: { xs: 2, sm: 3 } }}>
        <CardContent>
          <Box sx={{ textAlign: 'center', mb: 3 }}>
            <Typography variant="h5" sx={{ fontWeight: 700, color: 'text.primary' }}>
              Customer Login
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
              Sign in to manage your bank accounts & transactions
            </Typography>
          </Box>

          <Alert severity="info" sx={{ mb: 3 }}>
            <strong>Demo Mode:</strong> Authentication is not implemented yet.
            Click <strong>Sign In</strong> to preview the Customer Dashboard.
          </Alert>

          <Box component="form" onSubmit={handleLoginSubmit}>
            <TextField
              fullWidth
              label="Email Address"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              margin="normal"
              required
              size="small"
            />
            <TextField
              fullWidth
              label="Password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              margin="normal"
              required
              size="small"
            />

            <Button
              type="submit"
              fullWidth
              variant="contained"
              size="large"
              sx={{ mt: 3, mb: 2, py: 1.2 }}
            >
              Sign In (Demo)
            </Button>
          </Box>

          <Divider sx={{ my: 2.5 }}>
            <Typography variant="caption" color="text.secondary">
              SYSTEM STATUS
            </Typography>
          </Divider>

          {/* Backend Connection Health Test Widget (Task 9) */}
          <Box
            sx={{
              p: 2,
              borderRadius: 2,
              bgcolor: 'background.default',
              border: '1px solid rgba(0, 0, 0, 0.08)',
            }}
          >
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1 }}>
              <Typography variant="caption" sx={{ fontWeight: 700, letterSpacing: 0.5 }}>
                BACKEND CONNECTION (http://localhost:8080)
              </Typography>
              <Tooltip title="Retest backend health endpoint">
                <span>
                  <IconButton size="small" onClick={testBackendConnection} disabled={healthLoading}>
                    <RefreshIcon fontSize="small" />
                  </IconButton>
                </span>
              </Tooltip>
            </Box>

            {healthLoading && (
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <CircularProgress size={16} />
                <Typography variant="body2" color="text.secondary">
                  Testing GET /api/health...
                </Typography>
              </Box>
            )}

            {!healthLoading && healthStatus && (
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, color: 'success.main' }}>
                <CheckCircleIcon fontSize="small" />
                <Typography variant="body2" sx={{ fontWeight: 600 }}>
                  Connected: {healthStatus.status} - {healthStatus.message}
                </Typography>
              </Box>
            )}

            {!healthLoading && healthError && (
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, color: 'error.main' }}>
                <ErrorOutlinedIcon fontSize="small" />
                <Typography variant="caption" sx={{ fontWeight: 500 }}>
                  Backend Offline ({healthError}). Ensure backend is started on port 8080.
                </Typography>
              </Box>
            )}
          </Box>

          <Box sx={{ mt: 3, textAlign: 'center' }}>
            <Button
              variant="text"
              size="small"
              startIcon={<AdminPanelSettingsIcon />}
              onClick={() => navigate('/admin/login')}
            >
              Go to Admin Login
            </Button>
          </Box>
        </CardContent>
      </Card>
    </Box>
  );
}
