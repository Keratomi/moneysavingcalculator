package hu.keratomi.moneysavingcalculator

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import hu.keratomi.moneysavingcalculator.logic.DescriptionAndCostRow
import hu.keratomi.moneysavingcalculator.logic.GoogleDriveSyncHandler
import hu.keratomi.moneysavingcalculator.logic.createCalculationFromJson
import hu.keratomi.moneysavingcalculator.logic.createJsonString
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.alert_dialog_with_edittext.*
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var descriptionAndCostRow: DescriptionAndCostRow
    private lateinit var googleDriveSyncHandler: GoogleDriveSyncHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        googleDriveSyncHandler =
            GoogleDriveSyncHandler(this)
        googleDriveSyncHandler.googleAuth()

        descriptionAndCostRow = DescriptionAndCostRow(
            resources,
            this,
            mainLayout
        )
        descriptionAndCostRow.createAddNewRowButton()
        descriptionAndCostRow.newCostRow()

        setLoadedCalculationDisplay(getString(R.string.new_unsaved))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)

        return super.onCreateOptionsMenu(menu)
    }

    fun loadSelectedCalculation(calculationName: String, fileContent: String) {
        resetFields()

        descriptionAndCostRow.deleteAllRows()

        val calculationFromJson = createCalculationFromJson(fileContent)

        val allInComingMoney = findViewById<TextView>(R.id.allInComingMoney)
        allInComingMoney.text = String.format("%s", calculationFromJson.allInComingMoney)

        calculationFromJson.fixCosts.forEach {
            descriptionAndCostRow.newCostRow(it.description, String.format("%s", it.cost))
            setLoadedCalculationDisplay(
                calculationName.substringBeforeLast(
                    CALCULATION_DATA_FILE_EXTENSION
                )
            )
        }
        doCalculation()
    }

    fun startCalculation(view: View) {
        doCalculation()
    }

    fun createNewCostRow(view: View) {
        descriptionAndCostRow.newCostRow()
        scrollView2.post(Runnable {
            scrollView2.fullScroll(ScrollView.FOCUS_DOWN)
            descriptionAndCostRow.fixCosts.last().description.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(
                descriptionAndCostRow.fixCosts.last().description,
                InputMethodManager.SHOW_IMPLICIT
            )
        })
        findViewById<TextView>(R.id.savableMoney).text = "0"
    }

    fun deleteCurrentCostRow(view: View) {
        val rowContainer = view.parent as LinearLayout
        descriptionAndCostRow.deleteOneCostRow(rowContainer)
        findViewById<TextView>(R.id.savableMoney).text = "0"
    }

    fun getCalculationName(menuItem: MenuItem) {
        createWindowForGetCalculationName(
            this,
            layoutInflater,
            googleDriveSyncHandler.getJustCalculationNamesFromLoadedFiles(),
            saveCalculationAsAFile
        )
    }

    fun requestNewEmptyCalculation(menuItem: MenuItem) {
        questionBeforeDoActionWithLoadedCalculation(
            R.string.loose_current,
            this,
            createNewEmptyCalculation
        )
    }

    fun startCalculationLoadingProcess(menuItem: MenuItem) {
        questionBeforeDoActionWithLoadedCalculation(
            R.string.loose_current,
            this,
            filesFromGoogleDrive
        )
    }

    fun deleteLoadedCalculation(menuItem: MenuItem) {
        questionBeforeDoActionWithLoadedCalculation(
            R.string.do_you_really_want_to_delete,
            this,
            deleteLoadedCalculation
        )
    }

    private fun resetFields() {
        findViewById<TextView>(R.id.allInComingMoney).text = null
        findViewById<TextView>(R.id.savableMoney).text = null
    }

    private fun doCalculation() {
        val fixCostsSum = descriptionAndCostRow.fixCosts.toList()
            .filter { it.cost.text.toString().toIntOrNull() != null }
            .sumBy { it.cost.text.toString().toInt() }

        var allInComingMoneyAsNumber =
            findViewById<TextView>(R.id.allInComingMoney).text.toString().toIntOrNull()
        allInComingMoneyAsNumber =
            if (allInComingMoneyAsNumber == null) 0 else allInComingMoneyAsNumber

        val releaseForSaving = allInComingMoneyAsNumber - fixCostsSum
        val savableMoneyField = findViewById<TextView>(R.id.savableMoney)
        savableMoneyField.text = releaseForSaving.toString()
    }

    private fun setLoadedCalculationDisplay(loadedCalcuclationName: String) {
        val loadedCalcuclationNameDisplay =
            findViewById<TextView>(R.id.loadedCalcuclationNameDisplay)
        loadedCalcuclationNameDisplay.text = getString(R.string.loaded, loadedCalcuclationName)
    }

    private fun saveToLocalDriveThenUploadToGoogleDrive(fileName: String) {
        val allIncomingMoney = findViewById<EditText>(R.id.allInComingMoney).text.toString()

        val saveableString = createJsonString(allIncomingMoney, descriptionAndCostRow.fixCosts)

        val locallySavedFile =
            File(applicationContext.filesDir.path + "/" + fileName + CALCULATION_DATA_FILE_EXTENSION)
        locallySavedFile.writeText(saveableString)
        googleDriveSyncHandler.uploadOrUpdateFile(locallySavedFile)
    }

    val filesFromGoogleDrive = { _: DialogInterface, _: Int ->
        googleDriveSyncHandler.queryFileList()
    }

    val createNewEmptyCalculation = { _: DialogInterface, _: Int ->
        descriptionAndCostRow.deleteAllRows()
        descriptionAndCostRow.newCostRow()
        googleDriveSyncHandler.clearLoadedFileId()
        resetFields()
        setLoadedCalculationDisplay(getString(R.string.new_unsaved))
        doCalculation()
    }

    val deleteLoadedCalculation = { dialogInterface: DialogInterface, index: Int ->
        googleDriveSyncHandler.deleteLoadedCalculation()
        createNewEmptyCalculation(dialogInterface, index)
    }

    val saveCalculationAsAFile = { dialogInterface: DialogInterface, _: Int ->
        val calclulationName = (dialogInterface as AlertDialog).calculationName

        saveToLocalDriveThenUploadToGoogleDrive(calclulationName.text.toString())
        setLoadedCalculationDisplay(calclulationName.text.toString())
        Toast.makeText(
            applicationContext,
            getString(R.string.saved_successfully, calclulationName.text),
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        googleDriveSyncHandler.authActivityResultHandler(requestCode, resultCode, data)
    }
}
