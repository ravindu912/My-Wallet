package com.example.mywallet.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mywallet.data.PreferenceManager
import com.example.mywallet.data.Transaction
import kotlinx.coroutines.launch
import java.util.*

class DashboardViewModel(private val preferenceManager: PreferenceManager) : ViewModel() {
    private val _totalBalance = MutableLiveData<Double>(0.0)
    val totalBalance: LiveData<Double> = _totalBalance

    private val _totalIncome = MutableLiveData<Double>(0.0)
    val totalIncome: LiveData<Double> = _totalIncome

    private val _totalExpense = MutableLiveData<Double>(0.0)
    val totalExpense: LiveData<Double> = _totalExpense

    private val _categorySpending = MutableLiveData<Map<String, Double>>(emptyMap())
    val categorySpending: LiveData<Map<String, Double>> = _categorySpending

    private val _transactions = MutableLiveData<List<Transaction>>(emptyList())
    val transactions: LiveData<List<Transaction>> = _transactions

    init {
        loadDashboardData(System.currentTimeMillis())
    }

    fun loadDashboardData(timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            try {
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = timestamp
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val startOfMonth = calendar.timeInMillis
                calendar.add(Calendar.MONTH, 1)
                val startOfNextMonth = calendar.timeInMillis

                val transactions = preferenceManager.getTransactions()
                    .filter { it.date in startOfMonth until startOfNextMonth }
                
                _transactions.value = transactions
                calculateTotals(transactions)
                calculateCategorySpending(transactions)
            } catch (e: Exception) {
                e.printStackTrace()
                _transactions.value = emptyList()
                _totalBalance.value = 0.0
                _totalIncome.value = 0.0
                _totalExpense.value = 0.0
                _categorySpending.value = emptyMap()
            }
        }
    }

    private fun calculateTotals(transactions: List<Transaction>) {
        try {
            val income = transactions
                .filter { it.type == Transaction.Type.INCOME }
                .sumOf { it.amount }

            val expense = transactions
                .filter { it.type == Transaction.Type.EXPENSE }
                .sumOf { it.amount }

            _totalIncome.value = income
            _totalExpense.value = expense
            _totalBalance.value = income - expense
        } catch (e: Exception) {
            e.printStackTrace()
            _totalIncome.value = 0.0
            _totalExpense.value = 0.0
            _totalBalance.value = 0.0
        }
    }

    private fun calculateCategorySpending(transactions: List<Transaction>) {
        try {
            val incomeByCategory = transactions
                .filter { it.type == Transaction.Type.INCOME }
                .groupBy { it.category }
                .mapValues { it.value.sumOf { transaction -> transaction.amount } }
                .mapKeys { "Income: ${it.key}" }

            val expenseByCategory = transactions
                .filter { it.type == Transaction.Type.EXPENSE }
                .groupBy { it.category }
                .mapValues { it.value.sumOf { transaction -> transaction.amount } }
                .mapKeys { "Expense: ${it.key}" }

            val combinedSpending = incomeByCategory + expenseByCategory
            _categorySpending.value = combinedSpending
        } catch (e: Exception) {
            e.printStackTrace()
            _categorySpending.value = emptyMap()
        }
    }
} 
