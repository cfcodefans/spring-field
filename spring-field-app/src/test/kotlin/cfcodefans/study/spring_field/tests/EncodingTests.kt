package cfcodefans.study.spring_field.tests

import org.jasypt.encryption.StringEncryptor
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor
import org.jasypt.iv.RandomIvGenerator
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

open class EncodingTests {
    companion object {
        val log: Logger = LoggerFactory.getLogger(EncodingTests::class.java)

        fun bytesToHex(bytes: ByteArray): String {
            val sb = StringBuilder()
            for (b in bytes) {
                sb.append(String.format("%02x", b))
            }
            return sb.toString()
        }
    }

    @Test
    open fun testBytesToHex() {
        val mac: Mac = Mac.getInstance("HmacSHA512")
        mac.init(SecretKeySpec("dBulXWFUywd7ey9p335vTQUd7d1k0OoO".toByteArray(StandardCharsets.UTF_8),
                               "HmacSHA512"))

        run {
            val paramStr: String =
                "ARTICLENR=166&CITY=Philadelphia&COUNTRY=US&DOMAIN_FOR_IFRAME=https://www.fetish.com&EMAIL=malikfortune08@gmail.com&FIRSTNAME=Maya?s&HOUSENR=2&IP=71.162.209.51&LANGUAGE=EN&LASTNAME=James&PAGENUMBER=fetco&PAYMENTTYPE=CC&SALUTATION=1M&STREET=2405 n broad st&TANR=43039587&TESTSIGNUP=FALSE&USERNR=4028644&ZIPCODE=19132"
            val result: String = bytesToHex(mac.doFinal(paramStr.toByteArray(StandardCharsets.UTF_8)))
            log.info("parameter: $paramStr\n\tresult = $result")
        }
        run {
            val paramStr: String =
                "ARTICLENR=166&CITY=Philadelphia&COUNTRY=US&DOMAIN_FOR_IFRAME=https://www.fetish.com&EMAIL=malikfortune08@gmail.com&FIRSTNAME=Maya’s&HOUSENR=2&IP=71.162.209.51&LANGUAGE=EN&LASTNAME=James&PAGENUMBER=fetco&PAYMENTTYPE=CC&SALUTATION=1M&STREET=2405 n broad st&TANR=43039587&TESTSIGNUP=FALSE&USERNR=4028644&ZIPCODE=19132"
            val result = bytesToHex(mac.doFinal(paramStr.toByteArray(StandardCharsets.UTF_8)))
            log.info("parameter: $paramStr\n\tresult = $result")
        }
    }

    @Test
    open fun testEncryption() {
        val encryptor: StringEncryptor = StandardPBEStringEncryptor().apply {
            setAlgorithm("PBEWITHHMACSHA512ANDAES_256")
            setPassword("ZDRmYjY4OTRhYjc5")
//            setPassword("serviceteam")
            setIvGenerator(RandomIvGenerator())
        }
        //TBEX4nC/X53iZrTi9HpWCqjnC9nIw3OCb5xN9AeydRWYLvP9fwE8e8Nf2pIy2iL8
        val pwd: String = ""
        var encoded: String = encryptor.encrypt(pwd)
//        encoded = "TBEX4nC/X53iZrTi9HpWCqjnC9nIw3OCb5xN9AeydRWYLvP9fwE8e8Nf2pIy2iL8"
//        encoded = "RoHH/bRBbHRdU6+dswwo9nsKaNIaYvCszVg/nQ/QcQBuYBUOPWRKq3ABIJRbUx8yRdefvv8CSWymll0xg99zkA=="
        encoded = "YY0pJxyulKH9ci4PNQBD1JAxtmtgIjzU2HRTYixXtaampJr3Nof04cC5IQFeCkqV"
        log.info("Kms-1234 to be encrypted ${encoded}, and decrypted as ${encryptor.decrypt(encoded)}")
    }
}
