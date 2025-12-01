package ru.olegkravtsov.markbucks

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.documentfile.provider.DocumentFile
import com.google.android.material.button.MaterialButtonToggleGroup
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var radioGroupType: MaterialButtonToggleGroup  // Changed type
    private lateinit var editTextAmount: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var buttonAdd: Button

    private lateinit var expenseCategories: Array<String>
    private lateinit var incomeCategories: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)

        if (sharedPreferences.getString("folder_uri", null) == null) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }

        expenseCategories = resources.getStringArray(R.array.expense_categories)
        incomeCategories = resources.getStringArray(R.array.income_categories)

        initViews()
        setupCategorySpinner()
    }

    private fun initViews() {
        radioGroupType = findViewById(R.id.radioGroupType)  // Now this cast is correct
        editTextAmount = findViewById(R.id.editTextAmount)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        buttonAdd = findViewById(R.id.buttonAdd)

        radioGroupType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                updateCategories()
            }
        }

        buttonAdd.setOnClickListener {
            saveTransaction()
        }
    }

    private fun setupCategorySpinner() {
        updateCategories()
    }

    private fun updateCategories() {
        val categories = if (radioGroupType.checkedButtonId == R.id.radioIncome) {
            incomeCategories
        } else {
            expenseCategories
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter
    }

    private fun saveTransaction() {
        val amountText = editTextAmount.text.toString()
        if (amountText.isBlank()) {
            showToast(getString(R.string.enter_amount))
            return
        }

        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            showToast(getString(R.string.invalid_amount))
            return
        }

        val type = if (radioGroupType.checkedButtonId == R.id.radioIncome) "income" else "expense"
        val category = spinnerCategory.selectedItem as String

        saveToMarkdownFile(type, amount, category)
    }

    private fun saveToMarkdownFile(type: String, amount: Double, category: String) {
        try {
            val folderUriString = sharedPreferences.getString("folder_uri", null) ?: run {
                showToast(getString(R.string.folder_not_selected))
                return
            }

            val folderUri = Uri.parse(folderUriString)
            val folder = DocumentFile.fromTreeUri(this, folderUri)

            if (folder == null || !folder.exists()) {
                showToast(getString(R.string.folder_not_found))
                return
            }

            val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
            val fileName = "transaction_${SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())}.md"

            val content = """---
created: $timestamp
type: "$type"
amount: $amount
category: "$category"
---
"""

            val file = folder.createFile("text/markdown", fileName)
            if (file != null) {
                contentResolver.openOutputStream(file.uri)?.use { outputStream ->
                    outputStream.write(content.toByteArray())
                }
                showToast(getString(R.string.transaction_saved))
                editTextAmount.text.clear()
            } else {
                showToast(getString(R.string.file_creation_error))
            }

        } catch (e: Exception) {
            showToast(getString(R.string.save_error, e.message))
            e.printStackTrace()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}