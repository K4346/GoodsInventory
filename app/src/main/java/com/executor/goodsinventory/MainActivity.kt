package com.executor.goodsinventory

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.executor.goodsinventory.databinding.ActivityMainBinding
import com.executor.goodsinventory.domain.utils.UtilsObject
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        UtilsObject.initModels(application)

        val binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        navView.setupWithNavController(navController)
    }

    private fun deleteAllContent(file: File, vararg filename: String) {
        if (file.isDirectory) for (child in file.listFiles()) deleteAllContent(child)
        for (fn in filename) if (file.name
                .equals(filename)
        ) file.delete()
    }

    override fun onDestroy() {
        getExternalFilesDir(null)?.let { deleteAllContent(it) }
        super.onDestroy()
    }
}