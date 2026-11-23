package com.example.peliselprimazo.core.database.di

import com.example.peliselprimazo.core.database.PelisDatabase
import com.example.peliselprimazo.core.database.getDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

actual val databaseModule: Module = module {
    single { getDatabase() }
    single { get<PelisDatabase>().userContentDao }
    single { get<PelisDatabase>().movieDao }
    single { get<PelisDatabase>().userDao }
    single { get<PelisDatabase>().searchHistoryDao }
}
