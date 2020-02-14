package piuk.blockchain.android.ui.dashboard.announcements

import io.reactivex.android.schedulers.AndroidSchedulers
import org.koin.dsl.context.Context
import org.koin.dsl.context.ParameterProvider
import org.koin.dsl.module.applicationContext
import piuk.blockchain.android.ui.dashboard.announcements.rule.BackupPhraseAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.BitpayAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.BuyBitcoinAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.IntroTourAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.KycForAirdropsAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.StxCompleteAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.KycIncompleteAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.KycMoreInfoAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.KycResubmissionAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.PaxAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.PitAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.RegisterFingerprintsAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.RegisteredForAirdropMiniAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.SimpleBuyPendingBuyAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.SimpleBuyFinishSignupAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.SwapAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.TransferBitcoinAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.TwoFAAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.VerifyEmailAnnouncement

val dashboardAnnouncementsModule = applicationContext {

    context("Payload") {

        bean {
            val availableAnnouncements = getAllAnnouncements(this@context, it)

            AnnouncementList(
                mainScheduler = AndroidSchedulers.mainThread(),
                availableAnnouncements = availableAnnouncements,
                orderAdapter = get(),
                dismissRecorder = get()
            )
        }

        factory {
            AnnouncementConfigAdapterImpl(
                config = get()
            )
        }.bind(AnnouncementConfigAdapter::class)

        factory {
            AnnouncementQueries(
                nabuToken = get(),
                settings = get(),
                nabu = get(),
                tierService = get(),
                sbStateFactory = get()
            )
        }

        factory {
            KycResubmissionAnnouncement(
                kycTiersQueries = get(),
                dismissRecorder = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            KycIncompleteAnnouncement(
                kycTiersQueries = get(),
                sunriverCampaignRegistration = get(),
                dismissRecorder = get(),
                mainScheduler = AndroidSchedulers.mainThread()
            )
        }.bind(AnnouncementRule::class)

        factory {
            KycMoreInfoAnnouncement(
                tierService = get(),
                coinifyWalletService = get(),
                showPopupFeatureFlag = get("ff_notify_coinify_users_to_kyc"),
                dismissRecorder = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            PitAnnouncement(
                pitLink = get(),
                dismissRecorder = get(),
                featureFlag = get("ff_pit_announcement"),
                analytics = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            PaxAnnouncement(
                analytics = get(),
                dismissRecorder = get(),
                walletStatus = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            IntroTourAnnouncement(
                dismissRecorder = get(),
                prefs = get(),
                analytics = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            BitpayAnnouncement(
                dismissRecorder = get(),
                walletStatus = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            SwapAnnouncement(
                dataManager = get("merge"),
                queries = get(),
                dismissRecorder = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            VerifyEmailAnnouncement(
                dismissRecorder = get(),
                walletSettings = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            TwoFAAnnouncement(
                dismissRecorder = get(),
                walletStatus = get(),
                walletSettings = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            BackupPhraseAnnouncement(
                dismissRecorder = get(),
                walletStatus = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            BuyBitcoinAnnouncement(
                dismissRecorder = get(),
                walletStatus = get(),
                buyDataManager = get(),
                simpleBuyAvailability = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            RegisterFingerprintsAnnouncement(
                dismissRecorder = get(),
                fingerprints = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            TransferBitcoinAnnouncement(
                dismissRecorder = get(),
                walletStatus = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            KycForAirdropsAnnouncement(
                dismissRecorder = get(),
                queries = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            RegisteredForAirdropMiniAnnouncement(
                dismissRecorder = get(),
                queries = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            StxCompleteAnnouncement(
                dismissRecorder = get(),
                queries = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            SimpleBuyFinishSignupAnnouncement(
                dismissRecorder = get(),
                analytics = get(),
                queries = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            SimpleBuyPendingBuyAnnouncement(
                dismissRecorder = get(),
                analytics = get(),
                queries = get()
            )
        }.bind(AnnouncementRule::class)
    }

    bean {
        DismissRecorder(
            prefs = get(),
            clock = get()
        )
    }

    bean {
        object : DismissClock {
            override fun now(): Long = System.currentTimeMillis()
        }
    }.bind(DismissClock::class)
}

fun getAllAnnouncements(koinContext: Context, params: ParameterProvider): List<AnnouncementRule> {
    return koinContext.definitions
        .filter { it.types.contains(AnnouncementRule::class) }
        .map { it.definition.invoke(params) as AnnouncementRule }
}
