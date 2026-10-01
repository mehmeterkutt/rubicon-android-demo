package com.rubicon.demo

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Process
import com.rubicon.analytics.Analytics
import com.rubicon.demo.config.DemoSettings
import com.rubicon.demo.net.ApiCalls
import com.rubicon.demo.net.FakeBackend
import com.rubicon.demo.sdk.DisabledAnalytics
import com.rubicon.demo.sdk.EventFeed
import com.rubicon.demo.sdk.SdkLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject

/**
 * Creates the SDK the way an app should, once, in `onCreate`, from the saved [DemoSettings]; every
 * process runs this, including `:remote`, where the SDK does nothing (Disk 2d).
 */
class RubiconApp : Application() {
    lateinit var settings: DemoSettings
        private set
    lateinit var analytics: Analytics
        private set
    lateinit var api: ApiCalls
        private set
    lateinit var backend: FakeBackend
        private set

    /** Set when the SDK refused the configuration (it throws when `debug` is on). */
    var startupError: String? = null
        private set

    val feed = EventFeed()
    val sdkLog = SdkLog()

    /** What the app told the SDK about the user; the SDK has no getter for these (Public API 8). */
    private val _person = MutableStateFlow(Person())
    val person: StateFlow<Person> = _person

    override fun onCreate() {
        super.onCreate()
        settings = DemoSettings.load(this)
        analytics =
            try {
                Analytics(this, settings.toConfig(feed, sdkLog))
            } catch (t: Throwable) {
                startupError = "${t.javaClass.simpleName}: ${t.message}"
                DisabledAnalytics()
            }
        api = ApiCalls(analytics, settings.captureHosts, settings.maskPathIds, settings.apiBase, settings.host)
        backend = FakeBackend(settings.host)
        _person.value = Person.load(this)
    }

    fun identified(
        userId: String,
    ) = updatePerson { if (it.userId != null && it.userId != userId) Person(userId = userId) else it.copy(userId = userId) }

    fun registered(
        key: String,
        value: Any?,
    ) = updatePerson { it.copy(superProperties = it.superProperties + (key to value.toString())) }

    fun unregistered(key: String) = updatePerson { it.copy(superProperties = it.superProperties - key) }

    fun personCleared() = updatePerson { Person() }

    private fun updatePerson(change: (Person) -> Person) {
        _person.update(change)
        _person.value.save(this)
    }

    /** Saves [next] and restarts the process, so the SDK is created again with it. */
    fun restart(next: DemoSettings = settings) {
        DemoSettings.save(this, next)
        val launch = packageManager.getLaunchIntentForPackage(packageName)!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(launch)
        Process.killProcess(Process.myPid())
    }

    data class Person(
        val userId: String? = null,
        val superProperties: Map<String, String> = emptyMap(),
    ) {
        fun save(context: Context) {
            val json = JSONObject().put("userId", userId).put("superProperties", JSONObject(superProperties))
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, json.toString()).apply()
        }

        companion object {
            private const val PREFS = "demo_person"
            private const val KEY = "person"

            fun load(context: Context): Person {
                val text = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return Person()
                return runCatching {
                    val o = JSONObject(text)
                    val props = o.getJSONObject("superProperties")
                    Person(if (o.isNull("userId")) null else o.getString("userId"), props.keys().asSequence().associateWith { props.getString(it) })
                }.getOrDefault(Person())
            }
        }
    }
}

val Context.demo: RubiconApp get() = applicationContext as RubiconApp
