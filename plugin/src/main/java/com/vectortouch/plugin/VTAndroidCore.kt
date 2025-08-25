package com.vectortouch.plugin

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.ext.SdkExtensions
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot


class VTAndroidCore(godot: Godot): GodotPlugin(godot) {
	override fun getPluginName() = BuildConfig.GODOT_PLUGIN_NAME

	override fun getPluginSignals(): MutableSet<SignalInfo> {
		val signals: MutableSet<SignalInfo> = mutableSetOf()
		signals.add(SignalInfo("file_picker_callback", Array::class.java))
		signals.add(SignalInfo("reference_img_picker_callback", String::class.java, String::class.java))
		return signals
	}

	override fun onMainActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onMainActivityResult(requestCode, resultCode, data)

		if (requestCode == FilePicker.IMAGE_PICKER_REQUEST) {
			val uri: Uri? = data?.data
			uri?.let {
				emitSignal("reference_img_picker_callback", it.toString(), getFileType(uri))
			}
		}

		if (requestCode == FilePicker.FILE_PICKER_REQUEST) {
			if (resultCode == Activity.RESULT_CANCELED) {
				Log.d(pluginName, "File picker canceled")
				return
			}
			if (resultCode == Activity.RESULT_OK) {
				val selectedFiles: MutableList<String> = mutableListOf()
				val clipData = data?.clipData

				if (clipData != null) {
					// Handle multiple file selection.
					for (i in 0 until clipData.itemCount) {
						val uri = clipData.getItemAt(i).uri
						uri?.let {
							try {
								context.contentResolver.takePersistableUriPermission(
									it,
									Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
								)
							} catch (e: SecurityException) {
								Log.w(pluginName, "Unable to persist URI: $it", e)
							}
							selectedFiles.add(it.toString())
						}
					}
				} else {
					val uri: Uri? = data?.data
					uri?.let {
						try {
							context.contentResolver.takePersistableUriPermission(
								it,
								Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
							)
						} catch (e: SecurityException) {
							Log.w(pluginName, "Unable to persist URI: $it", e)
						}
						selectedFiles.add(it.toString())
					}
				}

				emitSignal("file_picker_callback", selectedFiles.toTypedArray())
			}
		}
	}

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

	@UsedByGodot
	fun showFilePicker(currentDirectory: String, filename: String, fileMode: Int, filters: Array<String>) {
		val activity = activity ?: return
		FilePicker.show(activity, currentDirectory, filename, fileMode, filters)
	}

	@UsedByGodot
	fun writeToUri(uri: String, data: ByteArray) {
		FilePicker.write(context.contentResolver, uri.toUri(), data)
	}

	@UsedByGodot
	fun readFromUri(uri: String): ByteArray? {
		return FilePicker.read(context.contentResolver, uri.toUri())
	}

	@UsedByGodot
	fun pickReferenceImage() {
		val intent: Intent
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && SdkExtensions.getExtensionVersion(Build.VERSION_CODES.R) >= 2) {
			intent = Intent(MediaStore.ACTION_PICK_IMAGES)
		} else {
			intent = Intent(Intent.ACTION_GET_CONTENT)
			intent.addCategory(Intent.CATEGORY_OPENABLE)
		}
		intent.type = "image/*"
		activity?.startActivityForResult(intent, FilePicker.IMAGE_PICKER_REQUEST)
	}

	private fun getFileType(uri: Uri): String {
		val mimeType = context.contentResolver.getType(uri)
		return if (mimeType != null) {
			 MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "unknown"
		 } else {
			 "unknown"
		 }
	}
}
