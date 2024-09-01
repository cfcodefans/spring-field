package cfcodefans.study.spring_field.tests;


import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class EncodingTests {
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        final Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec("dBulXWFUywd7ey9p335vTQUd7d1k0OoO".getBytes(StandardCharsets.UTF_8),
                "HmacSHA512"));

        {
            String paramStr = "ARTICLENR=166&CITY=Philadelphia&COUNTRY=US&DOMAIN_FOR_IFRAME=https://www.fetish.com&EMAIL=malikfortune08@gmail.com&FIRSTNAME=Maya?s&HOUSENR=2&IP=71.162.209.51&LANGUAGE=EN&LASTNAME=James&PAGENUMBER=fetco&PAYMENTTYPE=CC&SALUTATION=1M&STREET=2405 n broad st&TANR=43039587&TESTSIGNUP=FALSE&USERNR=4028644&ZIPCODE=19132";
            String result = bytesToHex(mac.doFinal(paramStr.getBytes(StandardCharsets.UTF_8)));
            System.out.println("parameter: " + paramStr + "\n\tresult = " + result);
        }
        System.out.println();
        {
            String paramStr = "ARTICLENR=166&CITY=Philadelphia&COUNTRY=US&DOMAIN_FOR_IFRAME=https://www.fetish.com&EMAIL=malikfortune08@gmail.com&FIRSTNAME=Maya’s&HOUSENR=2&IP=71.162.209.51&LANGUAGE=EN&LASTNAME=James&PAGENUMBER=fetco&PAYMENTTYPE=CC&SALUTATION=1M&STREET=2405 n broad st&TANR=43039587&TESTSIGNUP=FALSE&USERNR=4028644&ZIPCODE=19132";
            String result = bytesToHex(mac.doFinal(paramStr.getBytes(StandardCharsets.UTF_8)));
            System.out.println("parameter: " + paramStr + "\n\tresult = " + result);
        }
    }
}
