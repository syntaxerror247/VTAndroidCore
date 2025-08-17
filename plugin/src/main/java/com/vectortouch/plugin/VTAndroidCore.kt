package com.vectortouch.plugin

import android.util.Log
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.UsedByGodot


class VTAndroidCore(godot: Godot): GodotPlugin(godot) {
	override fun getPluginName() = BuildConfig.GODOT_PLUGIN_NAME

	@UsedByGodot
	fun setWindowColor(colorStr: String) {
		val color = try {
			colorStr.toColorInt()
		} catch (e: java.lang.IllegalArgumentException) {
			Log.w(pluginName, "Failed to parse background color: $colorStr", e)
			return
		}
		val decorView = activity?.window?.decorView ?: return
		runOnHostThread {
			decorView.setBackgroundColor(color)
			godot.setSystemBarsAppearance()
		}
	}

	@UsedByGodot
	fun toggleStatusBar(visible: Boolean) {
		val window = activity?.window ?: return
		val controller = WindowInsetsControllerCompat(window, window.decorView)
		if (visible) {
			controller.show(WindowInsetsCompat.Type.statusBars())
		} else {
			controller.hide(WindowInsetsCompat.Type.systemBars())
			controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
		}
	}
}
