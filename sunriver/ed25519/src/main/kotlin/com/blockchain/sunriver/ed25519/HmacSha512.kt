package com.blockchain.sunriver.ed25519

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private val HMAC_SHA512 = "HmacSHA512"

fun hmacSha512(byteKey: ByteArray, seed: ByteArray) =
    getInstance().apply {
        init(SecretKeySpec(byteKey, HMAC_SHA512))
    }.doFinal(seed)

private fun getInstance() = Mac.getInstance(HMAC_SHA512)
