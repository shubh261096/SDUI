package com.iffelse.divkit

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.yandex.div.DivDataTag
import com.yandex.div.core.Div2Context
import com.yandex.div.core.DivActionHandler
import com.yandex.div.core.DivConfiguration
import com.yandex.div.core.DivViewFacade
import com.yandex.div.core.expression.variables.DivVariableController
import com.yandex.div.core.view2.Div2View
import com.yandex.div.data.DivParsingEnvironment
import com.yandex.div.json.ParsingErrorLogger
import com.yandex.div2.DivAction
import com.yandex.div2.DivData
import org.json.JSONObject


class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var context: Div2Context
    private var lastDivVariableController: DivVariableController? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val divConfiguration = DivConfiguration.Builder(CoilDivImageLoader(this))
            .actionHandler(object : DivActionHandler() {
                override fun getUseActionUid(): Boolean {
                    Log.i("TAG", "getUseActionUid: ")
                    return super.getUseActionUid()
                }

                override fun handleAction(action: DivAction, view: DivViewFacade): Boolean {
                    Log.i("TAG", "handleAction: " + action.url?.evaluate(view.expressionResolver))
                    val stringUri = action.url?.evaluate(view.expressionResolver)!!
                    when (stringUri.scheme) {
                        "action" -> {
                            if (stringUri.host == "click") {
                                val value =
                                    stringUri.query?.let { getQueryParamValue(it, "key") }
                                Toast.makeText(this@MainActivity, "Action Clicked", Toast.LENGTH_SHORT).show()
                                Log.i(TAG, "handleAction: Value = $value")
                            }
                        }
                    }
                    return super.handleAction(action, view)
                }

                override fun handlePayload(payload: JSONObject) {
                    Log.i("TAG", "handlePayload: $payload")
                    super.handlePayload(payload)
                }
            })
            .visualErrorsEnabled(false)
            .build()
        this.context = Div2Context(
            baseContext = this,
            configuration = divConfiguration,
            lifecycleOwner = this
        )

        lastDivVariableController = context.divVariableController
        updateUIWithJSON()
    }


    private fun updateUIWithJSON() {
        runOnUiThread {
            val linearLayout = findViewById<LinearLayout>(R.id.container)
            linearLayout.removeAllViews()

            val divJson = DivAssetReader(context).read("demo.json")
            var templateJson = divJson.optJSONObject("templates")
            val cardJson = divJson.getJSONObject("card")
            val div = DivViewFactory(context, templateJson).createView(cardJson)

            linearLayout.addView(div)
            div.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            div.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        }
    }

    fun getQueryParamValue(query: String, paramName: String): String? {
        val params = query.split('&')
        for (param in params) {
            val pair = param.split('=')
            if (pair.size == 2 && pair[0] == paramName) {
                return pair[1]
            }
        }
        return null
    }

}

class DivViewFactory(
    private val context: Div2Context,
    private val templatesJson: JSONObject? = null
) {

    private val environment = DivParsingEnvironment(ParsingErrorLogger.ASSERT).apply {
        if (templatesJson != null) parseTemplates(templatesJson)
    }

    fun createView(cardJson: JSONObject): Div2View {
        val divData = DivData(environment, cardJson)
        return Div2View(context).apply {
            setData(divData, DivDataTag(divData.logId))
        }
    }
}