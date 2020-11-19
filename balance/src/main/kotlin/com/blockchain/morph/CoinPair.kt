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
    BTC_TO_ALGO("btc_algo", CryptoCurrency.BTC, CryptoCurrency.ALGO),
    BTC_TO_USDT("btc_usdt", CryptoCurrency.BTC, CryptoCurrency.USDT),
    BTC_TO_DGLD("btc_wdgld", CryptoCurrency.BTC, CryptoCurrency.DGLD),

    ETH_TO_ETH("eth_eth", CryptoCurrency.ETHER, CryptoCurrency.ETHER),
    ETH_TO_BTC("eth_btc", CryptoCurrency.ETHER, CryptoCurrency.BTC),
    ETH_TO_BCH("eth_bch", CryptoCurrency.ETHER, CryptoCurrency.BCH),
    ETH_TO_XLM("eth_xlm", CryptoCurrency.ETHER, CryptoCurrency.XLM),
    ETH_TO_PAX("eth_pax", CryptoCurrency.ETHER, CryptoCurrency.PAX),
    ETH_TO_ALGO("eth_algo", CryptoCurrency.ETHER, CryptoCurrency.ALGO),
    ETH_TO_USDT("eth_usdt", CryptoCurrency.ETHER, CryptoCurrency.USDT),
    ETH_TO_DGLD("eth_wdgld", CryptoCurrency.ETHER, CryptoCurrency.DGLD),

    BCH_TO_BCH("bch_bch", CryptoCurrency.BCH, CryptoCurrency.BCH),
    BCH_TO_BTC("bch_btc", CryptoCurrency.BCH, CryptoCurrency.BTC),
    BCH_TO_ETH("bch_eth", CryptoCurrency.BCH, CryptoCurrency.ETHER),
    BCH_TO_XLM("bch_xlm", CryptoCurrency.BCH, CryptoCurrency.XLM),
    BCH_TO_PAX("bch_pax", CryptoCurrency.BCH, CryptoCurrency.PAX),
    BCH_TO_ALGO("bch_algo", CryptoCurrency.BCH, CryptoCurrency.ALGO),
    BCH_TO_USDT("bch_usdt", CryptoCurrency.BCH, CryptoCurrency.USDT),
    BCH_TO_DGLD("bch_wdgld", CryptoCurrency.BCH, CryptoCurrency.DGLD),

    XLM_TO_XLM("xlm_xlm", CryptoCurrency.XLM, CryptoCurrency.XLM),
    XLM_TO_BTC("xlm_btc", CryptoCurrency.XLM, CryptoCurrency.BTC),
    XLM_TO_ETH("xlm_eth", CryptoCurrency.XLM, CryptoCurrency.ETHER),
    XLM_TO_BCH("xlm_bch", CryptoCurrency.XLM, CryptoCurrency.BCH),
    XLM_TO_PAX("xlm_pax", CryptoCurrency.XLM, CryptoCurrency.PAX),
    XLM_TO_ALGO("xlm_algo", CryptoCurrency.XLM, CryptoCurrency.ALGO),
    XLM_TO_USDT("xlm_usdt", CryptoCurrency.XLM, CryptoCurrency.USDT),
    XLM_TO_DGLD("xlm_wdgld", CryptoCurrency.XLM, CryptoCurrency.DGLD),

    PAX_TO_PAX("pax_pax", CryptoCurrency.PAX, CryptoCurrency.PAX),
    PAX_TO_BTC("pax_btc", CryptoCurrency.PAX, CryptoCurrency.BTC),
    PAX_TO_ETH("pax_eth", CryptoCurrency.PAX, CryptoCurrency.ETHER),
    PAX_TO_BCH("pax_bch", CryptoCurrency.PAX, CryptoCurrency.BCH),
    PAX_TO_XLM("pax_xlm", CryptoCurrency.PAX, CryptoCurrency.XLM),
    PAX_TO_ALGO("pax_algo", CryptoCurrency.PAX, CryptoCurrency.ALGO),
    PAX_TO_USDT("pax_usdt", CryptoCurrency.PAX, CryptoCurrency.USDT),
    PAX_TO_DGLD("pax_wdgld", CryptoCurrency.PAX, CryptoCurrency.DGLD),

    ALGO_TO_ALGO("algo_algo", CryptoCurrency.ALGO, CryptoCurrency.ALGO),
    ALGO_TO_BTC("algo_btc", CryptoCurrency.ALGO, CryptoCurrency.BTC),
    ALGO_TO_ETH("algo_eth", CryptoCurrency.ALGO, CryptoCurrency.ETHER),
    ALGO_TO_BCH("algo_bch", CryptoCurrency.ALGO, CryptoCurrency.BCH),
    ALGO_TO_XLM("algo_xlm", CryptoCurrency.ALGO, CryptoCurrency.XLM),
    ALGO_TO_PAX("algo_pax", CryptoCurrency.ALGO, CryptoCurrency.PAX),
    ALGO_TO_USDT("algo_usdt", CryptoCurrency.ALGO, CryptoCurrency.USDT),
    ALGO_TO_DGLD("algo_wdgld", CryptoCurrency.ALGO, CryptoCurrency.DGLD),

    USDT_TO_USDT("usdt_usdt", CryptoCurrency.USDT, CryptoCurrency.USDT),
    USDT_TO_BTC("usdt_btc", CryptoCurrency.USDT, CryptoCurrency.BTC),
    USDT_TO_ETH("usdt_eth", CryptoCurrency.USDT, CryptoCurrency.ETHER),
    USDT_TO_BCH("usdt_bch", CryptoCurrency.USDT, CryptoCurrency.BCH),
    USDT_TO_XLM("usdt_xlm", CryptoCurrency.USDT, CryptoCurrency.XLM),
    USDT_TO_PAX("usdt_pax", CryptoCurrency.USDT, CryptoCurrency.PAX),
    USDT_TO_ALGO("usdt_algo", CryptoCurrency.USDT, CryptoCurrency.ALGO),
    USDT_TO_DGLD("usdt_wdgld", CryptoCurrency.USDT, CryptoCurrency.DGLD),

    DGLD_TO_DGLD("wdgld_wdgld", CryptoCurrency.DGLD, CryptoCurrency.DGLD),
    DGLD_TO_BTC("wdgld_btc", CryptoCurrency.DGLD, CryptoCurrency.BTC),
    DGLD_TO_ETH("wdgld_eth", CryptoCurrency.DGLD, CryptoCurrency.ETHER),
    DGLD_TO_BCH("wdgld_bch", CryptoCurrency.DGLD, CryptoCurrency.BCH),
    DGLD_TO_XLM("wdgld_xlm", CryptoCurrency.DGLD, CryptoCurrency.XLM),
    DGLD_TO_PAX("wdgld_pax", CryptoCurrency.DGLD, CryptoCurrency.PAX),
    DGLD_TO_ALGO("wdgld_algo", CryptoCurrency.DGLD, CryptoCurrency.ALGO),
    DGLD_TO_USDT("wdgld_usdt", CryptoCurrency.DGLD, CryptoCurrency.USDT);

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
            CryptoCurrency.ALGO -> CoinPair.BTC_TO_ALGO
            CryptoCurrency.USDT -> CoinPair.BTC_TO_USDT
            CryptoCurrency.DGLD -> CoinPair.BTC_TO_DGLD
        }
        CryptoCurrency.ETHER -> when (other) {
            CryptoCurrency.ETHER -> CoinPair.ETH_TO_ETH
            CryptoCurrency.BTC -> CoinPair.ETH_TO_BTC
            CryptoCurrency.BCH -> CoinPair.ETH_TO_BCH
            CryptoCurrency.XLM -> CoinPair.ETH_TO_XLM
            CryptoCurrency.PAX -> CoinPair.ETH_TO_PAX
            CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
            CryptoCurrency.ALGO -> CoinPair.ETH_TO_ALGO
            CryptoCurrency.USDT -> CoinPair.ETH_TO_USDT
            CryptoCurrency.DGLD -> CoinPair.ETH_TO_DGLD
        }
        CryptoCurrency.BCH -> when (other) {
            CryptoCurrency.BCH -> CoinPair.BCH_TO_BCH
            CryptoCurrency.BTC -> CoinPair.BCH_TO_BTC
            CryptoCurrency.ETHER -> CoinPair.BCH_TO_ETH
            CryptoCurrency.XLM -> CoinPair.BCH_TO_XLM
            CryptoCurrency.PAX -> CoinPair.BCH_TO_PAX
            CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
            CryptoCurrency.ALGO -> CoinPair.BCH_TO_ALGO
            CryptoCurrency.USDT -> CoinPair.BCH_TO_USDT
            CryptoCurrency.DGLD -> CoinPair.BCH_TO_DGLD
        }
        CryptoCurrency.XLM -> when (other) {
            CryptoCurrency.XLM -> CoinPair.XLM_TO_XLM
            CryptoCurrency.BTC -> CoinPair.XLM_TO_BTC
            CryptoCurrency.ETHER -> CoinPair.XLM_TO_ETH
            CryptoCurrency.BCH -> CoinPair.XLM_TO_BCH
            CryptoCurrency.PAX -> CoinPair.XLM_TO_PAX
            CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
            CryptoCurrency.ALGO -> CoinPair.XLM_TO_ALGO
            CryptoCurrency.USDT -> CoinPair.XLM_TO_USDT
            CryptoCurrency.DGLD -> CoinPair.XLM_TO_DGLD
        }
        CryptoCurrency.PAX -> when (other) {
            CryptoCurrency.PAX -> CoinPair.PAX_TO_PAX
            CryptoCurrency.BTC -> CoinPair.PAX_TO_BTC
            CryptoCurrency.ETHER -> CoinPair.PAX_TO_ETH
            CryptoCurrency.BCH -> CoinPair.PAX_TO_BCH
            CryptoCurrency.XLM -> CoinPair.PAX_TO_XLM
            CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
            CryptoCurrency.ALGO -> CoinPair.PAX_TO_ALGO
            CryptoCurrency.USDT -> CoinPair.PAX_TO_USDT
            CryptoCurrency.DGLD -> CoinPair.PAX_TO_DGLD
        }
        CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
        CryptoCurrency.ALGO -> when (other) {
            CryptoCurrency.ALGO -> CoinPair.ALGO_TO_ALGO
            CryptoCurrency.BTC -> CoinPair.ALGO_TO_BTC
            CryptoCurrency.ETHER -> CoinPair.ALGO_TO_ETH
            CryptoCurrency.BCH -> CoinPair.ALGO_TO_BCH
            CryptoCurrency.XLM -> CoinPair.ALGO_TO_XLM
            CryptoCurrency.PAX -> CoinPair.ALGO_TO_PAX
            CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
            CryptoCurrency.USDT -> CoinPair.ALGO_TO_USDT
            CryptoCurrency.DGLD -> CoinPair.ALGO_TO_DGLD
        }
        CryptoCurrency.USDT -> when (other) {
            CryptoCurrency.USDT -> CoinPair.USDT_TO_USDT
            CryptoCurrency.BTC -> CoinPair.USDT_TO_BTC
            CryptoCurrency.ETHER -> CoinPair.USDT_TO_ETH
            CryptoCurrency.BCH -> CoinPair.USDT_TO_BCH
            CryptoCurrency.XLM -> CoinPair.USDT_TO_XLM
            CryptoCurrency.PAX -> CoinPair.USDT_TO_PAX
            CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
            CryptoCurrency.ALGO -> CoinPair.USDT_TO_ALGO
            CryptoCurrency.DGLD -> CoinPair.USDT_TO_DGLD
        }
        CryptoCurrency.DGLD -> when (other) {
            CryptoCurrency.DGLD -> CoinPair.DGLD_TO_DGLD
            CryptoCurrency.BTC -> CoinPair.DGLD_TO_BTC
            CryptoCurrency.ETHER -> CoinPair.DGLD_TO_ETH
            CryptoCurrency.BCH -> CoinPair.DGLD_TO_BCH
            CryptoCurrency.XLM -> CoinPair.DGLD_TO_XLM
            CryptoCurrency.PAX -> CoinPair.DGLD_TO_PAX
            CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
            CryptoCurrency.ALGO -> CoinPair.DGLD_TO_ALGO
            CryptoCurrency.USDT -> CoinPair.DGLD_TO_USDT
        }
    }
