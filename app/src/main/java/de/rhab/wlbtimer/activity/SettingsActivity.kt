package de.rhab.wlbtimer.activity

import android.os.Bundle
import android.support.annotation.Keep
import android.support.v7.app.AppCompatActivity
import de.rhab.wlbtimer.fragment.SettingsPrefFragment


@Keep
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction().replace(android.R.id.content, SettingsPrefFragment()).commit()
    }

}
