package com.tau.ephuapp.activities.main


import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.AndroidJUnit4
import com.kbj.androxlsxparser.mxlsxparser.StreamingReader
import com.tau.ephuapp.R
import org.apache.poi.ss.usermodel.Workbook
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.isEmptyOrNullString
import org.hamcrest.Matchers.not
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.InputStream
import java.lang.Exception


@LargeTest
@RunWith(AndroidJUnit4::class)
class ConteoUnoTest {
    val TAG = "STRESS_TEST"
    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(MainActivity::class.java)

    @Rule
    @JvmField
    var mGrantPermissionRule =
        GrantPermissionRule.grant(
            "android.permission.CAMERA",
            "android.permission.READ_PHONE_STATE"
        )

    @Test
    fun stressTest() {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        var workbook: Workbook? = null
        try {
            val assetsManager = InstrumentationRegistry.getInstrumentation().targetContext.assets
            val inputStream: InputStream = assetsManager.open("test.xlsx")
            workbook = StreamingReader.builder()
                .rowCacheSize(100) // number of rows to keep in memory (defaults to 10)
                .bufferSize(4096) // buffer size to use when reading InputStream to file (defaults to 1024)
                .open(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                    inputStream
                ) // InputStream or File for XLSX file (required)
        }catch (e: Exception){
            Log.e("ConteoUnoTest", "Error al cargar archivo de pruebas", e)
            e.printStackTrace()
            return
        }
        Thread.sleep(7000)
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

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(4000)

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

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(4000)

        pressTaskOptionsBtn()

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(250)
        try {
            val iniciarBtn = onView(
                allOf(
                    withId(R.id.title), withSubstring("iniciar"),
                    isDisplayed()
                )
            ).check(matches(isDisplayed()))
            iniciarBtn.perform(click())
        } catch(e: Exception){
            try {
                val pausarBtn = onView(
                    allOf(
                        withId(R.id.title), withSubstring("pausar"),
                        isDisplayed()
                    )
                ).check(matches(isDisplayed()))
                pausarBtn.perform(click())
                pressTaskOptionsBtn()
                val reanudarBtn2 = onView(
                    allOf(
                        withId(R.id.title), withSubstring("reanudar"),
                        isDisplayed()
                    )
                )
                reanudarBtn2.perform(click())
            } catch(e: Exception){
                val reanudarBtn = onView(
                    allOf(
                        withId(R.id.title), withSubstring("reanudar"),
                        isDisplayed()
                    )
                )
                reanudarBtn.perform(click())
            }
        }
        Thread.sleep(4000)
        // ya con la tarea abierta
        try {
            val skuInput = onView(
                allOf(
                    withId(R.id.skuEt),
                    isDisplayed()
                )
            )
            skuInput.check(matches(isDisplayed()))
            val sheet = workbook.getSheetAt(0)
            Log.i(TAG, "Examinando hoja 0:${sheet.sheetName}")
            for (row in sheet) {
                Log.i(TAG, "escaneando fila ${row.rowNum}")
                if(row.rowNum >= 16){
                    val location = row.getCell(0).stringCellValue.split(" ")[1]
                    var isValidLocation = true
                    do {
                        try {
                            onView(
                                allOf(
                                    withId(R.id.locationCodeTv),
                                    withSubstring(location),
                                    isDisplayed()
                                )
                            ).check(matches(isDisplayed()))
                            isValidLocation = true
                        } catch (e: Exception) {
                            isValidLocation = false
                            try {
                                pressRightBtn()
                            } catch (e2: Exception) {
                                e2.printStackTrace()
                                Log.i(TAG, "No quedan mas ubicaciones por explorar")
                            }
                        }
                    } while(!isValidLocation)
                    if (isValidLocation) {
                        val sku = row.getCell(2).stringCellValue
                        // si el sku leido es vacio o null, pasamos al siguiente registro
                        if(sku.isNullOrEmpty()){
                            Log.e(TAG, "El sku de la fila ${row.rowNum} esta vacio o es invalido")
                            continue
                        }
                        val qty = row.getCell(6).numericCellValue
                        // si la cantidad es invalida
                        if(qty.isNaN()){
                            Log.e(TAG, "La cantidad de la fila ${row.rowNum} esta vacia o es invalida")
                            continue
                        }
                        Log.i(TAG, "Leido sku $sku con cantidad: $qty en la fila ${row.rowNum}")
                        skuInput.perform(click())
                        skuInput.perform(replaceText(sku), closeSoftKeyboard())
                        skuInput.perform(pressImeActionButton())
                        try {
                            onView(
                                allOf(
                                    withId(R.id.descriptionEt),
                                    not(withText(isEmptyOrNullString())),
                                    isDisplayed()
                                )
                            ).check(matches(isDisplayed()))
                        } catch (e: Exception) {
                            Log.e(TAG, "item $sku No encontrado. Pasando al siguiente registro...")
                            continue
                        }
                        val qtyInput = onView(
                            allOf(
                                withId(R.id.quantityEt),
                                isDisplayed()
                            )
                        )
                        qtyInput.perform(click())
                        qtyInput.perform(
                            replaceText(qty.toString()),
                            closeSoftKeyboard()
                        )
                        pressSaveBtn()
                        acceptBtnOnDialog()
                    }
                }
            }
            //saveAllCounts()
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

    private fun pressTaskOptionsBtn() {
        val optionsBtn = onView(
            allOf(
                withId(R.id.optionsBtn),
                hasSibling(
                    withText("TI # 3100 - 1")
                )
            )
        )
        optionsBtn.perform(click())
    }

    private fun pressSaveBtn() {
        val saveBtn = onView(
            allOf(
                withId(R.id.saveBtn)
            )
        )
        saveBtn.perform(scrollTo(), click())
    }

    private fun acceptBtnOnDialog() {
        val aceptarBtnOnDialog = onView(
            allOf(
                withId(android.R.id.button1), withText("Aceptar"),
                isDisplayed()
            )
        )
        aceptarBtnOnDialog.perform(scrollTo(), click())
    }

    private fun pressRightBtn() {
        // Pasar a siguiente ubicacion
        val rightBtn = onView(
            allOf(
                withId(R.id.rightBtn),
                isDisplayed()
            )
        )
        rightBtn.perform(click())
    }

    private fun saveAllCounts(){
        // guardar todos los conteos registrados
        val saveAllCountsBtn = onView(
            allOf(
                withId(R.id.doneBtn),
                isDisplayed()
            )
        )
        saveAllCountsBtn.perform(click())

        acceptBtnOnDialog()
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
