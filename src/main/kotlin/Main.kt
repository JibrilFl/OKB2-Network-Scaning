package org.example

import org.json.JSONObject
import java.io.File
import java.net.NetworkInterface
import java.net.SocketException
import java.sql.DriverManager

data class Config(val computerName: String)

val url = "urlDB"
val user = "user login DB"
val password = "password"

fun loadConfig(): Config {
    val configFile = File("config.json")
    return if (configFile.exists()) {
        try {
            val json = JSONObject(configFile.readText())
            Config(json.getString("computerName"))
        } catch (e: Exception) {
            println("Error loading config: ${e.message}")
            Config("Без имени") // Имя ПК по умолчанию
        }
    } else {
        Config("Без имени") // Имя ПК по умолчанию
    }
}

fun getNetworkInfo(): List<Pair<String, String?>> {
    val networkInfo = mutableListOf<Pair<String, String?>>()
    try {
        NetworkInterface.getNetworkInterfaces().asSequence().filter {
            try {
                it.isUp && !it.isLoopback && !it.isVirtual
            } catch (e: SocketException) {
                false
            }
        }.forEach { networkInterface ->
            val mac = networkInterface.hardwareAddress?.joinToString(":") { String.format("%02X", it) }

            networkInterface.inetAddresses.asSequence().filter { it is java.net.Inet4Address }.forEach {inetAddress ->
                networkInfo.add(Pair(inetAddress.hostAddress, mac))
            }
        }
    } catch (e: SocketException) {
        println("Error getting network information: ${e.message}")
    }
    return networkInfo
}

fun updateDatabase(ip: String, mac: String, config: Config) {
    try {
        Class.forName("org.mariadb.jdbc.Driver") // Загрузка драйвера MariaDB
        DriverManager.getConnection(url, user, password).use { conn -> // Добавляем имя пользователя и пароль
            conn.autoCommit = false
            val callableStatement  = conn.prepareCall("{call update_device_info(?, ?, ?)}")

            getNetworkInfo().forEach {
                mac?.let {
                    callableStatement.setString(1, config.computerName)
                    callableStatement.setString(2, mac)
                    callableStatement.setString(3, ip)
                    callableStatement.addBatch()
                }
            }

            callableStatement.executeBatch()
            conn.commit()
            conn.autoCommit = true
        }
    } catch (e: Exception) {
        println("Database error: ${e.message}")
    }
}

fun main() {
    val config = loadConfig()
    println("Service started. Computer name: ${config.computerName}")

    while (true) {
        val networkInfo = getNetworkInfo()
        networkInfo.forEachIndexed { id, (ip, mac) ->
            if (id == 0) {
                mac?.let { updateDatabase(ip, it, config) }
            }
        }

        Thread.sleep(60000) // Проверка каждую минуту
    }
}