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
package org.gradle.plugins.ide.eclipse.model;

import java.util.Properties;

import org.gradle.api.internal.PropertiesTransformer;
import org.gradle.plugins.ide.eclipse.model.internal.EclipseJavaVersionMapper;
import org.gradle.plugins.ide.internal.generator.PropertiesPersistableConfigurationObject;

/**
 * Represents the Eclipse JDT settings.
 */
public class Encoding extends PropertiesPersistableConfigurationObject {

    private Properties encodings;

    public Encoding(PropertiesTransformer transformer) {
        super(transformer);
        Properties defaults = new Properties();
        defaults.put("eclipse.preferences.version", "1");
        encodings = new Properties(defaults);
    }

    @Override
    protected String getDefaultResourceName() {
        return "defaultEncodingPrefs.properties";
    }

    @Override
    protected void load(Properties properties) {
        //encodings.addAll(properties);
    }

    @Override
    protected void store(Properties properties) {
        properties.putAll(encodings);
    }

    public void configure(Properties properties){
        encodings.putAll(properties);
    }
}
