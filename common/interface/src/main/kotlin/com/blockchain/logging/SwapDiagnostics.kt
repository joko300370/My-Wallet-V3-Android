package com.blockchain.logging

import info.blockchain.balance.CryptoValue

interface SwapDiagnostics {
    var accountBalance: CryptoValue?
}
