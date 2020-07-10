package com.blockchain.morph

import info.blockchain.balance.CryptoCurrency

enum class CoinPair(
    val pairCode: String,
    val from: CryptoCurrency,
    val to: CryptoCurrency,
    val pairCodeUpper: String = pairCode.toUpperCase().replace("_", "-")
) {

    BTC_TO_BTC("btc_btc", CryptoCurrency.BTC, CryptoCurrency.BTC),
    BTC_TO_ETH("btc_eth", CryptoCurrency.BTC, CryptoCurrency.ETHER),
    BTC_TO_BCH("btc_bch", CryptoCurrency.BTC, CryptoCurrency.BCH),
    BTC_TO_XLM("btc_xlm", CryptoCurrency.BTC, CryptoCurrency.XLM),
    BTC_TO_PAX("btc_pax", CryptoCurrency.BTC, CryptoCurrency.PAX),
    BTC_TO_ALG("btc_algo", CryptoCurrency.BTC, CryptoCurrency.ALGO),
    BTC_TO_USDT("btc_usdt", CryptoCurrency.BTC, CryptoCurrency.USDT),

    ETH_TO_ETH("eth_eth", CryptoCurrency.ETHER, CryptoCurrency.ETHER),
    ETH_TO_BTC("eth_btc", CryptoCurrency.ETHER, CryptoCurrency.BTC),
    ETH_TO_BCH("eth_bch", CryptoCurrency.ETHER, CryptoCurrency.BCH),
    ETH_TO_XLM("eth_xlm", CryptoCurrency.ETHER, CryptoCurrency.XLM),
    ETH_TO_PAX("eth_pax", CryptoCurrency.ETHER, CryptoCurrency.PAX),
    ETH_TO_ALG("eth_algo", CryptoCurrency.ETHER, CryptoCurrency.ALGO),
    ETH_TO_USDT("eth_usdt", CryptoCurrency.ETHER, CryptoCurrency.USDT),

    BCH_TO_BCH("bch_bch", CryptoCurrency.BCH, CryptoCurrency.BCH),
    BCH_TO_BTC("bch_btc", CryptoCurrency.BCH, CryptoCurrency.BTC),
    BCH_TO_ETH("bch_eth", CryptoCurrency.BCH, CryptoCurrency.ETHER),
    BCH_TO_XLM("bch_xlm", CryptoCurrency.BCH, CryptoCurrency.XLM),
    BCH_TO_PAX("bch_pax", CryptoCurrency.BCH, CryptoCurrency.PAX),
    BCH_TO_ALG("bch_algo", CryptoCurrency.BCH, CryptoCurrency.ALGO),
    BCH_TO_USDT("bch_usdt", CryptoCurrency.BCH, CryptoCurrency.USDT),

    XLM_TO_XLM("xlm_xlm", CryptoCurrency.XLM, CryptoCurrency.XLM),
    XLM_TO_BTC("xlm_btc", CryptoCurrency.XLM, CryptoCurrency.BTC),
    XLM_TO_ETH("xlm_eth", CryptoCurrency.XLM, CryptoCurrency.ETHER),
    XLM_TO_BCH("xlm_bch", CryptoCurrency.XLM, CryptoCurrency.BCH),
    XLM_TO_PAX("xlm_pax", CryptoCurrency.XLM, CryptoCurrency.PAX),
    XLM_TO_ALG("xlm_algo", CryptoCurrency.XLM, CryptoCurrency.ALGO),
    XLM_TO_USDT("xlm_usdt", CryptoCurrency.XLM, CryptoCurrency.USDT),

    PAX_TO_PAX("pax_pax", CryptoCurrency.PAX, CryptoCurrency.PAX),
    PAX_TO_BTC("pax_btc", CryptoCurrency.PAX, CryptoCurrency.BTC),
    PAX_TO_ETH("pax_eth", CryptoCurrency.PAX, CryptoCurrency.ETHER),
    PAX_TO_BCH("pax_bch", CryptoCurrency.PAX, CryptoCurrency.BCH),
    PAX_TO_XLM("pax_xlm", CryptoCurrency.PAX, CryptoCurrency.XLM),
    PAX_TO_ALG("pax_algo", CryptoCurrency.PAX, CryptoCurrency.ALGO),
    PAX_TO_USDT("pax_usdt", CryptoCurrency.PAX, CryptoCurrency.USDT),

    ALG_TO_ALG("algo_algo", CryptoCurrency.ALGO, CryptoCurrency.ALGO),
    ALG_TO_BTC("algo_btc", CryptoCurrency.ALGO, CryptoCurrency.BTC),
    ALG_TO_ETH("algo_eth", CryptoCurrency.ALGO, CryptoCurrency.ETHER),
    ALG_TO_BCH("algo_bch", CryptoCurrency.ALGO, CryptoCurrency.BCH),
    ALG_TO_XLM("algo_xlm", CryptoCurrency.ALGO, CryptoCurrency.XLM),
    ALG_TO_PAX("algo_pax", CryptoCurrency.ALGO, CryptoCurrency.PAX),
    ALG_TO_USDT("algo_usdt", CryptoCurrency.ALGO, CryptoCurrency.USDT),

    USDT_TO_USDT("usdt_usdt", CryptoCurrency.USDT, CryptoCurrency.USDT),
    USDT_TO_BTC("usdt_btc", CryptoCurrency.USDT, CryptoCurrency.BTC),
    USDT_TO_ETH("usdt_eth", CryptoCurrency.USDT, CryptoCurrency.ETHER),
    USDT_TO_BCH("usdt_bch", CryptoCurrency.USDT, CryptoCurrency.BCH),
    USDT_TO_XLM("usdt_xlm", CryptoCurrency.USDT, CryptoCurrency.XLM),
    USDT_TO_PAX("usdt_pax", CryptoCurrency.USDT, CryptoCurrency.PAX),
    USDT_TO_ALG("usdt_algo", CryptoCurrency.USDT, CryptoCurrency.ALGO) ;

    val sameInputOutput = from == to

    fun inverse() = to to from

    companion object {

        fun fromPairCode(pairCode: String): CoinPair {
            return fromPairCodeOrNull(pairCode) ?: throw IllegalStateException("Attempt to get invalid pair $pairCode")
        }

        fun fromPairCodeOrNull(pairCode: String?): CoinPair? {
            pairCode?.split('_')?.let {
                if (it.size == 2) {
                    val from = CryptoCurrency.fromNetworkTicker(it.first())
                    val to = CryptoCurrency.fromNetworkTicker(it.last())
                    if (from != null && to != null) {
                        return from to to
                    }
                }
            }
            return null
        }
    }
}

infix fun CryptoCurrency.to(other: CryptoCurrency) =
    when (this) {
        CryptoCurrency.BTC -> when (other) {
            CryptoCurrency.BTC -> CoinPair.BTC_TO_BTC
            CryptoCurrency.ETHER -> CoinPair.BTC_TO_ETH
            CryptoCurrency.BCH -> CoinPair.BTC_TO_BCH
            CryptoCurrency.XLM -> CoinPair.BTC_TO_XLM
            CryptoCurrency.PAX -> CoinPair.BTC_TO_PAX
            CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
            CryptoCurrency.ALGO -> CoinPair.BTC_TO_ALG
            CryptoCurrency.USDT -> CoinPair.BTC_TO_USDT
        }
        CryptoCurrency.ETHER -> when (other) {
            CryptoCurrency.ETHER -> CoinPair.ETH_TO_ETH
            CryptoCurrency.BTC -> CoinPair.ETH_TO_BTC
            CryptoCurrency.BCH -> CoinPair.ETH_TO_BCH
            CryptoCurrency.XLM -> CoinPair.ETH_TO_XLM
            CryptoCurrency.PAX -> CoinPair.ETH_TO_PAX
            CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
            CryptoCurrency.ALGO -> CoinPair.ETH_TO_ALG
            CryptoCurrency.USDT -> CoinPair.ETH_TO_USDT
        }
        CryptoCurrency.BCH -> when (other) {
            CryptoCurrency.BCH -> CoinPair.BCH_TO_BCH
            CryptoCurrency.BTC -> CoinPair.BCH_TO_BTC
            CryptoCurrency.ETHER -> CoinPair.BCH_TO_ETH
            CryptoCurrency.XLM -> CoinPair.BCH_TO_XLM
            CryptoCurrency.PAX -> CoinPair.BCH_TO_PAX
            CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
            CryptoCurrency.ALGO -> CoinPair.BCH_TO_ALG
            CryptoCurrency.USDT -> CoinPair.BCH_TO_USDT
        }
        CryptoCurrency.XLM -> when (other) {
            CryptoCurrency.XLM -> CoinPair.XLM_TO_XLM
            CryptoCurrency.BTC -> CoinPair.XLM_TO_BTC
            CryptoCurrency.ETHER -> CoinPair.XLM_TO_ETH
            CryptoCurrency.BCH -> CoinPair.XLM_TO_BCH
            CryptoCurrency.PAX -> CoinPair.XLM_TO_PAX
            CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
            CryptoCurrency.ALGO -> CoinPair.XLM_TO_ALG
            CryptoCurrency.USDT -> CoinPair.XLM_TO_USDT
        }
        CryptoCurrency.PAX -> when (other) {
            CryptoCurrency.PAX -> CoinPair.PAX_TO_PAX
            CryptoCurrency.BTC -> CoinPair.PAX_TO_BTC
            CryptoCurrency.ETHER -> CoinPair.PAX_TO_ETH
            CryptoCurrency.BCH -> CoinPair.PAX_TO_BCH
            CryptoCurrency.XLM -> CoinPair.PAX_TO_XLM
            CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
            CryptoCurrency.ALGO -> CoinPair.PAX_TO_ALG
            CryptoCurrency.USDT -> CoinPair.PAX_TO_USDT
        }
        CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
        CryptoCurrency.ALGO -> when (other) {
            CryptoCurrency.ALGO -> CoinPair.ALG_TO_ALG
            CryptoCurrency.BTC -> CoinPair.ALG_TO_BTC
            CryptoCurrency.ETHER -> CoinPair.ALG_TO_ETH
            CryptoCurrency.BCH -> CoinPair.ALG_TO_BCH
            CryptoCurrency.XLM -> CoinPair.ALG_TO_XLM
            CryptoCurrency.PAX -> CoinPair.ALG_TO_PAX
            CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
            CryptoCurrency.USDT -> CoinPair.ALG_TO_USDT
        }
        CryptoCurrency.USDT -> when (other) {
            CryptoCurrency.USDT -> CoinPair.USDT_TO_USDT
            CryptoCurrency.BTC -> CoinPair.USDT_TO_BTC
            CryptoCurrency.ETHER -> CoinPair.USDT_TO_ETH
            CryptoCurrency.BCH -> CoinPair.USDT_TO_BCH
            CryptoCurrency.XLM -> CoinPair.USDT_TO_XLM
            CryptoCurrency.PAX -> CoinPair.USDT_TO_PAX
            CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
            CryptoCurrency.ALGO -> CoinPair.USDT_TO_ALG
        }
    }
