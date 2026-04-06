// server.js - Standalone ZaloPay test server
const express = require('express');
const { setupRoutes, createPayment, processCallback, queryOrderStatus } = require('./zalopay');

const app = express();
const PORT = 3001;

app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// CORS for local testing
app.use((req, res, next) => {
    res.header('Access-Control-Allow-Origin', '*');
    res.header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.header('Access-Control-Allow-Headers', 'Content-Type');
    if (req.method === 'OPTIONS') return res.sendStatus(200);
    next();
});

// Setup ZaloPay routes
setupRoutes(app);

// Health check
app.get('/', (req, res) => {
    res.json({
        service: 'ZaloPay Payment Service',
        status: 'running',
        endpoints: [
            'POST /api/zalopay/create',
            'POST /api/zalopay/callback',
            'POST /api/zalopay/query',
        ],
    });
});

app.listen(PORT, () => {
    console.log(`\n🚀 ZaloPay Service running on http://localhost:${PORT}`);
    console.log(`📋 Endpoints:`);
    console.log(`   POST /api/zalopay/create  - Tạo thanh toán`);
    console.log(`   POST /api/zalopay/callback - Xử lý callback từ ZaloPay`);
    console.log(`   POST /api/zalopay/query    - Truy vấn trạng thái đơn hàng\n`);
});
