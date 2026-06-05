package pro.jayeshseth.devcontrols.adb

import android.content.Context
import android.os.Build
import android.sun.security.x509.AlgorithmId
import android.sun.security.x509.CertificateAlgorithmId
import android.sun.security.x509.CertificateIssuerName
import android.sun.security.x509.CertificateSerialNumber
import android.sun.security.x509.CertificateSubjectName
import android.sun.security.x509.CertificateValidity
import android.sun.security.x509.CertificateVersion
import android.sun.security.x509.CertificateX509Key
import android.sun.security.x509.X500Name
import android.sun.security.x509.X509CertImpl
import android.sun.security.x509.X509CertInfo
import io.github.muntashirakon.adb.AbsAdbConnectionManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date
import java.util.Random

/**
 * Singleton ADB connection manager. Persists an RSA key pair + self-signed
 * X.509 cert in the app's private storage so the device's wireless-ADB server
 * recognises us across reboots once paired.
 */
class AdbConnectionManagerImpl private constructor(context: Context) : AbsAdbConnectionManager() {

    private val pk: PrivateKey
    private val cert: Certificate

    init {
        setApi(Build.VERSION.SDK_INT)

        val existingKey = readPrivateKey(context)
        if (existingKey != null) {
            pk = existingKey
            cert = readCertificate(context)
                ?: throw IllegalStateException("Private key found but cert missing")
        } else {
            val gen = KeyPairGenerator.getInstance("RSA")
            gen.initialize(KEY_SIZE, SecureRandom.getInstance("SHA1PRNG"))
            val pair: KeyPair = gen.generateKeyPair()
            pk = pair.private
            cert = generateCertificate(pair, DEVICE_NAME)
            writePrivateKey(context, pk)
            writeCertificate(context, cert)
        }
    }

    override fun getPrivateKey(): PrivateKey = pk
    override fun getCertificate(): Certificate = cert
    override fun getDeviceName(): String = DEVICE_NAME

    companion object {
        private const val KEY_SIZE = 2048
        private const val DEVICE_NAME = "DevControls"
        private const val PRIVATE_KEY_FILE = "adb_private.key"
        private const val CERTIFICATE_FILE = "adb_certificate.pem"

        @Volatile
        private var instance: AdbConnectionManagerImpl? = null

        fun get(context: Context): AdbConnectionManagerImpl =
            instance ?: synchronized(this) {
                instance ?: AdbConnectionManagerImpl(context.applicationContext)
                    .also { instance = it }
            }

        private fun keyFile(context: Context) = File(context.filesDir, PRIVATE_KEY_FILE)
        private fun certFile(context: Context) = File(context.filesDir, CERTIFICATE_FILE)

        fun hasStoredKeys(context: Context): Boolean =
            keyFile(context).exists() && certFile(context).exists()

        private fun readPrivateKey(context: Context): PrivateKey? {
            val f = keyFile(context)
            if (!f.exists()) return null
            val bytes = FileInputStream(f).use { it.readBytes() }
            val spec = PKCS8EncodedKeySpec(bytes)
            return KeyFactory.getInstance("RSA").generatePrivate(spec)
        }

        private fun writePrivateKey(context: Context, key: PrivateKey) {
            FileOutputStream(keyFile(context)).use { it.write(key.encoded) }
        }

        private fun readCertificate(context: Context): Certificate? {
            val f = certFile(context)
            if (!f.exists()) return null
            return FileInputStream(f).use { CertificateFactory.getInstance("X.509").generateCertificate(it) }
        }

        private fun writeCertificate(context: Context, cert: Certificate) {
            FileOutputStream(certFile(context)).use { it.write(cert.encoded) }
        }

        private fun generateCertificate(pair: KeyPair, subject: String): X509Certificate {
            val notBefore = Date()
            val notAfter = Date(notBefore.time + 30L * 365 * 24 * 3600 * 1000) // ~30 years
            val info = X509CertInfo()
            info[X509CertInfo.VALIDITY] = CertificateValidity(notBefore, notAfter)
            info[X509CertInfo.SERIAL_NUMBER] =
                CertificateSerialNumber(Random().nextInt() and 0x7fffffff)
            val name = X500Name("CN=$subject")
            info[X509CertInfo.SUBJECT] = CertificateSubjectName(name)
            info[X509CertInfo.ISSUER] = CertificateIssuerName(name)
            info[X509CertInfo.KEY] = CertificateX509Key(pair.public)
            info[X509CertInfo.VERSION] = CertificateVersion(CertificateVersion.V3)
            info[X509CertInfo.ALGORITHM_ID] =
                CertificateAlgorithmId(AlgorithmId.get("SHA256WithRSA"))
            val cert = X509CertImpl(info)
            cert.sign(pair.private, "SHA256withRSA")
            return cert
        }
    }
}
