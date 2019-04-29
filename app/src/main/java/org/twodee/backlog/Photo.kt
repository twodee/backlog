package org.twodee.backlog

import java.io.File

data class Photo(val file: File) {
  val year: Int
  val month: Int
  val day: Int

  init {
    year = file.parentFile.name.toInt()
    val matches = "^(\\d+)_(\\d+)$".toRegex().matchEntire(file.nameWithoutExtension)
    if (matches != null) {
      month = matches.groupValues[1].toInt()
      day = matches.groupValues[2].toInt()
    } else {
      month = 1
      day = 1
    }
  }
}