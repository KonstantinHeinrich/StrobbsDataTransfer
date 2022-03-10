package ru.bgidilliya.strobbsdatatransfer

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import android.util.AttributeSet

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }

}

class SummarisedEditTextPreference @JvmOverloads constructor(context: Context,
                                                             attrs: AttributeSet? = null) : EditTextPreference(context, attrs) {

    private var mOnChangeListener: OnPreferenceChangeListener? = null

    init {
        super.setOnPreferenceChangeListener { preference, newValue ->
            summary = newValue as String
            mOnChangeListener?.onPreferenceChange(preference, newValue) ?: true
        }
    }

    override fun onAttachedToHierarchy(preferenceManager: PreferenceManager) {
        super.onAttachedToHierarchy(preferenceManager)
        summary = sharedPreferences.getString(key, null)
    }

    override fun setOnPreferenceChangeListener(
        onPreferenceChangeListener: OnPreferenceChangeListener) {
        mOnChangeListener = onPreferenceChangeListener
    }
}