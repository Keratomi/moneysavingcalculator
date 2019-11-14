package hu.keratomi.moneysavingcalculator

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater

fun questionBeforeLoadCalculation(context: Context, okFunction: (dialog: DialogInterface, which: Int) -> Unit) {

    val builder = AlertDialog.Builder(context)

    with(builder)
    {
        setTitle(android.R.string.dialog_alert_title)
        setMessage(R.string.loose_current)
        setPositiveButton(
            android.R.string.ok,
            DialogInterface.OnClickListener(function = okFunction)
        )
        setNegativeButton(android.R.string.no) { _, _ -> Unit }
        show()
    }
}

fun createWindowWithSavedCalculationList(context: Context, items: Array<String>, okFunction: (dialog: DialogInterface, which: Int) -> Unit) {
    val builder = AlertDialog.Builder(context)
    with(builder)
    {
        setTitle(R.string.mentett_kalkulaciok)
        setItems(items, DialogInterface.OnClickListener(function = okFunction))

        setPositiveButton(android.R.string.cancel) { _, _ -> Unit }
        show()
    }
}

fun createWindowForGetCalculationName(context: Context, inflater: LayoutInflater, okFunction: (dialog: DialogInterface, which: Int) -> Unit) {
    val builder = AlertDialog.Builder(context)
    with(builder)
    {
        setTitle(R.string.milyen_neven_mentsem)
        val dialogLayout = inflater.inflate(R.layout.alert_dialog_with_edittext, null)
        setView(dialogLayout)
        setPositiveButton(android.R.string.ok, DialogInterface.OnClickListener(function = okFunction))
        show()
    }
}