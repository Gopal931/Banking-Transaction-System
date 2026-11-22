import { useState } from 'react';
import {
  Box,
  AppBar,
  Toolbar,
  Typography,
  IconButton,
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Divider,
  Button,
  Chip,
  useTheme,
  useMediaQuery,
} from '@mui/material';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import MenuIcon from '@mui/icons-material/Menu';
import DashboardIcon from '@mui/icons-material/Dashboard';
import PeopleIcon from '@mui/icons-material/People';
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet';
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import AssessmentIcon from '@mui/icons-material/Assessment';
import LogoutIcon from '@mui/icons-material/Logout';
import PersonIcon from '@mui/icons-material/Person';

const DRAWER_WIDTH = 260;

const adminNavItems = [
  { label: 'Dashboard', path: '/admin/dashboard', icon: <DashboardIcon /> },
  { label: 'Customers', path: '/admin/customers', icon: <PeopleIcon /> },
  { label: 'Accounts', path: '/admin/accounts', icon: <AccountBalanceWalletIcon /> },
  { label: 'Transactions', path: '/admin/transactions', icon: <ReceiptLongIcon /> },
  { label: 'Failed Transactions', path: '/admin/failed-transactions', icon: <WarningAmberIcon /> },
  { label: 'Reports', path: '/admin/reports', icon: <AssessmentIcon /> },
];

export default function AdminLayout() {
  const [mobileOpen, setMobileOpen] = useState(false);
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const navigate = useNavigate();
  const location = useLocation();

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen);
  };

  const drawerContent = (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Box sx={{ p: 2.5, bgcolor: '#1e293b', color: '#fff' }}>
        <Typography variant="h6" sx={{ fontWeight: 700, letterSpacing: 0.5 }}>
          NewGen Banking
        </Typography>
        <Typography variant="caption" sx={{ color: '#94a3b8' }}>
          Administration Console
        </Typography>
      </Box>
      <Divider />
      <List sx={{ px: 1, py: 2, flexGrow: 1 }}>
        {adminNavItems.map((item) => {
          const isSelected = location.pathname === item.path;
          return (
            <ListItem key={item.label} disablePadding sx={{ mb: 0.5 }}>
              <ListItemButton
                selected={isSelected}
                onClick={() => {
                  navigate(item.path);
                  if (isMobile) setMobileOpen(false);
                }}
                sx={{
                  borderRadius: 1.5,
                  '&.Mui-selected': {
                    bgcolor: 'secondary.main',
                    color: '#fff',
                    '&:hover': { bgcolor: 'secondary.dark' },
                    '& .MuiListItemIcon-root': { color: '#fff' },
                  },
                }}
              >
                <ListItemIcon sx={{ minWidth: 40, color: isSelected ? '#fff' : 'text.secondary' }}>
                  {item.icon}
                </ListItemIcon>
                <ListItemText
                  primary={item.label}
                  primaryTypographyProps={{ fontSize: '0.9rem', fontWeight: isSelected ? 600 : 500 }}
                />
              </ListItemButton>
            </ListItem>
          );
        })}
      </List>
      <Divider />
      <Box sx={{ p: 2 }}>
        <Button
          fullWidth
          variant="outlined"
          size="small"
          startIcon={<PersonIcon />}
          onClick={() => navigate('/customer/login')}
          sx={{ mb: 1.5 }}
        >
          Switch to Customer Portal
        </Button>
        <Button
          fullWidth
          variant="text"
          color="inherit"
          size="small"
          startIcon={<LogoutIcon />}
          onClick={() => navigate('/admin/login')}
        >
          Logout Admin Demo
        </Button>
      </Box>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: 'background.default' }}>
      <AppBar
        position="fixed"
        elevation={0}
        sx={{
          width: { md: `calc(100% - ${DRAWER_WIDTH}px)` },
          ml: { md: `${DRAWER_WIDTH}px` },
          bgcolor: '#ffffff',
          color: 'text.primary',
          borderBottom: '1px solid rgba(0, 0, 0, 0.08)',
        }}
      >
        <Toolbar>
          <IconButton
            color="inherit"
            edge="start"
            onClick={handleDrawerToggle}
            sx={{ mr: 2, display: { md: 'none' } }}
          >
            <MenuIcon />
          </IconButton>
          <Box sx={{ flexGrow: 1, display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <Typography variant="h6" noWrap sx={{ fontWeight: 600 }}>
              Administration Console
            </Typography>
            <Chip label="Admin Prototype" size="small" color="secondary" variant="outlined" />
          </Box>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Typography variant="body2" sx={{ display: { xs: 'none', sm: 'block' }, fontWeight: 500 }}>
              System Administrator (Demo)
            </Typography>
            <IconButton color="inherit" onClick={() => navigate('/admin/login')} title="Logout">
              <LogoutIcon fontSize="small" />
            </IconButton>
          </Box>
        </Toolbar>
      </AppBar>

      <Box
        component="nav"
        sx={{ width: { md: DRAWER_WIDTH }, flexShrink: { md: 0 } }}
        aria-label="admin navigation"
      >
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={handleDrawerToggle}
          ModalProps={{ keepMounted: true }}
          sx={{
            display: { xs: 'block', md: 'none' },
            '& .MuiDrawer-paper': { boxSizing: 'border-box', width: DRAWER_WIDTH },
          }}
        >
          {drawerContent}
        </Drawer>
        <Drawer
          variant="permanent"
          sx={{
            display: { xs: 'none', md: 'block' },
            '& .MuiDrawer-paper': { boxSizing: 'border-box', width: DRAWER_WIDTH, borderRight: '1px solid rgba(0, 0, 0, 0.08)' },
          }}
          open
        >
          {drawerContent}
        </Drawer>
      </Box>

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: { xs: 2, sm: 3 },
          width: { md: `calc(100% - ${DRAWER_WIDTH}px)` },
          mt: 8,
          minHeight: 'calc(100vh - 64px)',
        }}
      >
        <Outlet />
      </Box>
    </Box>
  );
}
