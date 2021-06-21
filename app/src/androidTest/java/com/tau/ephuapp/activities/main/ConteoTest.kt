package com.tau.ephuapp.activities.main


import android.content.ContentProvider
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.LargeTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.kbj.androxlsxparser.mxlsxparser.StreamingReader
import com.tau.ephuapp.R
import com.tau.ephuapp.classes.Utilities
import com.tau.ephuapp.models.ItemCount
import com.tau.ephuapp.models.ParameterType
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.joda.time.DateTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random


@LargeTest
@RunWith(AndroidJUnit4ClassRunner::class)
class ConteoTest {
    private enum class CountType{
        Count, Recount
    }
    private val skusList = arrayOf(
        "44306",
        "4242",
        "10346",
        "8633",
        "4430",
        "6221",
        "6219",
        "12202",
        "6222",
        "6226",
        "15535",
        "11816",
        "4445",
        "11754",
        "10006",
        "10015",
        "10004",
        "10005",
        "11753",
        "10012",
        "10003",
        "10008",
        "10011",
        "4446",
        "4909",
        "4910",
        "5471",
        "14883",
        "8649",
        "8605",
        "8602",
        "12476",
        "14848",
        "14852",
        "14850",
        "14851",
        "15561",
        "15562",
        "7203",
        "7201",
        "7206",
        "7202",
        "1411",
        "1433",
        "1435",
        "13181",
        "10397",
        "10398",
        "10400",
        "10401",
        "5152",
        "5150",
        "5151",
        "14677",
        "14876",
        "11310",
        "2352",
        "14898",
        "14896",
        "14897",
        "14676",
        "14874",
        "11308",
        "2350",
        "14519",
        "14678",
        "14875",
        "14520",
        "11309",
        "2351",
        "15859",
        "14725",
        "14724",
        "14727",
        "14620",
        "4810",
        "4811",
        "4865",
        "4800",
        "4807",
        "11405",
        "11404",
        "5014",
        "4555",
        "14577",
        "11160",
        "10347",
        "4550",
        "4531",
        "4448",
        "5384",
        "4507",
        "4140",
        "15584",
        "4528",
        "4505",
        "4520",
        "5013",
        "4336",
        "3247",
        "14854",
        "14853",
        "15744",
        "10460",
        "14722",
        "15543",
        "13441",
        "11011",
        "14908",
        "14903",
        "2605",
        "14905",
        "14907",
        "14906",
        "1888",
        "2974",
        "8110",
        "4091",
        "5059",
        "4306",
        "10750",
        "15539",
        "8207",
        "15745",
        "14723",
        "14624",
        "10632",
        "10009"
    )
    @Rule
    @JvmField
    var mActivityTestRule = ActivityScenarioRule(MainActivity::class.java)

    @Rule
    @JvmField
    var mGrantPermissionRule: GrantPermissionRule =
            GrantPermissionRule.grant(
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.READ_PHONE_STATE
            )

    @Test
    fun conteoTest() {
        val assetsManager = InstrumentationRegistry.getInstrumentation().targetContext.assets
        val inputStream: InputStream = assetsManager.open("test.xlsx")
        val workbook: Workbook = StreamingReader.builder()
            .rowCacheSize(100) // number of rows to keep in memory (defaults to 10)
            .bufferSize(4096) // buffer size to use when reading InputStream to file (defaults to 1024)
            .open(InstrumentationRegistry.getInstrumentation().targetContext, inputStream) // InputStream or File for XLSX file (required)

        val appCompatImageButton = onView(
            allOf(
                withId(R.id.next), withContentDescription("NEXT"),
                childAtPosition(
                    allOf(
                        withId(R.id.background),
                        childAtPosition(
                            withId(android.R.id.content),
                            0
                        )
                    ),
                    6
                ),
                isDisplayed()
            )
        )
        appCompatImageButton.perform(click())

        val materialButton = onView(
            allOf(
                withId(R.id.done), withText("DONE"),
                childAtPosition(
                    allOf(
                        withId(R.id.background),
                        childAtPosition(
                            withId(android.R.id.content),
                            0
                        )
                    ),
                    7
                ),
                isDisplayed()
            )
        )
        materialButton.perform(click())

        val materialButton2 = onView(
            allOf(
                withId(R.id.done), withText("DONE"),
                childAtPosition(
                    allOf(
                        withId(R.id.background),
                        childAtPosition(
                            withId(android.R.id.content),
                            0
                        )
                    ),
                    7
                ),
                isDisplayed()
            )
        )
        materialButton2.perform(click())
        Thread.sleep(3000)
        var taskCountId: Int = 3100
        var taskRecountId: Int = 3101
        var doingCountType: CountType = CountType.Count
        val taskCard = onView(
            allOf(
                withText("TI # ${taskCountId} - 1"),
                withId(R.id.taskNumberTv),
                isDisplayed()
            )
        )
        if (taskCard != null) {
            taskCard.perform(click())
            val appCompatImageButton2 = onView(
                allOf(
                    withId(R.id.optionsBtn),
                    childAtPosition(
                            childAtPosition(
                                    withId(R.id.itemCard),
                                    0
                            ),
                            2
                    ),
                    isDisplayed()
                )
            )
            appCompatImageButton2.perform(click())
            Thread.sleep(2000)
            try {
                onView(
                        allOf(
                                withId(R.id.title), withText("pausar"),
                                childAtPosition(
                                        childAtPosition(
                                                withId(R.id.content),
                                                0
                                        ),
                                        0
                                ),
                                isDisplayed()
                        )
                ).perform(click())
                onView(
                        allOf(
                                withId(R.id.optionsBtn),
                                childAtPosition(
                                        childAtPosition(
                                                withId(R.id.itemCard),
                                                0
                                        ),
                                        2
                                ),
                                isDisplayed()
                        )
                ).perform(click())
                Thread.sleep(2000)
                onView(
                        allOf(
                                withId(R.id.title), withText("reanudar"),
                                childAtPosition(
                                        childAtPosition(
                                                withId(R.id.content),
                                                0
                                        ),
                                        0
                                ),
                                isDisplayed()
                        )
                ).perform(click())
            } catch (e: Exception)
            {
                e.printStackTrace()
                onView(
                        allOf(
                                withId(R.id.title), withText("reanudar"),
                                childAtPosition(
                                        childAtPosition(
                                                withId(R.id.content),
                                                0
                                        ),
                                        0
                                ),
                                isDisplayed()
                        )
                ).perform(click())
            }
            Thread.sleep(1000)
            for (sheetIndex in 0..workbook.numberOfSheets)
            {
                val sheet = workbook.getSheetAt(sheetIndex)
                println(sheet.sheetName)
                for (row in 16..sheet.physicalNumberOfRows) {
                    // si la ubicacion no es la actual, pasamos a la siguiente
                    if(true) {
                        try {
                            val appCompatImageButton3 = onView(
                                    allOf(
                                            withId(R.id.rightBtn),
                                            isDisplayed()
                                    )
                            )
                            // presionamos el boton de siguiente ubicacion
                            appCompatImageButton3.perform(click())
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    val textInputEditText = onView(
                            allOf(
                                    withId(R.id.skuEt),
                                    isDisplayed()
                            )
                    )
                    // ingresamos un sku de la lista
                    textInputEditText.perform(
                            replaceText(
                                    skusList[Random.nextInt(
                                            0,
                                            skusList.size - 1
                                    )]
                            ), closeSoftKeyboard()
                    )

                    val textInputEditText2 = onView(
                            allOf(
                                    withId(R.id.skuEt),
                                    isDisplayed()
                            )
                    )
                    // le damos a la accion buscar
                    textInputEditText2.perform(pressImeActionButton())

                    Thread.sleep(1000)
                    val quantity = Random.nextInt(10, 400)
                    val textInputEditText3 = onView(
                            allOf(
                                    withId(R.id.quantityEt),
                                    isDisplayed()
                            )
                    )
                    // ingresamos una cantidad
                    textInputEditText3.perform(replaceText(quantity.toString()), closeSoftKeyboard())
                    try {
                        val day = getNormalizedNumber(Random.nextInt(1, 28))
                        val month = getNormalizedNumber(Random.nextInt(1, 12))
                        val year = getNormalizedNumber(Random.nextInt(2020, 2030))
                        Log.i("generatedDate", "$day/$month/$year")
                        val textInputEditText4 = onView(
                                allOf(
                                        withId(R.id.expiryDateEt),
                                        isDisplayed()
                                )
                        )
                        // ingresamos una fecha
                        /*textInputEditText4.perform(replaceText("$day/MM/YYYY"), closeSoftKeyboard())
                textInputEditText4.perform(replaceText("$day/$month/YYYY"), closeSoftKeyboard())*/
                        textInputEditText4.perform(
                                replaceText("$day/$month/$year"),
                                closeSoftKeyboard()
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    try {
                        val floatingActionButton = onView(
                                allOf(
                                        withId(R.id.saveBtn),
                                        isDisplayed()
                                )
                        )
                        // le damos al boton guardar
                        floatingActionButton.perform(scrollTo(), click())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    try {
                        val materialButton3 = onView(
                                allOf(
                                        withId(android.R.id.button1), withText("Aceptar"),
                                        isDisplayed()
                                )
                        )
                        // aceptamos en la ventana de dialogo
                        materialButton3.perform(scrollTo(), click())
                        Thread.sleep(2000)
                        //nextPosition()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun generateItemCount(): ItemCount{
        val countToSave = ItemCount(id = Random.nextInt(999999, 99999999), localId = UUID.randomUUID().toString())
        countToSave.ephuDeviceId = Utilities.getAndroidId(InstrumentationRegistry.getInstrumentation().targetContext)
        countToSave.dirty = true
        countToSave.readTimestamp = DateTime().toLocalDateTime().toString()
        countToSave.uploaded = false
        countToSave.sent = false
        return countToSave
    }

    private fun getNormalizedNumber(number: Int): String{
        if(number < 10){
            return "0$number"
        }
        return number.toString()
    }

    private fun finishCount(){
        val materialButton3 = onView(
            allOf(
                withId(R.id.doneBtn), withText("GUARDAR PENDIENTES"),
                childAtPosition(
                    childAtPosition(
                        withId(android.R.id.content),
                        0
                    ),
                    6
                ),
                isDisplayed()
            )
        )
        materialButton3.perform(click())

        val materialButton4 = onView(
            allOf(
                withId(android.R.id.button1), withText("Aceptar"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.buttonPanel),
                        0
                    ),
                    3
                )
            )
        )
        materialButton4.perform(scrollTo(), click())
    }

    private fun nextPosition(){
        val appCompatImageButton3 = onView(
            allOf(
                withId(R.id.rightBtn),
                childAtPosition(
                    childAtPosition(
                        withId(android.R.id.content),
                        0
                    ),
                    3
                ),
                isDisplayed()
            )
        )
        appCompatImageButton3.perform(click())
    }

    private fun registerCount(sku: String){
        val textInputEditText = onView(
            allOf(
                withId(R.id.skuEt),
                isDisplayed()
            )
        )
        textInputEditText.perform(replaceText("044306"), closeSoftKeyboard())

        val textInputEditText2 = onView(
            allOf(
                withId(R.id.skuEt), withText("044306"),
                isDisplayed()
            )
        )
        textInputEditText2.perform(pressImeActionButton())
        Thread.sleep(1000)
        val quantity = Random.nextInt(400)
        val textInputEditText3 = onView(
            allOf(
                withId(R.id.quantityEt),
                isDisplayed()
            )
        )
        textInputEditText3.perform(replaceText(quantity.toString()), closeSoftKeyboard())
        val day = Random.nextInt(1, 28)
        val month = Random.nextInt(1, 12)
        val year = Random.nextInt(2020, 2030)
        val textInputEditText4 = onView(
            allOf(
                withId(R.id.expiryDateEt),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.expiryDateContainer),
                        0
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        textInputEditText4.perform(replaceText("$day/$month/$year"), closeSoftKeyboard())

        val floatingActionButton = onView(
            allOf(
                withId(R.id.saveBtn),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.editLy),
                        1
                    ),
                    2
                )
            )
        )
        floatingActionButton.perform(scrollTo(), click())

        val materialButton3 = onView(
            allOf(
                withId(android.R.id.button1), withText("Aceptar"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.buttonPanel),
                        0
                    ),
                    3
                )
            )
        )
        materialButton3.perform(scrollTo(), click())
    }

    private fun childAtPosition(
        parentMatcher: Matcher<View>, position: Int
    ): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
