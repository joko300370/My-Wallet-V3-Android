package piuk.blockchain.android.ui.kyc.countryselection

import com.blockchain.android.testutils.rxInit
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.NabuCountryResponse
import com.blockchain.kyc.models.nabu.NabuStateResponse
import com.blockchain.kyc.models.nabu.Scope
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.api.data.WalletOptions
import io.reactivex.Single
import org.amshove.kluent.any
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import piuk.blockchain.android.ui.kyc.countryselection.models.CountrySelectionState
import piuk.blockchain.android.ui.kyc.countryselection.util.CountryDisplayModel
import piuk.blockchain.androidbuysell.services.BuyConditions

class KycCountrySelectionPresenterTest {

    private lateinit var subject: KycCountrySelectionPresenter
    private val view: KycCountrySelectionView = mock()
    private val nabuDataManager: NabuDataManager = mock()
    private val buyConditions: BuyConditions = mock()
    private val mockWalletOptions: WalletOptions = mock(defaultAnswer = RETURNS_DEEP_STUBS)

    @Suppress("unused")
    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = KycCountrySelectionPresenter(nabuDataManager, buyConditions)
        subject.initView(view)
    }

    @Test
    fun `onViewReady error loading countries`() {
        // Arrange
        whenever(view.regionType).thenReturn(RegionType.Country)
        whenever(nabuDataManager.getCountriesList(Scope.None)).thenReturn(Single.error { Throwable() })
        // Act
        subject.onViewReady()
        // Assert
        verify(nabuDataManager).getCountriesList(Scope.None)
        verify(view).renderUiState(any(CountrySelectionState.Loading::class))
        verify(view).renderUiState(any(CountrySelectionState.Error::class))
    }

    @Test
    fun `onViewReady loading countries success`() {
        // Arrange
        whenever(view.regionType).thenReturn(RegionType.Country)
        whenever(nabuDataManager.getCountriesList(Scope.None))
            .thenReturn(Single.just(emptyList()))
        // Act
        subject.onViewReady()
        // Assert
        verify(nabuDataManager).getCountriesList(Scope.None)
        verify(view).renderUiState(any(CountrySelectionState.Loading::class))
        verify(view).renderUiState(any(CountrySelectionState.Data::class))
    }

    @Test
    fun `onViewReady loading states success`() {
        // Arrange
        whenever(view.regionType).thenReturn(RegionType.State)
        whenever(nabuDataManager.getStatesList("US", Scope.None))
            .thenReturn(Single.just(emptyList()))
        // Act
        subject.onViewReady()
        // Assert
        verify(nabuDataManager).getStatesList("US", Scope.None)
        verify(view).renderUiState(any(CountrySelectionState.Loading::class))
        verify(view).renderUiState(any(CountrySelectionState.Data::class))
    }

    @Test
    fun `onRegionSelected requires state selection`() {
        // Arrange
        whenever(view.regionType).thenReturn(RegionType.Country)
        whenever(nabuDataManager.getCountriesList(Scope.None))
            .thenReturn(Single.just(emptyList()))
        val countryDisplayModel = CountryDisplayModel(
            name = "United States",
            countryCode = "US"
        )
        // Act
        subject.onRegionSelected(countryDisplayModel)
        // Assert
        verify(nabuDataManager).getCountriesList(Scope.None)
        verify(view).requiresStateSelection()
    }

    @Test
    fun `onRegionSelected in restricted countries`() {
        // Arrange
        whenever(view.regionType).thenReturn(RegionType.Country)
        val countryList =
            listOf(NabuCountryResponse("UK", "United Kingdom", listOf("KYC"), emptyList()))
        whenever(nabuDataManager.getCountriesList(Scope.None))
            .thenReturn(Single.just(countryList))
        whenever(buyConditions.buySellCountries()).thenReturn(Single.just(listOf("US")))
        val countryDisplayModel = CountryDisplayModel(
            name = "United Kingdom",
            countryCode = "UK"
        )
        // Act
        subject.onRegionSelected(countryDisplayModel, campaignType = CampaignType.BuySell)
        // Assert
        verify(view).invalidCountry(countryDisplayModel)
    }

    @Test
    fun `onRegionSelected not in restricted countries`() {
        // Arrange
        whenever(view.regionType).thenReturn(RegionType.Country)
        val countryCode = "UK"
        val countryList =
            listOf(NabuCountryResponse("UK", "United Kingdom", listOf("KYC"), emptyList()))
        whenever(nabuDataManager.getCountriesList(Scope.None))
            .thenReturn(Single.just(countryList))
        whenever(buyConditions.buySellCountries()).thenReturn(Single.just(listOf("UK")))
        val countryDisplayModel = CountryDisplayModel(
            name = "United Kingdom",
            countryCode = "UK"
        )
        // Act
        subject.onRegionSelected(countryDisplayModel, campaignType = CampaignType.BuySell)
        // Assert
        verify(view).continueFlow(countryCode)
    }

    @Test
    fun `onRegionSelected state not found, not in kyc region`() {
        // Arrange
        whenever(view.regionType).thenReturn(RegionType.State)
        whenever(nabuDataManager.getStatesList("US", Scope.None))
            .thenReturn(Single.just(emptyList()))
        val countryDisplayModel = CountryDisplayModel(
            name = "United States",
            countryCode = "US",
            isState = true,
            state = "US-AL"
        )
        // Act
        subject.onRegionSelected(countryDisplayModel)
        // Assert
        verify(nabuDataManager).getStatesList("US", Scope.None)
        verify(view).invalidCountry(countryDisplayModel)
    }

    @Test
    fun `onRegionSelected state found, in kyc region`() {
        // Arrange
        whenever(view.regionType).thenReturn(RegionType.State)
        val countryCode = "US"
        whenever(nabuDataManager.getStatesList("US", Scope.None))
            .thenReturn(
                Single.just(
                    listOf(
                        NabuStateResponse(
                            code = "US-AL",
                            name = "Alabama",
                            scopes = listOf("KYC"),
                            countryCode = "US"
                        )
                    )
                )
            )
        val countryDisplayModel = CountryDisplayModel(
            name = "United States",
            countryCode = "US",
            isState = true,
            state = "US-AL"
        )
        // Act
        subject.onRegionSelected(countryDisplayModel)
        // Assert
        verify(nabuDataManager).getStatesList("US", Scope.None)
        verify(view).continueFlow(countryCode)
    }

    @Test
    fun `onRegionSelected country found`() {
        // Arrange
        whenever(view.regionType).thenReturn(RegionType.Country)
        val countryCode = "UK"
        val countryList =
            listOf(NabuCountryResponse("UK", "United Kingdom", listOf("KYC"), emptyList()))
        whenever(nabuDataManager.getCountriesList(Scope.None))
            .thenReturn(Single.just(countryList))
        val countryDisplayModel = CountryDisplayModel(
            name = "United Kingdom",
            countryCode = "UK"
        )
        // Act
        subject.onRegionSelected(countryDisplayModel)
        // Assert
        verify(nabuDataManager).getCountriesList(Scope.None)
        verify(view).continueFlow(countryCode)
    }

    @Test
    fun `onRegionSelected country found but is US so requires state selection`() {
        // Arrange
        whenever(view.regionType).thenReturn(RegionType.Country)
        val countryList =
            listOf(NabuCountryResponse("US", "United States", listOf("KYC"), emptyList()))
        whenever(nabuDataManager.getCountriesList(Scope.None))
            .thenReturn(Single.just(countryList))
        val countryDisplayModel = CountryDisplayModel(
            name = "United States",
            countryCode = "US"
        )
        // Act
        subject.onRegionSelected(countryDisplayModel)
        // Assert
        verify(nabuDataManager).getCountriesList(Scope.None)
        verify(view).requiresStateSelection()
    }
}
