package com.example.peliselprimazo.core.database.di

import com.example.peliselprimazo.core.database.PelisDatabase
import org.koin.dsl.module

val databaseModule = module {
    single { get<PelisDatabase>().userContentDao }
    single { get<PelisDatabase>().movieDao }
    single { get<PelisDatabase>().userDao }
    single { get<PelisDatabase>().searchHistoryDao }
}
