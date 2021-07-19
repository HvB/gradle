/*
 * Copyright 2011 the original author or authors.
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
package org.gradle.plugins.ide.eclipse.model;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.plugins.ide.api.PropertiesFileContentMerger;
import org.gradle.plugins.ide.api.XmlFileContentMerger;
import org.gradle.util.internal.ConfigureUtil;

import javax.inject.Inject;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.gradle.util.internal.ConfigureUtil.configure;

/**
 * Enables fine-tuning wtp facet details of the Eclipse plugin
 * <p>
 * Advanced configuration closures beforeMerged and whenMerged receive {@link WtpFacet} object as parameter.
 *
 * <pre class='autoTested'>
 * plugins {
 *     id 'war' // or 'ear' or 'java'
 *     id 'eclipse'
 * }
 *
 * eclipse {
 *   encoding {
 *     //you can add some extra wtp facets or update existing ones; mandatory keys: 'name', 'version':
 *     resourceEncoding resource: 'src', encoding: 'UTF-8'
 *     file {
 *       //if you want to mess with the resulting XML in whatever way you fancy
 *       withProperties {
 *         it.put('myProperty', 'is mime and only mine')
 *       }
 *
 *       //beforeMerged and whenMerged closures are the highest voodoo for the tricky edge cases.
 *       //the type passed to the closures is {@link java.util.Properties}
 *
 *       //closure executed after wtp facet file content is loaded from existing file
 *       //but before gradle build information is merged
 *       beforeMerged { properties -&gt;
 *         //tinker with {@link java.util.Properties} here
 *       }
 *
 *       //closure executed after wtp facet file content is loaded from existing file
 *       //and after gradle build information is merged
 *       whenMerged { properties -&gt;
 *         //you can tinker with the {@link java.util.Properties} here
 *       }
 *     }
 *   }
 * }
 *
 * </pre>
 */
public class EclipseResourceEncoding{

    private final PropertiesFileContentMerger file;
    private Map<String, String> encodings = new LinkedHashMap<>();
    private Map<String, String> defaultEncodings = new LinkedHashMap<>();
    private Project project;
    private ProjectLayout layout;

    @Inject
    public EclipseResourceEncoding(PropertiesFileContentMerger file, Project project) {
        this.file = file;
        this.project = project;
    }

    /**
     * See {@link #file(Action) }
     */
    public PropertiesFileContentMerger getFile() {
        return file;
    }

    /**
     * Enables advanced configuration like tinkering with the output XML
     * or affecting the way existing wtp facet file content is merged with gradle build information
     * <p>
     * The object passed to whenMerged{} and beforeMerged{} closures is of type {@link WtpFacet}
     * <p>
     *
     * For example see docs for {@link EclipseResourceEncoding}
     */
    public void file(Closure closure) {
        configure(closure, file);
    }

    /**
     * Enables advanced configuration like tinkering with the output XML
     * or affecting the way existing wtp facet file content is merged with gradle build information.
     * <p>
     *
     * For example see docs for {@link EclipseResourceEncoding}
     *
     * @since 3.5
     */
    public void file(Action<? super PropertiesFileContentMerger> action) {
        action.execute(file);
    }

    public Map<String, String> getEncodings() {
        Map<String, String> combinedEncodings = new LinkedHashMap<>(this.getDefaultEncodings());
        combinedEncodings.putAll(this.encodings);
        return combinedEncodings;
    }

    public String getProperty(String key) {
        return this.getEncodings().get(key);
    }

    public void setEncodings(Map<String, String> encodings) {
        this.encodings = encodings;
    }

    public Map<String, String> getDefaultEncodings() {
        return this.defaultEncodings;
    }

    public void setDefaultEncodings(Map<String, String> encodings) {
        this.defaultEncodings = encodings;
    }

    protected Project getProject() {
        return project;
    }

    @Inject
    protected ProjectLayout getLayout() {
        throw new UnsupportedOperationException();
    }

    @Inject
    protected ProviderFactory getProviderFactory() {
        throw new UnsupportedOperationException();
    }

    public void resourceEncoding(Map<String, String> args) {
        resourceEncoding(args.get("resource"), args.get("encoding"));
    }

    public void resourceEncoding(String path, String encoding) {
        String key = "encoding/<project>";
        if (path != null && !"<project>".equals(path)) {
            key = "encoding//" + path.replaceFirst("^/*", "").replaceFirst("/*$", "");
        }
        encodings.put(key, encoding);
    }

    public void projectEncoding(Map<String, String> args) {
        projectEncoding(args.get("encoding"));
    }

    public void projectEncoding(String encoding) {
        resourceEncoding(null, encoding);
    }

    @SuppressWarnings("unchecked")
    public void mergeEncodings(Encoding encodingPref) {
        file.getBeforeMerged().execute(encodingPref);

        encodingPref.configure(getEncodings());

        file.getWhenMerged().execute(encodingPref);
    }

}
