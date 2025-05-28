document.addEventListener('DOMContentLoaded', function() {
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });

    const popoverTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="popover"]'));
    popoverTriggerList.map(function (popoverTriggerEl) {
        return new bootstrap.Popover(popoverTriggerEl);
    });

    setTimeout(function() {
        const alerts = document.querySelectorAll('.alert-dismissible');
        alerts.forEach(function(alert) {
            const bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        });
    }, 5000);

    document.addEventListener('visibilitychange', function() {
        if (document.visibilityState === 'visible') {
            const chatContainer = document.getElementById('chat-container');
            if (chatContainer) {
                if (window.location.pathname.includes('/client/')) {
                    const chatApi = window.chatApi || {};
                    if (chatApi.checkAndUpdateStatusBasedOnAppointment) {
                        chatApi.checkAndUpdateStatusBasedOnAppointment();
                    }
                } else if (window.location.pathname.includes('/psychologist/')) {
                    const psychChatApi = window.psychChatApi || {};
                    if (psychChatApi.checkAndUpdateStatusBasedOnAppointment) {
                        psychChatApi.checkAndUpdateStatusBasedOnAppointment();
                    }
                }
            }
        }
    });

    if (document.getElementById('chat-container')) {
        if (window.location.pathname.includes('/client/')) {
            initializeChat();

            setInterval(function() {
                if (document.getElementById('chat-container') && document.visibilityState === 'visible') {
                    const chatApi = window.chatApi || {};
                    if (chatApi.checkAndUpdateStatusBasedOnAppointment) {
                        chatApi.checkAndUpdateStatusBasedOnAppointment();
                    }
                }
            }, 10000);
        } else if (window.location.pathname.includes('/psychologist/')) {
            initializePsychologistChat();

            setInterval(function() {
                if (document.getElementById('chat-container') && document.visibilityState === 'visible') {
                    const psychChatApi = window.psychChatApi || {};
                    if (psychChatApi.checkAndUpdateStatusBasedOnAppointment) {
                        psychChatApi.checkAndUpdateStatusBasedOnAppointment();
                    }
                }
            }, 10000);
        }
    }

    if (document.getElementById('appointmentForm')) {
        initializeAppointmentForm();
    }

    if (document.getElementById('journalForm')) {
        initializeJournalForm();
    }
});

function initializeChat() {
    let stompClient = null;
    const userId = document.getElementById('currentUserId').value;
    const receiverId = document.getElementById('receiverId').value;
    const chatContainer = document.getElementById('chat-container');
    const messageInput = document.getElementById('message-input');
    const sendButton = document.getElementById('send-message');

    if (window.chatInitialized) {
        return;
    }
    window.chatInitialized = true;

    function checkAndUpdateStatusBasedOnAppointment() {
        const statusIndicator = document.getElementById('connection-status');
        if (!statusIndicator || !receiverId) return;

        fetch(`/api/chat/psychologist-status?clientId=${userId}&psychologistId=${receiverId}`)
            .then(response => response.json())
            .then(status => {
                console.log("Updated client status based on appointment check:", status);
                statusIndicator.className = getStatusClass(status.status);
                statusIndicator.innerText = status.status;
            })
            .catch(error => {
                console.error('Error checking psychologist status:', error);
            });
    }

    function connect() {
        if (stompClient && stompClient.connected) {
            return;
        }

        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);

        stompClient.debug = null;

        stompClient.connect({}, function(frame) {
            console.log('Connected: ' + frame);

            const statusIndicator = document.getElementById('connection-status');
            if (statusIndicator && receiverId) {
                statusIndicator.className = 'ms-2 badge bg-warning';
                statusIndicator.innerText = 'Waiting for ' + (userId === document.getElementById('currentUserId').value ? 'psychologist' : 'client');

                if (window.location.pathname.includes('/client/')) {
                    fetch(`/api/chat/psychologist-status?clientId=${userId}&psychologistId=${receiverId}`)
                        .then(response => response.json())
                        .then(status => {
                            statusIndicator.className = getStatusClass(status.status);
                            statusIndicator.innerText = status.status;
                        })
                        .catch(error => {
                            console.error('Error checking psychologist status:', error);
                        });
                } else {
                    fetch(`/api/chat/client-status?psychologistId=${userId}&clientId=${receiverId}`)
                        .then(response => response.json())
                        .then(status => {
                            statusIndicator.className = getStatusClass(status.status);
                            statusIndicator.innerText = status.status;
                        })
                        .catch(error => {
                            console.error('Error checking client status:', error);
                        });
                }
            }

            stompClient.subscribe('/topic/public', function(message) {
                const messageBody = JSON.parse(message.body);
                if (messageBody.type === 'JOIN' || messageBody.type === 'LEAVE') {
                    showMessage(messageBody);

                    if (messageBody.senderId == receiverId) {
                        const statusIndicator = document.getElementById('connection-status');
                        if (statusIndicator) {
                            if (messageBody.type === 'JOIN') {
                                checkAndUpdateStatusBasedOnAppointment();
                            } else if (messageBody.type === 'LEAVE') {
                                statusIndicator.className = 'ms-2 badge bg-warning';
                                statusIndicator.innerText = 'Waiting for ' + (userId === document.getElementById('currentUserId').value ? 'psychologist' : 'client');
                            }
                        }
                    }
                }
            });

            stompClient.subscribe('/user/' + userId + '/queue/messages', function(message) {
                console.log('Received personal message:', message.body);
                const receivedMessage = JSON.parse(message.body);

                window.messageEncryption.decryptFromTransmission(receivedMessage)
                    .then(decryptedMessage => {
                        showMessage(decryptedMessage);
                        checkAndUpdateStatusBasedOnAppointment();
                    })
                    .catch(error => {
                        console.error('Error decrypting received message:', error);
                        showMessage({
                            ...receivedMessage,
                            content: '[Message could not be decrypted]'
                        });
                        checkAndUpdateStatusBasedOnAppointment();
                    });
            });

            stompClient.subscribe('/user/' + userId + '/queue/notifications', function(notification) {
                console.log('Received notification:', notification.body);
                showNotification(JSON.parse(notification.body));
            });

            sendJoinMessage();

            loadPreviousMessages();
        }, function(error) {
            console.log('Error connecting to WebSocket: ' + error);

            const statusIndicator = document.getElementById('connection-status');
            if (statusIndicator && receiverId) {
                statusIndicator.className = 'ms-2 badge bg-danger';
                statusIndicator.innerText = 'Disconnected';
            }

            setTimeout(connect, 5000);
        });
    }

    function sendJoinMessage() {
        if (stompClient) {
            const joinMessage = {
                type: 'JOIN',
                senderId: userId,
                senderName: document.getElementById('currentUserName').value,
                timestamp: new Date()
            };

            stompClient.send('/app/chat.join', {}, JSON.stringify(joinMessage));
        }
    }

    function sendMessage() {
        if (stompClient && messageInput.value.trim() !== '') {
            const plainContent = messageInput.value.trim();

            const chatMessage = {
                type: 'CHAT',
                senderId: userId,
                senderName: document.getElementById('currentUserName').value,
                receiverId: receiverId,
                receiverName: document.getElementById('receiverName').value,
                content: plainContent,
                timestamp: new Date()
            };

            window.messageEncryption.encryptForTransmission(chatMessage)
                .then(encryptedMessage => {
                    console.log('Sending encrypted message:', encryptedMessage);
                    stompClient.send('/app/chat.sendMessage', {}, JSON.stringify(encryptedMessage));

                    showMessage(chatMessage, true);
                    messageInput.value = '';
                })
                .catch(error => {
                    console.error('Error encrypting message:', error);
                    stompClient.send('/app/chat.sendMessage', {}, JSON.stringify(chatMessage));
                    showMessage(chatMessage, true);
                    messageInput.value = '';
                });
        }
    }

    function showMessage(message, isSent = false) {
        if (!message) return;

        if ((message.type === 'JOIN' || message.type === 'LEAVE') &&
            (receiverId || document.querySelector('input[name="psychologistId"]'))) {
            return;
        }

        const messageElement = document.createElement('div');
        messageElement.classList.add('chat-message');

        if (isSent || message.senderId == userId) {
            messageElement.classList.add('message-sent');
        } else {
            messageElement.classList.add('message-received');
        }

        const messageContent = document.createElement('div');
        messageContent.classList.add('message-content');
        messageContent.innerText = message.content;

        const messageTime = document.createElement('div');
        messageTime.classList.add('message-time', 'small', 'text-white-50');
        const timestamp = message.timestamp ? new Date(message.timestamp) : new Date();
        messageTime.innerText = timestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

        messageElement.appendChild(messageContent);
        messageElement.appendChild(messageTime);
        chatContainer.appendChild(messageElement);

        chatContainer.scrollTop = chatContainer.scrollHeight;
    }

    function showNotification(notification) {
        if (notification.senderId != receiverId) {
            const toast = document.createElement('div');
            toast.classList.add('toast', 'show');
            toast.setAttribute('role', 'alert');
            toast.setAttribute('aria-live', 'assertive');
            toast.setAttribute('aria-atomic', 'true');

            toast.innerHTML = `
                <div class="toast-header">
                    <strong class="me-auto">${notification.senderName}</strong>
                    <small>Just now</small>
                    <button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Close"></button>
                </div>
                <div class="toast-body">
                    ${notification.message}
                </div>
            `;

            const toastContainer = document.getElementById('toast-container');
            if (toastContainer) {
                toastContainer.appendChild(toast);

                setTimeout(() => {
                    toast.remove();
                }, 5000);
            }
        }
    }

    function disconnect() {
        if (stompClient) {
            const leaveMessage = {
                type: 'LEAVE',
                senderId: userId,
                senderName: document.getElementById('currentUserName').value,
                timestamp: new Date()
            };

            stompClient.send('/app/chat.leave', {}, JSON.stringify(leaveMessage));

            stompClient.disconnect();
            console.log('Disconnected from WebSocket');
        }
    }

    function loadPreviousMessages() {
        fetch(`/api/messages/conversation?user1Id=${userId}&user2Id=${receiverId}`)
            .then(response => response.json())
            .then(messages => {
                messages.sort((a, b) => new Date(a.sentAt) - new Date(b.sentAt));

                chatContainer.innerHTML = '';

                if (messages.length === 0) {
                    const noMessagesElement = document.createElement('div');
                    noMessagesElement.className = 'text-center py-4';
                    noMessagesElement.innerHTML = '<p class="text-muted">No messages yet. Start the conversation!</p>';
                    chatContainer.appendChild(noMessagesElement);
                } else {
                    processMessagesSequentially(messages, 0);
                }

                markConversationAsRead();

                chatContainer.scrollTop = chatContainer.scrollHeight;
            })
            .catch(error => {
                console.error('Error loading messages:', error);
                chatContainer.innerHTML = `
                    <div class="alert alert-danger m-3">
                        Error loading messages. Please try refreshing the page.
                    </div>
                `;
            });
    }

    function processMessagesSequentially(messages, index) {
        if (index >= messages.length) {
            return;
        }

        const message = messages[index];
        const messageData = {
            senderId: message.senderId,
            senderName: message.senderName,
            receiverId: message.receiverId,
            receiverName: message.receiverName,
            content: message.content,
            timestamp: message.sentAt,
            encrypted: message.encrypted,
            encryptionIv: message.encryptionIv,
            encryptionHmac: message.encryptionHmac
        };

        if (message.encrypted) {
            const encryptedData = {
                content: message.content,
                iv: message.encryptionIv,
                hmac: message.encryptionHmac,
                encrypted: true
            };

            window.messageEncryption.decryptMessage(encryptedData, message.senderId, message.receiverId)
                .then(decryptedContent => {
                    messageData.content = decryptedContent;
                    showMessage(messageData);
                    processMessagesSequentially(messages, index + 1);
                })
                .catch(error => {
                    console.error('Error decrypting message:', error);
                    messageData.content = '[Message could not be decrypted]';
                    showMessage(messageData);
                    processMessagesSequentially(messages, index + 1);
                });
        } else {
            showMessage(messageData);
            processMessagesSequentially(messages, index + 1);
        }
    }

    function markConversationAsRead() {
        fetch(`/api/messages/conversation/read?userId=${userId}&otherUserId=${receiverId}`, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content || ''
            }
        })
            .then(response => {
                updateUnreadCount();
            })
            .catch(error => console.error('Error marking conversation as read:', error));
    }

    function updateUnreadCount() {
        const unreadBadge = document.getElementById('unread-messages-badge');
        if (unreadBadge) {
            fetch(`/api/messages/unread/${userId}`)
                .then(response => response.json())
                .then(messages => {
                    const count = messages.length;
                    if (count > 0) {
                        unreadBadge.textContent = count;
                        unreadBadge.classList.remove('d-none');
                    } else {
                        unreadBadge.classList.add('d-none');
                    }
                })
                .catch(error => console.error('Error getting unread count:', error));
        }
    }

    if (sendButton) {
        sendButton.addEventListener('click', sendMessage);
    }

    if (messageInput) {
        messageInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                sendMessage();
                e.preventDefault();
            }
        });
    }

    connect();

    setInterval(updateUnreadCount, 30000);

    window.addEventListener('beforeunload', function() {
        disconnect();
        window.chatInitialized = false;
    });

    window.psychChatApi = {
        connect,
        disconnect,
        sendMessage,
        loadPreviousMessages,
        checkAndUpdateStatusBasedOnAppointment
    };
    
    return window.psychChatApi;
}

function initializePsychologistChat() {
    let stompClient = null;
    const userId = document.getElementById('currentUserId').value;
    const receiverId = document.getElementById('receiverId').value;
    const chatContainer = document.getElementById('chat-container');
    const messageInput = document.getElementById('message-input');
    const sendButton = document.getElementById('send-message');

    if (window.psychologistChatInitialized) {
        return;
    }
    window.psychologistChatInitialized = true;

    const viewAppointmentsModal = document.getElementById('viewAppointmentsModal');
    if (viewAppointmentsModal) {
        viewAppointmentsModal.addEventListener('show.bs.modal', function() {
            const clientId = receiverId;
            const appointmentsTableBody = document.getElementById('appointmentsTableBody');
            const clientNameSpan = document.querySelector('#viewAppointmentsModalLabel .client-name');
            const clientName = document.getElementById('receiverName').value;

            if (clientNameSpan) {
                clientNameSpan.textContent = clientName;
            }

            appointmentsTableBody.innerHTML = `
                <tr>
                    <td colspan="4" class="text-center">
                        <div class="spinner-border spinner-border-sm text-primary me-2" role="status">
                            <span class="visually-hidden">Loading...</span>
                        </div>
                        Loading appointments...
                    </td>
                </tr>
            `;

            fetch(`/api/appointments/client/${clientId}`)
                .then(response => response.json())
                .then(appointments => {
                    if (appointments.length === 0) {
                        appointmentsTableBody.innerHTML = `
                            <tr>
                                <td colspan="4" class="text-center">
                                    <i class="bi bi-calendar-x text-muted me-2"></i>
                                    No appointments found for this client.
                                </td>
                            </tr>
                        `;
                        return;
                    }

                    appointments.sort((a, b) => new Date(b.startTime) - new Date(a.startTime));

                    appointmentsTableBody.innerHTML = appointments.map(appointment => `
                        <tr>
                            <td>${formatDate(appointment.startTime)}</td>
                            <td>${formatTime(appointment.startTime)} - ${formatTime(appointment.endTime)}</td>
                            <td>
                                <span class="badge ${
                                    appointment.status === 'SCHEDULED' ? 'bg-primary' :
                                    appointment.status === 'COMPLETED' ? 'bg-success' :
                                    appointment.status === 'CANCELLED' ? 'bg-danger' : 'bg-warning'
                                }">
                                    ${appointment.status}
                                </span>
                            </td>
                            <td>${appointment.notes || '-'}</td>
                        </tr>
                    `).join('');
                })
                .catch(error => {
                    console.error('Error loading appointments:', error);
                    appointmentsTableBody.innerHTML = `
                        <tr>
                            <td colspan="4" class="text-center text-danger">
                                <i class="bi bi-exclamation-circle me-2"></i>
                                Error loading appointments. Please try again.
                            </td>
                        </tr>
                    `;
                });
        });
    }

    function checkAndUpdateStatusBasedOnAppointment() {
        const statusIndicator = document.getElementById('connection-status');
        if (!statusIndicator || !receiverId) return;

        const isClientView = window.location.pathname.includes('/client/');
        
        if (isClientView) {
            fetch(`/api/chat/psychologist-status?clientId=${userId}&psychologistId=${receiverId}`)
                .then(response => response.json())
                .then(status => {
                    statusIndicator.className = getStatusClass(status.status);
                    statusIndicator.innerText = status.status;
                })
                .catch(error => {
                    console.error('Error checking psychologist status:', error);
                });
        } else {
            fetch(`/api/chat/client-status?psychologistId=${userId}&clientId=${receiverId}`)
                .then(response => response.json())
                .then(status => {
                    statusIndicator.className = getStatusClass(status.status);
                    statusIndicator.innerText = status.status;
                })
                .catch(error => {
                    console.error('Error checking client status:', error);
                });
        }
    }

    function checkAndUpdateStatusBasedOnAppointment() {
        const statusIndicator = document.getElementById('connection-status');
        if (!statusIndicator || !receiverId) return;

        fetch(`/api/chat/client-status?psychologistId=${userId}&clientId=${receiverId}`)
            .then(response => response.json())
            .then(status => {
                statusIndicator.className = getStatusClass(status.status);
                statusIndicator.innerText = status.status;
            })
            .catch(error => {
                console.error('Error checking client status:', error);
            });
    }

    function connect() {
        if (stompClient && stompClient.connected) {
            return;
        }

        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);

        stompClient.debug = null;

        stompClient.connect({}, function(frame) {
            console.log('Psychologist connected: ' + frame);

            const statusIndicator = document.getElementById('connection-status');
            if (statusIndicator && receiverId) {
                statusIndicator.className = 'ms-2 badge bg-warning';
                statusIndicator.innerText = 'Waiting for client';

                if (window.location.pathname.includes('/client/')) {
                    fetch(`/api/chat/psychologist-status?clientId=${userId}&psychologistId=${receiverId}`)
                        .then(response => response.json())
                        .then(status => {
                            statusIndicator.className = getStatusClass(status.status);
                            statusIndicator.innerText = status.status;
                        })
                        .catch(error => {
                            console.error('Error checking psychologist status:', error);
                        });
                } else {
                    fetch(`/api/chat/client-status?psychologistId=${userId}&clientId=${receiverId}`)
                        .then(response => response.json())
                        .then(status => {
                            statusIndicator.className = getStatusClass(status.status);
                            statusIndicator.innerText = status.status;
                        })
                        .catch(error => {
                            console.error('Error checking client status:', error);
                        });
                }
            }

            stompClient.subscribe('/topic/public', function(message) {
                console.log('Received public message:', message.body);
                const messageBody = JSON.parse(message.body);

                if (messageBody.type === 'JOIN' || messageBody.type === 'LEAVE') {
                    const statusIndicator = document.getElementById('connection-status');
                    if (statusIndicator && receiverId) {
                        if (messageBody.type === 'JOIN' && messageBody.senderId == receiverId) {
                            checkAndUpdateStatusBasedOnAppointment();
                        } else if (messageBody.type === 'LEAVE' && messageBody.senderId == receiverId) {
                            statusIndicator.className = 'ms-2 badge bg-warning';
                            statusIndicator.innerText = 'Waiting for client';
                        }
                    }
                }
            });

            stompClient.subscribe('/user/' + userId + '/queue/messages', function(message) {
                console.log('Psychologist received private message:', message.body);
                const receivedMessage = JSON.parse(message.body);

                window.messageEncryption.decryptFromTransmission(receivedMessage)
                    .then(decryptedMessage => {
                        showMessage(decryptedMessage);
                        checkAndUpdateStatusBasedOnAppointment();
                    })
                    .catch(error => {
                        console.error('Error decrypting received message:', error);
                        showMessage({
                            ...receivedMessage,
                            content: '[Message could not be decrypted]'
                        });
                        checkAndUpdateStatusBasedOnAppointment();
                    });
            });

            stompClient.subscribe('/user/' + userId + '/queue/notifications', function(notification) {
                console.log('Psychologist received notification:', notification.body);
                showNotification(JSON.parse(notification.body));
            });

            sendJoinMessage();

            loadPreviousMessages();
        }, function(error) {
            console.log('Error connecting to WebSocket:', error);

            const statusIndicator = document.getElementById('connection-status');
            if (statusIndicator && receiverId) {
                statusIndicator.className = 'ms-2 badge bg-danger';
                statusIndicator.innerText = 'Disconnected';
            }

            setTimeout(connect, 5000);
        });
    }

    function sendJoinMessage() {
        if (stompClient) {
            const joinMessage = {
                type: 'JOIN',
                senderId: userId,
                senderName: document.getElementById('currentUserName').value,
                timestamp: new Date()
            };

            stompClient.send('/app/chat.join', {}, JSON.stringify(joinMessage));
        }
    }

    function sendMessage() {
        if (stompClient && messageInput.value.trim() !== '') {
            const plainContent = messageInput.value.trim();
            
            const chatMessage = {
                type: 'CHAT',
                senderId: userId,
                senderName: document.getElementById('currentUserName').value,
                receiverId: receiverId,
                receiverName: document.getElementById('receiverName') ? document.getElementById('receiverName').value : 'Client',
                content: plainContent,
                timestamp: new Date()
            };

            window.messageEncryption.encryptForTransmission(chatMessage)
                .then(encryptedMessage => {
                    console.log('Psychologist sending encrypted message:', encryptedMessage);
                    stompClient.send('/app/chat.sendMessage', {}, JSON.stringify(encryptedMessage));

                    showMessage(chatMessage, true);
                    messageInput.value = '';
                })
                .catch(error => {
                    console.error('Error encrypting psychologist message:', error);
                    stompClient.send('/app/chat.sendMessage', {}, JSON.stringify(chatMessage));
                    showMessage(chatMessage, true);
                    messageInput.value = '';
                });
        }
    }

    function showMessage(message, isSent = false) {
        if (!message) return;

        if (message.type === 'JOIN' || message.type === 'LEAVE') {
            return;
        }

        if (!isSent && message.senderId != receiverId && message.receiverId != receiverId) {
            return;
        }

        const messageElement = document.createElement('div');
        messageElement.classList.add('chat-message');

        if (isSent || message.senderId == userId) {
            messageElement.classList.add('message-sent');
        } else {
            messageElement.classList.add('message-received');
        }

        const messageContent = document.createElement('div');
        messageContent.classList.add('message-content');
        messageContent.innerText = message.content;

        const messageTime = document.createElement('div');
        messageTime.classList.add('message-time', 'small', 'text-white-50');
        const timestamp = message.timestamp ? new Date(message.timestamp) : new Date();
        messageTime.innerText = timestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

        messageElement.appendChild(messageContent);
        messageElement.appendChild(messageTime);
        if (chatContainer) {
            chatContainer.appendChild(messageElement);

            chatContainer.scrollTop = chatContainer.scrollHeight;
        }
    }

    function showNotification(notification) {
        if (notification.senderId != receiverId) {
            const toast = document.createElement('div');
            toast.classList.add('toast', 'show');
            toast.setAttribute('role', 'alert');
            toast.setAttribute('aria-live', 'assertive');
            toast.setAttribute('aria-atomic', 'true');

            toast.innerHTML = `
                <div class="toast-header">
                    <strong class="me-auto">${notification.senderName}</strong>
                    <small>Just now</small>
                    <button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Close"></button>
                </div>
                <div class="toast-body">
                    ${notification.message}
                </div>
            `;

            const toastContainer = document.getElementById('toast-container');
            if (toastContainer) {
                toastContainer.appendChild(toast);

                setTimeout(() => {
                    toast.remove();
                }, 5000);
            }

            updateClientUnreadBadges(notification.senderId);
        }
    }

    function updateClientUnreadBadges(clientId) {
        const clientLinks = document.querySelectorAll('.client-list a');
        clientLinks.forEach(link => {
            const linkClientId = link.getAttribute('href').split('clientId=')[1];
            if (linkClientId === clientId.toString()) {
                const badge = link.querySelector('.unread-badge');
                if (badge) {
                    const currentCount = parseInt(badge.textContent || '0');
                    badge.textContent = currentCount + 1;
                    badge.classList.remove('d-none');
                }
            }
        });
    }

    function disconnect() {
        if (stompClient) {
            const leaveMessage = {
                type: 'LEAVE',
                senderId: userId,
                senderName: document.getElementById('currentUserName').value,
                timestamp: new Date()
            };

            stompClient.send('/app/chat.leave', {}, JSON.stringify(leaveMessage));

            stompClient.disconnect();
            console.log('Psychologist disconnected from WebSocket');
        }
    }

    function loadPreviousMessages() {
        fetch(`/api/messages/conversation?user1Id=${userId}&user2Id=${receiverId}`)
            .then(response => response.json())
            .then(messages => {
                messages.sort((a, b) => new Date(a.sentAt) - new Date(b.sentAt));

                if (chatContainer) {
                    chatContainer.innerHTML = '';

                    if (messages.length === 0) {
                        const noMessagesElement = document.createElement('div');
                        noMessagesElement.className = 'text-center py-4';
                        noMessagesElement.innerHTML = '<p class="text-muted">No messages yet. Start the conversation!</p>';
                        chatContainer.appendChild(noMessagesElement);
                    } else {
                        processPsychologistMessagesSequentially(messages, 0);
                    }

                    chatContainer.scrollTop = chatContainer.scrollHeight;
                }

                const clientLinks = document.querySelectorAll('.client-list a');
                clientLinks.forEach(link => {
                    const linkClientId = link.getAttribute('href').split('clientId=')[1];
                    if (linkClientId === receiverId.toString()) {
                        const badge = link.querySelector('.unread-badge');
                        if (badge) {
                            badge.classList.add('d-none');
                            badge.textContent = '0';
                        }
                    }
                });

                markConversationAsRead();
            })
            .catch(error => {
                console.error('Error loading messages:', error);
                if (chatContainer) {
                    chatContainer.innerHTML = '<div class="alert alert-danger m-3">Error loading messages. Please try refreshing the page.</div>';
                }
            });
    }

    function processPsychologistMessagesSequentially(messages, index) {
        if (index >= messages.length) {
            return;
        }

        const message = messages[index];
        const messageData = {
            senderId: message.senderId,
            senderName: message.senderName,
            receiverId: message.receiverId,
            receiverName: message.receiverName,
            content: message.content,
            timestamp: message.sentAt,
            encrypted: message.encrypted,
            encryptionIv: message.encryptionIv,
            encryptionHmac: message.encryptionHmac
        };

        if (message.encrypted) {
            const encryptedData = {
                content: message.content,
                iv: message.encryptionIv,
                hmac: message.encryptionHmac,
                encrypted: true
            };

            window.messageEncryption.decryptMessage(encryptedData, message.senderId, message.receiverId)
                .then(decryptedContent => {
                    messageData.content = decryptedContent;
                    showMessage(messageData);
                    processPsychologistMessagesSequentially(messages, index + 1);
                })
                .catch(error => {
                    console.error('Error decrypting message:', error);
                    messageData.content = '[Message could not be decrypted]';
                    showMessage(messageData);
                    processPsychologistMessagesSequentially(messages, index + 1);
                });
        } else {
            showMessage(messageData);
            processPsychologistMessagesSequentially(messages, index + 1);
        }
    }

    function markConversationAsRead() {
        fetch(`/api/messages/conversation/read?userId=${userId}&otherUserId=${receiverId}`, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content || ''
            }
        }).catch(error => console.error('Error marking conversation as read:', error));
    }

    if (sendButton) {
        sendButton.addEventListener('click', sendMessage);
    }

    if (messageInput) {
        messageInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                sendMessage();
                e.preventDefault();
            }
        });
    }

    connect();

    window.addEventListener('beforeunload', function() {
        disconnect();
        window.psychologistChatInitialized = false;
    });

    window.psychChatApi = {
        connect,
        disconnect,
        sendMessage,
        loadPreviousMessages,
        checkAndUpdateStatusBasedOnAppointment
    };
    
    return window.psychChatApi;
}

function initializeAppointmentForm() {
    const appointmentForm = document.getElementById('appointmentForm');
    const psychologistSelect = document.getElementById('psychologistId');
    const dateInput = document.getElementById('appointmentDate');
    const startTimeInput = document.getElementById('startTime');
    const endTimeInput = document.getElementById('endTime');

    if (psychologistSelect) {
        psychologistSelect.addEventListener('change', function() {
            if (this.value && dateInput.value) {
                fetchAvailableTimes();
            }
        });
    }

    if (dateInput) {
        dateInput.addEventListener('change', function() {
            if (this.value && psychologistSelect.value) {
                fetchAvailableTimes();
            }
        });
    }

    function fetchAvailableTimes() {
        const psychologistId = psychologistSelect.value;
        const date = dateInput.value;

        if (!psychologistId || !date) return;

        fetch(`/api/appointments/available?psychologistId=${psychologistId}&date=${date}`)
            .then(response => response.json())
            .then(times => {
                updateAvailableTimes(times);
            })
            .catch(error => console.error('Error fetching available times:', error));
    }

    function updateAvailableTimes(availableTimes) {
        const timeContainer = document.getElementById('availableTimesContainer');
        if (!timeContainer) return;

        timeContainer.innerHTML = '';

        if (availableTimes.length === 0) {
            timeContainer.innerHTML = '<div class="alert alert-info">No available times on this date. Please select another date.</div>';
            return;
        }

        const timeGrid = document.createElement('div');
        timeGrid.classList.add('row', 'g-2', 'mt-2');

        availableTimes.forEach(timeSlot => {
            const startTime = new Date(timeSlot.startTime);
            const endTime = new Date(timeSlot.endTime);

            const formattedStartTime = startTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
            const formattedEndTime = endTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

            const timeCol = document.createElement('div');
            timeCol.classList.add('col-md-3', 'col-6');

            const timeBtn = document.createElement('button');
            timeBtn.type = 'button';
            timeBtn.classList.add('btn', 'btn-outline-primary', 'w-100');
            timeBtn.innerText = `${formattedStartTime} - ${formattedEndTime}`;
            timeBtn.dataset.startTime = timeSlot.startTime;
            timeBtn.dataset.endTime = timeSlot.endTime;

            timeBtn.addEventListener('click', function() {
                document.querySelectorAll('#availableTimesContainer .btn-primary').forEach(btn => {
                    btn.classList.remove('btn-primary');
                    btn.classList.add('btn-outline-primary');
                });

                this.classList.remove('btn-outline-primary');
                this.classList.add('btn-primary');

                startTimeInput.value = this.dataset.startTime;
                endTimeInput.value = this.dataset.endTime;
            });

            timeCol.appendChild(timeBtn);
            timeGrid.appendChild(timeCol);
        });

        timeContainer.appendChild(timeGrid);
    }

    if (appointmentForm) {
        appointmentForm.addEventListener('submit', function(e) {
            if (!startTimeInput.value || !endTimeInput.value) {
                e.preventDefault();
                alert('Please select an appointment time.');
            }
        });
    }
}

function initializeJournalForm() {
    const journalForm = document.getElementById('journalForm');
    const moodRatingInput = document.getElementById('moodRating');
    const moodEmojis = document.querySelectorAll('.mood-emoji');

    if (moodEmojis.length > 0) {
        moodEmojis.forEach(emoji => {
            emoji.addEventListener('click', function() {
                const rating = this.dataset.rating;

                moodEmojis.forEach(e => e.classList.remove('active'));

                this.classList.add('active');

                moodRatingInput.value = rating;
            });
        });
    }

    if (journalForm) {
        journalForm.addEventListener('submit', function(e) {
            if (journalForm.getAttribute('data-ajax') === 'true') {
                e.preventDefault();

                const formData = new FormData(journalForm);
                const jsonData = {};

                formData.forEach((value, key) => {
                    jsonData[key] = value;
                });

                fetch(journalForm.action, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content || ''
                    },
                    body: JSON.stringify(jsonData)
                })
                    .then(response => {
                        if (response.ok) {
                            window.location.href = '/client/journal';
                        } else {
                            throw new Error('Failed to save journal entry');
                        }
                    })
                    .catch(error => {
                        console.error('Error:', error);
                        alert('Failed to save journal entry. Please try again.');
                    });
            }
        });
    }
}

function formatDate(date) {
    if (!date) return '';
    const d = new Date(date);
    return d.toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' });
}

function formatTime(date) {
    if (!date) return '';
    const d = new Date(date);
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

function formatDateTime(date) {
    if (!date) return '';
    return `${formatDate(date)} at ${formatTime(date)}`;
}

function updateConnectionStatus(connected) {
    const statusIndicator = document.getElementById('connection-status');
    if (statusIndicator) {
        if (connected) {
            statusIndicator.className = 'ms-2 badge bg-success';
            statusIndicator.innerText = 'Connected';
        } else {
            statusIndicator.className = 'ms-2 badge bg-danger';
            statusIndicator.innerText = 'Disconnected';
        }
    }
}

function getStatusClass(status) {
    switch (status) {
        case 'Connected':
        case 'Connected for appointment':
            return 'ms-2 badge bg-success';
        case 'Waiting for client':
        case 'Waiting for psychologist':
            return 'ms-2 badge bg-warning';
        case 'Offline':
            return 'ms-2 badge bg-secondary';
        case 'Busy':
            return 'ms-2 badge bg-danger';
        case 'Disconnected':
            return 'ms-2 badge bg-danger';
        default:
            return 'ms-2 badge bg-secondary';
    }
}

function showToast(message, type = 'success') {
    const toastContainer = document.getElementById('toast-container');
    if (!toastContainer) return;

    const toastElement = document.createElement('div');
    toastElement.className = 'toast show';
    toastElement.setAttribute('role', 'alert');
    toastElement.setAttribute('aria-live', 'assertive');
    toastElement.setAttribute('aria-atomic', 'true');

    const iconClass = type === 'success' ? 'bi-check-circle-fill text-success' : 'bi-exclamation-circle-fill text-danger';
    const title = type === 'success' ? 'Success' : 'Error';

    toastElement.innerHTML = `
        <div class="toast-header">
            <i class="bi ${iconClass} me-2"></i>
            <strong class="me-auto">${title}</strong>
            <small>Just now</small>
            <button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Close"></button>
        </div>
        <div class="toast-body">
            ${message}
        </div>
    `;

    toastContainer.appendChild(toastElement);

    setTimeout(() => {
        toastElement.remove();
    }, 5000);
}