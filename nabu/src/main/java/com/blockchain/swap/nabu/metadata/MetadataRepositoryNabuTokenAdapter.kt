package com.blockchain.swap.nabu.metadata

import com.blockchain.exceptions.MetadataNotFoundException
import com.blockchain.metadata.MetadataRepository
import com.blockchain.swap.nabu.CreateNabuToken
import com.blockchain.swap.nabu.NabuToken
import com.blockchain.swap.nabu.models.tokenresponse.NabuOfflineTokenResponse
import com.blockchain.swap.nabu.models.tokenresponse.mapFromMetadata
import com.blockchain.swap.nabu.models.tokenresponse.mapToMetadata
import com.blockchain.rx.maybeCache
import io.reactivex.Maybe
import io.reactivex.Single
import piuk.blockchain.androidcore.data.metadata.MetadataManager

internal class MetadataRepositoryNabuTokenAdapter(
    private val metadataRepository: MetadataRepository,
    private val createNabuToken: CreateNabuToken,
    private val metadataManager: MetadataManager
) : NabuToken {

    private fun createMetaData() = Maybe.defer {
        createNabuToken.createNabuOfflineToken()
            .map {
                it.mapToMetadata()
            }
            .flatMapMaybe {
                metadataManager.attemptMetadataSetup()
                    .andThen(metadataRepository.saveMetadata(
                        it,
                        NabuCredentialsMetadata::class.java,
                        NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
                    ))
                    .andThen(Maybe.just(it))
            }
    }

    private val defer = Maybe.defer {
        metadataRepository.loadMetadata(
            NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE,
            NabuCredentialsMetadata::class.java
        )
    }.maybeCache()
        .onErrorReturn { NabuCredentialsMetadata.invalid() }
        .filter { it.isValid() }

    override fun fetchNabuToken(): Single<NabuOfflineTokenResponse> {
        return defer.switchIfEmpty(createMetaData())
            .map { metadata ->
                if (!metadata.isValid()) throw MetadataNotFoundException("Nabu Token is empty")
                metadata.mapFromMetadata()
            }
            .toSingle()
    }
}
