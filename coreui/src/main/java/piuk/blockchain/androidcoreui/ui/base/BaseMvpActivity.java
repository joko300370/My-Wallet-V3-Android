package piuk.blockchain.androidcoreui.ui.base;

import android.os.Bundle;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;

import piuk.blockchain.androidcoreui.BuildConfig;
import piuk.blockchain.androidcoreui.utils.logging.Logging;

@Deprecated // "Use the kotlin-friendly MvpActivity, MvpPresenter, MvpView instead"
public abstract class BaseMvpActivity<VIEW extends View, PRESENTER extends BasePresenter<VIEW>>
    extends BaseAuthActivity {

    private PRESENTER presenter;

    @CallSuper
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        presenter = createPresenter();
        presenter.initView(getView());

        logScreenView();
    }

    /**
     * Allows us to disable logging of screen viewing on unimportant pages.
     */
    protected void logScreenView() {
        if (!BuildConfig.DEBUG) {
            Logging.INSTANCE.logContentView(getClass().getSimpleName());
        }
    }

    @CallSuper
    @Override
    protected void onPause() {
        super.onPause();
        presenter.onViewPaused();
    }

    @CallSuper
    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.onViewDestroyed();
        presenter = null;
    }

    protected void onViewReady() {
        presenter.onViewReady();
    }

    protected PRESENTER getPresenter() {
        return presenter;
    }

    protected abstract PRESENTER createPresenter();

    protected abstract VIEW getView();
}
