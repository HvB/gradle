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
package org.gradle.plugins.ide.eclipse;

import org.gradle.api.internal.PropertiesTransformer;
import org.gradle.api.tasks.Internal;
import org.gradle.plugins.ide.api.PropertiesFileContentMerger;
import org.gradle.plugins.ide.api.PropertiesGeneratorTask;
import org.gradle.plugins.ide.eclipse.model.EclipseResourceEncoding;
import org.gradle.plugins.ide.eclipse.model.Encoding;
import org.gradle.work.DisableCachingByDefault;

import javax.inject.Inject;

/**
 * Generates the Eclipse JDT configuration file. If you want to fine tune the eclipse configuration
 * <p>
 * At this moment nearly all configuration is done via {@link EclipseJdt}.
 */
@DisableCachingByDefault(because = "Not made cacheable, yet")
public class GenerateEclipseResourceEncoding extends PropertiesGeneratorTask<Encoding> {

    private EclipseResourceEncoding encoding;

    public GenerateEclipseResourceEncoding() {
        encoding = getInstantiator().newInstance(EclipseResourceEncoding.class, new PropertiesFileContentMerger(getTransformer()));
    }

    @Inject
    public GenerateEclipseResourceEncoding(EclipseResourceEncoding encoding) {
        this.encoding = encoding;
    }

    @Override
    protected Encoding create() {
        return new Encoding(getTransformer());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void configure(Encoding encodingPref) {
        EclipseResourceEncoding encodingModel = getEncoding();
        //encodingModel.getFile().getBeforeMerged().execute(encodingPref);
        //encodingModel.getFile().getWhenMerged().execute(encodingPref);
        encodingModel.mergeEncodings(encodingPref);
    }

    @Override
    protected PropertiesTransformer getTransformer() {
        if (encoding == null) {
            return super.getTransformer();
        }
        return encoding.getFile().getTransformer();
    }

    /**
     * Eclipse EclipseResourceEncoding model that contains information needed to resource encoding file.
     */
    @Internal
    public EclipseResourceEncoding getEncoding() {
        return encoding;
    }

    public void setEncoding(EclipseResourceEncoding encoding) {
        this.encoding = encoding;
    }

}
