package com.tau.ephuapp.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.tau.ephuapp.R
import com.tau.ephuapp.databinding.ActivityCalculatorBinding
import java.lang.Exception
import java.util.*

class Operator{
    companion object{
        const val DIVISION = "/"
        const val MULTIPLICATION = "x"
        const val SUM = "+"
        const val SUSTRACT = "-"
        const val EQUAL = "="
    }
}
enum class Errors{
    ZERO_DIVISION, UNKNOW
}
class CalculatorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCalculatorBinding
    private var currentResult: Double? = 0.0
    private var currentInputText: String = "0"
    private var operationsChain: Stack<String> = Stack<String>()
    private var isEditingCurrentNumber: Boolean = true
    private var hasBeenTotalized: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalculatorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        // operators listeners
        binding.clearAllBtn.setOnClickListener {
            resetAll()
        }
        binding.returnBtn.setOnClickListener {
            setResult(RESULT_OK, Intent().putExtra("result", currentResult))
            finish()
        }
        binding.delBtn.setOnClickListener {
            if(isEditingCurrentNumber){
                currentInputText = when(currentInputText.length > 1){
                    true -> {
                        currentInputText.dropLast(1)
                    }
                    else -> 0.toString()
                }
                updateInputText()
            } else if(operationsChain.isNotEmpty()) {
                operationsChain.removeElementAt(operationsChain.lastIndex)
                executeCalc()
            }
        }
        binding.divisionBtn.setOnClickListener {
            if(isValidNumber(currentInputText) && currentInputText.toDouble() != 0.0) {
                Log.i(TAG, "number is not zero: $currentInputText")
                checkIfIsEditing()
                isEditingCurrentNumber = false
                addOperator(Operator.DIVISION)
                executeCalc()
            }
        }
        binding.productBtn.setOnClickListener {
            if(isValidNumber(currentInputText)) {
                checkIfIsEditing()
                isEditingCurrentNumber = false
                addOperator(Operator.MULTIPLICATION)
                executeCalc()
            }
        }
        binding.sustractBtn.setOnClickListener {
            if(isValidNumber(currentInputText)) {
                checkIfIsEditing()
                isEditingCurrentNumber = false
                addOperator(Operator.SUSTRACT)
                executeCalc()
            }
        }
        binding.aditionBtn.setOnClickListener {
            if(isValidNumber(currentInputText)) {
                checkIfIsEditing()
                isEditingCurrentNumber = false
                addOperator(Operator.SUM)
                executeCalc()
            }
        }
        binding.equalBtn.setOnClickListener {
            checkIfIsEditing()
            isEditingCurrentNumber = false
            hasBeenTotalized = true
            addOperator(Operator.EQUAL)
            executeCalc()
        }
        binding.colonBtn.setOnClickListener {
            if(isEditingCurrentNumber && !currentInputText.contains(".")) {
                currentInputText = currentInputText.plus(".")
                updateInputText()
            }
        }
        binding.absoluteBtn.setOnClickListener {
            if(isEditingCurrentNumber && isValidNumber(currentInputText) && currentInputText.toDouble() != 0.0) {
                currentInputText = (currentInputText.toDouble() * -1).toString()
                updateInputText(true)
            }
        }
        // numbers listeners
        binding.num0Btn.setOnClickListener {
            onInputInitialCheck()
            currentInputText = currentInputText.plus("0")
            updateInputText()
        }
        binding.num1Btn.setOnClickListener {
            onInputInitialCheck()
            currentInputText = currentInputText.plus("1")
            updateInputText(true)
        }
        binding.num2Btn.setOnClickListener {
            onInputInitialCheck()
            currentInputText = currentInputText.plus("2")
            updateInputText(true)
        }
        binding.num3Btn.setOnClickListener {
            onInputInitialCheck()
            currentInputText = currentInputText.plus("3")
            updateInputText(true)
        }
        binding.num4Btn.setOnClickListener {
            onInputInitialCheck()
            currentInputText = currentInputText.plus("4")
            updateInputText(true)
        }
        binding.num5Btn.setOnClickListener {
            onInputInitialCheck()
            currentInputText = currentInputText.plus("5")
            updateInputText(true)
        }
        binding.num6Btn.setOnClickListener {
            onInputInitialCheck()
            currentInputText = currentInputText.plus("6")
            updateInputText(true)
        }
        binding.num7Btn.setOnClickListener {
            onInputInitialCheck()
            currentInputText = currentInputText.plus("7")
            updateInputText(true)
        }
        binding.num8Btn.setOnClickListener {
            onInputInitialCheck()
            currentInputText = currentInputText.plus("8")
            updateInputText(true)
        }
        binding.num9Btn.setOnClickListener {
            onInputInitialCheck()
            currentInputText = currentInputText.plus("9")
            updateInputText(true)
        }
        updateInputText()
    }

    private fun onInputInitialCheck() {
        if(!isEditingCurrentNumber){
            currentInputText = ""
        }
        if(hasBeenTotalized){
            operationsChain.clear()
            updateInputChainText()
            hasBeenTotalized = false
        }
        isEditingCurrentNumber = true
        clearInputIfIsZero()
    }

    private fun addOperator(operator: String) {
        if(operationsChain.isNotEmpty() && operationsChain.lastElement() != operator) {
            if(!isValidNumber(operationsChain.lastElement())){
                operationsChain.removeElementAt(operationsChain.lastIndex)
            }
            operationsChain.add(operator)
        }
    }

    private fun checkIfIsEditing() {
        if(isEditingCurrentNumber){
            val numStr = sanitizeNumberText(currentInputText)
            Log.i(TAG, "adding $numStr to the chain...")
            operationsChain.add(numStr)
            //isEditingCurrentNumber = false
        } else if(hasBeenTotalized) {
            operationsChain.clear()
            val numStr = sanitizeNumberText(currentInputText)
            Log.i(TAG, "adding $numStr to the chain...")
            operationsChain.add(numStr)
            updateInputChainText()
            hasBeenTotalized = false
        }
    }

    fun updateInputText(sanitizeNumber: Boolean = false){
        Log.i(TAG, "currentInputText: $currentInputText")
        binding.currentNumber.text = if(!sanitizeNumber) {
            currentInputText
        } else {
            sanitizeNumberText(currentInputText)
        }
    }

    private fun sanitizeNumberText(numberStr: String): String {
        Log.i(TAG, "sanitizing string: $numberStr")
        val sanitizedStr: String =  if(numberStr.isEmpty()){
            "0"
        } else if(numberStr.contains('.') && isValidNumber(numberStr.substringAfter('.')) &&
                numberStr.substringAfter('.').toDouble() == 0.0){
            numberStr.substringBefore('.')
        } else if(numberStr.last() == '.'){
            numberStr.dropLast(1)
        } else {
            numberStr
        }
        Log.i(TAG, "sanitized string: $sanitizedStr")
        return sanitizedStr
    }

    fun updateInputChainText(){
        if(operationsChain.isNotEmpty()) {
            binding.operationsChain.text = operationsChain.reduce { acc, s ->
                acc.plus(s)
            }
        } else {
            binding.operationsChain.text = ""
        }
    }

    fun executeCalc(){
        var result: Double? = null
        var operandA: Double? = null
        var operandB: Double? = null
        var operator: String? = null
        var errorFound: Errors? = null
        updateInputChainText()
        if(operationsChain.isNotEmpty()) {
            if (operationsChain.size < 3) {
                Log.i(TAG, "operationsChain: $operationsChain")
                val first = operationsChain.first()
                Log.i(TAG, "first: $first")
                result = first.toDouble()
            } else {
                for (number in operationsChain) {
                    if (operandA == null && isValidNumber(number)) {
                        operandA = number.toDouble()
                        result = operandA
                    } else if (operator == null && !isValidNumber(number)) {
                        operator = number
                    } else if (operandB == null && isValidNumber(number)) {
                        operandB = number.toDouble()
                    }
                    if (operandA != null && operandB != null && operator != null) {
                        result = when (operator) {
                            "+" -> operandA + operandB
                            "-" -> operandA - operandB
                            "x" -> operandA * operandB
                            "/" -> {
                                if (operandB != 0.0) {
                                    operandA / operandB
                                } else {
                                    errorFound = Errors.ZERO_DIVISION
                                    null
                                }
                            }
                            else -> 0.0
                        }
                        operandA = result
                        operandB = null
                        operator = null
                    }
                    if (errorFound != null) {
                        showError(errorFound)
                        break
                    }
                }
            }
        }
        if(errorFound == null) {
            currentResult = result ?: 0.0
            currentInputText = currentResult.toString()
            updateInputText(true)
        }
    }

    private fun showError(error: Errors) {
        resetAll()
        binding.operationsChain.text = when(error){
            Errors.ZERO_DIVISION -> getString(R.string.zero_division_error)
            else -> getString(R.string.unknown_error)
        }
        binding.currentNumber.text = getString(R.string.error)
    }

    fun isValidNumber(text: String): Boolean{
        return try {
            text.toDouble()
            true
        } catch (e: Exception){
            false
        }
    }

    fun resetAll(){
        currentResult = 0.0
        currentInputText = "0"
        operationsChain.clear()
        isEditingCurrentNumber = false
        hasBeenTotalized = false
        updateInputText()
        updateInputChainText()
    }

    fun clearInputIfIsZero(){
        if(currentInputText == "0"){
            currentInputText = ""
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    companion object{
        private const val TAG = "CalculatorActivity"
    }
}