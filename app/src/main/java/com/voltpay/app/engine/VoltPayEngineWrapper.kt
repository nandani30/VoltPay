package com.voltpay.app.engine

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

interface UssdResultCallback {
    fun onSuccess(message: String)
    fun onError(error: String, carrierText: String)
}

object VoltPayEngineWrapper {
    
    private val scope = CoroutineScope(Dispatchers.Main)
    private var currentJob: Job? = null
    private var engine: UssdEngine? = null

    fun initialize(context: Context) {
        if (engine == null) {
            engine = UssdEngine(context.applicationContext, null)
        }
    }

    fun isServiceEnabled(): Boolean {
        return engine?.isServiceEnabled() == true
    }

    fun checkBalance(callback: UssdResultCallback) {
        val eng = engine ?: return
        val runner = ActionRunner(eng)
        
        currentJob?.cancel()
        currentJob = scope.launch {
            val run = runner.runAction(Actions.CheckBalance, emptyMap(), this)
            val result = run.result.await()
            if (result.success) {
                callback.onSuccess(result.resultText)
            } else {
                callback.onError("Failed to check balance", result.resultText)
            }
        }
    }

    fun sendMoney(upiId: String, amount: Double, callback: UssdResultCallback) {
        val eng = engine ?: return
        val runner = ActionRunner(eng)
        
        currentJob?.cancel()
        currentJob = scope.launch {
            val vars = mapOf(
                "upi_id" to upiId,
                "amount" to (if (amount == Math.floor(amount)) amount.toInt().toString() else amount.toString())
            )
            val run = runner.runAction(Actions.SendUpi, vars, this)
            val result = run.result.await()
            if (result.success) {
                callback.onSuccess(result.resultText)
            } else {
                callback.onError("Payment Failed", result.resultText)
            }
        }
    }

    fun cancel() {
        currentJob?.cancel()
        scope.launch {
            engine?.cancel()
        }
    }
}
