/*
 * Licenser
 * Copyright (c) 2015, Minecrell <https://github.com/Minecrell>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.minecrell.gradle.licenser

import static org.gradle.api.plugins.JavaBasePlugin.CHECK_TASK_NAME

import groovy.text.SimpleTemplateEngine
import net.minecrell.gradle.licenser.header.Header
import net.minecrell.gradle.licenser.tasks.LicenseCheck
import net.minecrell.gradle.licenser.tasks.LicenseTask
import net.minecrell.gradle.licenser.tasks.LicenseUpdate
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.SourceSet

class Licenser implements Plugin<Project> {

    private static final String CHECK_TASK = 'checkLicense'
    private static final String FORMAT_TASK = 'updateLicense'
    private static final String ANDROID_TASK = 'Android'

    private Project project
    private LicenseExtension extension

    @Override
    void apply(Project project) {
        this.project = project

        project.with {
            this.extension = extensions.create('license', LicenseExtension)
            extension.header = project.file('LICENSE')
            plugins.withType(JavaBasePlugin) {
                extension.sourceSets = project.sourceSets
            }

            ['com.android.library', 'com.android.application'].each {
                plugins.withId(it) {
                    extension.androidSourceSets = android.sourceSets
                }
            }

            def globalCheck = task(CHECK_TASK + 's')
            task('licenseCheck', dependsOn: globalCheck)
            def globalFormat = task(FORMAT_TASK + 's')
            task('licenseFormat', dependsOn: globalFormat)

            afterEvaluate {
                project.tasks.findByName(CHECK_TASK_NAME)?.dependsOn globalCheck
            }

            // Wait a bit until creating the tasks
            afterEvaluate {
                def header = new Header(extension.style, extension.keywords, {
                    File header = extension.header
                    if (header != null && header.exists()) {
                        def text = header.getText(extension.charset)

                        Map<String, String> properties = extension.ext.properties
                        if (properties != null && !properties.isEmpty()) {
                            def engine = new SimpleTemplateEngine()
                            def template = engine.createTemplate(text).make(properties)
                            text = template.toString()
                        }

                        return text
                    }

                    return ""
                }, extension.newLine)

                extension.sourceSets.each {
                    def check = createTask(CHECK_TASK, LicenseCheck, header, it)
                    check.ignoreFailures = extension.ignoreFailures
                    globalCheck.dependsOn check
                    globalFormat.dependsOn createTask(FORMAT_TASK, LicenseUpdate, header, it)
                }

                extension.androidSourceSets.each {
                    def check = createAndroidTask(CHECK_TASK, LicenseCheck, header, it)
                    check.ignoreFailures = extension.ignoreFailures
                    globalCheck.dependsOn check
                    globalFormat.dependsOn createAndroidTask(FORMAT_TASK, LicenseUpdate, header, it)
                }
            }
        }
    }

    Project getProject() {
        project
    }

    LicenseExtension getExtension() {
        extension
    }

    private <T extends LicenseTask> T createTask(String name, Class<T> type, Header expectedHeader, SourceSet sourceSet) {
        return makeTask(sourceSet.getTaskName(name, null), type, expectedHeader, sourceSet.allSource)
    }

    private <T extends LicenseTask> T createAndroidTask(String name, Class<T> type, Header expectedHeader, Object sourceSet) {
        return makeTask(name + ANDROID_TASK + sourceSet.name.capitalize(), type, expectedHeader,
                project.files(sourceSet.java.sourceFiles, sourceSet.res.sourceFiles))
    }

    private <T extends LicenseTask> T makeTask(String name, Class<T> type, Header expectedHeader, FileCollection files) {
        return (T) project.task(name, type: type) { T task ->
            task.header = expectedHeader
            task.files = files
            task.filter = extension.filter
            task.charset = extension.charset
        }
    }

}
