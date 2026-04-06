// zalopay-test.js
// Chạy: node zalopay-test.js
// Sau đó dán kết quả vào Postman

const crypto = require('crypto');

const appId      = '2553';
const appUser    = 'FashionShop';
const key1       = 'PcY4iZIKFCIdgZvA6ueMcMHHUbRLYjPL';
const key2       = 'kLtgPl8HHhfvMuDHPwKfgfsY4Ydm9eIz';
const amount     = 50000;
const callbackUrl = 'http://localhost:8080/checkout/zalopay-callback';
const embedData  = JSON.stringify({ redirecturl: 'http://localhost:8080/checkout/zalopay-return' });
const items      = JSON.stringify([{ name: 'Thanh toán Fashion Shop', amount: amount }]);

// Generate app_trans_id: yyMMdd_hhmmss_appId_random
const now = new Date();
const date = now.toISOString().slice(2, 10).replace(/-/g, '');
const time = now.toTimeString().slice(0, 8).replace(/:/g, '');
const random = Math.floor(100000 + Math.random() * 900000);
const appTransId = `${date}_${time}_${appId}_${random}`;

const appTime = Date.now();

// ==========================================
// TEST 1: CREATE PAYMENT
// ==========================================
const createSignData =
    appId + '|' +
    appTransId + '|' +
    appUser + '|' +
    amount + '|' +
    appTime + '|' +
    embedData + '|' +
    items;

const createMac = crypto.createHmac('sha256', key1).update(createSignData).digest('hex');

console.log('\n═══════════════════════════════════════════════');
console.log(' TEST 1: CREATE PAYMENT ORDER');
console.log('═══════════════════════════════════════════════');
console.log('');
console.log('Method: POST');
console.log('URL:    https://sb-openapi.zalopay.vn/v2/create');
console.log('');
console.log('Headers:');
console.log('  Content-Type: application/x-www-form-urlencoded');
console.log('');
console.log('Body (x-www-form-urlencoded):');
console.log('');

const params = {
    app_id: appId,
    app_user: appUser,
    app_time: appTime.toString(),
    amount: amount.toString(),
    app_trans_id: appTransId,
    embed_data: embedData,
    item: items,
    description: `Fashion Shop - Thanh toan ${amount.toLocaleString()} VND`,
    bank_code: 'zalopayapp',
    callback_url: callbackUrl,
    mac: createMac,
};

// Print in Postman-friendly format
Object.entries(params).forEach(([k, v]) => {
    if (k === 'mac') {
        console.log(`  ${k} = ${v}  ← MAC signature`);
    } else {
        console.log(`  ${k} = ${v}`);
    }
});

// Also save for later use
console.log('\n═══════════════════════════════════════════════');
console.log(' SAVE THESE VALUES');
console.log('═══════════════════════════════════════════════');
console.log('');
console.log('appTransId:', appTransId);
console.log('appTime:',    appTime);
console.log('mac:',        createMac);

// ==========================================
// TEST 2: QUERY ORDER STATUS
// ==========================================
const querySignData = appId + '|' + appTransId + '|' + key1;
const queryMac = crypto.createHmac('sha256', key1).update(querySignData).digest('hex');

console.log('\n═══════════════════════════════════════════════');
console.log(' TEST 2: QUERY ORDER STATUS (sau khi tạo order)');
console.log('═══════════════════════════════════════════════');
console.log('');
console.log('Method: POST');
console.log('URL:    https://sb-openapi.zalopay.vn/v2/query');
console.log('');
console.log('Headers:');
console.log('  Content-Type: application/x-www-form-urlencoded');
console.log('');
console.log('Body (x-www-form-urlencoded):');
console.log('');
console.log('  app_id       =', appId);
console.log('  app_trans_id =', appTransId);
console.log('  mac          =', queryMac, '← MAC signature');

// ==========================================
// TEST 3: SIMULATE CALLBACK
// ==========================================
const zpTransId = '123456789';
const callbackSignData =
    appId + '|' +
    appTransId + '|' +
    amount + '|' +
    zpTransId + '|' +
    '1'; // channel

const callbackMac = crypto.createHmac('sha256', key2).update(callbackSignData).digest('hex');

console.log('\n═══════════════════════════════════════════════');
console.log(' TEST 3: CALLBACK (ZaloPay gọi tới server của bạn)');
console.log('═══════════════════════════════════════════════');
console.log('');
console.log('Method: POST');
console.log('URL:    http://localhost:8080/checkout/zalopay-callback');
console.log('');
console.log('Headers:');
console.log('  Content-Type: application/x-www-form-urlencoded');
console.log('');
console.log('Body (x-www-form-urlencoded):');
console.log('');
console.log('  app_id          =', appId);
console.log('  app_trans_id    =', appTransId);
console.log('  app_user        =', appUser);
console.log('  amount          =', amount);
console.log('  app_time        =', appTime);
console.log('  embed_data      =', embedData);
console.log('  item            =', items);
console.log('  zp_trans_id     =', zpTransId);
console.log('  channel         =', '1');
console.log('  type            =', '1');  // 1 = success
console.log('  callback_mac    =', callbackMac, '← MAC (dùng key2)');
console.log('');
console.log('═══════════════════════════════════════════════');
console.log(' Done! Copy các giá trị trên vào Postman');
console.log('═══════════════════════════════════════════════\n');
