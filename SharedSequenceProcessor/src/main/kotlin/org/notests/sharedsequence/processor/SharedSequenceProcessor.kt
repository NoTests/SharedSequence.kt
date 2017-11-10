package org.notests.sharedsequence.processor

import org.notests.sharedsequence.annotations.SharedSequence
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic


fun SharedSequenceProcessor.processTemplate(template: String, packageName: String, replacements: HashMap<String, String>): String {
    fun setPackageName(template: String, packageName: String): String =
            (listOf("package " + packageName) + template.split("\n", "\r").drop(1)).joinToString("\n")

    fun getTemplateContent(template: String) = this::class.java.classLoader.getResource(template).readText()

    return setPackageName(getTemplateContent(template), packageName).replace(Regex("_(\\w|\\d)+_")) {
        replacements[it.value]!!
    }
}

class SharedSequenceProcessor : AbstractProcessor() {
  override fun process(annotations: MutableSet<out TypeElement>, env: RoundEnvironment): Boolean {

    processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, "Started")
    val kotlinGenerated = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION]

    val annotatedClasses = env.getElementsAnnotatedWith(SharedSequence::class.java)
    annotatedClasses.forEach {
      val el = it as? TypeElement ?: throw Exception("Should be on an object!")
      val pckg = processingEnv.elementUtils.getPackageOf(el).qualifiedName.toString()
      val objectName = el.simpleName.toString()
      val sharedSequenceName = el.getAnnotation(SharedSequence::class.java).value

      var replacements = hashMapOf(
              "_Template_" to sharedSequenceName,
              "_scheduler_" to "$objectName.scheduler",
              "_share_" to "$objectName.share"
      )

      val source = processTemplate("template.kt", pckg, replacements)

      //processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, replacedSource)

      File("$kotlinGenerated/$objectName.kt/${pckg.replace(".", "/")}", "${sharedSequenceName}.kt").apply {
        parentFile.mkdirs()
        writeText(source)
      }
    }

    for (c1 in annotatedClasses)
      for (c2 in annotatedClasses) {

        val el1 = c1 as? TypeElement ?: throw Exception("Should be on an object!")
        val el2 = c2 as? TypeElement ?: throw Exception("Should be on an object!")

        val objectName1 = el1.simpleName.toString()
        val objectName2 = el2.simpleName.toString()

        val sharedSequenceName1 = el1.getAnnotation(SharedSequence::class.java).value
        val sharedSequenceName2 = el2.getAnnotation(SharedSequence::class.java).value

        val pckg = processingEnv.elementUtils.getPackageOf(el1).qualifiedName.toString()

        var replacements = hashMapOf(
                "_Template_" to sharedSequenceName1,
                "_Template2_" to sharedSequenceName2
        )

        val source = processTemplate("templatetemplate.kt", pckg, replacements)

        File("$kotlinGenerated/$objectName1+$objectName2.kt/${pckg.replace(".", "/")}", "$sharedSequenceName1+$sharedSequenceName2.kt").apply {
          parentFile.mkdirs()
          writeText(source)
        }
      }

    return true
  }

  override fun getSupportedAnnotationTypes() = setOf(SharedSequence::class.java.canonicalName)
  override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()
  override fun getSupportedOptions() = setOf(GENERATE_KOTLIN_CODE_OPTION)

  companion object {
    val GENERATE_KOTLIN_CODE_OPTION = "generate.kotlin.code"
    val KAPT_KOTLIN_GENERATED_OPTION = "kapt.kotlin.generated"
  }
}
