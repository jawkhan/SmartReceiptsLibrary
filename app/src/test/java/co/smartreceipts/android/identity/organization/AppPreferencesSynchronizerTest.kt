package co.smartreceipts.android.identity.organization

import co.smartreceipts.android.identity.apis.organizations.AppSettings
import co.smartreceipts.android.settings.UserPreferenceManager
import co.smartreceipts.android.settings.catalog.UserPreference
import com.hadisatrio.optional.Optional
import com.nhaarman.mockito_kotlin.*
import io.reactivex.Single
import org.intellij.lang.annotations.Language
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AppPreferencesSynchronizerTest {

    // Class under test
    private lateinit var appPreferencesSynchronizer: AppPreferencesSynchronizer

    private val organizationPreferences1 = AppSettings.OrganizationPreferences(preferencesJsonObject)
    private val organizationPreferences2 = AppSettings.OrganizationPreferences(simplifiedPreferencesJsonObject)

    private val userPreferencesManager = mock<UserPreferenceManager>()

    @Before
    fun setUp() {
        appPreferencesSynchronizer = AppPreferencesSynchronizer(userPreferencesManager)

        whenever(userPreferencesManager.userPreferencesSingle).thenReturn(Single.just(UserPreference.values()))
    }

    @Test
    fun checkFloatPreferenceTest() {

        val preference = UserPreference.Receipts.MinimumReceiptPrice
        val preferenceName = RuntimeEnvironment.application.getString(preference.name)
        whenever(userPreferencesManager.name(eq(preference))).thenReturn(preferenceName)
        whenever(userPreferencesManager.get(eq(preference))).thenReturn(10f)

        Assert.assertEquals(false, appPreferencesSynchronizer.checkPreferenceMatch(organizationPreferences1, preference).get())

        verify(userPreferencesManager, never()).set(eq(preference), any())

    }

    @Test
    fun applyFloatPreferenceWhenEqualsTest() {

        val preference = UserPreference.Receipts.MinimumReceiptPrice
        val preferenceName = RuntimeEnvironment.application.getString(preference.name)
        whenever(userPreferencesManager.name(eq(preference))).thenReturn(preferenceName)
        whenever(userPreferencesManager.get(eq(preference))).thenReturn(5.5f)

        Assert.assertEquals(Optional.of(true), appPreferencesSynchronizer.applyPreference(organizationPreferences1, preference))

        verify(userPreferencesManager, never()).set(eq(preference), any())

    }

    @Test
    fun applyFloatPreferenceWhenNotEqualsTest() {

        val preference = UserPreference.Receipts.MinimumReceiptPrice
        val preferenceName = RuntimeEnvironment.application.getString(preference.name)
        whenever(userPreferencesManager.name(eq(preference))).thenReturn(preferenceName)
        whenever(userPreferencesManager.get(eq(preference))).thenReturn(10f)

        Assert.assertEquals(Optional.of(false), appPreferencesSynchronizer.applyPreference(organizationPreferences1, preference))

        verify(userPreferencesManager, times(1)).set(eq(preference), any())
        verify(userPreferencesManager, times(1)).set(eq(preference), eq(5.5f))

    }

    @Test
    fun applyIntPreferenceTest() {

        val preference = UserPreference.General.DefaultReportDuration
        val preferenceName = RuntimeEnvironment.application.getString(preference.name)
        whenever(userPreferencesManager.name(eq(preference))).thenReturn(preferenceName)
        whenever(userPreferencesManager.get(eq(preference))).thenReturn(8)

        Assert.assertEquals(Optional.of(false), appPreferencesSynchronizer.applyPreference(organizationPreferences1, preference))

        verify(userPreferencesManager, times(1)).set(eq(preference), any())
        verify(userPreferencesManager, times(1)).set(eq(preference), eq(6))

    }

    @Test
    fun applyBooleanPreferenceTest() {

        val preference = UserPreference.General.IncludeCostCenter
        val preferenceName = RuntimeEnvironment.application.getString(preference.name)
        whenever(userPreferencesManager.name(eq(preference))).thenReturn(preferenceName)
        whenever(userPreferencesManager.get(eq(preference))).thenReturn(false)

        Assert.assertEquals(Optional.of(false), appPreferencesSynchronizer.applyPreference(organizationPreferences1, preference))

        verify(userPreferencesManager, times(1)).set(eq(preference), any())
        verify(userPreferencesManager, times(1)).set(eq(preference), eq(true))

    }

    @Test
    fun applyStringPreferenceTest() {

        val preference = UserPreference.General.DefaultCurrency
        val preferenceName = RuntimeEnvironment.application.getString(preference.name)
        whenever(userPreferencesManager.name(eq(preference))).thenReturn(preferenceName)
        whenever(userPreferencesManager.get(eq(preference))).thenReturn("another str")

        Assert.assertEquals(Optional.of(false), appPreferencesSynchronizer.applyPreference(organizationPreferences1, preference))

        verify(userPreferencesManager, times(1)).set(eq(preference), any())
        verify(userPreferencesManager, times(1)).set(eq(preference), eq("str"))

    }

    @Test
    fun checkOrganizationPreferencesWhenNotSameTest() {

        prepareForSimplifiedResponse()

        val preference1 = UserPreference.General.DefaultReportDuration
        val preference1Name = RuntimeEnvironment.application.getString(preference1.name)
        whenever(userPreferencesManager.name(eq(preference1))).thenReturn(preference1Name)
        whenever(userPreferencesManager.get(eq(preference1))).thenReturn(15)

        appPreferencesSynchronizer.checkOrganizationPreferencesMatch(organizationPreferences2).test()
            .assertComplete()
            .assertNoErrors()
            .assertResult(false)
    }

    @Test
    fun checkOrganizationPreferencesWhenSameTest() {

        prepareForSimplifiedResponse()

        appPreferencesSynchronizer.checkOrganizationPreferencesMatch(organizationPreferences2).test()
            .assertComplete()
            .assertNoErrors()
            .assertResult(true)
    }

    @Test
    fun applyOrganizationPreferencesTest() {

        prepareForSimplifiedResponse()

        val preference1 = UserPreference.General.DefaultReportDuration
        val preference1Name = RuntimeEnvironment.application.getString(preference1.name)
        whenever(userPreferencesManager.name(eq(preference1))).thenReturn(preference1Name)
        whenever(userPreferencesManager.get(eq(preference1))).thenReturn(15)

        appPreferencesSynchronizer.applyOrganizationPreferences(organizationPreferences2).test()
            .assertComplete()
            .assertNoErrors()

        verify(userPreferencesManager).set(eq(preference1), any())
        verify(userPreferencesManager).set(eq(preference1), eq(6))
        verify(userPreferencesManager, never()).set(eq(UserPreference.General.DefaultCurrency), any())
        verify(userPreferencesManager, never()).set(eq(UserPreference.General.IncludeCostCenter), any())
        verify(userPreferencesManager, never()).set(eq(UserPreference.Receipts.MinimumReceiptPrice), any())
    }


    private fun prepareForSimplifiedResponse() {
        val context = RuntimeEnvironment.application

        val preference1 = UserPreference.General.DefaultReportDuration
        val preference1Name = context.getString(preference1.name)
        whenever(userPreferencesManager.name(eq(preference1))).thenReturn(preference1Name)
        whenever(userPreferencesManager.get(eq(preference1))).thenReturn(6)

        val preference2 = UserPreference.General.DefaultCurrency
        val preference2Name = context.getString(preference2.name)
        whenever(userPreferencesManager.name(eq(preference2))).thenReturn(preference2Name)
        whenever(userPreferencesManager.get(eq(preference2))).thenReturn("str")

        val preference3 = UserPreference.General.IncludeCostCenter
        val preference3Name = context.getString(preference3.name)
        whenever(userPreferencesManager.name(eq(preference3))).thenReturn(preference3Name)
        whenever(userPreferencesManager.get(eq(preference3))).thenReturn(true)

        val preference4 = UserPreference.Receipts.MinimumReceiptPrice
        val preference4Name = context.getString(preference4.name)
        whenever(userPreferencesManager.name(eq(preference4))).thenReturn(preference4Name)
        whenever(userPreferencesManager.get(eq(preference4))).thenReturn(5.5f)
    }


    companion object {
        @Language("JSON")
        private val preferencesJsonObject = JSONObject(
            """
            {
                    "TripDuration": 6,
                    "isocurr": "str",
                    "dateseparator": "-",
                    "trackcostcenter": true,
                    "PredictCats": true,
                    "MatchNameCats": true,
                    "MatchCommentCats": true,
                    "OnlyIncludeExpensable": false,
                    "ExpensableDefault": null,
                    "IncludeTaxField": false,
                    "TaxPercentage": null,
                    "PreTax": false,
                    "EnableAutoCompleteSuggestions": true,
                    "MinReceiptPrice": 5.5,
                    "DefaultToFirstReportDate": null,
                    "ShowReceiptID": false,
                    "UseFullPage": false,
                    "UsePaymentMethods": true,
                    "IncludeCSVHeaders": true,
                    "PrintByIDPhotoKey": false,
                    "PrintCommentByPhoto": false,
                    "EmailTo": "email@to",
                    "EmailCC": "email@cc",
                    "EmailBCC": "email@bcc",
                    "EmailSubject": "email subject",
                    "SaveBW": true,
                    "LayoutIncludeReceiptDate": true,
                    "LayoutIncludeReceiptCategory": true,
                    "LayoutIncludeReceiptPicture": true,
                    "MileageTotalInReport": true,
                    "MileageRate": 10,
                    "MileagePrintTable": false,
                    "MileageAddToPDF": false,
                    "PdfFooterString": "custom footer string"
            }
        """
        )

        @Language("JSON")
        private val simplifiedPreferencesJsonObject = JSONObject(
            """
            {
                    "TripDuration": 6,
                    "isocurr": "str",
                    "trackcostcenter": true,
                    "TaxPercentage": null,
                    "MinReceiptPrice": 5.5
            }
        """
        )

    }

}