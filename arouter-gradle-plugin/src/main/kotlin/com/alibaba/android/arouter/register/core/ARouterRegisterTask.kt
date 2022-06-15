package com.alibaba.android.arouter.register.core

import com.alibaba.android.arouter.register.utils.ScanSetting
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

import com.alibaba.android.arouter.register.utils.ScanSetting.GENERATE_TO_CLASS_NAME
import com.alibaba.android.arouter.register.utils.ScanSetting.GENERATE_TO_METHOD_NAME

internal abstract class ARouterRegisterTask : DefaultTask() {

    @get:InputFiles
    abstract val allJars: ListProperty<RegularFile>

    @get:InputFiles
    abstract val allDirs: ListProperty<Directory>

    @get:OutputFile
    abstract val output: RegularFileProperty

    @TaskAction
    fun taskAction() {
        val rootClasses = mutableSetOf<String>()
        val interceptorClasses = mutableSetOf<String>()
        val providerClasses = mutableSetOf<String>()
        var logisticsCenterInput: Pair<File, String>? = null

        // 1. First pass: Scan all inputs to find routes and the location of LogisticsCenter.
        (allJars.get().map { it.asFile } + allDirs.get().map { it.asFile }).forEach { inputFile ->
            if (inputFile.isDirectory) {
                inputFile.walk().filter { it.isFile && it.name.endsWith(".class") }.forEach { classFile ->
                    val classBytes = classFile.readBytes()
                    val reader = ClassReader(classBytes)
                    reader.interfaces?.forEach {
                        when (it) {
                            ScanSetting.I_ROUTE_ROOT -> rootClasses.add(reader.className.replace('/', '.'))
                            ScanSetting.I_INTERCEPTOR_GROUP -> interceptorClasses.add(reader.className.replace('/', '.'))
                            ScanSetting.I_PROVIDER_GROUP -> providerClasses.add(reader.className.replace('/', '.'))
                        }
                    }
                    if (reader.className == GENERATE_TO_CLASS_NAME) {
                        logisticsCenterInput = Pair(inputFile, classFile.relativeTo(inputFile).path)
                    }
                }
            } else { // It's a jar
                val jar = JarFile(inputFile)
                jar.entries().asSequence().filter { it.name.endsWith(".class") }.forEach { entry ->
                    val classBytes = jar.getInputStream(entry).readBytes()
                    val reader = ClassReader(classBytes)
                    reader.interfaces?.forEach {
                        when (it) {
                            ScanSetting.I_ROUTE_ROOT -> rootClasses.add(reader.className.replace('/', '.'))
                            ScanSetting.I_INTERCEPTOR_GROUP -> interceptorClasses.add(reader.className.replace('/', '.'))
                            ScanSetting.I_PROVIDER_GROUP -> providerClasses.add(reader.className.replace('/', '.'))
                        }
                    }
                    if (reader.className == GENERATE_TO_CLASS_NAME) {
                        logisticsCenterInput = Pair(inputFile, entry.name)
                    }
                }
                jar.close()
            }
        }

        val classesToRegister = rootClasses + interceptorClasses + providerClasses

        // 2. Second pass: Write all files to the output jar, transforming LogisticsCenter on the fly.
        val outputJar = output.get().asFile
        val jos = JarOutputStream(BufferedOutputStream(FileOutputStream(outputJar)))
        val writtenEntries = mutableSetOf<String>()

        (allJars.get().map { it.asFile } + allDirs.get().map { it.asFile }).forEach { inputFile ->
            if (inputFile.isDirectory) {
                inputFile.walk().filter { it.isFile }.forEach { classFile ->
                    val entryName = classFile.relativeTo(inputFile).path
                    if (writtenEntries.add(entryName)) {
                        jos.putNextEntry(JarEntry(entryName))
                        if (inputFile == logisticsCenterInput?.first && entryName == logisticsCenterInput?.second) {
                            jos.write(transformLogisticsCenter(classFile.readBytes(), classesToRegister))
                        } else {
                            jos.write(classFile.readBytes())
                        }
                        jos.closeEntry()
                    }
                }
            } else { // It's a jar
                val jar = JarFile(inputFile)
                jar.entries().asSequence().forEach { entry ->
                    if (writtenEntries.add(entry.name)) {
                        jos.putNextEntry(JarEntry(entry.name))
                        if (inputFile == logisticsCenterInput?.first && entry.name == logisticsCenterInput?.second) {
                            jos.write(transformLogisticsCenter(jar.getInputStream(entry).readBytes(), classesToRegister))
                        } else {
                            jos.write(jar.getInputStream(entry).readBytes())
                        }
                        jos.closeEntry()
                    }
                }
                jar.close()
            }
        }
        jos.close()
    }

    private fun transformLogisticsCenter(inputBytes: ByteArray, routeClasses: Set<String>): ByteArray {
        val writer = ClassWriter(0)
        val visitor = LogisticsCenterClassVisitor(writer, routeClasses)
        ClassReader(inputBytes).accept(visitor, 0)
        return writer.toByteArray()
    }
}

internal class LogisticsCenterClassVisitor(cv: ClassVisitor, private val routeClasses: Set<String>) : ClassVisitor(Opcodes.ASM7, cv) {

    override fun visitMethod(access: Int, name: String?, desc: String?, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
        if (name == GENERATE_TO_METHOD_NAME) {
            val mv = super.visitMethod(access, name, desc, signature, exceptions)
            mv.visitCode()
            mv.visitInsn(Opcodes.ICONST_1)
            mv.visitFieldInsn(Opcodes.PUTSTATIC, GENERATE_TO_CLASS_NAME, "registerByPlugin", "Z")
            routeClasses.forEach {
                mv.visitLdcInsn(it)
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, GENERATE_TO_CLASS_NAME, "register", "(Ljava/lang/String;)V", false)
            }
            mv.visitInsn(Opcodes.RETURN)
            mv.visitMaxs(1, 1)
            mv.visitEnd()
            return null
        }
        return super.visitMethod(access, name, desc, signature, exceptions)
    }
}
