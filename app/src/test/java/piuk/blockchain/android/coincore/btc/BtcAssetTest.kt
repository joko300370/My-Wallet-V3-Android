package piuk.blockchain.android.coincore.btc

class BtcAssetTest
//     private val payloadDataManager: PayloadDataManager = mock()
//     private val bchDataManager: BchDataManager = mock()
//     private val metadataManager: MetadataManager = mock()
//         //  TODO: These will break things when fully testing onViewReady()
//         val btcAccount = Account().apply {
//             label = "LABEL"
//             xpub = "X_PUB"
//         }
//         val bchAccount = GenericMetadataAccount().apply {
//             label = "LABEL"
//             xpub = "X_PUB"
//         }
//         whenever(payloadDataManager.accounts).thenReturn(listOf(btcAccount))
//         whenever(payloadDataManager.legacyAddresses).thenReturn(mutableListOf())
//         whenever(bchDataManager.getAccountMetadataList()).thenReturn(listOf(bchAccount))
//         whenever(payloadDataManager.defaultAccountIndex).thenReturn(0)
//         whenever(bchDataManager.getDefaultAccountPosition()).thenReturn(0)
//         whenever(payloadDataManager.getAddressBalance(any())).thenReturn(CryptoValue.ZeroBtc)
//         whenever(bchDataManager.getAddressBalance(any())).thenReturn(BigInteger.ZERO)
// @Test
// fun createNewAccountSuccessful() {
//     //  Arrange
// //         val account: Account = mock()
// //         whenever(account.xpub).thenReturn("xpub")
//     whenever(payloadDataManager.createNewAccount(ArgumentMatchers.anyString(), ArgumentMatchers.isNull<String>()))
//         .thenReturn(Observable.just(account))
//     whenever(bchDataManager.serializeForSaving()).thenReturn("")
//     whenever(metadataManager.saveToMetadata(any(), ArgumentMatchers.anyInt())).thenReturn(Completable.complete())
//     whenever
//     //  Act
//     subject.createNewAccount("")
//     //  Assert
//     verify(payloadDataManager).createNewAccount(ArgumentMatchers.anyString(), ArgumentMatchers.isNull())
//     verify(bchDataManager).createAccount("xpub")
//     verify(bchDataManager).serializeForSaving()
//     verify(metadataManager).saveToMetadata(any(), ArgumentMatchers.anyInt())
//     verify(activity).showProgressDialog(ArgumentMatchers.anyInt())
//     verify(activity).dismissProgressDialog()
//     verify(activity).showSuccess(ArgumentMatchers.anyInt())
//     verify(coinsWebSocketStrategy).subscribeToXpubBtc("xpub")
// }
// 
// @Test
// fun createNewAccountDecryptionException() {
//     //  Arrange
//     whenever(payloadDataManager.createNewAccount(ArgumentMatchers.anyString(), ArgumentMatchers.isNull<String>()))
//         .thenReturn(Observable.error(DecryptionException()))
//     //  Act
//     subject.createNewAccount("")
//     //  Assert
//     verify(activity).showProgressDialog(ArgumentMatchers.anyInt())
//     verify(activity).dismissProgressDialog()
//     verify(activity).showError(ArgumentMatchers.anyInt())
//     Mockito.verifyNoMoreInteractions(activity)
// }
// 
// @Test
// fun createNewAccountPayloadException() {
//     //  Arrange
//     whenever(payloadDataManager.createNewAccount(ArgumentMatchers.anyString(), ArgumentMatchers.isNull<String>()))
//         .thenReturn(Observable.error(PayloadException()))
//     //  Act
//     subject.createNewAccount("")
//     //  Assert
//     verify(activity).showProgressDialog(ArgumentMatchers.anyInt())
//     verify(activity).dismissProgressDialog()
//     verify(activity).showToast(ArgumentMatchers.anyInt(), eq(ToastCustom.TYPE_ERROR))
//     Mockito.verifyNoMoreInteractions(activity)
// }
// 
// @Test
// fun createNewAccountUnknownException() {
//     //  Arrange
//     whenever(payloadDataManager.createNewAccount(ArgumentMatchers.anyString(), ArgumentMatchers.isNull<String>()))
//         .thenReturn(Observable.error(Exception()))
//     //  Act
//     subject.createNewAccount("")
//     //  Assert
//     verify(activity).showProgressDialog(ArgumentMatchers.anyInt())
//     verify(activity).dismissProgressDialog()
//     verify(activity).showToast(ArgumentMatchers.anyInt(), eq(ToastCustom.TYPE_ERROR))
//     Mockito.verifyNoMoreInteractions(activity)
// }
// 
// @Test
// fun updateLegacyAddressSuccessful() {
//     //  Arrange
//     val legacyAddress = LegacyAddress().apply {
//         address = "address1"
//     }
//     whenever(payloadDataManager.updateLegacyAddress(legacyAddress))
//         .thenReturn(Completable.complete())
//     //  Act
//     subject.updateLegacyAddress(legacyAddress)
//     //  Assert
//     verify(payloadDataManager).updateLegacyAddress(legacyAddress)
//     verify(subject).createCoincoreAddress(legacyAddress)
//     verify(activity).showProgressDialog(ArgumentMatchers.anyInt())
//     verify(activity).dismissProgressDialog()
//     verify(activity).showToast(ArgumentMatchers.anyInt(), eq(ToastCustom.TYPE_OK))
// }

// @Test
// fun updateLegacyAddressFailed() {
//     //  Arrange
//     val legacyAddress = LegacyAddress()
//     whenever(payloadDataManager.updateLegacyAddress(legacyAddress))
//         .thenReturn(Completable.error(Throwable()))
//     //  Act
//     subject.updateLegacyAddress(legacyAddress)
//     //  Assert
//     verify(payloadDataManager).updateLegacyAddress(legacyAddress)
//     verify(activity).showProgressDialog(ArgumentMatchers.anyInt())
//     verify(activity).dismissProgressDialog()
//     verify(activity).showToast(ArgumentMatchers.anyInt(), eq(ToastCustom.TYPE_ERROR))
//     Mockito.verifyNoMoreInteractions(activity)
// }
// 
// @Test
// fun importBip38AddressWithValidPassword() {
//     //  Arrange
// 
//     //  Act
//     subject.importBip38Address(
//         "6PRJmkckxBct8jUwn6UcJbickdrnXBiPP9JkNW83g4VyFNsfEuxas39pSS",
//         "password"
//     )
//     //  Assert
//     verify(activity).showProgressDialog(ArgumentMatchers.anyInt())
//     verify(activity).dismissProgressDialog()
// }
// 
// @Test
// fun importBip38AddressWithIncorrectPassword() {
//     //  Arrange
// 
//     //  Act
//     subject.importBip38Address(
//         "6PRJmkckxBct8jUwn6UcJbickdrnXBiPP9JkNW83g4VyFNsfEuxas39pSS",
//         "notthepassword"
//     )
//     //  Assert
//     verify(activity).showProgressDialog(ArgumentMatchers.anyInt())
//     verify(activity).showToast(ArgumentMatchers.anyInt(), eq(ToastCustom.TYPE_ERROR))
//     verify(activity).dismissProgressDialog()
//     Mockito.verifyNoMoreInteractions(activity)
// }
// 
// @Test
// fun onAddressScannedBip38() {
//     //  Arrange
// 
//     //  Act
//     subject.onAddressScanned("6PRJmkckxBct8jUwn6UcJbickdrnXBiPP9JkNW83g4VyFNsfEuxas39pSS")
//     //  Assert
//     verify(activity).showBip38PasswordDialog("6PRJmkckxBct8jUwn6UcJbickdrnXBiPP9JkNW83g4VyFNsfEuxas39pSS")
//     Mockito.verifyNoMoreInteractions(activity)
// }
// 
// @Test
// fun onAddressScannedNonBip38() {
//     //  Arrange
//     whenever(payloadDataManager.getKeyFromImportedData(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
//         .thenReturn(Observable.just(Mockito.mock(ECKey::class.java)))
//     //  Act
//     subject.onAddressScanned("L1FQxC7wmmRNNe2YFPNXscPq3kaheiA4T7SnTr7vYSBW7Jw1A7PD")
//     //  Assert
//     verify(payloadDataManager).getKeyFromImportedData(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())
//     verify(activity).showProgressDialog(ArgumentMatchers.anyInt())
//     verify(activity).dismissProgressDialog()
// }
// 
// @Test
// fun onAddressScannedNonBip38Failure() {
//     //  Arrange
//     whenever(payloadDataManager.getKeyFromImportedData(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
//         .thenReturn(Observable.error(Throwable()))
//     //  Act
//     subject.onAddressScanned("L1FQxC7wmmRNNe2YFPNXscPq3kaheiA4T7SnTr7vYSBW7Jw1A7PD")
//     whenever(payloadDataManager.getKeyFromImportedData(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
//         .thenReturn(Observable.just(Mockito.mock(ECKey::class.java)))
//     //  Assert
//     verify(payloadDataManager).getKeyFromImportedData(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())
//     verify(activity).showProgressDialog(ArgumentMatchers.anyInt())
//     verify(activity).dismissProgressDialog()
//     verify(activity).showToast(ArgumentMatchers.anyInt(), ArgumentMatchers.anyString())
// }
// 
// @Test
// fun onAddressScannedWatchOnlyInvalidAddress() {
//     //  Arrange
// 
//     //  Act
//     subject.onAddressScanned("test")
//     //  Assert
//     verify(activity).showToast(ArgumentMatchers.anyInt(), eq(ToastCustom.TYPE_ERROR))
//     Mockito.verifyNoMoreInteractions(activity)
// }
// 
// @Test
// fun onAddressScannedWatchOnlyNullAddress() {
//     //  Arrange
// 
//     //  Act
//     subject.onAddressScanned(null)
//     //  Assert
//     verify(activity).showToast(ArgumentMatchers.anyInt(), eq(ToastCustom.TYPE_ERROR))
//     Mockito.verifyNoMoreInteractions(activity)
// }
// 
// @Test
// fun onAddressScannedWatchAddressNotInWallet() {
//     //  Arrange
//     val mockPayload = Mockito.mock(Wallet::class.java, Mockito.RETURNS_DEEP_STUBS)
// 
//     whenever(mockPayload.legacyAddressStringList.contains(any<Any>())).thenReturn(false)
//     whenever(payloadDataManager.wallet).thenReturn(mockPayload)
//     //  Act
//     subject.onAddressScanned("17UovdU9ZvepPe75igTQwxqNME1HbnvMB7")
//     //  Assert
//     verify(activity).showWatchOnlyUnsupportedMsg()
//     Mockito.verifyNoMoreInteractions(activity)
// }
// 
// @SuppressLint("VisibleForTests")
// @Test
// fun handlePrivateKeyWhenKeyIsNull() {
//     //  Arrange
// 
//     //  Act
//     subject.handlePrivateKey(null, null)
//     //  Assert
//     verify(activity).showToast(R.string.no_private_key, ToastCustom.TYPE_ERROR)
//     Mockito.verifyNoMoreInteractions(activity)
// }
// 
// @SuppressLint("VisibleForTests")
// @Test
// fun handlePrivateKeyExistingAddressSuccess() {
//     //  Arrange
//     whenever(environmentSettings.environment).thenReturn(Environment.PRODUCTION)
//     val mockECKey = Mockito.mock(ECKey::class.java)
//     whenever(mockECKey.hasPrivKey()).thenReturn(true)
//     val legacyAddress = LegacyAddress()
//     whenever(payloadDataManager.setKeyForLegacyAddress(mockECKey, null))
//         .thenReturn(Observable.just(legacyAddress))
// 
//     //  Act
//     subject.handlePrivateKey(mockECKey, null)
//     //  Assert
//     verify(activity).showToast(ArgumentMatchers.anyInt(), eq(ToastCustom.TYPE_OK))
//     verify(activity).showRenameImportedAddressDialog(legacyAddress)
// }
// 
// @SuppressLint("VisibleForTests")
// @Test
// fun handlePrivateKeyExistingAddressFailure() {
//     //  Arrange
//     val mockECKey = Mockito.mock(ECKey::class.java)
//     whenever(mockECKey.hasPrivKey()).thenReturn(true)
//     whenever(payloadDataManager.setKeyForLegacyAddress(mockECKey, null))
//         .thenReturn(Observable.error(Throwable()))
//     //  Act
//     subject.handlePrivateKey(mockECKey, null)
//     //  Assert
//     verify(activity).showToast(ArgumentMatchers.anyInt(), eq(ToastCustom.TYPE_ERROR))
//     Mockito.verifyNoMoreInteractions(activity)
// }