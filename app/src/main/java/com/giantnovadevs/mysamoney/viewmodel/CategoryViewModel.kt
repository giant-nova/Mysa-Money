package com.giantnovadevs.mysamoney.viewmodel


import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.giantnovadevs.mysamoney.data.AppDatabase
import com.giantnovadevs.mysamoney.data.Category
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class CategoryViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.getInstance(app).categoryDao()

    val categories: StateFlow<List<Category>> = dao.getAll().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    fun addCategory(name: String) = viewModelScope.launch { dao.insert(Category(name = name)) }
    fun deleteCategory(category: Category) = viewModelScope.launch { dao.delete(category) }
    fun updateCategory(category: Category) = viewModelScope.launch { dao.update(category) }
}