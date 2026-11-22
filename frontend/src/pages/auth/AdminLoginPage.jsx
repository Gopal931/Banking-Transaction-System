import { useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  TextField,
  Button,
  Alert,
  Divider,
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import SecurityIcon from '@mui/icons-material/Security';
import PersonIcon from '@mui/icons-material/Person';

export default function AdminLoginPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('admin@newgenbanking.com');
  const [password, setPassword] = useState('adminpass123');

  const handleLoginSubmit = (e) => {
    e.preventDefault();
    // UI Demonstration only - no real authentication
    navigate('/admin/dashboard');
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        bgcolor: '#0f172a',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        p: 2,
      }}
    >
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 3 }}>
        <SecurityIcon sx={{ fontSize: 36, color: '#38bdf8' }} />
        <Typography variant="h5" sx={{ fontWeight: 700, color: '#ffffff' }}>
          NewGen Banking
        </Typography>
      </Box>

      <Card sx={{ maxWidth: 460, width: '100%', p: { xs: 2, sm: 3 }, bgcolor: '#ffffff' }}>
        <CardContent>
          <Box sx={{ textAlign: 'center', mb: 3 }}>
            <Typography variant="h5" sx={{ fontWeight: 700, color: '#0f172a' }}>
              Admin Login
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
              System Administration & Core Operations Console
            </Typography>
          </Box>

          <Alert severity="warning" sx={{ mb: 3 }}>
            <strong>Demo Mode:</strong> Authentication is not implemented yet.
            Click <strong>Sign In as Admin</strong> to preview the Admin Dashboard.
          </Alert>

          <Box component="form" onSubmit={handleLoginSubmit}>
            <TextField
              fullWidth
              label="Admin Email"
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
              color="secondary"
              size="large"
              sx={{ mt: 3, mb: 2, py: 1.2 }}
            >
              Sign In as Admin (Demo)
            </Button>
          </Box>

          <Divider sx={{ my: 2 }} />

          <Box sx={{ textAlign: 'center' }}>
            <Button
              variant="text"
              size="small"
              startIcon={<PersonIcon />}
              onClick={() => navigate('/customer/login')}
            >
              Return to Customer Login
            </Button>
          </Box>
        </CardContent>
      </Card>
    </Box>
  );
}
