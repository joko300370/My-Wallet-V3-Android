package piuk.blockchain.androidcore.data.metadata

import com.blockchain.android.testutils.rxInit
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.exceptions.InvalidCredentialsException
import info.blockchain.wallet.keys.MasterKey
import info.blockchain.wallet.metadata.MetadataDerivation
import info.blockchain.wallet.metadata.MetadataInteractor
import info.blockchain.wallet.metadata.data.RemoteMetadataNodes
import io.reactivex.Completable
import io.reactivex.Maybe
import org.amshove.kluent.itReturns
import org.bitcoinj.crypto.HDKeyDerivation
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.extensions.then
import piuk.blockchain.androidcore.utils.extensions.thenMaybe
import java.lang.IllegalStateException

class MetadataManagerTest {

    private lateinit var subject: MetadataManager
    private val payloadDataManager: PayloadDataManager = mock()
    private val metadataInteractor: MetadataInteractor = mock()
    private val metadataDerivation: MetadataDerivation = MetadataDerivation()

    private val seed = "15e23aa73d25994f1921a1256f93f72c"
    private val mockMasterKey: MasterKey = mock {
        on { toDeterministicKey() } itReturns HDKeyDerivation.createMasterPrivateKey(seed.toByteArray())
    }

    private val fakeRemoteMetadata = RemoteMetadataNodes().apply {
        mdid =
            "xprv9vM7oGsyw3AdQdzPjRvPAHCC7hEzUhENoeq59qPxjxL5XsMos78qEd3P6dkPpNt8xgvQTiUXcTjbU" +
                    "nHtKShbGu7X3o7bdbw5yLFGhiaXkVk"
        metadata =
            "xprv9vM7oGsuM9zGW2tneNriS8NJF6DNrZEKvYMXSwP8SJNJRUuX6iXjZLQCCy52cXJKKb6XwWF3vr6mQCyy9d5msL9" +
                    "TrycrBmbPibKd2LhzjDW"
    }.toJson()

    @Suppress("unused")
    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = MetadataManager(
            payloadDataManager,
            metadataInteractor,
            metadataDerivation,
            mock()
        )
        whenever(payloadDataManager.metadataCredentials).thenReturn(MetadataCredentials("8cdf0e8e-c7b1-4a6" +
                "f-acb7-f1681d3abf97",
            "sharedKey",
            "1234"))
    }

    @Test
    fun `attemptMetadataSetup load success`() {
        // Arrange
        whenever(metadataInteractor.loadRemoteMetadata(any())).thenReturn(Maybe.just(fakeRemoteMetadata))
        // Act
        val testObserver = subject.attemptMetadataSetup().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    fun `attemptMetadataSetup load fails without 2nd pw, correct metadata should created and saved`() {
        // Arrange
        whenever(metadataInteractor.loadRemoteMetadata(any())).thenReturn(Maybe.empty())
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(false)
        whenever(payloadDataManager.masterKey).thenReturn(mockMasterKey)
        whenever(metadataInteractor.putMetadata(any(), any())).thenReturn(Completable.complete())

        // Act
        val testObserver = subject.attemptMetadataSetup().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        Mockito.verify(payloadDataManager).isDoubleEncrypted
        Mockito.verify(metadataInteractor)
            .putMetadata(eq("{\"metadata\":\"xprv9v8qUhWNur6b9if9MZhw1hvxnsWgonfw9dv1hzVAoCeVpSAWrXd7woo" +
                    "27QqegYXnmYYTDgYjFJTYXTvAu6ZZnS6P7SSkTZTurJ5STEb7952\",\"mdid\":\"xprv9v8qUhWTVjGx3v" +
                    "stKw6J8HV4CNmZB1oTPJGzoyYz9ZK2YcNviZesiy4KhmgyPF635czS66bL8iANAoJbheDbMV7V41Lc9TYg43AE6vF2pFT\"}"),
                any())
    }

    @Test
    fun `attemptMetadataSetup load fails with 2nd pw`() {
        // Arrange
        whenever(metadataInteractor.loadRemoteMetadata(any())).thenReturn(Maybe.empty())
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(true)
        whenever(payloadDataManager.masterKey).thenReturn(mockMasterKey)
        whenever(metadataInteractor.putMetadata(any(), any())).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.attemptMetadataSetup().test()
        // Assert
        testObserver.assertNotComplete()
        testObserver.assertError {
            it is InvalidCredentialsException
        }
        Mockito.verify(payloadDataManager).isDoubleEncrypted
    }

    @Test
    fun `fetch metadata fails when they havent been loaded`() {
        // Arrange
        val testObserver = subject.fetchMetadata(0).test()
        testObserver.assertNotComplete()
        testObserver.assertValueCount(0)
        testObserver.assertError {
            it is IllegalStateException
        }
    }

    @Test
    fun `fetch metadata succeeds when they have loaded`() {
        whenever(metadataInteractor.loadRemoteMetadata(any())).thenReturn(Maybe.just(fakeRemoteMetadata))
        // Arrange

        val test = subject.attemptMetadataSetup()
            .thenMaybe { subject.fetchMetadata(0) }.test()

        test.assertValueAt(0,
            "{\"metadata\":\"xprv9vM7oGsuM9zGW2tneNriS8NJF6DNrZEKvYMXSwP8SJNJRUuX6iXjZLQCCy52cXJKKb6XwWF3vr6mQC" +
                    "yy9d5msL9TrycrBmbPibKd2LhzjDW\",\"mdid\":\"xprv9vM7oGsyw3AdQdzPjRvPAHCC7hEzUhENoeq59qPxjxL5XsMos" +
                    "78qEd3P6dkPpNt8xgvQTiUXcTjbUnHtKShbGu7X3o7bdbw5yLFGhiaXkVk\"}")
        test.assertComplete()
    }

    @Test
    fun `save metadata succeeds when they have loaded`() {
        whenever(metadataInteractor.loadRemoteMetadata(any())).thenReturn(Maybe.just(fakeRemoteMetadata))
        whenever(metadataInteractor.putMetadata(any(), any())).thenReturn(Completable.complete())
        // Arrange
        val test =
            subject.attemptMetadataSetup()
                .then { subject.saveToMetadata("metadata save payload", 0) }
                .test()

        test.assertComplete()
    }

    @Test
    fun `save metadata fails when put metadata fails`() {
        whenever(metadataInteractor.loadRemoteMetadata(any())).thenReturn(Maybe.just(fakeRemoteMetadata))
        whenever(metadataInteractor.putMetadata(any(), any())).thenReturn(Completable.error(IllegalStateException()))
        // Arrange
        val test = subject.attemptMetadataSetup()
            .then { subject.saveToMetadata("metadata save payload", 0) }.test()
        test.assertError { it is IllegalStateException }
    }
}