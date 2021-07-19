/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.gradle.plugins.ide.eclipse

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.plugins.ide.eclipse.model.BuildCommand
import org.gradle.test.fixtures.AbstractProjectBuilderSpec
import java.util.Properties
import spock.util.environment.RestoreSystemProperties

class EclipsePluginTest extends AbstractProjectBuilderSpec {

    private EclipsePlugin eclipsePlugin

    def setup() {
        eclipsePlugin = project.objects.newInstance(EclipsePlugin)
    }

    def "can configure project encoding"() {
        when:
        eclipsePlugin.apply(project)
        project.eclipse {
            encoding {
                projectEncoding encoding: 'UTF-16'
            }
        }

        then:
        project.eclipse.encoding.encodings['encoding/<project>'] == 'UTF-16'
    }

    def "by default project encoding is defined with system 'file.encoding' property"() {
        when:
        eclipsePlugin.apply(project)
        project.apply(plugin: 'java')
        project.eclipse {
            encoding {
                resourceEncoding resource: 'src/main/java', encoding: 'UTF-8'
            }
        }

        then:
        project.eclipse.encoding.encodings['encoding/<project>'] == System.properties.getProperty('file.encoding')
    }

    @RestoreSystemProperties
    def "can configure project encoding with system 'file.encoding' property"() {
        setup:
        System.properties.setProperty('file.encoding', charset)

        when:
        eclipsePlugin.apply(project)
        project.apply(plugin: 'java')
        project.eclipse {
            encoding {
                resourceEncoding resource: 'src/main/java', encoding: 'UTF-8'
            }
        }

        then:
        project.eclipse.encoding.encodings['encoding//src/main/java'] == 'UTF-8'
        project.eclipse.encoding.encodings['encoding/<project>'] == System.properties.getProperty('file.encoding')
        project.eclipse.encoding.encodings['encoding/<project>'] == charset

        where:
        charset << ['cp1252', 'Cp870', 'ISO-8859-1']
    }

    def "can configure resource encoding explicitely"() {
        when:
        eclipsePlugin.apply(project)
        project.apply(plugin: 'java')
        project.eclipse {
            encoding {
                projectEncoding encoding: charset1
                resourceEncoding resource: 'src/main/java', encoding: charset2
            }
        }

        then:
        project.eclipse.encoding.encodings['encoding/<project>'] == charset1
        project.eclipse.encoding.encodings['encoding//src/main/java'] == charset2

        where:
        charset1 << ['cp1252', 'Cp870', null, 'ISO-8859-1']
        charset2 << ['Cp870', null, 'cp1252', 'ISO-8859-1']
    }


    def "can configure resource encoding using main JavaCompile task"() {
        when:
        eclipsePlugin.apply(project)
        project.apply(plugin: 'java')
        project.eclipse {
            encoding {
                projectEncoding encoding: 'UTF-16'
            }
        }
        project.compileJava.options.encoding = 'UTF-8'

        then:
        project.eclipse.encoding.encodings['encoding//src/main/java'] == 'UTF-8'
        project.eclipse.encoding.encodings['encoding//src/test/java'] == null
        project.eclipse.encoding.encodings['encoding/<project>'] == 'UTF-16'
    }

    def "can configure resource encoding using test JavaCompile task"() {
        when:
        eclipsePlugin.apply(project)
        project.apply(plugin: 'java')
        project.eclipse {
            encoding {
                projectEncoding encoding: 'UTF-16'
            }
        }
        project.compileTestJava.options.encoding = 'UTF-8'

        then:
        project.eclipse.encoding.encodings['encoding//src/main/java'] == null
        project.eclipse.encoding.encodings['encoding//src/test/java'] == 'UTF-8'
        project.eclipse.encoding.encodings['encoding/<project>'] == 'UTF-16'
    }

    def "explicit encoding take precedence over implicit encoding"() {
        when:
        eclipsePlugin.apply(project)
        project.apply(plugin: 'java')
        project.eclipse {
            encoding {
                projectEncoding encoding: 'UTF-16'
                resourceEncoding resource: 'src/main/java', encoding: charset
            }
        }
        project.tasks.withType(JavaCompile){ options.encoding = 'UTF-8' }

        then:
        project.eclipse.encoding.encodings['encoding//src/main/java'] == charset
        project.eclipse.encoding.encodings['encoding//src/test/java'] == 'UTF-8'
        project.eclipse.encoding.encodings['encoding/<project>'] == 'UTF-16'
        
        where:
        charset << ['cp1252', 'Cp870', 'ISO-8859-1', null]
    }

    def applyToBaseProject_shouldOnlyHaveEclipseProjectTask() {
        when:
        eclipsePlugin.apply(project)

        then:
        project.tasks.findByPath(':eclipseClasspath') == null
        assertThatCleanEclipseDependsOn(project, project.cleanEclipseProject)
        checkEclipseProjectTask([], [])
    }

    def applyToJavaProject_shouldOnlyHaveProjectAndClasspathTaskForJava() {
        when:
        eclipsePlugin.apply(project)
        project.apply(plugin: 'java-base')
        project.evaluate()
        then:
        assertThatCleanEclipseDependsOn(project, project.cleanEclipseProject)
        assertThatCleanEclipseDependsOn(project, project.cleanEclipseClasspath)
        checkEclipseProjectTask([new BuildCommand('org.eclipse.jdt.core.javabuilder')], ['org.eclipse.jdt.core.javanature'])
        checkEclipseClasspath([])
        checkEclipseJdt()

        when:
        project.apply(plugin: 'java')

        then:
        checkEclipseClasspath([project.configurations.compileClasspath, project.configurations.runtimeClasspath, project.configurations.testCompileClasspath, project.configurations.testRuntimeClasspath])
    }

    def applyToScalaProject_shouldHaveProjectAndClasspathTaskForScala() {
        def scalaIdeContainer = ['org.scala-ide.sdt.launching.SCALA_CONTAINER']

        when:
        eclipsePlugin.apply(project)
        project.apply(plugin: 'scala-base')
        project.evaluate()

        then:
        assertThatCleanEclipseDependsOn(project, project.cleanEclipseProject)
        assertThatCleanEclipseDependsOn(project, project.cleanEclipseClasspath)
        checkEclipseProjectTask([new BuildCommand('org.scala-ide.sdt.core.scalabuilder')],
                ['org.scala-ide.sdt.core.scalanature', 'org.eclipse.jdt.core.javanature'])
        checkEclipseClasspath([], scalaIdeContainer)

        when:
        project.apply(plugin: 'scala')

        then:
        checkEclipseClasspath([project.configurations.compileClasspath, project.configurations.runtimeClasspath, project.configurations.testCompileClasspath, project.configurations.testRuntimeClasspath], scalaIdeContainer)
    }

    def applyToGroovyProject_shouldHaveProjectAndClasspathTaskForGroovy() {
        when:
        eclipsePlugin.apply(project)
        project.apply(plugin: 'groovy-base')
        project.evaluate()

        then:
        assertThatCleanEclipseDependsOn(project, project.cleanEclipseProject)
        assertThatCleanEclipseDependsOn(project, project.cleanEclipseClasspath)
        checkEclipseProjectTask([new BuildCommand('org.eclipse.jdt.core.javabuilder')], ['org.eclipse.jdt.groovy.core.groovyNature',
                'org.eclipse.jdt.core.javanature'])
        checkEclipseClasspath([])

        when:
        project.apply(plugin: 'groovy')

        then:
        checkEclipseClasspath([project.configurations.compileClasspath, project.configurations.runtimeClasspath, project.configurations.testCompileClasspath, project.configurations.testRuntimeClasspath])
    }

    def "creates empty classpath model for non java projects"() {
        when:
        eclipsePlugin.apply(project)

        then:
        project.eclipse.classpath
        project.eclipse.classpath.defaultOutputDir
    }

    def "configures internal class folders"() {
        when:
        eclipsePlugin.apply(project)
        project.apply(plugin: 'java')

        project.sourceSets.main.output.dir 'generated-folder'
        project.sourceSets.main.output.dir 'ws-generated'

        project.sourceSets.test.output.dir 'generated-test'
        project.sourceSets.test.output.dir 'test-resources'

        project.sourceSets.test.output.dir '../some/external/dir'

        then:
        def folders = project.eclipseClasspath.classpath.classFolders
        folders == [project.file('generated-folder'), project.file('ws-generated'), project.file('generated-test'), project.file('test-resources'), project.file('../some/external/dir')]
    }

    def "configures internal class folders for custom source sets"() {
        when:
        eclipsePlugin.apply(project)
        project.apply(plugin: 'java')
        project.sourceSets.create('custom')
        project.sourceSets.custom.output.dir 'custom-output'

        then:
        project.eclipseClasspath.classpath.classFolders == [project.file('custom-output')]
    }

    private void checkEclipseProjectTask(List buildCommands, List natures) {
        GenerateEclipseProject eclipseProjectTask = project.eclipseProject
        assert eclipseProjectTask instanceof GenerateEclipseProject
        assert project.tasks.eclipse.taskDependencies.getDependencies(project.tasks.eclipse).contains(eclipseProjectTask)
        assert eclipseProjectTask.outputFile == project.file('.project')

        assert project.eclipse.project.buildCommands == buildCommands
        assert project.eclipse.project.natures == natures
    }

    private void checkEclipseClasspath(def configurations, def additionalContainers = []) {
        def classpath = project.eclipse.classpath
        def classpathTask = project.tasks.eclipseClasspath

        assert classpathTask instanceof GenerateEclipseClasspath
        assert classpathTask.classpath == classpath
        assert classpathTask.outputFile == project.file('.classpath')
        assert project.tasks.eclipse.taskDependencies.getDependencies(project.tasks.eclipse).contains(classpathTask)

        assert classpath.sourceSets == project.sourceSets
        assert classpath.plusConfigurations == configurations
        assert classpath.minusConfigurations == []

        assert classpath.containers == ["org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/${project.eclipse.jdt.getJavaRuntimeName()}/"] + additionalContainers as Set
        assert classpath.defaultOutputDir == new File(project.projectDir, 'bin/default')
    }

    private void checkEclipseJdt() {
        GenerateEclipseJdt eclipseJdt = project.eclipseJdt
        assert project.tasks.eclipse.taskDependencies.getDependencies(project.tasks.eclipse).contains(eclipseJdt)
        assert eclipseJdt.outputFile == project.file('.settings/org.eclipse.jdt.core.prefs')
    }

    void assertThatCleanEclipseDependsOn(Project project, Task dependsOnTask) {
        assert dependsOnTask instanceof Delete
        assert project.cleanEclipse.taskDependencies.getDependencies(project.cleanEclipse).contains(dependsOnTask)
    }
}

