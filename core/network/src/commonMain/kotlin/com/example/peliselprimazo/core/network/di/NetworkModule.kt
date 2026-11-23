package com.example.peliselprimazo.core.network.di

import com.example.peliselprimazo.core.network.api.*
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

val networkModule = module {

    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                })
            }
            install(Logging) {
                level = LogLevel.BODY
            }
        }
    }

    single { StreamtapeApi(get()) }
    single { TmdbApi(get(), apiKeyProvider = { get(named("TmdbApiKey")) }) }
    single { ClickaduApi(get()) }
    single { MyBidApi(get()) }
    single { TranslationApi(get()) }
}
