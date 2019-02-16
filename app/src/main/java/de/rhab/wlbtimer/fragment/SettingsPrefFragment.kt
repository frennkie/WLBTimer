package de.rhab.wlbtimer.fragment

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.EditTextPreference
import android.support.annotation.Keep
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import de.rhab.wlbtimer.BuildConfig
import de.rhab.wlbtimer.R
import de.rhab.wlbtimer.activity.SignInActivity
import de.rhab.wlbtimer.model.WlbUser
import java.util.*


@Keep
class SettingsPrefFragment : PreferenceFragmentCompat() {

    private var mAuthListener: FirebaseAuth.AuthStateListener? = null

    private val mAuth = FirebaseAuth.getInstance()

    private val db = FirebaseFirestore.getInstance()

    private lateinit var userSharedPrefs: SharedPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // check user authentication - don't forget onStart() and onStop()
        mAuthListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser == null) {
                startActivity(Intent(activity, SignInActivity::class.java))
            }
        }

        userSharedPrefs = context?.getSharedPreferences(
            "${BuildConfig.APPLICATION_ID}_prefs_${mAuth.currentUser!!.uid}", 0
        )!!

        addPreferencesFromResource(R.xml.pref_settings)

        val prefSettingsAccount = findPreference(getString(R.string.pref_settings_account_key))
        bindPreferenceSummaryToValue(prefSettingsAccount)

        val prefSettingsAccountLast = findPreference(getString(R.string.pref_settings_account_last_sign_in_key))
        bindPreferenceSummaryToValue(prefSettingsAccountLast)

        val prefSettingsAccountNickname = findPreference(getString(R.string.pref_settings_account_nickname_key))
        bindPreferenceSummaryToValue(prefSettingsAccountNickname)

        val prefSettingsWorkDaysSelect = findPreference(getString(R.string.pref_settings_work_days_select_key))
        bindPreferenceSummaryToValue(prefSettingsWorkDaysSelect)

        if (mAuth.currentUser != null) {
            prefSettingsAccount.summary = mAuth.currentUser!!.email
        } else {
            prefSettingsAccount.summary = "test@example.com"
        }

        val userRef = db.collection(WlbUser.FBP).document(mAuth.currentUser!!.uid)

        userRef.addSnapshotListener(EventListener<DocumentSnapshot> { documentSnapshot, e ->
            if (e != null) {
                Log.w(TAG, "get failed with ", e)
                return@EventListener
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {

                val mWlbUser = documentSnapshot.toObject(WlbUser::class.java)!!
                if (mWlbUser.last_sign_in != null) {
                    prefSettingsAccountLast.summary = mWlbUser.last_sign_in!!.toDate().toString()
                }

                if (mWlbUser.settings == null) {
                    // if there are no settings (e.g. first start) then add defaults
                    // ToDo(frennkie) clean up
                    val mDefaultSettings = HashMap<String, Any>()
                    mDefaultSettings[getString(R.string.pref_settings_account_nickname_key)] = "San Francisco"
                    mDefaultSettings[getString(R.string.pref_settings_work_days_select_key)] =
                        listOf("Mo", "Tu", "We", "Th", "Fr")
                    mDefaultSettings["capital"] = false
                    mDefaultSettings["population"] = 860000
                    mDefaultSettings["regions"] = listOf("west_coast", "norcal")
                    userRef.update(WlbUser.FBP_SETTINGS, mDefaultSettings)
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Failed to update Firestore! Error: ", e)
                        }
                        .addOnSuccessListener {
                            Log.d(TAG, "Success")
                        }

                } else {

                    if (mWlbUser.settings != null) {
                        if (mWlbUser.settings!![prefSettingsAccountNickname.key] != null) {
                            val mNickname = mWlbUser.settings!![prefSettingsAccountNickname.key].toString()
                            updatePreference(prefSettingsAccountNickname, mNickname, false)

                        }
                        if (mWlbUser.settings!![prefSettingsWorkDaysSelect.key] != null) {
                            // ToDo(frennkie) this need urgent review.. all settings all called when one is changed
                            // also WorkDay is not ordered - so it could be Mon, Fri, Thu, Tue ...) - might also be an
                            // issue in other languages
                            Log.d(TAG, "WorkDaysSelect called")
//                            val mNickname = mWlbUser.settings!![prefSettingsAccountNickname.key].toString()
//                            updatePreference(prefSettingsAccountNickname, mNickname, false)

                        }
                    }
                }
            }
        })

    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        return when (preference.key) {
            getString(R.string.pref_settings_account_key) -> {
                true
            }
            else -> {
                super.onPreferenceTreeClick(preference)
            }
        }
    }

    private fun updateFirestore(pref: Preference, value: Any) {
        // update Firestore - remote changes (e.g. from another client via Firestore) require reopening activity
        Log.d(TAG, "Writing to Firestore - Key: ${WlbUser.FBP_SETTINGS}.${pref.key} Value: $value")
        val userRef = db.collection(WlbUser.FBP).document(mAuth.currentUser!!.uid)
        userRef.update("${WlbUser.FBP_SETTINGS}.${pref.key}", value)
            .addOnFailureListener { e ->
                Log.w(TAG, "Failed to update Firestore! Error: ", e)
            }
            .addOnSuccessListener {
                Log.d(TAG, "success!")
            }
    }

    private fun updatePreference(preference: Preference, value: Any?, updateFirestore: Boolean) {

        Log.d(TAG, "sBindPref - key: ${preference.key} value: \"$value\" (T: ${preference.javaClass.name})")

        when (preference) {
            is CheckBoxPreference -> {
                val pref: CheckBoxPreference = preference
                val booleanValue = value as Boolean

                if (updateFirestore) {
                    updateFirestore(preference, value)
                }

                userSharedPrefs.edit().putBoolean(pref.key, booleanValue).apply()

                Log.d(TAG, "CheckBox: Update summary (${pref.key})")
                pref.summary = value.toString()

            }

            is android.support.v14.preference.MultiSelectListPreference -> {
                val pref: android.support.v14.preference.MultiSelectListPreference = preference

                if (value != null) {
                    val stringValue = value as Set<*>
                    val mySet = HashSet<String>()

                    for (myString in stringValue) {
                        mySet.add(myString.toString())
                    }

                    if (updateFirestore) {
                        updateFirestore(preference, mySet.toList())
                    }

                    userSharedPrefs.edit().putStringSet(pref.key, mySet).apply()

                    Log.d(TAG, "MultiList: Update summary (${pref.key})")
                    pref.summary = mySet.toList().joinToString()

                }

            }

            is android.support.v7.preference.EditTextPreference -> {
                val pref: android.support.v7.preference.EditTextPreference = preference

                if (value != null) {
                    val stringValue = value.toString()
                    if (!stringValue.isEmpty()) {

                        if (updateFirestore) {
                            updateFirestore(preference, value)
                        }

                        userSharedPrefs.edit().putString(pref.key, stringValue).apply()

                        Log.d(TAG, "EditText: Update summary (${pref.key})")
                        pref.summary = stringValue
                        pref.text = stringValue

                    }
                }
            }

            else -> {
                val pref: Preference = preference

                if (value != null) {
                    val stringValue = value.toString()
                    if (!stringValue.isEmpty()) {

                        if (updateFirestore) {
                            updateFirestore(preference, value)
                        }

                        userSharedPrefs.edit().putString(pref.key, stringValue).apply()

                        Log.d(TAG, "Else: Update summary (${pref.key})")
                        pref.summary = stringValue

                    }
                }
            }
        }


    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private val sBindPreferenceSummaryToValueListener =
        android.support.v7.preference.Preference.OnPreferenceChangeListener { preference, value ->

            updatePreference(preference, value, true)

//                if (preference is ListPreference) {
//                    // For list preferences, look up the correct display value in
//                    // the preference's 'entries' list.
//                    val index = preference.findIndexOfValue(stringValue)
//
//                    // Set the summary to reflect the new value.
//                    preference.setSummary(
//                        if (index >= 0)
//                            preference.entries[index]
//                        else
//                            null
//                    )
//
//                } else if (preference is CheckBoxPreference) {
//                    assert(true)  // TODO(frennkie) NO-OP

//                val boolValue = value as Boolean
//
//                Log.i("FooBar", "fragment: ${preference.fragment}")
//
//                if (boolValue) {
//                    preference.summary = boolValue.toString() + " is on!"
//                } else {
//                    preference.summary = boolValue.toString() + " is off!"
//                }


//            } else if (preference is RingtonePreference) {
//                // For ringtone preferences, look up the correct display value
//                // using RingtoneManager.
//                if (TextUtils.isEmpty(stringValue)) {
//                    // Empty values correspond to 'silent' (no ringtone).
//                    preference.setSummary(R.string.pref_ringtone_silent)
//
//                } else {
//                    val ringtone = RingtoneManager.getRingtone(
//                            preference.getContext(), Uri.parse(stringValue))
//
//                    if (ringtone == null) {
//                        // Clear the summary if there was a lookup error.
//                        preference.setSummary(null)
//                    } else {
//                        // Set the summary to reflect the new ringtone display
//                        // title.
//                        val title = ringtone.getTitle(preference.getContext())
//                        preference.setSummary(title)
//                    }
//                }
//
//                } else {
//                    // For all other preferences, set the summary to the value's
//                    // simple string representation.
//                    preference.summary = stringValue
//                }
            true
        }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see .sBindPreferenceSummaryToValueListener
     */
    private fun bindPreferenceSummaryToValue(mPreference: Preference) {
        // Set the listener to watch for value changes.
        mPreference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener

        // Trigger the listener immediately with the preference's current value.
        when (mPreference) {
            is CheckBoxPreference -> {
                val pref: CheckBoxPreference = mPreference
                sBindPreferenceSummaryToValueListener.onPreferenceChange(
                    mPreference,
                    userSharedPrefs.getBoolean(pref.key, false)
                )

            }
            is EditTextPreference -> {
                val pref: EditTextPreference = mPreference
                sBindPreferenceSummaryToValueListener.onPreferenceChange(
                    mPreference,
                    userSharedPrefs.getString(pref.key, "")
                )
            }

            is android.support.v7.preference.EditTextPreference -> {
                val pref: android.support.v7.preference.EditTextPreference = mPreference
                sBindPreferenceSummaryToValueListener.onPreferenceChange(
                    mPreference,
                    userSharedPrefs.getString(pref.key, "")
                )
            }

            is android.support.v14.preference.MultiSelectListPreference -> {
                val pref: Preference = mPreference
                sBindPreferenceSummaryToValueListener.onPreferenceChange(
                    mPreference,
                    userSharedPrefs.getStringSet(pref.key, null)
                )
            }

            else -> {
                val pref: Preference = mPreference
                sBindPreferenceSummaryToValueListener.onPreferenceChange(
                    mPreference,
                    userSharedPrefs.getString(pref.key, "")
                )
            }
        }

    }


    override fun onStart() {
        super.onStart()
        mAuth.addAuthStateListener(mAuthListener!!)
    }

    override fun onStop() {
        super.onStop()
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener!!)
        }

    }

    companion object {

        private const val TAG = "SettingsPrefFragment"

    }

}
