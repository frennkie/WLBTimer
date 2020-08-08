package de.rhab.wlbtimer.activity

import android.os.Bundle
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import de.rhab.wlbtimer.fragment.SettingsPrefFragment


@Keep
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction().replace(android.R.id.content, SettingsPrefFragment()).commit()
    }

}
