package org.example

import org.json.JSONObject
import java.io.File
import java.net.NetworkInterface
import java.net.SocketException
import java.sql.DriverManager
import java.io.BufferedReader
import java.io.InputStreamReader

fun isNetworkConnectedWindows(): Boolean {
    val process = Runtime.getRuntime().exec("powershell -Command Get-NetConnectionProfile | Where-Object { \$_.IPv4Connectivity -eq 'Internet' }")
    val reader = BufferedReader(InputStreamReader(process.inputStream))
    var line: String?
    while (reader.readLine().also { line = it } != null) {
        if (line?.contains("Name") == true) {
            return true
        }
    }
    return false
}

fun isNetworkConnectedLinux(): Boolean {
    val process = Runtime.getRuntime().exec("nmcli -t -f STATE general")
    val reader = BufferedReader(InputStreamReader(process.inputStream))
    val line = reader.readLine()
    return line?.trim() == "connected"
}

fun isNetworkConnected(): Boolean {
    val osName = System.getProperty("os.name").toLowerCase()
    return if (osName.contains("win")) {
        isNetworkConnectedWindows()
    } else if (osName.contains("nix") || osName.contains("linux") || osName.contains("nux") || osName.contains("mac")) {
        isNetworkConnectedLinux()
    } else {
        throw UnsupportedOperationException("Unsupported OS: $osName")
    }
}


data class Config(val computerName: String)

fun loadConfig(): Config {
    val configFile = File("config.json")
    return if (configFile.exists()) {
        try {
            val json = JSONObject(configFile.readText())
            Config(json.getString("computerName"))
        } catch (e: Exception) {
            println("Error loading config: ${e.message}")
            Config("Файл config заполнен неверно") // Имя ПК по умолчанию
        }
    } else {
        Config("Не удается открыть файл config") // Имя ПК по умолчанию
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
        DriverManager.getConnection("url", "user", "password").use { conn -> // Добавляем имя пользователя и пароль
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

    var isConnected = false
    var dataSent = false

    val config = loadConfig()
    println("Произведен запуск сервиса")

    while (true) {
        try {
            val newIsConnected = isNetworkConnected()

            if (!isConnected && newIsConnected) {
                println("Соединение установлено")

                if (!dataSent) {
                    val networkInfo = getNetworkInfo()
                    networkInfo.forEachIndexed { id, (ip, mac) ->
                        if (id == 0) {
                            mac?.let { updateDatabase(ip, it, config) }
                        }
                    }

                    dataSent = true
                }

                isConnected = true
            } else if (isConnected && !newIsConnected) {
                println("Соединение разорвано")
                dataSent = false
                isConnected = false
            }

            Thread.sleep(3000)

        } catch (e: Exception) {
            println("Не удается установить соединение")
            e.printStackTrace()
            Thread.sleep(5000)
        }
    }
}