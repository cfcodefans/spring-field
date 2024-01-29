package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object BusinessDelegate {
    private val log: Logger = LoggerFactory.getLogger(BusinessDelegate::class.java)

    fun interface IVideoStreamingService {
        fun doProc(): Unit
    }

    open class NetflixService : IVideoStreamingService {
        companion object {
            private val log: Logger = LoggerFactory.getLogger(NetflixService::class.java)
        }

        override fun doProc(): Unit = log.info("NetflixService is now processing")
    }

    open class YouTubeService : IVideoStreamingService {
        companion object {
            private val log: Logger = LoggerFactory.getLogger(YouTubeService::class.java)
        }

        override fun doProc(): Unit = log.info("YouTubeService is now processing")
    }

    open class BusinessLookup(val netflixService: NetflixService,
                              val youTubeService: YouTubeService) {
        open fun getBusinessService(movie: String): IVideoStreamingService =
            if (movie.lowercase().contains("die hard")) netflixService else youTubeService
    }

    open class BusinessDelegate(val lookup: BusinessLookup) {
        open fun playbackMovie(movie: String) = lookup.getBusinessService(movie).doProc()
    }


    open class MobileClient(val bizDelegate: BusinessDelegate) {
        open fun playbackMovie(movie: String) = bizDelegate.playbackMovie(movie)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val client: MobileClient = MobileClient(
                bizDelegate = BusinessDelegate(lookup = BusinessLookup(
                        netflixService = NetflixService(),
                        youTubeService = YouTubeService()
                )))
        client.playbackMovie("Die Hard 2")
        client.playbackMovie("Maradona: The Greatest Ever")
    }
}