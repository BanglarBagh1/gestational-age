package com.example.gestationalagecalculator

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private var selectedLMP: LocalDate? = null
    private var selectedEDD: LocalDate? = null
    private var selectedRef: LocalDate = LocalDate.now()

    private lateinit var btnPickLMP: Button
    private lateinit var btnPickEDD: Button
    private lateinit var btnPickRef: Button
    private lateinit var btnCalcLmp: Button
    private lateinit var btnCalcEdd: Button
    private lateinit var btnCalcGa: Button
    private lateinit var tvResultLmp: TextView
    private lateinit var tvResultEdd: TextView
    private lateinit var tvResultGa: TextView
    private lateinit var etWeeks: EditText
    private lateinit var etDays: EditText

    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnPickLMP = findViewById(R.id.btnPickLMP)
        btnPickEDD = findViewById(R.id.btnPickEDD)
        btnPickRef = findViewById(R.id.btnPickRef)
        btnCalcLmp = findViewById(R.id.btnCalcLmp)
        btnCalcEdd = findViewById(R.id.btnCalcEdd)
        btnCalcGa = findViewById(R.id.btnCalcGa)
        tvResultLmp = findViewById(R.id.tvResultLmp)
        tvResultEdd = findViewById(R.id.tvResultEdd)
        tvResultGa = findViewById(R.id.tvResultGa)
        etWeeks = findViewById(R.id.etWeeks)
        etDays = findViewById(R.id.etDays)

        btnPickLMP.setOnClickListener { showDatePicker("LMP") { d -> selectedLMP = d; btnPickLMP.text = format(d) } }
        btnPickEDD.setOnClickListener { showDatePicker("EDD") { d -> selectedEDD = d; btnPickEDD.text = format(d) } }
        btnPickRef.setOnClickListener { showDatePicker("Reference") { d -> selectedRef = d; btnPickRef.text = format(d) } }

        btnCalcLmp.setOnClickListener {
            val lmp = selectedLMP
            if (lmp != null) {
                val ga = calculateGAFromLMP(lmp, selectedRef)
                tvResultLmp.text = "GA: ${ga.weeks}w ${ga.days}d\nLMP: ${format(lmp)}\nAs of: ${format(selectedRef)}"
            } else {
                Toast.makeText(this, "Select LMP date first", Toast.LENGTH_SHORT).show()
            }
        }

        btnCalcEdd.setOnClickListener {
            val edd = selectedEDD
            if (edd != null) {
                val ga = calculateGAFromEDD(edd, selectedRef)
                tvResultEdd.text = "GA: ${ga.weeks}w ${ga.days}d\nEDD: ${format(edd)}\nAs of: ${format(selectedRef)}"
            } else {
                Toast.makeText(this, "Select EDD date first", Toast.LENGTH_SHORT).show()
            }
        }

        btnCalcGa.setOnClickListener {
            val wStr = etWeeks.text.toString()
            val dStr = etDays.text.toString()
            if (wStr.isNotEmpty() && dStr.isNotEmpty()) {
                val w = wStr.toIntOrNull()
                val d = dStr.toIntOrNull()
                if (w != null && d != null && w in 0..45 && d in 0..6) {
                    val lmp = calculateLMPFromGA(w, d, selectedRef)
                    val edd = calculateEDDFromGA(w, d, selectedRef)
                    tvResultGa.text = "LMP: ${format(lmp)}\nEDD: ${format(edd)}\nAs of: ${format(selectedRef)}"
                } else {
                    Toast.makeText(this, "Invalid GA (0-45w, 0-6d)", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Enter weeks and days", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun format(date: LocalDate): String = dateFormatter.format(date)

    private fun showDatePicker(title: String, onSelect: (LocalDate) -> Unit) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(title)
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
            onSelect(date)
        }
        picker.show(supportFragmentManager, title)
    }

    private data class GAResult(val weeks: Int, val days: Int)

    private fun calculateGAFromLMP(lmp: LocalDate, asOf: LocalDate): GAResult {
        val days = java.time.Duration.between(lmp.atStartOfDay(), asOf.atStartOfDay()).toDays().toInt()
        val weeks = (days / 7)
        val remainingDays = days % 7
        return GAResult(weeks.coerceAtLeast(0), remainingDays.coerceAtLeast(0))
    }

    private fun calculateGAFromEDD(edd: LocalDate, asOf: LocalDate): GAResult {
        val daysUntil = java.time.Duration.between(asOf.atStartOfDay(), edd.atStartOfDay()).toDays().toInt()
        val weeks = 40 - (daysUntil / 7)
        val remainingDays = 7 - (daysUntil % 7)
        // Adjust when remaining is 7
        val finalDays = if (remainingDays == 7) 0 else remainingDays
        val finalWeeks = if (remainingDays == 7) weeks + 1 else weeks
        return GAResult(finalWeeks.coerceIn(0, 45), finalDays.coerceIn(0, 6))
    }

    private fun calculateLMPFromGA(weeks: Int, days: Int, asOf: LocalDate): LocalDate {
        val totalDays = weeks * 7 + days
        return asOf.minusDays(totalDays.toLong())
    }

    private fun calculateEDDFromGA(weeks: Int, days: Int, asOf: LocalDate): LocalDate {
        val totalDays = (40 - weeks) * 7 - days
        return asOf.plusDays(totalDays.toLong())
    }
}
