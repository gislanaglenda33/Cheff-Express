package com.example.data.mapper

import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

interface RecipeTextTranslator {
    suspend fun translateEnToPt(text: String): String
    suspend fun translateBatchEnToPt(texts: List<String>): List<String>
    suspend fun translatePtToEn(text: String): String
}

object NoOpRecipeTextTranslator : RecipeTextTranslator {
    override suspend fun translateEnToPt(text: String): String = text
    override suspend fun translateBatchEnToPt(texts: List<String>): List<String> = texts
    override suspend fun translatePtToEn(text: String): String =
        CulinaryPtBrLocalizer.toApiSearchQuery(text) ?: text
}

class RemotePtBrTranslator(
    okHttpClient: OkHttpClient
) : RecipeTextTranslator {

    private val client: OkHttpClient = okHttpClient.newBuilder()
        .callTimeout(6, TimeUnit.SECONDS)
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }
    private val enToPtCache = ConcurrentHashMap<String, String>()
    private val ptToEnCache = ConcurrentHashMap<String, String>()

    override suspend fun translateEnToPt(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return ""
        enToPtCache[trimmed]?.let { return it }

        val translated = requestTranslation(trimmed, from = "en", to = "pt") ?: trimmed
        if (translated != trimmed) {
            enToPtCache[trimmed] = translated
        }
        return translated
    }

    override suspend fun translateBatchEnToPt(texts: List<String>): List<String> {
        if (texts.isEmpty()) return emptyList()
        val results = texts.map { it.trim() }.toMutableList()
        val missingIndices = results.indices.filter { idx ->
            val value = results[idx]
            value.isNotBlank() && !enToPtCache.containsKey(value)
        }
        results.forEachIndexed { idx, value ->
            enToPtCache[value]?.let { results[idx] = it }
        }
        if (missingIndices.isEmpty()) return results

        val chunks = missingIndices.chunked(20)
        for (chunkIndices in chunks) {
            val singleLineSafe = chunkIndices.all { !results[it].contains('\n') }
            if (singleLineSafe && chunkIndices.size > 1) {
                val joined = chunkIndices.joinToString("\n") { results[it] }
                val batchTranslated = requestTranslation(joined, from = "en", to = "pt")
                val splitLines = batchTranslated?.split('\n')?.map { it.trim() }
                if (splitLines != null && splitLines.size == chunkIndices.size) {
                    chunkIndices.forEachIndexed { pos, originalIdx ->
                        val src = results[originalIdx]
                        val dst = splitLines[pos].ifBlank { src }
                        enToPtCache[src] = dst
                        results[originalIdx] = dst
                    }
                    continue
                }
            }

            coroutineScope {
                chunkIndices.chunked(4).forEach { wave ->
                    val translatedWave = wave.map { idx ->
                        async { idx to translateEnToPt(results[idx]) }
                    }.awaitAll()
                    translatedWave.forEach { (idx, translated) ->
                        results[idx] = translated
                    }
                }
            }
        }
        return results
    }

    override suspend fun translatePtToEn(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return ""
        CulinaryPtBrLocalizer.toApiSearchQuery(trimmed)?.let { return it }
        ptToEnCache[trimmed]?.let { return it }

        val translated = requestTranslation(trimmed, from = "pt", to = "en") ?: trimmed
        ptToEnCache[trimmed] = translated
        return translated
    }

    private suspend fun requestTranslation(
        text: String,
        from: String,
        to: String
    ): String? {
        return try {
            val formBody = FormBody.Builder()
                .add("q", text)
                .build()

            val request = Request.Builder()
                .url("https://translate.googleapis.com/translate_a/single?client=gtx&sl=$from&tl=$to&dt=t")
                .header("User-Agent", "Mozilla/5.0")
                .post(formBody)
                .build()

            val bodyString = awaitCall(client.newCall(request)) ?: return null
            parseTranslatedSegments(bodyString)
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Exception) {
            null
        }
    }

    private fun parseTranslatedSegments(rawJson: String): String? = runCatching {
        val root = json.parseToJsonElement(rawJson).jsonArray
        val sentences = root.firstOrNull()?.jsonArray ?: return null
        buildString {
            for (element in sentences) {
                val segment = element.jsonArray.firstOrNull()?.jsonPrimitive?.contentOrNull
                if (!segment.isNullOrEmpty()) {
                    append(segment)
                }
            }
        }.trim().takeIf { it.isNotBlank() }
    }.getOrNull()

    private suspend fun awaitCall(call: Call): String? = suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (cont.isActive) cont.resume(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { res ->
                    val body = if (res.isSuccessful) res.body?.string() else null
                    if (cont.isActive) cont.resume(body)
                }
            }
        })
    }
}
