import { Box, Card, CardContent, Typography, Button, Alert } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';

/**
 * Reusable placeholder for banking modules planned for later phases.
 */
export default function PlaceholderPage({ title, description, backUrl = '/customer/dashboard' }) {
  const navigate = useNavigate();

  return (
    <Box sx={{ p: { xs: 2, sm: 3 } }}>
      <Card sx={{ p: 4, maxWidth: 800, mx: 'auto', mt: 2 }}>
        <CardContent sx={{ textAlign: 'center', py: 4 }}>
          <Typography variant="h4" gutterBottom sx={{ fontWeight: 700, color: 'primary.main' }}>
            {title}
          </Typography>
          <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
            {description}
          </Typography>

          <Alert severity="info" sx={{ mb: 4, textAlign: 'left', maxWidth: 600, mx: 'auto' }}>
            <strong>Phase 1 Foundation:</strong> This banking interface is part of our UI scaffolding.
            Real transactional operations and database processing will be integrated in subsequent phases.
          </Alert>

          <Button
            variant="contained"
            startIcon={<ArrowBackIcon />}
            onClick={() => navigate(backUrl)}
          >
            Back to Dashboard
          </Button>
        </CardContent>
      </Card>
    </Box>
  );
}
