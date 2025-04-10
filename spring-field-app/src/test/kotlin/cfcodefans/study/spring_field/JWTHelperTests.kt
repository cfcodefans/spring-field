package cfcodefans.study.spring_field

import cfcodefans.study.spring_field.standalone_web.template.JWTHelper
import org.junit.jupiter.api.Test


open class JWTHelperTests {

    @Test
    fun testGenerateAndValidateToken() {
        val token = JWTHelper.generateToken("testUser")
        println(token)
        val claim = JWTHelper.validateToken(token)
        println(claim?.subject.toString())
    }
}