package com.zhenduanqi.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TokenEncryptionUtilTest {

    private final TokenEncryptionUtil util = new TokenEncryptionUtil("test-secret-key-16chars");

    @Test
    void encrypt_shouldNotReturnPlainText() {
        String plainText = "my-secret-token-123";
        String encrypted = util.encrypt(plainText);

        assertNotNull(encrypted);
        assertNotEquals(plainText, encrypted);
        assertFalse(encrypted.contains(plainText));
    }

    @Test
    void decrypt_shouldRestoreOriginalText() {
        String plainText = "my-secret-token-456";
        String encrypted = util.encrypt(plainText);
        String decrypted = util.decrypt(encrypted);

        assertEquals(plainText, decrypted);
    }

    @Test
    void encrypt_shouldProduceDifferentOutputForSameInput() {
        String plainText = "same-token-every-time";
        String encrypted1 = util.encrypt(plainText);
        String encrypted2 = util.encrypt(plainText);

        assertNotEquals(encrypted1, encrypted2);
    }

    @Test
    void decrypt_shouldWorkForEmptyString() {
        String encrypted = util.encrypt("");
        String decrypted = util.decrypt(encrypted);
        assertEquals("", decrypted);
    }
}
