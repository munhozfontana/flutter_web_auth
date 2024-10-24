package com.linusu.flutter_web_auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri

import androidx.browser.customtabs.CustomTabsIntent

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar

class FlutterWebAuthPlugin(private var context: Context? = null, private var channel: MethodChannel? = null): MethodCallHandler, FlutterPlugin , ActivityAware{

    private var activity: Activity? = null

    companion object {
    val callbacks = mutableMapOf<String, Result>()

    @JvmStatic
    fun registerWith(registrar: Registrar) {
        val plugin = FlutterWebAuthPlugin()
        plugin.initInstance(registrar.messenger(), registrar.context())
    }

  }

  fun initInstance(messenger: BinaryMessenger, context: Context) {
      this.context = context
      channel = MethodChannel(messenger, "flutter_web_auth")
      channel?.setMethodCallHandler(this)
  }

  override public fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
      initInstance(binding.getBinaryMessenger(), binding.getApplicationContext())
  }

  override public fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
      context = null
      channel = null
  }

  override fun onMethodCall(call: MethodCall, resultCallback: Result) {
    when (call.method) {
        "authenticate" -> {
            val url = Uri.parse(call.argument<String>("url"))
            val callbackUrlScheme = call.argument<String>("callbackUrlScheme")!!
            val preferEphemeral = call.argument<Boolean>("preferEphemeral")!!


            callbacks[callbackUrlScheme] = resultCallback

            val intent = CustomTabsIntent.Builder().build()
            val keepAliveIntent = Intent(context, KeepAliveService::class.java)

            if (preferEphemeral) {
                intent.intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            }

            intent.intent.putExtra("android.support.customtabs.extra.KEEP_ALIVE", keepAliveIntent)

            // Usar a activity ou o contexto atual
            activity?.let {
                intent.launchUrl(it, url)
            } ?: resultCallback.error("NO_ACTIVITY", "Activity not available", null)
        }
        "cleanUpDanglingCalls" -> {
          callbacks.forEach{ (_, danglingResultCallback) ->
              danglingResultCallback.error("CANCELED", "User canceled login", null)
          }
          callbacks.clear()
          resultCallback.success(null)
        }
        else -> resultCallback.notImplemented()
    }
  }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {
        activity = null
    }
}
