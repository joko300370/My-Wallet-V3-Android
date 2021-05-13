package piuk.blockchain.android.ui.customviews

import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.util.Timer
import java.util.TimerTask

class EditTextUpdateThrottle(
    private val updateFn: (Editable?) -> Unit,
    private val updateDelayMillis: Long = DEFAULT_UPDATE_DELAY
) : TextWatcher {

    private var timer: Timer? = null

    private val handler: Handler = Handler(Looper.getMainLooper())

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        // do nothing
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        timer?.cancel()
    }

    override fun afterTextChanged(s: Editable?) {
        timer?.cancel()
        timer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    handler.post { updateFn.invoke(s) }
                }
            }, updateDelayMillis)
        }
    }

    companion object {
        private const val DEFAULT_UPDATE_DELAY: Long = 300L
    }
}

fun EditText.installUpdateThrottle(updateFn: (Editable?) -> Unit) =
    addTextChangedListener(EditTextUpdateThrottle(updateFn))
