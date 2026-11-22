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
  Divider,
} from '@mui/material';
import PeopleIcon from '@mui/icons-material/People';
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet';
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import StatCard from '../../components/StatCard';

// Sample static demonstration data for Admin (Task 7)
const DEMO_FAILED_TRANSACTIONS = [
  { id: 'FTX-901', customer: 'Johnathan Doe', account: '•••• •••• 4102', amount: '$1,200.00', reason: 'Insufficient Account Balance', time: '12 mins ago' },
  { id: 'FTX-902', customer: 'Sarah Connor', account: '•••• •••• 9811', amount: '$15,000.00', reason: 'Daily Transfer Limit Exceeded', time: '1 hour ago' },
  { id: 'FTX-903', customer: 'Wayne Enterprise Corp', account: '•••• •••• 5520', amount: '$4,500.00', reason: 'Invalid Routing Number', time: '3 hours ago' },
  { id: 'FTX-904', customer: 'David Martinez', account: '•••• •••• 1184', amount: '$350.00', reason: 'Suspicious Security Rule Trigger', time: '5 hours ago' },
];

export default function AdminDashboard() {
  return (
    <Box sx={{ maxWidth: 1200, mx: 'auto' }}>
      {/* Demo Notice Banner */}
      <Alert severity="warning" sx={{ mb: 3 }}>
        <strong>Admin Prototype Demo:</strong> Displaying static administrative metrics and failure logs.
        Operational banking metrics will connect to real database queries in later phases.
      </Alert>

      {/* Header */}
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" sx={{ fontWeight: 700, color: 'text.primary' }}>
          Admin Dashboard
        </Typography>
        <Typography variant="body2" color="text.secondary">
          System-wide customer accounts, transaction volume, and risk monitoring (Demo Data)
        </Typography>
      </Box>

      {/* KPI Stat Cards (Task 7) */}
      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', md: 'repeat(4, 1fr)' },
          gap: 2.5,
          mb: 4,
        }}
      >
        <StatCard
          title="Total Customers"
          value="1,428"
          subtitle="Registered banking clients"
          icon={<PeopleIcon />}
          color="#0f3a68"
        />
        <StatCard
          title="Total Accounts"
          value="2,894"
          subtitle="Checking, Savings & CDs"
          icon={<AccountBalanceWalletIcon />}
          color="#00838f"
        />
        <StatCard
          title="Total Transactions"
          value="48,390"
          subtitle="Processed this month"
          icon={<ReceiptLongIcon />}
          color="#2e7d32"
        />
        <StatCard
          title="Failed Transactions"
          value="14"
          subtitle="Flagged for admin review"
          icon={<WarningAmberIcon />}
          color="#d32f2f"
        />
      </Box>

      {/* Failed Transactions Review Table */}
      <Card>
        <CardContent sx={{ p: 3 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Box>
              <Typography variant="h6" sx={{ fontWeight: 600 }}>
                Recent Failed / Flagged Transactions (Demo Data)
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Requires manual operator inspection or exception resolution
              </Typography>
            </Box>
            <Chip label="Demo Log" size="small" color="error" variant="outlined" />
          </Box>
          <Divider sx={{ mb: 2 }} />

          <TableContainer>
            <Table size="medium">
              <TableHead>
                <TableRow sx={{ '& th': { fontWeight: 600, color: 'text.secondary' } }}>
                  <TableCell>Txn ID</TableCell>
                  <TableCell>Customer</TableCell>
                  <TableCell>Account</TableCell>
                  <TableCell align="right">Amount</TableCell>
                  <TableCell>Failure Reason</TableCell>
                  <TableCell align="right">Logged</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {DEMO_FAILED_TRANSACTIONS.map((txn) => (
                  <TableRow key={txn.id} hover>
                    <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.85rem' }}>
                      {txn.id}
                    </TableCell>
                    <TableCell sx={{ fontWeight: 500 }}>{txn.customer}</TableCell>
                    <TableCell sx={{ letterSpacing: 0.5, fontSize: '0.85rem' }}>{txn.account}</TableCell>
                    <TableCell align="right" sx={{ fontWeight: 600 }}>
                      {txn.amount}
                    </TableCell>
                    <TableCell>
                      <Chip label={txn.reason} size="small" color="error" sx={{ height: 22 }} />
                    </TableCell>
                    <TableCell align="right" sx={{ color: 'text.secondary', fontSize: '0.85rem' }}>
                      {txn.time}
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
