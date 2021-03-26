package info.blockchain.wallet.payload.data

fun Wallet.nonArchivedImportedAddressStrings() =
    nonArchivedImportedAddresses()
        .addressSet()

fun Wallet.spendableImportedAddressStrings() =
    nonArchivedImportedAddresses()
        .addressSet()

fun Wallet.allSpendableAccountsAndAddresses() =
    activeXpubs() + spendableImportedAddressStrings()

fun Wallet.allNonArchivedAccountsAndAddresses() =
    activeXpubs() + nonArchivedImportedAddressStrings()

private fun Wallet.nonArchivedImportedAddresses() =
    importedAddressList
        .filterNot { it.isArchived }

private fun Iterable<ImportedAddress>.addressSet() =
    map { it.address }
        .toSet()

fun Wallet.activeXpubs() =
    hdWallets?.get(0)?.activeXpubs?.toSet() ?: emptySet()
