package com.example.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object CryptoManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
    private const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
    private const val TRANSFORMATION = "$ALGORITHM/$BLOCK_MODE/$PADDING"
    private const val KEY_ALIAS = "spkl_secret_vault_key"
    private const val TAG_LENGTH_BITS = 128
    private const val IV_LENGTH_BYTES = 12

    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    private fun getSecretKey(): SecretKey {
        val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey ?: createSecretKey()
    }

    private fun createSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(ALGORITHM, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(BLOCK_MODE)
            .setEncryptionPaddings(PADDING)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * Encrypts plain bytes using AES-256-GCM.
     * The 12-byte IV is prepended to the returned byte array.
     */
    fun encrypt(bytes: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(bytes)

        val output = ByteArrayOutputStream()
        output.write(iv)
        output.write(encryptedData)
        return output.toByteArray()
    }

    /**
     * Decrypts encrypted bytes (with prepended 12-byte IV).
     */
    fun decrypt(encryptedBytesWithIv: ByteArray): ByteArray {
        if (encryptedBytesWithIv.size < IV_LENGTH_BYTES) {
            throw IllegalArgumentException("Data terlalu pendek untuk didekripsi")
        }
        val iv = encryptedBytesWithIv.copyOfRange(0, IV_LENGTH_BYTES)
        val cipherBytes = encryptedBytesWithIv.copyOfRange(IV_LENGTH_BYTES, encryptedBytesWithIv.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
        return cipher.doFinal(cipherBytes)
    }

    fun encryptStream(inputStream: InputStream, outputStream: OutputStream) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        outputStream.write(cipher.iv)

        val buffer = ByteArray(4096)
        var read: Int
        while (inputStream.read(buffer).also { read = it } != -1) {
            val chunk = cipher.update(buffer, 0, read)
            if (chunk != null) outputStream.write(chunk)
        }
        val finalBytes = cipher.doFinal()
        if (finalBytes != null) outputStream.write(finalBytes)
    }

    fun decryptStream(inputStream: InputStream, outputStream: OutputStream) {
        val iv = ByteArray(IV_LENGTH_BYTES)
        val ivRead = inputStream.read(iv)
        if (ivRead < IV_LENGTH_BYTES) {
            throw IllegalArgumentException("Format data terenkripsi tidak valid")
        }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

        val buffer = ByteArray(4096)
        var read: Int
        while (inputStream.read(buffer).also { read = it } != -1) {
            val chunk = cipher.update(buffer, 0, read)
            if (chunk != null) outputStream.write(chunk)
        }
        val finalBytes = cipher.doFinal()
        if (finalBytes != null) outputStream.write(finalBytes)
    }
}
