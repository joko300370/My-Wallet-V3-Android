package info.blockchain.wallet.metadata

import info.blockchain.wallet.metadata.data.MetadataResponse
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.jackson.JacksonConverterFactory

class MetadataInteractorTest {

    private lateinit var metadataInteractor: MetadataInteractor
    private val address = "1JxR3UVbSFShHB7YUnBKc3WGviXzELB7FA"

    private val fakeMetadataResponse = MetadataResponse(
        version = 1,
        payload = "/FE353C/kzLs0kkU7NyOVExrT0yLIGgHYYSMKb8PCVk=",
        signature = "H0SgSn2QqiJAkVW6XuVZOTur6y8KlQ0qbLhK0oL6/PS3fy7TSSBYCEWk3nlYJyQD9IYwYZK5yGFCxQ55asy+3y4=",
        prevMagicHash = null,
        typeId = -1,
        createdAt = 1583754936000,
        updatedAt = 1583754936000,
        address = address
    )

    private val mockWebServer = MockWebServer()
    private val metadataDerivation = MetadataDerivation()
    private val fakeMetadata = Metadata.newInstance(
        metaDataHDNode = metadataDerivation.deserializeMetadataNode("xprv9vM7oGsuM9zGW2tneNriS8NJF6DNrZEK" +
                "vYMXSwP8SJNJRUuX6iXjZLQCCy52cXJKKb6XwWF3vr6mQCyy9d5msL9TrycrBmbPibKd2LhzjDW"),
        type = 6,
        isEncrypted = true,
        encryptionKey = null,
        metadataDerivation = metadataDerivation
    )

    @Before
    fun setUp() {
        val metadataService = Retrofit.Builder()
            .client(OkHttpClient.Builder().build())
            .baseUrl(mockWebServer.url("/").toString())
            .addConverterFactory(JacksonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build().create(MetadataService::class.java)

        metadataInteractor = MetadataInteractor(metadataService)
    }

    @Test
    fun `fetchMagic with success`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(fakeMetadataResponse.toJson())
        )

        val test = metadataInteractor.fetchMagic(address).test()

        test.assertComplete()
        test.assertValueCount(1)
    }

    @Test
    fun `fetchMagic failure, failure is propagated`() {

        val notFoundResponse = "{\"message\":\"not_found\"}"
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody(notFoundResponse)
        )
        val test = metadataInteractor.fetchMagic(address).test()

        test.assertNotComplete()
        test.assertNoValues()
    }

    @Test
    fun `get metadata with error 404, empty should be returned`() {

        val notFoundResponse = "{\"message\":\"not_found\"}"
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody(notFoundResponse)
        )

        val test = metadataInteractor.loadRemoteMetadata(fakeMetadata).isEmpty.test()

        test.assertValueAt(0, true)
        test.assertComplete()
    }

    @Test
    fun `get metadata with error different than 404, error should be returned`() {

        val notFoundResponse = "{\"error\":\"unknown_error\"}"
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody(notFoundResponse)
        )

        val test = metadataInteractor.loadRemoteMetadata(fakeMetadata).test()

        test.assertError {
            it is HttpException && it.code() == 400
        }
        test.assertNotComplete()
    }

    @Test
    fun `get metadata with success`() {

        val fakeMetadataJson = fakeMetadataResponse.toJson()
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(fakeMetadataJson)
        )

        val test = metadataInteractor.loadRemoteMetadata(fakeMetadata).test()
        test.assertComplete()
        test.assertValueCount(1)
        test.assertValueAt(0, "{\"trades\":[]}")
    }

    @Test
    fun `get metadata with failure`() {

        val notFoundResponse = "{\"error\":\"unknown_error\"}"
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody(notFoundResponse)
        )

        val test = metadataInteractor.loadRemoteMetadata(fakeMetadata).test()
        test.assertError {
            it is HttpException && it.code() == 400
        }
        test.assertNotComplete()
    }
}