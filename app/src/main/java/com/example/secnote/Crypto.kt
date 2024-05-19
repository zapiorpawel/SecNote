package com.example.secnote

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.ByteArrayOutputStream
import java.security.Key
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import java.nio.charset.StandardCharsets
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
public class Crypto {

    private val transformation = "AES/GCM/NoPadding"
    private val keySize = 256
    private val iterationCount = 10000
    private val salt = "some_random_salt".toByteArray()
    private val ivSize = 12

    fun encryptWithPassword(data: String, password: String): ByteArray {
        val cipher = Cipher.getInstance(transformation)
        val key = generateKeyFromPassword(password)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return iv + encryptedData
    }

    fun decryptWithPassword(data: ByteArray, password: String): String {
        val cipher = Cipher.getInstance(transformation)
        val iv = data.copyOfRange(0, ivSize)
        val encryptedData = data.copyOfRange(ivSize, data.size)
        val key = generateKeyFromPassword(password)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        val decryptedData = cipher.doFinal(encryptedData)
        return String(decryptedData, StandardCharsets.UTF_8)
    }

    private fun generateKeyFromPassword(password: String): SecretKey {
        val keySpec = PBEKeySpec(password.toCharArray(), salt, iterationCount, keySize)
        val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = keyFactory.generateSecret(keySpec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
    fun keyGen(keyAlias:String){
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
            setUserAuthenticationRequired(true) // Require authentication on every use of the key
            setInvalidatedByBiometricEnrollment(true) // Invalidate the key if a new fingerprint is enrolled
        }.build()
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    fun encrypt(keyAlias:String, input:String): ByteArray {
        val AES_MODE = (KeyProperties.KEY_ALGORITHM_AES
                + "/" + KeyProperties.BLOCK_MODE_CBC
                + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7)
        val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null, null)
        // byte[] input

        // byte[] input
        val key: Key = keyStore.getKey(keyAlias, null)

        val cipher: Cipher = Cipher.getInstance(AES_MODE)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val encryptedBytes: ByteArray = cipher.doFinal(input.toByteArray(Charsets.UTF_8))
        val iv: ByteArray = cipher.iv
        var outputStream = ByteArrayOutputStream( )
        outputStream.write( iv )
        outputStream.write( encryptedBytes )

        return outputStream.toByteArray( )
    }

    fun decrypt(keyAlias:String, input:ByteArray): String {
        var iv = ByteArray(0)
        var encryptedData = ByteArray(0)

        for(i in 0..15)
            iv += input[i]
        for(i in 16 until input.size)
            encryptedData += input[i]

        val AES_MODE = (KeyProperties.KEY_ALGORITHM_AES
                + "/" + KeyProperties.BLOCK_MODE_CBC
                + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7)
        val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null, null)

        val key : Key = keyStore.getKey(keyAlias, null)

        var cipher : Cipher = Cipher.getInstance(AES_MODE)
        val params : IvParameterSpec = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, key, params)

        val decrypted = cipher.doFinal(encryptedData)

        return String(decrypted, Charsets.UTF_8)
    }
}