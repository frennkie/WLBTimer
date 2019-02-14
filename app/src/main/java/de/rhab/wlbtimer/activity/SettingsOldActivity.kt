package de.rhab.wlbtimer.activity

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.preference.*
import android.view.MenuItem
import sharefirebasepreferences.crysxd.de.lib.SharedFirebasePreferences
import sharefirebasepreferences.crysxd.de.lib.SharedFirebasePreferences.NAME_PLACEHOLDER
import sharefirebasepreferences.crysxd.de.lib.SharedFirebasePreferences.UID_PLACEHOLDER
import sharefirebasepreferences.crysxd.de.lib.SharedFirebasePreferencesContextWrapper
import java.util.*
import android.preference.ListPreference
import android.support.v7.app.AlertDialog
import android.util.Log
import android.widget.ListView
import de.rhab.wlbtimer.R


/**
 * A [PreferenceActivity] that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 *
 *
 * See [Settings API Guide](http://developer.android.com/guide/topics/ui/settings.html) for more
 * information on developing a Settings UI.
 */
class SettingsOldActivity : AppCompatPreferenceActivity() {

    // https://github.com/crysxd/shared-firebase-preferences
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(SharedFirebasePreferencesContextWrapper(newBase))
        SharedFirebasePreferences.setPathPattern(String.format(Locale.ENGLISH,
                "users/%s/%s", UID_PLACEHOLDER, NAME_PLACEHOLDER))
        SharedFirebasePreferences.getDefaultInstance(this).keepSynced(true)
    }

    /**
     * {@inheritDoc}
     */
    override fun onIsMultiPane(): Boolean {
        return isXLargeTablet(this)
    }

    /**
     * {@inheritDoc}
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    override fun onBuildHeaders(target: List<PreferenceActivity.Header>) {
        loadHeadersFromResource(R.xml.pref_headers, target)
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    override fun isValidFragment(fragmentName: String): Boolean {
        return (PreferenceFragment::class.java.name == fragmentName
                || GeneralPreferenceFragment::class.java.name == fragmentName
                || DataSyncPreferenceFragment::class.java.name == fragmentName)
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    class GeneralPreferenceFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_general)
            setHasOptionsMenu(true)

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("example_text"))
            bindPreferenceSummaryToValue(findPreference("default_category_work"))
//            bindPreferenceSummaryToValue(findPreference("example_list"))

            // ToDo(frennkie) Do something here (e.g. write summary
            // to work_days like: Mo, Tu, We, Th, Fr )
//            bindPreferenceSummaryToValue(findPreference("work_days"))
//            bindPreferenceSummaryToValue(findPreference("wd_monday"))
//            bindPreferenceSummaryToValue(findPreference("wd_tuesday"))
//            bindPreferenceSummaryToValue(findPreference("wd_wednesday"))
//            bindPreferenceSummaryToValue(findPreference("wd_thursday"))
//            bindPreferenceSummaryToValue(findPreference("wd_friday"))
//            bindPreferenceSummaryToValue(findPreference("wd_saturday"))
//            bindPreferenceSummaryToValue(findPreference("wd_sunday"))

        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            val id = item.itemId
            if (id == android.R.id.home) {
                startActivity(Intent(activity, SettingsOldActivity::class.java))
                return true
            }
            return super.onOptionsItemSelected(item)
        }

        // Hide divider of PreferenceFragment
        override fun onActivityCreated(savedInstanceState: Bundle?) {
            super.onActivityCreated(savedInstanceState)

            val listView = view.findViewById<ListView>(android.R.id.list)
            if (listView != null) {
                listView.divider = null
            }
        }
    }


/*


    /**
     * This fragment shows category preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    class CategoriesPreferenceFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_categories)
            setHasOptionsMenu(true)

            val listPreference = findPreference("pref_default_language") as ListPreference
//            setListPreferenceData(listPreference)
            setListPreferenceCategoryWorkData(listPreference)
            bindPreferenceSummaryToValue(findPreference("pref_default_language"))

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("categories_default_category"))

        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            val id = item.itemId
            if (id == android.R.id.home) {
                startActivity(Intent(activity, SettingsActivity::class.java))
                return true
            }
            return super.onOptionsItemSelected(item)
        }

        // Hide divider of PreferenceFragment
        override fun onActivityCreated(savedInstanceState: Bundle?) {
            super.onActivityCreated(savedInstanceState)

            val listView = view.findViewById<ListView>(android.R.id.list)
            if (listView != null) {
                listView.divider = null
            }
        }


        private fun setListPreferenceCategoryWorkData(listPreference: ListPreference) {

            // get current currentUser
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {

//                val database = FirebaseDatabase.getInstance()
//                val myCategoryWorkEntriesRef = database.reference
//                        .child(WlbUser.FBP)
//                        .child(user.uid)
//                        .child("category_work")
//
////                var categoryWorkEntries: Array<String>? = arrayOf()
//                var categoryWorkEntries: Array<String>? = arrayOf("foo", "bar")
//
//                // Read from the database
//                myCategoryWorkEntriesRef.addValueEventListener(object : ValueEventListener {
//                    override fun onDataChange(dataSnapshot: DataSnapshot) {
//                        // This method is called once with the initial value and again
//                        // whenever data at this location is updated.
//
//                        for (postSnapshot in dataSnapshot.children) {
//                            // TODO: handle the post
//                            val value = postSnapshot.getValue(Category::class.java)
//                            if (value != null) {
//                                Log.d("FooBar0", "FooBar Value is: $value")
//                                categoryWorkEntries = categoryWorkEntries!!.plus(value.title.toString())
//                                listPreference.entries = listPreference.entries.plus(value.title.toString())
//                                listPreference.entryValues = listPreference.entryValues.plus(value.title.toString())
//                            }
//                        }
//                    }
//
//                    override fun onCancelled(error: DatabaseError) {
//                        // Failed to read value
//                        Log.w("FooBar1", "Failed to read value.", error.toException())
//                    }
//                })
//
//                Log.d("FooBar2", "Size: ${listPreference.entries!!.size}")
//                Log.d("FooBar3", listPreference.entries.toString())
//
//                Log.d("FooBar2", "Size: ${categoryWorkEntries!!.size}")
//                Log.d("FooBar3", categoryWorkEntries.toString())

                listPreference.entries = arrayOf("foo", "bar")
                listPreference.entryValues = arrayOf("foo", "bar")
            }

        }

*/

/*
        /**
         * Updates the ... available to be selected.
         *
         * @param listPreference  The preference to be updated
         */
        private fun setListPreferenceData(listPreference: ListPreference) {
//            listPreference.entries = ApiHandler().getLanguages(activity).keys.toTypedArray()


            // get current currentUser
            val user = FirebaseAuth.getInstance().currentUser

            listPreference.entries = arrayOf("foo", "bar")
            listPreference.setDefaultValue("1")
            listPreference.entryValues = arrayOf("Foo", "Bar")
        }

    }

*/

    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    class DataSyncPreferenceFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_data_sync)
            setHasOptionsMenu(true)

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
//            bindPreferenceSummaryToValue(findPreference("cloud_sync_switch"))
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            val id = item.itemId
            if (id == android.R.id.home) {
                startActivity(Intent(activity, SettingsOldActivity::class.java))
                return true
            }
            return super.onOptionsItemSelected(item)
        }

        // Disable SwitchPreference change state but still listen to click
        override fun onPreferenceTreeClick(preferenceScreen: PreferenceScreen?, preference: Preference): Boolean {
            return when (preference.key) {
                getString(R.string.pref_key_cloud_sync) -> {
                    (preference as SwitchPreference).isChecked = true

                    AlertDialog.Builder(activity)
                            .setTitle("Setting locked")
                            .setMessage("This setting can't be changed.")
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()
                    true
                }
                else -> {
                    super.onPreferenceTreeClick(preferenceScreen, preference)
                }
            }
        }

        // Hide divider of PreferenceFragment
        override fun onActivityCreated(savedInstanceState: Bundle?) {
            super.onActivityCreated(savedInstanceState)

            val listView = view.findViewById<ListView>(android.R.id.list)
            if (listView != null) {
                listView.divider = null
            }
        }
    }

    companion object {

        const val PREFS_NAME = "WLBTimer_Settings"

        /**
         * A preference value change listener that updates the preference's summary
         * to reflect its new value.
         */
        private val sBindPreferenceSummaryToValueListener = Preference.OnPreferenceChangeListener { preference, value ->
            Log.d("FooBar", "Key: ${preference.key} - Value $value")

            val stringValue = value.toString()

            if (preference is ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                val index = preference.findIndexOfValue(stringValue)

                // Set the summary to reflect the new value.
                preference.setSummary(
                        if (index >= 0)
                            preference.entries[index]
                        else
                            null)

            } else if (preference is CheckBoxPreference) {
                assert(true)  // TODO(frennkie) NO-OP

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

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.summary = stringValue
            }
            true
        }

        /**
         * Helper method to determine if the device has an extra-large screen. For
         * example, 10" tablets are extra-large.
         */
        private fun isXLargeTablet(context: Context): Boolean {
            return context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_XLARGE
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
        private fun bindPreferenceSummaryToValue(preference: Preference) {
            // Set the listener to watch for value changes.
            preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener

            // Trigger the listener immediately with the preference's
            // current value.
            if (preference is CheckBoxPreference) {
                sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                        PreferenceManager
                                .getDefaultSharedPreferences(preference.context)
                                .getBoolean(preference.key, false))
            } else {
                sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                        PreferenceManager
                                .getDefaultSharedPreferences(preference.context)
                                .getString(preference.key, ""))
            }
        }
    }
}
