package com.example.waterintakecalculator

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var rgUnit: RadioGroup
    private lateinit var rbMetric: RadioButton
    private lateinit var tvWeightLabel: TextView
    private lateinit var etWeight: EditText
    private lateinit var etExercise: EditText
    private lateinit var btnFormulaInfo: TextView
    private lateinit var tvTargetMl: TextView
    private lateinit var tvTargetAltUnits: TextView
    private lateinit var tvProgressLabel: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnAddGlass: LinearLayout
    private lateinit var btnAddBottle: LinearLayout
    private lateinit var tvCupLabel: TextView
    private lateinit var tvBottleLabel: TextView
    private lateinit var btnCustomAdd: Button
    private lateinit var btnUndo: Button
    private lateinit var btnResetAll: Button

    private var targetWaterMl: Double = 0.0
    private var currentConsumedMl: Double = 0.0
    private var lastAddedMl: Double = 0.0
    private var hasShownGoalAlert: Boolean = false
    private var isMetric: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // bind views
        rgUnit = findViewById(R.id.rgUnit)
        rbMetric = findViewById(R.id.rbMetric)
        tvWeightLabel = findViewById(R.id.tvWeightLabel)
        etWeight = findViewById(R.id.etWeight)
        etExercise = findViewById(R.id.etExercise)
        btnFormulaInfo = findViewById(R.id.btnFormulaInfo)
        tvTargetMl = findViewById(R.id.tvTargetMl)
        tvTargetAltUnits = findViewById(R.id.tvTargetAltUnits)
        tvProgressLabel = findViewById(R.id.tvProgressLabel)
        progressBar = findViewById(R.id.progressBar)
        btnAddGlass = findViewById(R.id.btnAddGlass)
        btnAddBottle = findViewById(R.id.btnAddBottle)
        tvCupLabel = findViewById(R.id.tvCupLabel)
        tvBottleLabel = findViewById(R.id.tvBottleLabel)
        btnCustomAdd = findViewById(R.id.btnCustomAdd)
        btnUndo = findViewById(R.id.btnUndo)
        btnResetAll = findViewById(R.id.btnResetAll)

        // tunjuk formula
        btnFormulaInfo.setOnClickListener {
            showFormulaDialog()
        }

        // instant calculation (text watcher)
        val inputWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                calculateTargetInstantly()
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        etWeight.addTextChangedListener(inputWatcher)
        etExercise.addTextChangedListener(inputWatcher)

        // unit convert
        rgUnit.setOnCheckedChangeListener { _, checkedId ->
            isMetric = (checkedId == R.id.rbMetric)
            tvWeightLabel.text = if (isMetric) "Weight (kg):" else "Weight (lbs):"
            etWeight.hint = if (isMetric) "e.g. 60" else "e.g. 132"

            if (isMetric) {
                tvCupLabel.text = "+250 ml Glass"
                tvBottleLabel.text = "+500 ml Bottle"
            } else {
                tvCupLabel.text = "+8 fl oz Glass"
                tvBottleLabel.text = "+16 fl oz Bottle"
            }
            calculateTargetInstantly()
        }

        // quick add glass
        btnAddGlass.setOnClickListener {
            if (targetWaterMl <= 0) {
                Toast.makeText(this, "please enter your weight first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val amount = if (isMetric) 250.0 else (8.0 * 29.5735)
            logWater(amount)
        }

        // botol tu
        btnAddBottle.setOnClickListener {
            if (targetWaterMl <= 0) {
                Toast.makeText(this, "please enter your weight first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val amount = if (isMetric) 500.0 else (16.0 * 29.5735)
            logWater(amount)
        }

        // custom amount
        btnCustomAdd.setOnClickListener {
            if (targetWaterMl <= 0) {
                Toast.makeText(this, "please enter your weight first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showCustomAmountDialog()
        }

        // undo
        btnUndo.setOnClickListener {
            if (lastAddedMl > 0 && currentConsumedMl > 0) {
                currentConsumedMl = (currentConsumedMl - lastAddedMl).coerceAtLeast(0.0)
                val undoneAmount = lastAddedMl
                lastAddedMl = 0.0

                if (currentConsumedMl < targetWaterMl) {
                    hasShownGoalAlert = false
                }

                updateProgressDisplay()
                val message = if (isMetric) {
                    "Undid -%.0f ml".format(undoneAmount)
                } else {
                    "Undid -%.1f oz".format(undoneAmount / 29.5735)
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Nothing to undo!", Toast.LENGTH_SHORT).show()
            }
        }

        // reset
        btnResetAll.setOnClickListener {
            resetAll()
        }
    }

    private fun showFormulaDialog() {
        val formulaMessage = if (isMetric) {
            "Daily hydration target is calculated as:\n\n" +
                    "1. Baseline Hydration:\n" +
                    "   Weight (kg) × 35 ml\n\n" +
                    "2. Sweat Loss Replenishment:\n" +
                    "   (Exercise mins ÷ 30) × 350 ml\n\n" +
                    "Formula:\n" +
                    "(Weight × 35) + ((Exercise ÷ 30) × 350)\n\n" +
                    "Example (60 kg & 30 mins):\n" +
                    "(60 × 35) + (1 × 350) = 2,450 ml"
        } else {
            "Daily hydration target is calculated as:\n\n" +
                    "1. Baseline (Normalized):\n" +
                    "   Weight (lbs) converted to kg × 35 ml\n\n" +
                    "2. Sweat Loss Replenishment:\n" +
                    "   (Exercise mins ÷ 30) × 350 ml\n\n" +
                    "Total is converted to fluid ounces (fl oz)."
        }

        AlertDialog.Builder(this)
            .setTitle("Hydration Formula")
            .setMessage(formulaMessage)
            .setPositiveButton("Got It!", null)
            .show()
    }

    private fun showCustomAmountDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = if (isMetric) "Enter amount in ml (e.g. 350)" else "Enter amount in oz (e.g. 12)"
            setPadding(50, 40, 50, 40)
        }

        AlertDialog.Builder(this)
            .setTitle(if (isMetric) "Log Custom Water (ml)" else "Log Custom Water (oz)")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val text = input.text.toString().trim()
                val amount = text.toDoubleOrNull() ?: 0.0
                if (amount > 0) {
                    val amountInMl = if (isMetric) amount else (amount * 29.5735)
                    logWater(amountInMl)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun calculateTargetInstantly() {
        val weightStr = etWeight.text.toString().trim()
        val exerciseStr = etExercise.text.toString().trim()

        if (weightStr.isEmpty()) {
            targetWaterMl = 0.0
            hasShownGoalAlert = false
            updateProgressDisplay()
            return
        }

        var weight = weightStr.toDoubleOrNull() ?: 0.0
        val exercise = exerciseStr.toDoubleOrNull() ?: 0.0

        if (weight <= 0.0) {
            targetWaterMl = 0.0
            hasShownGoalAlert = false
            updateProgressDisplay()
            return
        }

        if (!isMetric) {
            weight *= 0.453592
        }

        // formula: 35 ml per kg + (mins / 30 * 350 ml)
        targetWaterMl = (weight * 35.0) + ((exercise / 30.0) * 350.0)
        updateProgressDisplay()
    }

    private fun updateTargetDisplay(ml: Double) {
        val liters = ml / 1000.0
        val flOz = ml / 29.5735
        val cups = ml / 250.0

        tvTargetMl.text = String.format("%.0f ml", ml)

        if (isMetric) {
            tvTargetAltUnits.text = String.format("%.2f L  •  ~%.1f cups (250ml)", liters, cups)
        } else {
            val cupsImperial = flOz / 8.0
            tvTargetAltUnits.text = String.format("%.1f fl oz  •  ~%.1f cups (8 oz)", flOz, cupsImperial)
        }
    }

    private fun logWater(amountMl: Double) {
        lastAddedMl = amountMl
        currentConsumedMl += amountMl
        updateProgressDisplay()
    }

    private fun updateProgressDisplay() {
        if (targetWaterMl <= 0) {
            progressBar.progress = 0
            tvTargetMl.text = "0 ml"
            tvTargetAltUnits.text = "0.00 L  •  ~0.0 cups"
            tvProgressLabel.text = "Progress: 0 / 0 ml (0%)"
            tvProgressLabel.setTextColor(Color.parseColor("#334155"))
            return
        }

        val rawPercentage = ((currentConsumedMl / targetWaterMl) * 100).toInt()
        progressBar.progress = rawPercentage.coerceIn(0, 100)

        if (currentConsumedMl >= targetWaterMl) {
            tvProgressLabel.setTextColor(Color.parseColor("#059669")) // Emerald Green

            if (currentConsumedMl > targetWaterMl) {
                val surplusMl = currentConsumedMl - targetWaterMl
                if (isMetric) {
                    tvProgressLabel.text = String.format("🎉 Exceeded Goal! %.0f / %.0f ml (%d%%)", currentConsumedMl, targetWaterMl, rawPercentage)
                    tvTargetAltUnits.text = String.format("+%.0f ml beyond daily target", surplusMl)
                } else {
                    val surplusOz = surplusMl / 29.5735
                    val consumedOz = currentConsumedMl / 29.5735
                    val targetOz = targetWaterMl / 29.5735
                    tvProgressLabel.text = String.format("🎉 Exceeded Goal! %.1f / %.1f fl oz (%d%%)", consumedOz, targetOz, rawPercentage)
                    tvTargetAltUnits.text = String.format("+%.1f fl oz beyond daily target", surplusOz)
                }
            } else {
                tvProgressLabel.text = "Daily Goal Achieved yeay!! (100%)"
                updateTargetDisplay(targetWaterMl)
            }

            if (!hasShownGoalAlert) {
                hasShownGoalAlert = true
                showGoalReachedDialog()
            }
        } else {
            tvProgressLabel.setTextColor(Color.parseColor("#334155"))
            updateTargetDisplay(targetWaterMl)

            if (isMetric) {
                tvProgressLabel.text = String.format("Progress: %.0f / %.0f ml (%d%%)", currentConsumedMl, targetWaterMl, rawPercentage)
            } else {
                val consumedOz = currentConsumedMl / 29.5735
                val targetOz = targetWaterMl / 29.5735
                tvProgressLabel.text = String.format("Progress: %.1f / %.1f fl oz (%d%%)", consumedOz, targetOz, rawPercentage)
            }
        }
    }

    private fun showGoalReachedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Hydration Goal Reached!")
            .setMessage("Awesome job! You reached your daily hydration target. Keep glowing and staying healthy!")
            .setPositiveButton("Yay!", null)
            .show()
    }

    private fun resetAll() {
        etWeight.text.clear()
        etExercise.text.clear()
        currentConsumedMl = 0.0
        targetWaterMl = 0.0
        lastAddedMl = 0.0
        hasShownGoalAlert = false
        updateProgressDisplay()
        etWeight.requestFocus()
    }
}