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
import org.gradle.plugins.ide.api.PropertiesFileContentMerger;
import org.gradle.plugins.ide.api.XmlFileContentMerger;
import org.gradle.util.internal.ConfigureUtil;

import javax.inject.Inject;

import java.util.Arrays;
import java.util.Collections;
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
    private Properties encodings = new Properties();

    @Inject
    public EclipseResourceEncoding(PropertiesFileContentMerger file) {
        this.file = file;
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

    /**
     * The facets to be added as elements.
     * <p>
     * For examples see docs for {@link EclipseResourceEncoding}
     */
    public Properties getEncodings() {
        return encodings;
    }

    public void setEncodings(Properties encodings) {
        this.encodings = encodings;
    }

    /**
     * Adds a facet.
     * <p>
     * If a facet already exists with the given name then its version will be updated.
     * <p>
     * In the case of a "jst.ejb" facet, the incompatible "jst.utility" installed by default is also removed.
     * </p>
     * For examples see docs for {@link EclipseResourceEncoding}
     *
     * @param args A map that must contain a 'name' and 'version' key with corresponding values.
     */
    public void resourceEncoding(Map<String, String> args) {
        encodings.setProperty("encodings/" + args.getOrDefault("resource", "<project>"), args.get("encoding"));
    }

    public void projectEncoding(Map<String, String> args) {
        encodings.setProperty("encodings/<project>", args.get("encoding"));
    }

    @SuppressWarnings("unchecked")
    public void mergeEncodings(Encoding encodingPref) {
        file.getBeforeMerged().execute(encodingPref);
        encodingPref.configure(getEncodings());
        file.getWhenMerged().execute(encodingPref);
    }
    
}
