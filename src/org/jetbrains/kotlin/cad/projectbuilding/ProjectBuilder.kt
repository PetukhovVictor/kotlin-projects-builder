package org.jetbrains.kotlin.cad.projectbuilding

import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader

enum class BuildSystem { GRADLE, MAVEN, NO }
enum class BuildResult { SUCCESSFUL, FAILURE }

object ProjectBuilder {
    private fun writeBuildLog(stream: InputStream, projectPath: String) {
        val reader = BufferedReader(InputStreamReader(stream))
        val sb = StringBuffer()
        var line = reader.readLine()

        while (line != null) {
            sb.append(line + "\n")
            line = reader.readLine()
        }

        File("$projectPath/buildLog.txt").writeText(sb.toString())
    }

    private fun buildGradleProject(buildFilePath: String): Int {
        val process = ProcessBuilder("gradle", "build").run {
            directory(File(buildFilePath))
            redirectErrorStream(true)
            start()
        }

        writeBuildLog(process.inputStream, buildFilePath)

        return process.waitFor() // process exit code
    }

    private fun buildMavenProject(buildFilePath: String): Int {
        val process = ProcessBuilder("mvn", "install").run {
            directory(File(buildFilePath))
            redirectErrorStream(true)
            start()
        }

        writeBuildLog(process.inputStream, buildFilePath)

        return process.waitFor() // process exit code
    }

    private fun compileProject(projectPath: String): Int {
        val process = ProcessBuilder("kotlinc", projectPath).run {
            directory(File(projectPath))
            redirectErrorStream(true)
            start()
        }

        writeBuildLog(process.inputStream, projectPath)

        return process.waitFor() // process exit code
    }

    private fun findFile(path: String, filename: String): String? {
        File(path).walkTopDown().forEach {
            if (it.name == filename)
                return it.parentFile.absolutePath
        }

        return null
    }

    private fun detectBuildSystem(projectPath: String): Pair<BuildSystem, String> {
        findFile(projectPath, "build.gradle")?.let {
            return Pair(BuildSystem.GRADLE, it)
        }
        findFile(projectPath, "build.gradle.kts")?.let {
            return Pair(BuildSystem.GRADLE, it)
        }
        findFile(projectPath, "pom.xml")?.let {
            return Pair(BuildSystem.MAVEN, it)
        }

        return Pair(BuildSystem.NO, projectPath)
    }

    fun build(project: File, counter: Int, successfulCounter: Int, total: Int): BuildResult {
        val projectPath = "${project.absolutePath}/sources"
        val (buildSystem, buildFilePath) = detectBuildSystem(projectPath)
        val statusCode = when (buildSystem) {
            BuildSystem.GRADLE -> buildGradleProject(buildFilePath)
            BuildSystem.MAVEN -> buildMavenProject(buildFilePath)
            BuildSystem.NO -> 0 //compileProject(buildFilePath)
        }

        when (statusCode) {
            0 -> {
                println("${successfulCounter + 1}/$counter successful out of $total, $projectPath: $buildSystem BUILD SUCCESSFUL")
                File("successfulBuilds.txt").appendText("$projectPath: $buildSystem BUILD SUCCESSFUL${System.lineSeparator()}")
            }
            else -> {
                println("$successfulCounter/$counter successful out of $total, $projectPath: $buildSystem BUILD FAILED (see build log)")
                File("failureBuilds.txt").appendText("$projectPath: $buildSystem BUILD FAILED${System.lineSeparator()}")
            }
        }

        File("${File(buildFilePath).parentFile.absolutePath}/build").deleteRecursively()

        return if (statusCode == 0) BuildResult.SUCCESSFUL else BuildResult.FAILURE
    }
}
