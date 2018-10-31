package org.jetbrains.kotlin.cad

import org.jetbrains.kotlin.cad.projectbuilding.BuildResult
import org.jetbrains.kotlin.cad.projectbuilding.ProjectBuilder
import java.io.File

const val REPOS_PATH = "..."

fun repoVisit(callback: (File) -> Any) {
    File(REPOS_PATH).listFiles { file -> file.isDirectory }.forEach { userFolderName ->
        userFolderName.listFiles { file -> file.isDirectory }.forEach { repoFolderName ->
            callback(repoFolderName)
        }
    }
}

fun main(args: Array<String>) {
    var counter = 0
    var successfulCounter = 0
    var total = 0

    repoVisit { total++ }
    repoVisit { repoFolderName ->
        ProjectBuilder.build(repoFolderName, ++counter, successfulCounter, total).let {
            if (it == BuildResult.SUCCESSFUL) successfulCounter++
        }
    }
}
