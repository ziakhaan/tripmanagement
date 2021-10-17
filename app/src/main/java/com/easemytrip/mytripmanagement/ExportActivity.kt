package com.easemytrip.mytripmanagement

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.easemytrip.mytripmanagement.databinding.ActivityExportBinding
import com.easemytrip.mytripmanagement.preferences.LocationPreferences

class ExportActivity : AppCompatActivity() {

    lateinit var viewBiniding: ActivityExportBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBiniding = DataBindingUtil.setContentView(this, R.layout.activity_export)
        viewBiniding.tvExport.setText(LocationPreferences.getPreferenceAsJson(this))
    }
}