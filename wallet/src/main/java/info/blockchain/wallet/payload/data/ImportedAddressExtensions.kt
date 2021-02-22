package info.blockchain.wallet.payload.data

@Deprecated("Only used in tests")
fun ImportedAddress.archive() {
    tag = ImportedAddress.ARCHIVED_ADDRESS
}

@Deprecated("Only used in tests")
fun ImportedAddress.unarchive() {
    tag = ImportedAddress.NORMAL_ADDRESS
}

val ImportedAddress.isArchived: Boolean
    get() = tag == ImportedAddress.ARCHIVED_ADDRESS
