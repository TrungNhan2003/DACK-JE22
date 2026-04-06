const crypto = require('crypto');
const https = require('https');

// ==========================================
// ZALOPAY CONFIGURATION
// ==========================================
const ZALOPAY_CONFIG = {
    appId: '2553',
    key1: 'PcY4iZIKFCIdgZvA6ueMcMHHUbRLYjPL',
    key2: 'kLtgPl8HHhfvMuDHPwKfgfsY4Ydm9eIz',
    appUser: 'FashionShop',
    createOrderUrl: 'https://sb-openapi.zalopay.vn/v2/create',
    queryOrderUrl: 'https://sb-openapi.zalopay.vn/v2/query',
    redirectUrl: 'http://localhost:8080/checkout/zalopay-return',
    callbackUrl: 'http://localhost:8080/checkout/zalopay-callback',
};

// ==========================================
// GENERATE ORDER ID
// Format: yyMMdd_hhmmss_appId_6randomDigits
// ==========================================
function generateOrderId() {
    const now = new Date();
    const date = now.toISOString().slice(2, 10).replace(/-/g, '');
    const time = now.toTimeString().slice(0, 8).replace(/:/g, '');
    const random = Math.floor(100000 + Math.random() * 900000);
    return `${date}_${time}_${ZALOPAY_CONFIG.appId}_${random}`;
}

// ==========================================
// HMAC SHA256 SIGNING
// ==========================================
function computeHmacSha256(data, key) {
    return crypto.createHmac('sha256', key).update(data).digest('hex');
}

// ==========================================
// HTTPS REQUEST HELPER
// ==========================================
function httpsPost(url, formData) {
    return new Promise((resolve, reject) => {
        const urlObj = new URL(url);
        const body = new URLSearchParams(formData).toString();

        const options = {
            hostname: urlObj.hostname,
            port: 443,
            path: urlObj.pathname,
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'Content-Length': Buffer.byteLength(body),
            },
        };

        const req = https.request(options, (res) => {
            let data = '';
            res.on('data', (chunk) => (data += chunk));
            res.on('end', () => {
                try {
                    resolve(JSON.parse(data));
                } catch (e) {
                    reject(new Error(`Failed to parse response: ${data}`));
                }
            });
        });

        req.on('error', reject);
        req.write(body);
        req.end();
    });
}

// ==========================================
// CREATE PAYMENT ORDER
// ==========================================
async function createPayment(userId, amount) {
    try {
        if (amount <= 0) {
            return { success: false, message: 'Số tiền phải lớn hơn 0' };
        }
        if (amount > 50000000) {
            return { success: false, message: 'Số tiền không được vượt quá 50 triệu đồng' };
        }

        const appTransId = generateOrderId();
        const embedData = JSON.stringify({ redirecturl: ZALOPAY_CONFIG.redirectUrl });
        const items = JSON.stringify([{ name: 'Thanh toán Fashion Shop', amount: amount }]);
        const appTime = Date.now(); // milliseconds

        const requestData = {
            app_id: ZALOPAY_CONFIG.appId,
            app_user: ZALOPAY_CONFIG.appUser || userId.toString(),
            app_time: appTime.toString(),
            amount: Math.round(amount).toString(),
            app_trans_id: appTransId,
            embed_data: embedData,
            item: items,
            description: `Fashion Shop - Thanh toan ${Math.round(amount).toLocaleString()} VND`,
            bank_code: 'zalopayapp',
            callback_url: ZALOPAY_CONFIG.callbackUrl,
        };

        // Create signature
        const signData =
            requestData['app_id'] + '|' +
            requestData['app_trans_id'] + '|' +
            requestData['app_user'] + '|' +
            requestData['amount'] + '|' +
            requestData['app_time'] + '|' +
            requestData['embed_data'] + '|' +
            requestData['item'];

        requestData['mac'] = computeHmacSha256(signData, ZALOPAY_CONFIG.key1);

        console.log('📡 Calling ZaloPay API...');
        console.log('🔑 app_trans_id:', appTransId);
        console.log('💰 amount:', amount);

        const response = await httpsPost(ZALOPAY_CONFIG.createOrderUrl, requestData);
        console.log('✅ ZaloPay response:', JSON.stringify(response, null, 2));

        if (response.return_code !== 1) {
            console.error('❌ ZaloPay error:', response.return_message);
            return {
                success: false,
                message: response.return_message || 'Giao dịch thất bại',
            };
        }

        return {
            success: true,
            orderUrl: response.order_url,
            zpTransId: response.zp_trans_id,
            appTransId: appTransId,
            amount: amount,
            message: 'Tạo thanh toán thành công',
        };
    } catch (error) {
        console.error('❌ ZaloPay createPayment error:', error.message);
        return {
            success: false,
            message: 'Không thể kết nối tới ZaloPay. Vui lòng thử lại sau.',
        };
    }
}

// ==========================================
// VERIFY CALLBACK MAC
// ==========================================
function verifyCallbackMac(callbackData) {
    const dataStr =
        callbackData['app_id'] + '|' +
        callbackData['app_trans_id'] + '|' +
        callbackData['app_user'] + '|' +
        callbackData['amount'] + '|' +
        callbackData['app_time'] + '|' +
        callbackData['embed_data'] + '|' +
        callbackData['item'];

    const mac = computeHmacSha256(dataStr, ZALOPAY_CONFIG.key1);

    if (callbackData['mac'] === mac) {
        console.log('✅ Callback MAC verified');
        return true;
    }

    console.error('❌ Callback MAC verification failed');
    return false;
}

// ==========================================
// PROCESS CALLBACK
// ==========================================
function processCallback(callbackData) {
    try {
        console.log('📥 ZaloPay callback received');

        // Verify MAC (callback uses key2 for MAC)
        const dataStr =
            callbackData['app_id'] + '|' +
            callbackData['app_trans_id'] + '|' +
            callbackData['amount'] + '|' +
            callbackData['zp_trans_id'] + '|' +
            callbackData['channel'];

        const mac = computeHmacSha256(dataStr, ZALOPAY_CONFIG.key2);

        if (callbackData['mac'] !== mac && callbackData['callback_mac'] !== mac) {
            console.error('❌ Callback MAC verification failed');
            return { return_code: -1, return_message: 'MAC không hợp lệ' };
        }

        const appTransId = callbackData['app_trans_id'];
        const zpTransId = callbackData['zp_trans_id'];
        const type = callbackData['type']; // 1 = success, 2 = user canceled

        console.log(`📋 app_trans_id: ${appTransId}`);
        console.log(`📋 zp_trans_id: ${zpTransId}`);
        console.log(`📋 type: ${type}`);

        // type = 1 means payment successful
        if (type === 1) {
            console.log('✅ Payment successful');
            return { return_code: 1, return_message: 'success' };
        } else {
            console.log('❌ Payment failed or canceled');
            return { return_code: 0, return_message: 'Payment failed' };
        }
    } catch (error) {
        console.error('❌ Callback processing error:', error.message);
        return { return_code: -1, return_message: error.message };
    }
}

// ==========================================
// QUERY ORDER STATUS
// ==========================================
async function queryOrderStatus(appTransId) {
    try {
        if (!appTransId) {
            return { success: false, message: 'app_trans_id is required' };
        }

        const requestData = {
            app_id: ZALOPAY_CONFIG.appId,
            app_trans_id: appTransId,
        };

        // Signature: app_id + "|" + app_trans_id + "|" + key1
        const signData = requestData['app_id'] + '|' + requestData['app_trans_id'] + '|' + ZALOPAY_CONFIG.key1;
        requestData['mac'] = computeHmacSha256(signData, ZALOPAY_CONFIG.key1);

        console.log('📡 Querying ZaloPay order:', appTransId);

        const response = await httpsPost(ZALOPAY_CONFIG.queryOrderUrl, requestData);
        console.log('📋 Query response:', JSON.stringify(response, null, 2));

        if (response.return_code !== 1) {
            return {
                success: false,
                message: response.return_message || 'Query failed',
                rawResponse: response,
            };
        }

        return {
            success: true,
            status: response.sub_status || -1,
            zpTransId: response.zp_trans_id,
            amount: response.amount,
            rawResponse: response,
        };
    } catch (error) {
        console.error('❌ Query order error:', error.message);
        return { success: false, message: error.message };
    }
}

// ==========================================
// EXPRESS.JS ROUTER SETUP
// ==========================================
function setupRoutes(app) {
    // Create payment endpoint
    app.post('/api/zalopay/create', async (req, res) => {
        try {
            const { userId, amount } = req.body;
            const result = await createPayment(userId, amount);

            if (result.success) {
                res.json({
                    success: true,
                    orderUrl: result.orderUrl,
                    appTransId: result.appTransId,
                    zpTransId: result.zpTransId,
                });
            } else {
                res.status(400).json({ success: false, message: result.message });
            }
        } catch (error) {
            res.status(500).json({ success: false, message: error.message });
        }
    });

    // Callback endpoint (ZaloPay server calls this)
    app.post('/api/zalopay/callback', (req, res) => {
        try {
            const result = processCallback(req.body);
            res.json(result);
        } catch (error) {
            res.status(400).json({ return_code: -1, return_message: error.message });
        }
    });

    // Query order status endpoint
    app.post('/api/zalopay/query', async (req, res) => {
        try {
            const { appTransId } = req.body;
            const result = await queryOrderStatus(appTransId);

            if (result.success) {
                res.json({ success: true, status: result.status, zpTransId: result.zpTransId });
            } else {
                res.status(400).json({ success: false, message: result.message });
            }
        } catch (error) {
            res.status(500).json({ success: false, message: error.message });
        }
    });
}

module.exports = {
    createPayment,
    processCallback,
    queryOrderStatus,
    generateOrderId,
    setupRoutes,
    ZALOPAY_CONFIG,
};
