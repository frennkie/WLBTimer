package de.rhab.wlbtimer.activity

import android.os.Bundle
import android.support.annotation.Keep
import android.support.v7.app.AppCompatActivity
import de.rhab.wlbtimer.fragment.HelpPrefFragment


@Keep
class HelpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction().replace(android.R.id.content, HelpPrefFragment()).commit()
    }

}
