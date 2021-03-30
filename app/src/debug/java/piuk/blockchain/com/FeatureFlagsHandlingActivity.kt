package piuk.blockchain.com

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.blockchain.featureflags.InternalFeatureFlagApi
import org.koin.android.ext.android.inject
import piuk.blockchain.android.databinding.ActivityLocalFeatureFlagsBinding
import piuk.blockchain.android.ui.customviews.ToastCustom

class FeatureFlagsHandlingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocalFeatureFlagsBinding
    private val internalFlags: InternalFeatureFlagApi by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocalFeatureFlagsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val featureFlags = internalFlags.getAll()
        if (featureFlags.isEmpty()) {
            ToastCustom.makeText(
                this, "There are no local features defined", Toast.LENGTH_SHORT, ToastCustom.TYPE_ERROR
            )
        } else {
            val parent = binding.nestedParent
            featureFlags.entries.forEach { flag ->
                val switch = SwitchCompat(this)
                switch.text = flag.key.name
                switch.isChecked = flag.value
                switch.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        internalFlags.enable(flag.key)
                    } else {
                        internalFlags.disable(flag.key)
                    }
                }
                parent.addView(switch)
            }
        }
    }
}