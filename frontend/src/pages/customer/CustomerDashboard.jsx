import {
  Box,
  Typography,
  Card,
  CardContent,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  Alert,
  Paper,
  Divider,
} from '@mui/material';
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet';
import AccountBalanceIcon from '@mui/icons-material/AccountBalance';
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward';
import StatCard from '../../components/StatCard';

// Sample static demonstration data (Phase 1)
const DEMO_ACCOUNTS = [
  { id: 1, name: 'Standard Checking Account', number: '•••• •••• 8921', balance: '$12,450.25', type: 'Checking' },
  { id: 2, name: 'High-Yield Savings Account', number: '•••• •••• 3410', balance: '$35,800.50', type: 'Savings' },
];

const DEMO_TRANSACTIONS = [
  { id: 'TXN-101', date: '2026-11-21', description: 'Direct Deposit - Payroll', category: 'Salary', type: 'Credit', amount: '+$3,200.00', status: 'Completed' },
  { id: 'TXN-102', date: '2026-11-19', description: 'Metro Grocery Supermarket', category: 'Groceries', type: 'Debit', amount: '-$142.50', status: 'Completed' },
  { id: 'TXN-103', date: '2026-11-18', description: 'City Electric & Water Utility', category: 'Utilities', type: 'Debit', amount: '-$85.00', status: 'Completed' },
  { id: 'TXN-104', date: '2026-11-15', description: 'Internal Transfer to Savings', category: 'Transfer', type: 'Debit', amount: '-$500.00', status: 'Completed' },
  { id: 'TXN-105', date: '2026-11-12', description: 'Artisan Coffee Roasters', category: 'Dining', type: 'Debit', amount: '-$6.75', status: 'Completed' },
];

export default function CustomerDashboard() {
  return (
    <Box sx={{ maxWidth: 1200, mx: 'auto' }}>
      {/* Demo Notice Banner */}
      <Alert severity="info" sx={{ mb: 3 }}>
        <strong>Static Prototype Demo:</strong> Displaying sample banking data.
        Backend transactions and real account balances will be integrated in subsequent phases.
      </Alert>

      {/* Header */}
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" sx={{ fontWeight: 700, color: 'text.primary' }}>
          Customer Dashboard
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Overview of your banking balances and recent account activity (Demo Data)
        </Typography>
      </Box>

      {/* KPI Stat Cards */}
      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', md: 'repeat(3, 1fr)' },
          gap: 2.5,
          mb: 4,
        }}
      >
        <StatCard
          title="Total Balance"
          value="$48,250.75"
          subtitle="Combined checking & savings balance"
          icon={<AccountBalanceWalletIcon />}
          color="#0f3a68"
        />
        <StatCard
          title="Active Accounts"
          value="2"
          subtitle="1 Checking, 1 Savings"
          icon={<AccountBalanceIcon />}
          color="#00838f"
        />
        <StatCard
          title="Monthly Inflow"
          value="+$3,200.00"
          subtitle="Deposited in last 30 days"
          icon={<ArrowDownwardIcon />}
          color="#2e7d32"
        />
      </Box>

      {/* Accounts List Section */}
      <Card sx={{ mb: 4 }}>
        <CardContent sx={{ p: 3 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6" sx={{ fontWeight: 600 }}>
              My Accounts (Demo Data)
            </Typography>
            <Chip label="2 Accounts" size="small" variant="outlined" />
          </Box>
          <Divider sx={{ mb: 2 }} />

          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' },
              gap: 2,
            }}
          >
            {DEMO_ACCOUNTS.map((acc) => (
              <Paper
                key={acc.id}
                variant="outlined"
                sx={{ p: 2.5, borderRadius: 2, bgcolor: '#f8fafc' }}
              >
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
                  <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                    {acc.name}
                  </Typography>
                  <Chip label={acc.type} size="small" color="primary" />
                </Box>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5, letterSpacing: 1 }}>
                  {acc.number}
                </Typography>
                <Typography variant="h5" sx={{ fontWeight: 700, color: 'text.primary' }}>
                  {acc.balance}
                </Typography>
              </Paper>
            ))}
          </Box>
        </CardContent>
      </Card>

      {/* Recent Transactions Table */}
      <Card>
        <CardContent sx={{ p: 3 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6" sx={{ fontWeight: 600 }}>
              Recent Transactions (Demo Data)
            </Typography>
            <Chip label="Static Mock" size="small" color="default" />
          </Box>
          <Divider sx={{ mb: 2 }} />

          <TableContainer>
            <Table size="medium">
              <TableHead>
                <TableRow sx={{ '& th': { fontWeight: 600, color: 'text.secondary' } }}>
                  <TableCell>Date</TableCell>
                  <TableCell>Description</TableCell>
                  <TableCell>Category</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell align="right">Amount</TableCell>
                  <TableCell align="center">Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {DEMO_TRANSACTIONS.map((txn) => (
                  <TableRow key={txn.id} hover>
                    <TableCell sx={{ fontSize: '0.875rem' }}>{txn.date}</TableCell>
                    <TableCell sx={{ fontWeight: 500 }}>{txn.description}</TableCell>
                    <TableCell>
                      <Chip label={txn.category} size="small" variant="outlined" />
                    </TableCell>
                    <TableCell sx={{ color: 'text.secondary', fontSize: '0.875rem' }}>{txn.type}</TableCell>
                    <TableCell
                      align="right"
                      sx={{
                        fontWeight: 600,
                        color: txn.type === 'Credit' ? 'success.main' : 'error.main',
                      }}
                    >
                      {txn.amount}
                    </TableCell>
                    <TableCell align="center">
                      <Chip label={txn.status} size="small" color="success" sx={{ height: 22 }} />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>
    </Box>
  );
}
