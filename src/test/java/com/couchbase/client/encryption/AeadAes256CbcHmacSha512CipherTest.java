package com.couchbase.client.encryption;

import com.couchbase.client.core.deps.io.netty.buffer.ByteBufUtil;
import com.couchbase.client.encryption.errors.InvalidCiphertextException;
import com.couchbase.client.encryption.errors.InvalidCryptoKeyException;
import com.couchbase.client.encryption.internal.AeadAes256CbcHmacSha512Cipher;
import org.junit.jupiter.api.Test;

import static com.couchbase.client.core.util.Bytes.EMPTY_BYTE_ARRAY;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AeadAes256CbcHmacSha512CipherTest {
  private static final byte[] key = decodeHex("" +
      "00 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e 0f" +
      "10 11 12 13 14 15 16 17 18 19 1a 1b 1c 1d 1e 1f" +
      "20 21 22 23 24 25 26 27 28 29 2a 2b 2c 2d 2e 2f" +
      "30 31 32 33 34 35 36 37 38 39 3a 3b 3c 3d 3e 3f");

  private static final byte[] iv = decodeHex(
      "1a f3 8c 2d c2 b9 6f fd d8 66 94 09 23 41 bc 04");

  private static final byte[] plaintext = decodeHex("" +
      "41 20 63 69 70 68 65 72 20 73 79 73 74 65 6d 20" +
      "6d 75 73 74 20 6e 6f 74 20 62 65 20 72 65 71 75" +
      "69 72 65 64 20 74 6f 20 62 65 20 73 65 63 72 65" +
      "74 2c 20 61 6e 64 20 69 74 20 6d 75 73 74 20 62" +
      "65 20 61 62 6c 65 20 74 6f 20 66 61 6c 6c 20 69" +
      "6e 74 6f 20 74 68 65 20 68 61 6e 64 73 20 6f 66" +
      "20 74 68 65 20 65 6e 65 6d 79 20 77 69 74 68 6f" +
      "75 74 20 69 6e 63 6f 6e 76 65 6e 69 65 6e 63 65");

  private static final byte[] associatedData = decodeHex("" +
      "54 68 65 20 73 65 63 6f 6e 64 20 70 72 69 6e 63" +
      "69 70 6c 65 20 6f 66 20 41 75 67 75 73 74 65 20" +
      "4b 65 72 63 6b 68 6f 66 66 73");

  private static final byte[] ciphertext = decodeHex("" +
      "1a f3 8c 2d c2 b9 6f fd d8 66 94 09 23 41 bc 04" +
      "4a ff aa ad b7 8c 31 c5 da 4b 1b 59 0d 10 ff bd" +
      "3d d8 d5 d3 02 42 35 26 91 2d a0 37 ec bc c7 bd" +
      "82 2c 30 1d d6 7c 37 3b cc b5 84 ad 3e 92 79 c2" +
      "e6 d1 2a 13 74 b7 7f 07 75 53 df 82 94 10 44 6b" +
      "36 eb d9 70 66 29 6a e6 42 7e a7 5c 2e 08 46 a1" +
      "1a 09 cc f5 37 0d c8 0b fe cb ad 28 c7 3f 09 b3" +
      "a3 b7 5e 66 2a 25 94 41 0a e4 96 b2 e2 e6 60 9e" +
      "31 e6 e0 2c c8 37 f0 53 d2 1f 37 ff 4f 51 95 0b" +
      "be 26 38 d0 9d d7 a4 93 09 30 80 6d 07 03 b1 f6" +
      "4d d3 b4 c0 88 a7 f4 5c 21 68 39 64 5b 20 12 bf" +
      "2e 62 69 a8 c5 6a 81 6d bc 1b 26 77 61 95 5b c5");


  private static final AeadAes256CbcHmacSha512Cipher cipherWithRandomIv = new AeadAes256CbcHmacSha512Cipher();

  private static final AeadAes256CbcHmacSha512Cipher cipherWithFixedIv =
      new AeadAes256CbcHmacSha512Cipher(new FakeSecureRandom(iv), null);

  private static byte[] decodeHex(String hex) {
    return ByteBufUtil.decodeHexDump(hex.replaceAll("\\s", ""));
  }

  @Test
  void encrypt() throws Exception {
    byte[] actualCiphertext = cipherWithFixedIv.encrypt(key, plaintext, associatedData);
    assertArrayEquals(ciphertext, actualCiphertext);
  }

  @Test
  void decrypt() throws Exception {
    byte[] actualPlaintext = cipherWithFixedIv.decrypt(key, ciphertext, associatedData);
    assertArrayEquals(plaintext, actualPlaintext);
  }

  @Test
  void associatedDataCanBeEmpty() throws Exception {
    byte[] ciphertext = cipherWithFixedIv.encrypt(key, plaintext, EMPTY_BYTE_ARRAY);
    byte[] roundTripPlaintext = new AeadAes256CbcHmacSha512Cipher().decrypt(key, ciphertext, EMPTY_BYTE_ARRAY);
    assertArrayEquals(plaintext, roundTripPlaintext);
  }

  @Test
  void worksWithRealIvGenerator() throws Exception {
    byte[] ciphertext = cipherWithRandomIv.encrypt(key, plaintext, associatedData);
    byte[] roundTripPlaintext = cipherWithRandomIv.decrypt(key, ciphertext, associatedData);
    assertArrayEquals(plaintext, roundTripPlaintext);
  }

  @Test
  void decryptBadAssociatedData() throws Exception {
    final byte[] bogusAssociatedData = associatedData.clone();
    bogusAssociatedData[0]++;

    assertThrows(InvalidCiphertextException.class, () ->
        cipherWithFixedIv.decrypt(key, ciphertext, bogusAssociatedData));
  }

  @Test
  void decryptBadCiphertext() throws Exception {
    // Tampering with any byte of the ciphertext should trigger an integrity check failure
    for (int i = 0; i < ciphertext.length; i++) {
      final byte[] bogusCiphertext = ciphertext.clone();
      bogusCiphertext[i]++;

      assertThrows(InvalidCiphertextException.class, () ->
          cipherWithFixedIv.decrypt(key, bogusCiphertext, associatedData));
    }
  }

  @Test
  void decryptBadKeySize() throws Exception {
    assertThrows(InvalidCryptoKeyException.class, () ->
        cipherWithFixedIv.decrypt(new byte[1], ciphertext, associatedData));
  }

  @Test
  void encryptBadKeySize() throws Exception {
    assertThrows(InvalidCryptoKeyException.class, () ->
        cipherWithFixedIv.encrypt(new byte[1], ciphertext, associatedData));
  }
}
