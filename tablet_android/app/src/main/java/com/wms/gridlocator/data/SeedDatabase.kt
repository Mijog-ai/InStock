package com.wms.gridlocator.data

import at.favre.lib.crypto.bcrypt.BCrypt

suspend fun seedDatabase(repo: Repository) {
    val users = listOf(
        Triple("admin", "admin123", "Admin"),
        Triple("logistics1", "log123", "Logistics"),
        Triple("logistics2", "log123", "Logistics"),
        Triple("consumer1", "con123", "Consumer"),
    )
    for ((u, p, r) in users) {
        if (repo.getUser(u) == null) {
            val hash = BCrypt.withDefaults().hashToString(10, p.toCharArray())
            repo.createUser(u, hash, r)
        }
    }
}
