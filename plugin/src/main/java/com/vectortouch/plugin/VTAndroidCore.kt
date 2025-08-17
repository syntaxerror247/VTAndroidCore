package com.vectortouch.plugin

import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot


class VTAndroidCore(godot: Godot): GodotPlugin(godot) {
	override fun getPluginName() = BuildConfig.GODOT_PLUGIN_NAME

	override fun getPluginSignals(): MutableSet<SignalInfo> {
		val signals: MutableSet<SignalInfo> = mutableSetOf()
		signals.add(SignalInfo("connected"))
		return signals
	}

	@UsedByGodot
	fun initPlugin() {
		emitSignal("connected")
	}
}
