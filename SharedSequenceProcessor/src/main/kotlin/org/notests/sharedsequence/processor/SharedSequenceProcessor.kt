package org.notests.sharedsequence.processor

import org.notests.sharedsequence.annotations.SharedSequence
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import java.net.URI

class SharedSequenceProcessor : AbstractProcessor() {
  fun getTemplateContent(template: String): String {
      try {
        return URI("file:/Users/kzaher/Projects/SharedSequence.kt/SharedSequenceProcessor/src/main/resources/$template").toURL().readText()
      }
      catch(e: Exception) {
        return javaClass.classLoader.getResource(template).readText()
      }
  }

  override fun process(annotations: MutableSet<out TypeElement>, env: RoundEnvironment): Boolean {

    processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, "Started")
    val kotlinGenerated = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION]

    val annotatedClasses = env.getElementsAnnotatedWith(SharedSequence::class.java)
    annotatedClasses.forEach {
      val el = it as? TypeElement ?: throw Exception("Should be on an object!")
      val pckg = processingEnv.elementUtils.getPackageOf(el).qualifiedName.toString()
      val objectName = el.simpleName.toString()
      val sharedSequenceName = el.getAnnotation(SharedSequence::class.java).value

      val source = getTemplateContent("template.kt")

      val replacedSource = source.replace("_Package_", pckg)
        .replace("_Template_", sharedSequenceName)
        .replace("_scheduler_", "$objectName.scheduler")
        .replace("_share_", "$objectName.share")

      processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, replacedSource)

      File("$kotlinGenerated/$objectName.kt/${pckg.replace(".", "/")}", "${sharedSequenceName}.kt").apply {
        parentFile.mkdirs()
        writeText(replacedSource)
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

        val source = getTemplateContent("templatetemplate.kt")
        val replacedSource = source.replace("_Package_", pckg)
          .replace("_Template_", sharedSequenceName1)
          .replace("_Template2_", sharedSequenceName2)

        File("$kotlinGenerated/$objectName1+$objectName2.kt/${pckg.replace(".", "/")}", "$sharedSequenceName1+$sharedSequenceName2.kt").apply {
          parentFile.mkdirs()
          writeText(replacedSource)
        }
      }

    return true
  }

  override fun getSupportedAnnotationTypes() = setOf(SharedSequence::class.java.canonicalName)
  override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()
  override fun getSupportedOptions() = setOf(GENERATE_KOTLIN_CODE_OPTION)

  private companion object {
    val GENERATE_KOTLIN_CODE_OPTION = "generate.kotlin.code"
    val KAPT_KOTLIN_GENERATED_OPTION = "kapt.kotlin.generated"
  }
}
