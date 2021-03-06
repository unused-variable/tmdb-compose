package dev.shreyansh.tmdb.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.shreyansh.tmdb.data.api.TmdbService
import dev.shreyansh.tmdb.data.db.dao.GenreDao
import dev.shreyansh.tmdb.data.db.dao.MovieDao
import dev.shreyansh.tmdb.data.db.dao.TvShowDao
import dev.shreyansh.tmdb.data.repository.TmdbRepository
import kotlinx.coroutines.CoroutineDispatcher

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    fun provideTmdbRepository(genreDao: GenreDao, movieDao: MovieDao, tvShowDao: TvShowDao, service: TmdbService, @IoDispatcher ioDispatcher: CoroutineDispatcher): TmdbRepository {
        return TmdbRepository(genreDao, movieDao, tvShowDao, service, ioDispatcher)
    }
}