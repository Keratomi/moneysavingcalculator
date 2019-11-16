package hu.keratomi.moneysavingcalculator.logic

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.EditText
import hu.keratomi.moneysavingcalculator.data.FixCost
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.runners.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class JsonSerializerHelperTest {

    @Mock
    lateinit var mMockContext: Context

    @Before
    fun init() {

    }

    @Test
    fun shouldCreateJsonFromDataClasses() {
        val descriptionEdit = EditText(mMockContext)
        descriptionEdit.setText("description")

        val costEdit = EditText(mMockContext)
        costEdit.setText("50")


        val fixCosts = mutableListOf<FixCost>(FixCost(descriptionEdit, costEdit))
        val createdJson = createJsonString("150", fixCosts)

        Assert.assertThat(createdJson, CoreMatchers.`is`("aaa"))
    }
}