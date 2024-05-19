package com.example.secnote

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.ByteArrayOutputStream
import java.security.Key
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.spec.IvParameterSpec

class Crypto {

    private val transformation = "AES/GCM/NoPadding"
    private val keySize = 256
    private val iterationCount = 10000
    private val salt = "some_random_salt".toByteArray()
    private val ivSize = 12

    fun encryptWithPassword(data: String, password: String): ByteArray {
        val cipher = Cipher.getInstance(transformation)
        val keySpec = PBEKeySpec(password.toCharArray(), salt, iterationCount, keySize)
        val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = secretKeyFactory.generateSecret(keySpec).encoded
        val key = SecretKeySpec(keyBytes, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return iv + encryptedData
    }

    fun decryptWithPassword(data: ByteArray, password: String): String {
        val iv = data.copyOfRange(0, ivSize)
        val encryptedData = data.copyOfRange(ivSize, data.size)

        val keySpec = PBEKeySpec(password.toCharArray(), salt, iterationCount, keySize)
        val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = secretKeyFactory.generateSecret(keySpec).encoded
        val key = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance(transformation)
        val params = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, params)
        val decryptedData = cipher.doFinal(encryptedData)
        return String(decryptedData, StandardCharsets.UTF_8)
    }

    fun keyGen(keyAlias: String) {
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setRandomizedEncryptionRequired(true)
            .build()

        val keyGenerator: KeyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    private fun createKey(keyAlias: String) {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).apply {
            setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            setUserAuthenticationRequired(true)
            setInvalidatedByBiometricEnrollment(true)
        }.build()
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    fun encrypt(keyAlias: String, input: String): ByteArray {
        val AES_MODE = "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}"
        val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null, null)
        val key: Key = keyStore.getKey(keyAlias, null)
        val cipher: Cipher = Cipher.getInstance(AES_MODE)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encryptedBytes: ByteArray = cipher.doFinal(input.toByteArray(Charsets.UTF_8))
        val iv: ByteArray = cipher.iv
        return iv + encryptedBytes
    }

    fun decrypt(keyAlias: String, input: ByteArray): String {
        val iv = input.copyOfRange(0, 16)
        val encryptedData = input.copyOfRange(16, input.size)
        val AES_MODE = "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}"
        val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null, null)
        val key: Key = keyStore.getKey(keyAlias, null)
        val cipher: Cipher = Cipher.getInstance(AES_MODE)
        val params = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, key, params)
        val decrypted = cipher.doFinal(encryptedData)
        return String(decrypted, Charsets.UTF_8)
    }
}
