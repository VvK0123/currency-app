
package com.example.currencyapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory

class MainActivity : AppCompatActivity() {

    private lateinit var tvUsd: TextView
    private lateinit var tvEur: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvDate: TextView
    private lateinit var btnRefresh: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvUsd = findViewById(R.id.tvUsd)
        tvEur = findViewById(R.id.tvEur)
        tvStatus = findViewById(R.id.tvStatus)
        tvDate = findViewById(R.id.tvDate)
        btnRefresh = findViewById(R.id.btnRefresh)

        btnRefresh.setOnClickListener { loadRates() }
        loadRates()
    }

    private fun loadRates() {
        tvStatus.text = "Загрузка..."
        btnRefresh.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://www.cbr.ru/XML_daily.asp")
                val inputStream = url.openStream()
                
                val factory = DocumentBuilderFactory.newInstance()
                val builder = factory.newDocumentBuilder()
                val doc = builder.parse(inputStream)
                
                val valCurs = doc.documentElement
                val dateStr = valCurs.getAttribute("Date")
                
                val usdRate = findRateByCharCode(doc, "USD")
                val eurRate = findRateByCharCode(doc, "EUR")

                withContext(Dispatchers.Main) {
                    tvUsd.text = String.format("💵 USD: %.4f ₽", usdRate)
                    tvEur.text = String.format("💶 EUR: %.4f ₽", eurRate)
                    tvDate.text = "Курс на: $dateStr"
                    tvStatus.text = "Обновлено: ${SimpleDateFormat("HH:mm:ss").format(Date())}"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvUsd.text = "USD: ошибка"
                    tvEur.text = "EUR: ошибка"
                    tvStatus.text = "Нет соединения"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    btnRefresh.isEnabled = true
                }
            }
        }
    }

    private fun findRateByCharCode(doc: org.w3c.dom.Document, charCode: String): Double {
        val valutes = doc.getElementsByTagName("Valute")
        for (i in 0 until valutes.length) {
            val valute = valutes.item(i) as org.w3c.dom.Element
            val code = valute.getElementsByTagName("CharCode").item(0).textContent
            if (code == charCode) {
                val nominal = valute.getElementsByTagName("Nominal").item(0).textContent.toDouble()
                val value = valute.getElementsByTagName("Value").item(0).textContent.replace(",", ".").toDouble()
                return value / nominal
            }
        }
        return 0.0
    }
}
