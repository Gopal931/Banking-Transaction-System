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
import AccountBalanceIcon from '@mui/icons-material/AccountBalance';
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong';
import SwapHorizIcon from '@mui/icons-material/SwapHoriz';
import PersonIcon from '@mui/icons-material/Person';
import LogoutIcon from '@mui/icons-material/Logout';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';

const DRAWER_WIDTH = 260;

const customerNavItems = [
  { label: 'Dashboard', path: '/customer/dashboard', icon: <DashboardIcon /> },
  { label: 'My Accounts', path: '/customer/accounts', icon: <AccountBalanceIcon /> },
  { label: 'Transactions', path: '/customer/transactions', icon: <ReceiptLongIcon /> },
  { label: 'Transfer', path: '/customer/transfer', icon: <SwapHorizIcon /> },
  { label: 'Profile', path: '/customer/profile', icon: <PersonIcon /> },
];

export default function CustomerLayout() {
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
      <Box sx={{ p: 2.5, bgcolor: 'primary.dark', color: '#fff' }}>
        <Typography variant="h6" sx={{ fontWeight: 700, letterSpacing: 0.5 }}>
          NewGen Banking
        </Typography>
        <Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.7)' }}>
          Customer Online Banking
        </Typography>
      </Box>
      <Divider />
      <List sx={{ px: 1, py: 2, flexGrow: 1 }}>
        {customerNavItems.map((item) => {
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
                    bgcolor: 'primary.main',
                    color: '#fff',
                    '&:hover': { bgcolor: 'primary.dark' },
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
          startIcon={<AdminPanelSettingsIcon />}
          onClick={() => navigate('/admin/login')}
          sx={{ mb: 1.5 }}
        >
          Switch to Admin Portal
        </Button>
        <Button
          fullWidth
          variant="text"
          color="inherit"
          size="small"
          startIcon={<LogoutIcon />}
          onClick={() => navigate('/customer/login')}
        >
          Logout Demo
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
              Customer Portal
            </Typography>
            <Chip label="UI Prototype" size="small" color="primary" variant="outlined" />
          </Box>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Typography variant="body2" sx={{ display: { xs: 'none', sm: 'block' }, fontWeight: 500 }}>
              Alex Morgan (Demo User)
            </Typography>
            <IconButton color="inherit" onClick={() => navigate('/customer/login')} title="Logout">
              <LogoutIcon fontSize="small" />
            </IconButton>
          </Box>
        </Toolbar>
      </AppBar>

      <Box
        component="nav"
        sx={{ width: { md: DRAWER_WIDTH }, flexShrink: { md: 0 } }}
        aria-label="customer navigation"
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
