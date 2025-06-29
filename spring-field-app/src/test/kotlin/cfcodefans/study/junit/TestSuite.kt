package cfcodefans.study.junit

import org.junit.platform.suite.api.IncludeClassNamePatterns
import org.junit.platform.suite.api.SelectPackages
import org.junit.platform.suite.api.Suite
import org.junit.platform.suite.api.SuiteDisplayName
import kotlin.test.Test

@Suite
@SuiteDisplayName("Test Suite for Demo Project")
@SelectPackages("cfcodefans.study.junit")
@IncludeClassNamePatterns(".*Tests")
open class TestSuite {

}

open class StubATests {
    @Test
    open fun `dummy test 1`() {
        println("dummy test 1 ${ProcessHandle.current().pid()}")
    }
}

open class StubBTests {
    @Test
    open fun `dummy test 2`() {
        println("dummy test 2 ${ProcessHandle.current().pid()}")
    }
}