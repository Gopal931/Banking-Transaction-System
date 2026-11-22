import { createTheme } from '@mui/material/styles';

/**
 * Custom Material UI Theme for the Banking Application.
 * Professional navy & teal corporate banking color palette.
 */
const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#0f3a68',
      light: '#346396',
      dark: '#072445',
      contrastText: '#ffffff',
    },
    secondary: {
      main: '#00838f',
      light: '#3fb3be',
      dark: '#005662',
      contrastText: '#ffffff',
    },
    background: {
      default: '#f4f6f9',
      paper: '#ffffff',
    },
    text: {
      primary: '#1e293b',
      secondary: '#64748b',
    },
  },
  typography: {
    fontFamily: [
      '-apple-system',
      'BlinkMacSystemFont',
      '"Segoe UI"',
      'Roboto',
      '"Helvetica Neue"',
      'Arial',
      'sans-serif',
    ].join(','),
    h4: {
      fontWeight: 700,
    },
    h5: {
      fontWeight: 600,
    },
    h6: {
      fontWeight: 600,
    },
  },
  shape: {
    borderRadius: 8,
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          fontWeight: 600,
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: 12,
          boxShadow: '0 1px 4px 0 rgba(0, 0, 0, 0.08)',
        },
      },
    },
  },
});

export default theme;
