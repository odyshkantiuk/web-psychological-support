class MessageEncryption {
    constructor() {
        this.keyCache = new Map();
        this.initialized = false;
    }

    async initialize() {
        if (this.initialized) return;
        this.initialized = true;
        console.log('Message encryption system initialized');
    }

    async deriveConversationKey(userId1, userId2) {
        const conversationId = this.getConversationId(userId1, userId2);

        if (this.keyCache.has(conversationId)) {
            return this.keyCache.get(conversationId);
        }

        try {
            const response = await fetch(`/api/encryption/salt?user1=${userId1}&user2=${userId2}`, {
                method: 'GET',
                credentials: 'same-origin'
            });

            if (!response.ok) {
                throw new Error('Failed to get encryption salt');
            }

            const data = await response.json();
            const salt = data.salt;

            const keyMaterial = `${conversationId}:${salt}`;
            const key = CryptoJS.PBKDF2(keyMaterial, salt, {
                keySize: 256/32,
                iterations: 10000
            });

            this.keyCache.set(conversationId, key);
            return key;

        } catch (error) {
            console.error('Error deriving conversation key:', error);
            const fallbackKey = CryptoJS.PBKDF2(conversationId, 'fallback-salt', {
                keySize: 256/32,
                iterations: 1000
            });
            this.keyCache.set(conversationId, fallbackKey);
            return fallbackKey;
        }
    }

    async deriveJournalKey(userId) {
        const journalId = this.getJournalId(userId);

        if (this.keyCache.has(journalId)) {
            return this.keyCache.get(journalId);
        }

        try {
            const response = await fetch(`/api/encryption/journal-salt?userId=${userId}`, {
                method: 'GET',
                credentials: 'same-origin'
            });

            if (!response.ok) {
                throw new Error('Failed to get journal encryption salt');
            }

            const data = await response.json();
            const salt = data.salt;

            const keyMaterial = `${journalId}:${salt}`;
            const key = CryptoJS.PBKDF2(keyMaterial, salt, {
                keySize: 256/32,
                iterations: 10000
            });

            this.keyCache.set(journalId, key);
            return key;

        } catch (error) {
            console.error('Error deriving journal key:', error);
            const fallbackKey = CryptoJS.PBKDF2(journalId, 'fallback-salt', {
                keySize: 256/32,
                iterations: 1000
            });
            this.keyCache.set(journalId, fallbackKey);
            return fallbackKey;
        }
    }

    getConversationId(userId1, userId2) {
        const ids = [parseInt(userId1), parseInt(userId2)].sort((a, b) => a - b);
        return `conv_${ids[0]}_${ids[1]}`;
    }

    getJournalId(userId) {
        return `journal_${parseInt(userId)}`;
    }

    async encryptMessage(content, senderId, receiverId) {
        try {
            const key = await this.deriveConversationKey(senderId, receiverId);

            const iv = CryptoJS.lib.WordArray.random(128/8);

            const encrypted = CryptoJS.AES.encrypt(content, key, {
                iv: iv,
                mode: CryptoJS.mode.CBC,
                padding: CryptoJS.pad.Pkcs7
            });

            const hmac = CryptoJS.HmacSHA256(encrypted.toString(), key);

            return {
                content: encrypted.toString(),
                iv: iv.toString(),
                hmac: hmac.toString(),
                encrypted: true
            };

        } catch (error) {
            console.error('Error encrypting message:', error);
            throw new Error('Failed to encrypt message');
        }
    }

    async encryptJournal(content, userId) {
        try {
            const key = await this.deriveJournalKey(userId);

            const iv = CryptoJS.lib.WordArray.random(128/8);

            const encrypted = CryptoJS.AES.encrypt(content, key, {
                iv: iv,
                mode: CryptoJS.mode.CBC,
                padding: CryptoJS.pad.Pkcs7
            });

            const hmac = CryptoJS.HmacSHA256(encrypted.toString(), key);

            return {
                content: encrypted.toString(),
                iv: iv.toString(),
                hmac: hmac.toString(),
                encrypted: true
            };

        } catch (error) {
            console.error('Error encrypting journal:', error);
            throw new Error('Failed to encrypt journal');
        }
    }

    async decryptMessage(encryptedData, senderId, receiverId) {
        try {
            if (!encryptedData.encrypted) {
                return encryptedData.content;
            }

            const key = await this.deriveConversationKey(senderId, receiverId);

            if (encryptedData.hmac) {
                const expectedHmac = CryptoJS.HmacSHA256(encryptedData.content, key);
                if (expectedHmac.toString() !== encryptedData.hmac) {
                    throw new Error('Message authentication failed');
                }
            }

            const iv = CryptoJS.enc.Hex.parse(encryptedData.iv);

            const decrypted = CryptoJS.AES.decrypt(encryptedData.content, key, {
                iv: iv,
                mode: CryptoJS.mode.CBC,
                padding: CryptoJS.pad.Pkcs7
            });

            return decrypted.toString(CryptoJS.enc.Utf8);

        } catch (error) {
            console.error('Error decrypting message:', error);
            return '[Message could not be decrypted]';
        }
    }

    async decryptJournal(encryptedData, userId) {
        try {
            if (!encryptedData.encrypted) {
                return encryptedData.content;
            }

            const key = await this.deriveJournalKey(userId);

            if (encryptedData.hmac) {
                const expectedHmac = CryptoJS.HmacSHA256(encryptedData.content, key);
                if (expectedHmac.toString() !== encryptedData.hmac) {
                    throw new Error('Journal authentication failed');
                }
            }

            const iv = CryptoJS.enc.Hex.parse(encryptedData.iv);

            const decrypted = CryptoJS.AES.decrypt(encryptedData.content, key, {
                iv: iv,
                mode: CryptoJS.mode.CBC,
                padding: CryptoJS.pad.Pkcs7
            });

            return decrypted.toString(CryptoJS.enc.Utf8);

        } catch (error) {
            console.error('Error decrypting journal:', error);
            return '[Journal could not be decrypted]';
        }
    }

    async encryptForTransmission(chatMessage) {
        try {
            const encryptedData = await this.encryptMessage(
                chatMessage.content,
                chatMessage.senderId,
                chatMessage.receiverId
            );

            return {
                ...chatMessage,
                content: encryptedData.content,
                encrypted: true,
                encryptionIv: encryptedData.iv,
                encryptionHmac: encryptedData.hmac
            };
        } catch (error) {
            console.error('Error encrypting for transmission:', error);
            return chatMessage;
        }
    }

    async decryptFromTransmission(chatMessage) {
        try {
            if (!chatMessage.encrypted) {
                return chatMessage;
            }

            const encryptedData = {
                content: chatMessage.content,
                iv: chatMessage.encryptionIv,
                hmac: chatMessage.encryptionHmac,
                encrypted: true
            };

            const decryptedContent = await this.decryptMessage(
                encryptedData,
                chatMessage.senderId,
                chatMessage.receiverId
            );

            return {
                ...chatMessage,
                content: decryptedContent,
                encrypted: false
            };
        } catch (error) {
            console.error('Error decrypting from transmission:', error);
            return {
                ...chatMessage,
                content: '[Message could not be decrypted]'
            };
        }
    }

    clearKeyCache() {
        this.keyCache.clear();
        console.log('Encryption key cache cleared');
    }

    async testEncryption() {
        try {
            const testMessage = "Hello, this is a test message!";
            const userId1 = "1";
            const userId2 = "2";

            console.log('Testing encryption...');
            
            const encrypted = await this.encryptMessage(testMessage, userId1, userId2);
            console.log('Encrypted:', encrypted);
            
            const decrypted = await this.decryptMessage(encrypted, userId1, userId2);
            console.log('Decrypted:', decrypted);
            
            if (decrypted === testMessage) {
                console.log('✅ Encryption test passed!');
                return true;
            } else {
                console.error('❌ Encryption test failed!');
                return false;
            }
        } catch (error) {
            console.error('❌ Encryption test error:', error);
            return false;
        }
    }
}

window.messageEncryption = new MessageEncryption();

document.addEventListener('DOMContentLoaded', function() {
    window.messageEncryption.initialize();
});

window.addEventListener('beforeunload', function() {
    if (window.messageEncryption) {
        window.messageEncryption.clearKeyCache();
    }
});