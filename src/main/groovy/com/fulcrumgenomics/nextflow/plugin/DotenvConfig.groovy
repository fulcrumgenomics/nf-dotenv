package com.fulcrumgenomics.nextflow.plugin

import nextflow.config.spec.ConfigOption
import nextflow.config.spec.ConfigScope
import nextflow.config.spec.ScopeName
import nextflow.script.dsl.Description

@ScopeName('dotenv')
@Description('The `dotenv` scope allows you to configure the `nf-dotenv` plugin.')
class DotenvConfig implements ConfigScope {

    static final String DEFAULT_RELATIVE = '.'
    static final String DEFAULT_FILENAME = '.env'

    @ConfigOption
    @Description('Relative path to the dotenv file directory (default: .)')
    public final String relative

    @ConfigOption
    @Description('Dotenv filename (default: .env)')
    public final String filename

    DotenvConfig() {
        this.relative = DEFAULT_RELATIVE
        this.filename = DEFAULT_FILENAME
    }

    DotenvConfig(Map opts) {
        this.relative = (opts.relative ?: DEFAULT_RELATIVE).toString()
        this.filename = (opts.filename ?: DEFAULT_FILENAME).toString()
    }
}
