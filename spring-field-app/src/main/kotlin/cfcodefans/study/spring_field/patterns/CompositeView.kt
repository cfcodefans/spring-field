package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * refers to https://github.com/iluwatar/java-design-patterns/tree/master/composite-view
 * Intent:
 *      The purpose of the Composite View Pattern is to increase re-usability and flexibility
 *      when creating views for websites/webapps. This pattern seeks to decouple the content
 *      of the page from its layout, allowing changes to be made to either the content or layout
 *      of the page without impacting the other. This pattern also allows content to be
 *      easily reused across different views easily
 */
object CompositeView {
    private val log: Logger = LoggerFactory.getLogger(CompositeView::class.java)

    init {
        TODO("It is a web based project")
    }

}

