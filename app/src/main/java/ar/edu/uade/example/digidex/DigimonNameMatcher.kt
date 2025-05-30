package ar.edu.uade.example.digidex.util

import kotlin.math.max
import android.util.Log

// Algoritmo para normalizar y emparejar nombres de Digimon entre APIs

fun normalizeName(name: String): String {
    return name.lowercase()
        .replace(Regex("[^a-z0-9]"), "")
}

fun damerauLevenshtein(s1: String, s2: String): Int {
    val lenStr1 = s1.length
    val lenStr2 = s2.length

    val dist = Array(lenStr1 + 1) { IntArray(lenStr2 + 1) }

    for (i in 0..lenStr1) dist[i][0] = i
    for (j in 0..lenStr2) dist[0][j] = j

    for (i in 1..lenStr1) {
        for (j in 1..lenStr2) {
            val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1

            dist[i][j] = minOf(
                dist[i - 1][j] + 1,      // Deletion
                dist[i][j - 1] + 1,      // Insertion
                dist[i - 1][j - 1] + cost // Substitution
            )

            if (i > 1 && j > 1 && s1[i - 1] == s2[j - 2] && s1[i - 2] == s2[j - 1]) {
                dist[i][j] = minOf(dist[i][j], dist[i - 2][j - 2] + cost) // Transposition
            }
        }
    }

    return dist[lenStr1][lenStr2]
}
