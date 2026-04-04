/**
 * Floating chat widget (Shopee-style) — polls /chat/api/messages, POST FormData to /chat/send
 */
(function () {
    const root = document.getElementById('fs-chat-root');
    if (!root) return;

    const fab = document.getElementById('fs-chat-fab');
    const panel = document.getElementById('fs-chat-panel');
    const minimize = document.getElementById('fs-chat-minimize');
    const box = document.getElementById('fs-chat-messages');
    const input = document.getElementById('fs-chat-input');
    const sendBtn = document.getElementById('fs-chat-send');

    const pollUrl = root.getAttribute('data-poll-url') || '/chat/api/messages';
    const sendUrl = root.getAttribute('data-send-url') || '/chat/send';

    function csrfToken() {
        const m = document.querySelector('meta[name="_csrf"]');
        return m ? m.getAttribute('content') : '';
    }

    function csrfParameterName() {
        const m = document.querySelector('meta[name="_csrf_parameter"]');
        return m ? m.getAttribute('content') : '_csrf';
    }

    function escapeHtml(s) {
        if (s == null) return '';
        const d = document.createElement('div');
        d.textContent = s;
        return d.innerHTML;
    }

    function render(msgs) {
        if (!box) return;
        let html = '';
        for (let i = 0; i < msgs.length; i++) {
            const m = msgs[i];
            const mine = !m.fromAdmin;
            const cls = mine ? 'fs-chat-bubble--me' : 'fs-chat-bubble--shop';
            const who = mine ? 'Bạn' : 'Fashion Shop';
            html += '<div class="fs-chat-row ' + (mine ? 'fs-chat-row--me' : 'fs-chat-row--shop') + '">';
            if (!mine) {
                html += '<span class="fs-chat-avatar fs-chat-avatar--shop" aria-hidden="true"><i class="fas fa-tshirt"></i></span>';
            }
            html += '<div class="fs-chat-bubble-wrap">';
            html += '<div class="fs-chat-bubble ' + cls + '">' + escapeHtml(m.content || '') + '</div>';
            html += '<div class="fs-chat-time">' + escapeHtml(who);
            if (m.createdAt) {
                html += ' · ' + escapeHtml(String(m.createdAt).replace('T', ' ').substring(0, 16));
            }
            html += '</div></div>';
            if (mine) {
                html += '<span class="fs-chat-avatar fs-chat-avatar--me" aria-hidden="true"><i class="fas fa-user"></i></span>';
            }
            html += '</div>';
        }
        if (msgs.length === 0) {
            html = '<p class="fs-chat-empty text-muted text-center mb-0 small">Chào bạn! Cần hỗ trợ đơn hàng hay size áo? Hãy nhắn cho shop nhé.</p>';
        }
        box.innerHTML = html;
        box.scrollTop = box.scrollHeight;
    }

    let pollTimer = null;

    function startPoll() {
        if (pollTimer) return;
        pollTimer = setInterval(fetchMessages, 3500);
    }

    function stopPoll() {
        if (pollTimer) {
            clearInterval(pollTimer);
            pollTimer = null;
        }
    }

    async function fetchMessages() {
        try {
            const r = await fetch(pollUrl, { credentials: 'same-origin' });
            if (!r.ok) return;
            const data = await r.json();
            render(data);
        } catch (e) { /* ignore */ }
    }

    function openPanel() {
        panel.hidden = false;
        root.classList.add('fs-chat-root--open');
        root.setAttribute('aria-hidden', 'false');
        fab.setAttribute('aria-expanded', 'true');
        fetchMessages();
        startPoll();
        setTimeout(function () { if (input) input.focus(); }, 100);
    }

    function closePanel() {
        panel.hidden = true;
        root.classList.remove('fs-chat-root--open');
        root.setAttribute('aria-hidden', 'true');
        fab.setAttribute('aria-expanded', 'false');
        stopPoll();
    }

    function togglePanel() {
        if (panel.hidden) openPanel();
        else closePanel();
    }

    fab.addEventListener('click', function (e) {
        e.stopPropagation();
        togglePanel();
    });
    minimize.addEventListener('click', closePanel);

    async function sendMessage() {
        const text = (input && input.value) ? input.value.trim() : '';
        if (!text) return;
        const token = csrfToken();
        const param = csrfParameterName();
        const fd = new FormData();
        fd.append('content', text);
        if (token) fd.append(param, token);
        try {
            sendBtn.disabled = true;
            const r = await fetch(sendUrl, {
                method: 'POST',
                body: fd,
                credentials: 'same-origin',
                headers: { 'X-Requested-With': 'XMLHttpRequest' }
            });
            if (r.ok || r.redirected) {
                input.value = '';
                await fetchMessages();
            }
        } catch (e) { /* ignore */ }
        finally {
            sendBtn.disabled = false;
        }
    }

    sendBtn.addEventListener('click', sendMessage);
    if (input) {
        input.addEventListener('keydown', function (e) {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                sendMessage();
            }
        });
    }

    document.querySelectorAll('.fs-chat-open-trigger').forEach(function (el) {
        el.addEventListener('click', function (e) {
            e.preventDefault();
            openPanel();
        });
    });
})();
