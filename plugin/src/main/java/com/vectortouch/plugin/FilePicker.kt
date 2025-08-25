package com.vectortouch.plugin

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.Log
import android.webkit.MimeTypeMap

/**
 * Utility class for managing file selection and file picker activities.
 */
internal object FilePicker {
	private const val TAG = "VTFilePicker"

	const val FILE_PICKER_REQUEST = 100
	const val IMAGE_PICKER_REQUEST = 101

	// Constants for fileMode values
	private const val FILE_MODE_OPEN_FILE = 0
	private const val FILE_MODE_OPEN_FILES = 1
	private const val FILE_MODE_OPEN_DIR = 2
	private const val FILE_MODE_OPEN_ANY = 3
	private const val FILE_MODE_SAVE_FILE = 4

	/**
	 * Launches a file picker activity with specified settings based on the mode, initial directory,
	 * file type filters, and other parameters.
	 */
	fun show(activity: Activity?, currentDirectory: String, filename: String, fileMode: Int, filters: Array<String>) {
		val intent = when (fileMode) {
			FILE_MODE_OPEN_DIR -> Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
			FILE_MODE_SAVE_FILE -> Intent(Intent.ACTION_CREATE_DOCUMENT)
			else -> Intent(Intent.ACTION_OPEN_DOCUMENT)
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && currentDirectory.isNotEmpty()) {
			intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, currentDirectory)
		} else {
			Log.d(TAG, "Error cannot set initial directory")
		}
		if (fileMode == FILE_MODE_OPEN_FILES) {
			intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
		} else if (fileMode == FILE_MODE_SAVE_FILE) {
			intent.putExtra(Intent.EXTRA_TITLE, filename)
		}
		// ACTION_OPEN_DOCUMENT_TREE does not support intent type
		if (fileMode != FILE_MODE_OPEN_DIR) {
			val resolvedFilters = filters.map { resolveMimeType(it) }.distinct()
			intent.type = resolvedFilters.firstOrNull { it != "application/octet-stream" } ?: "*/*"
			if (resolvedFilters.size > 1) {
				intent.putExtra(Intent.EXTRA_MIME_TYPES, resolvedFilters.toTypedArray())
			}
			intent.addCategory(Intent.CATEGORY_OPENABLE)
		}
		intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
		activity?.startActivityForResult(intent, FILE_PICKER_REQUEST)
	}

	/**
	 * Retrieves the MIME type for a given file extension.
	 *
	 * @param ext the extension whose MIME type is to be determined.
	 * @return the MIME type as a string, or "application/octet-stream" if the type is unknown.
	 */
	private fun resolveMimeType(ext: String): String {
		val mimeTypeMap = MimeTypeMap.getSingleton()
		var input = ext

		// Fix for extensions like "*.txt" or ".txt".
		if (ext.contains(".")) {
			input = ext.substring(ext.indexOf(".") + 1);
		}

		// Check if the input is already a valid MIME type.
		if (mimeTypeMap.hasMimeType(input)) {
			return input
		}

		val resolvedMimeType = mimeTypeMap.getMimeTypeFromExtension(input)
		if (resolvedMimeType != null) {
			return resolvedMimeType
		}
		// Check for wildcard MIME types like "image/*".
		if (input.contains("/*")) {
			val category = input.substringBefore("/*")
			return when (category) {
				"image" -> "image/*"
				"video" -> "video/*"
				"audio" -> "audio/*"
				else -> "application/octet-stream"
			}
		}
		// Fallback to a generic MIME type if the input is neither a valid extension nor MIME type.
		return "application/octet-stream"
	}

	fun write(contentResolver: ContentResolver, uri: Uri, data: ByteArray) {
		try {
			contentResolver.openOutputStream(uri, "w")?.use { outputStream ->
				outputStream.write(data)
				outputStream.flush()
				Log.d(TAG, "Write completed!")
			}
		} catch (e: Exception) {
			Log.d(TAG, "Failed to write: ", e)
		}
	}

	fun read(contentResolver: ContentResolver, uri: Uri): ByteArray? {
		return try {

			contentResolver.openInputStream(uri)?.use { inputStream ->
				val data = inputStream.readBytes()
				Log.d(TAG, "Read completed!")
				data
			}
		} catch (e: Exception) {
			Log.d(TAG, "Failed to read", e)
			null
		}
	}


}
