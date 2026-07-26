(function () {
    const configs = {
        employee: {
            summaryUrl: '/api/notifications/summary',
            readUrl: id => '/api/notifications/' + id + '/read',
            listUrl: '/notifications'
        },
        customer: {
            summaryUrl: '/api/notifications/customer/summary',
            readUrl: id => '/api/notifications/customer/' + id + '/read',
            listUrl: '/customer/portal/notifications'
        }
    };

    let activeConfig = null;
    let stompClient = null;
    let realtimeAllowed = false;

    function init(type) {
        activeConfig = configs[type];
        if (!activeConfig) return;

        refresh(true);
    }

    function refresh(startRealtime) {
        if (!activeConfig) return Promise.resolve();

        return fetch(activeConfig.summaryUrl)
            .then(response => response.ok ? response.json() : Promise.reject(response))
            .then(data => {
                renderSummary(data);
                if (startRealtime) {
                    realtimeAllowed = true;
                    connectRealtime();
                }
            })
            .catch(error => {
                realtimeAllowed = false;
                if (!error || error.status !== 401) {
                    console.error('Lỗi tải thông báo:', error);
                }
            });
    }

    function renderSummary(data) {
        const badge = document.getElementById('bell-badge-count');
        const txtUnread = document.getElementById('quick-unread-txt');
        const container = document.getElementById('bell-items-container');
        if (!badge || !txtUnread || !container) return;

        const unreadCount = Number(data.unreadCount || 0);
        if (unreadCount > 0) {
            badge.textContent = unreadCount > 99 ? '99+' : unreadCount;
            badge.classList.remove('d-none');
            txtUnread.textContent = unreadCount + ' chưa đọc';
        } else {
            badge.classList.add('d-none');
            txtUnread.textContent = '0 chưa đọc';
        }

        const notifications = Array.isArray(data.notifications) ? data.notifications : [];
        if (notifications.length === 0) {
            container.innerHTML = '<li><div class="notification-empty"><i class="far fa-bell-slash me-1"></i>Không có thông báo mới</div></li>';
            return;
        }

        container.innerHTML = '';
        notifications.forEach(item => container.appendChild(buildDropdownItem(item)));
    }

    function buildDropdownItem(item) {
        const li = document.createElement('li');
        li.className = 'notification-item' + (!item.isRead ? ' is-unread' : '');
        li.setAttribute('role', 'button');
        li.tabIndex = 0;
        li.addEventListener('click', () => markAndOpen(item.id));
        li.addEventListener('keydown', event => {
            if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                markAndOpen(item.id);
            }
        });

        const title = document.createElement('div');
        title.className = 'notification-item-title text-truncate';

        const dot = document.createElement('span');
        dot.className = 'notification-dot';
        title.appendChild(dot);
        title.appendChild(document.createTextNode(item.title || 'Thông báo'));

        const message = document.createElement('div');
        message.className = 'notification-message text-truncate';
        message.textContent = item.message || '';

        li.appendChild(title);
        li.appendChild(message);
        return li;
    }

    function markAndOpen(id) {
        if (!activeConfig || !id) return;

        fetch(activeConfig.readUrl(id), { method: 'POST' })
            .then(() => {
                window.location.href = activeConfig.listUrl;
            })
            .catch(error => console.error('Lỗi xử lý đọc thông báo:', error));
    }

    function markInPage(id, rowId) {
        if (!activeConfig || !id) return;

        fetch(activeConfig.readUrl(id), { method: 'POST' })
            .then(response => {
                if (!response.ok) return;
                const row = document.getElementById(rowId || ('noti-row-' + id));
                if (row) {
                    row.classList.remove('unread', 'is-unread');
                    const action = row.querySelector('[data-notification-read-action]');
                    if (action) action.remove();
                    const badge = row.querySelector('.badge');
                    if (badge) badge.remove();
                    const icon = row.querySelector('.noti-icon-wrapper i, .noti-icon i');
                    if (icon) icon.className = 'far fa-bell';
                }
                refresh();
            })
            .catch(error => console.error('Lỗi cập nhật trạng thái:', error));
    }

    function connectRealtime() {
        if (stompClient || !window.WebSocket || !realtimeAllowed) return;

        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const socketUrl = protocol + '//' + window.location.host + '/ws-notifications';
        stompClient = new WebSocket(socketUrl);

        stompClient.addEventListener('open', function () {
            sendFrame('CONNECT', {
                'accept-version': '1.2',
                'heart-beat': '10000,10000'
            });
        });

        stompClient.addEventListener('message', function (message) {
            parseFrames(message.data).forEach(handleFrame);
        });

        stompClient.addEventListener('close', function () {
            stompClient = null;
            if (realtimeAllowed) {
                window.setTimeout(connectRealtime, 5000);
            }
        });
    }

    function sendFrame(command, headers, body) {
        if (!stompClient || stompClient.readyState !== WebSocket.OPEN) return;

        let frame = command + '\n';
        Object.keys(headers || {}).forEach(key => {
            frame += key + ':' + headers[key] + '\n';
        });
        frame += '\n' + (body || '') + '\u0000';
        stompClient.send(frame);
    }

    function parseFrames(raw) {
        return String(raw || '')
            .split('\u0000')
            .map(frame => frame.trim())
            .filter(Boolean);
    }

    function handleFrame(frame) {
        const separatorIndex = frame.indexOf('\n\n');
        const headerBlock = separatorIndex >= 0 ? frame.slice(0, separatorIndex) : frame;
        const body = separatorIndex >= 0 ? frame.slice(separatorIndex + 2) : '';
        const command = headerBlock.split('\n')[0];

        if (command === 'CONNECTED') {
            sendFrame('SUBSCRIBE', {
                id: 'notifications-subscription',
                destination: '/user/queue/notifications'
            });
            return;
        }

        if (command !== 'MESSAGE') return;

        let event = null;
        try {
            event = JSON.parse(body);
        } catch (error) {
            console.error('Thông báo realtime không hợp lệ:', error);
        }

        refresh();
        if (event && event.eventType === 'CREATED') {
            showToast(event.title || 'Thông báo mới', event.message || 'Bạn có cập nhật mới trong hệ thống.');
        }
    }

    function showToast(title, message) {
        let stack = document.querySelector('.notification-toast-stack');
        if (!stack) {
            stack = document.createElement('div');
            stack.className = 'notification-toast-stack';
            document.body.appendChild(stack);
        }

        const toast = document.createElement('div');
        toast.className = 'notification-toast';
        toast.innerHTML = '<div class="notification-toast-icon"><i class="fas fa-bell"></i></div>' +
            '<div><strong></strong><p></p></div>';
        toast.querySelector('strong').textContent = title;
        toast.querySelector('p').textContent = message;
        stack.appendChild(toast);

        window.setTimeout(() => toast.classList.add('show'), 20);
        window.setTimeout(() => {
            toast.classList.remove('show');
            window.setTimeout(() => toast.remove(), 250);
        }, 4500);
    }

    window.AppNotifications = {
        initEmployee: () => init('employee'),
        initCustomer: () => init('customer'),
        markEmployeeInPage: id => {
            activeConfig = configs.employee;
            markInPage(id);
        },
        markCustomerInPage: id => {
            activeConfig = configs.customer;
            markInPage(id);
        },
        refresh
    };
})();


