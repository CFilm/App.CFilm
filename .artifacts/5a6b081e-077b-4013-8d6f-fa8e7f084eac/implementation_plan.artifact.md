# Plan de Implementación: Paging 3 con Room para Listas Masivas

Este plan detalla la ampliación de consultas Paging 3 en `MovieDao` y `MovieRepository` para optimizar el rendimiento y el consumo de memoria en catálogos extensos de películas y series.

## User Review Required

> [!IMPORTANT]
> **Optimización de Memoria (Paging 3):**
> - Se añadirán consultas `PagingSource` filtradas por tipo (`MOVIE`, `TV`) en `MovieDao`.
> - Se expondrán en `MovieRepository` para catálogos masivos.

## Proposed Changes

### 1. [MODIFY] [MovieDao.kt](file:///C:/Users/Admin/AndroidStudioProjects/CFilm1/app/src/main/java/com/example/peliselprimazo/data/local/dao/MovieDao.kt)
- Añadir `getPagedMoviesByType(type: String): PagingSource<Int, MovieEntity>`.

### 2. [MODIFY] [MovieRepository.kt](file:///C:/Users/Admin/AndroidStudioProjects/CFilm1/app/src/main/java/com/example/peliselprimazo/domain/repository/MovieRepository.kt) & [MovieRepositoryImpl.kt](file:///C:/Users/Admin/AndroidStudioProjects/CFilm1/app/src/main/java/com/example/peliselprimazo/data/repository/MovieRepositoryImpl.kt)
- Añadir métodos para obtener contenido paginado por tipo de contenido.

## Verification Plan

### Automated Tests
- Compilación exitosa del proyecto con `gradle_build`.
