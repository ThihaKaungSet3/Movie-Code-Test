package non.shahad.moviecodemanagement.interactors

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import non.shahad.domain.flow.HomeDataFlow
import non.shahad.domain.flow.InteractorFlow
import non.shahad.domain.model.Movie
import non.shahad.domain.repository.HomeRepository
import non.shahad.domain.repository.MovieRepository
import java.util.*
import javax.inject.Inject

class HomeInteractor @Inject constructor(
    private val movieRepository: MovieRepository
) {

    fun streamHomeData(fresh: Boolean): Flow<HomeDataFlow> = flow {
        try {
            emit(HomeDataFlow.StartedLoading)

            val upcomingCache = movieRepository.cachedUpcomingMovies()
            val popularCache = movieRepository.cachedPopularMovies()

            emit(HomeDataFlow.Cached(upcomingCache,popularCache))

            if (fresh || upcomingCache.isExpired() || popularCache.isExpired()){
                val freshPopular = movieRepository.freshPopularMovies()
                val freshUpcoming = movieRepository.freshUpcomingMovies()

                movieRepository.storeToCache(freshPopular)
                movieRepository.storeToCache(freshUpcoming)

                emit(HomeDataFlow.FreshByCached(freshUpcoming, freshPopular))
            }

            emit(HomeDataFlow.Satisfied)

        } catch (e: Exception){
            emit(HomeDataFlow.Error(e.message!!, e))
        }
    }

    fun updateMovie(movie: Movie): Flow<InteractorFlow> = flow {
        try {
            emit(InteractorFlow.JobStarted)
            movieRepository.updateMovie(movie)
            emit(InteractorFlow.Done)
        } catch (e: Exception){
            emit(InteractorFlow.Error(e.message!!, e))
        }
    }

    private fun List<Movie>.isExpired(): Boolean {
        return isEmpty() || firstOrNull() == null || isCacheExpired(first().lastUpdateTimestamp)
    }


    private fun isCacheExpired(validTimestamp: Long): Boolean {
        val now = Date().time
        return now > validTimestamp
    }

}