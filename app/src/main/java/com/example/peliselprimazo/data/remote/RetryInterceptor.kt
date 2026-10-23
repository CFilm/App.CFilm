package com.example.peliselprimazo.data.remote

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException

class RetryInterceptor(private val maxRetries: Int = 3, private val initialBackoffMillis: Long = 1000L) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        var response: Response? = null
        var attempt = 0
        var lastException: IOException? = null

        while (attempt < maxRetries) {
            try {
                response = chain.proceed(request)
                if (response.isSuccessful || response.code !in listOf(500, 502, 503, 504, 408)) {
                    return response
                }
                response.close()
            } catch (e: SocketTimeoutException) {
                lastException = e
                Log.w("RetryInterceptor", "Timeout on attempt ${attempt + 1}, retrying...", e)
            } catch (e: IOException) {
                lastException = e
                Log.w("RetryInterceptor", "IOException on attempt ${attempt + 1}: ${e.message}", e)
            }

            attempt++
            if (attempt < maxRetries) {
                val backoff = initialBackoffMillis * (1L shl (attempt - 1)) // Exponential backoff: 1s, 2s, 4s...
                try {
                    Thread.sleep(backoff)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw IOException("Interrupted during retry backoff")
                }
            }
        }

        if (lastException != null) {
            throw lastException
        }
        return response ?: throw IOException("Max retries reached without successful response")
    }
}
