# nf-dotenv

[![CI](https://github.com/fulcrumgenomics/nf-dotenv/actions/workflows/test.yml/badge.svg?branch=main)](https://github.com/fulcrumgenomics/nf-dotenv/actions/workflows/test.yml?query=branch%3Amain)
[![Nextflow](https://img.shields.io/badge/Nextflow%20DSL2-%E2%89%A522.10.2-blue.svg)](https://www.nextflow.io/)
[![Java Versions](https://img.shields.io/badge/java-8_|_11_|_17_|_21-blue)](https://github.com/fulcrumgenomics/nf-dotenv)

Automatically source [dotenv](https://hexdocs.pm/dotenvy/dotenv-file-format.html) files into your Nextflow scope.

<p>
<a href float="left"="https://fulcrumgenomics.com"><img src=".github/logos/fulcrumgenomics.svg" alt="Fulcrum Genomics" height="100"/></a>
</p>

[Visit us at Fulcrum Genomics](www.fulcrumgenomics.com) to learn more about how we can power your Bioinformatics with nf-dotenv and beyond.

<a href="mailto:contact@fulcrumgenomics.com?subject=[GitHub inquiry]"><img src="https://img.shields.io/badge/Email_us-brightgreen.svg?&style=for-the-badge&logo=gmail&logoColor=white"/></a>
<a href="https://www.fulcrumgenomics.com"><img src="https://img.shields.io/badge/Visit_Us-blue.svg?&style=for-the-badge&logo=wordpress&logoColor=white"/></a>

## Quickstart

Add the plugin to your Nextflow config:

```nextflow
plugins { id 'nf-dotenv' }
```

And add the following import statement into your processes and workflow files:

```nextflow
include { dotenv } from "plugin/nf-dotenv"
```

Now you're ready to source environmental file variables in Nextflow contexts!

```nextflow
dotenv("KeyFromEnvironment")
```

## Configuration

This plugin support the following Nextflow configuration:

```nextflow
dotenv.filename = ".env" // Filename of the dotenv file
dotenv.relative = "."    // Relative path of the dotenv file to the main script
```

## A Real World Example

Let's say you have the following Nextflow project:

#### Dotenv (`.env`)

```ini
SAMTOOLS_VERSION=1.17
```
#### Docker Image (`Dockerfile`)

```dockerfile
FROM alpine:3.18

RUN apk add --update --no-cache \
      bash=5.2.15-r5 \
      build-base=0.5-r3 \
      bzip2-dev=1.0.8-r5 \
      xz-dev=5.4.3-r0 \
      zlib-dev=1.2.13-r1

ARG SAMTOOLS_VERSION

RUN wget https://github.com/samtools/samtools/releases/download/${SAMTOOLS_VERSION}/samtools-${SAMTOOLS_VERSION}.tar.bz2 \
    && tar -xjvf samtools-${SAMTOOLS_VERSION}.tar.bz2 \
    && cd samtools-${SAMTOOLS_VERSION} \
    && ./configure --without-curses --enable-configure-htslib \
    && make all all-htslib -j 8 \
    && make install install-htslib \
    && rm -r ../samtools-${SAMTOOLS_VERSION}
```

#### Docker Compose (`docker-compose.yaml`)

```yaml
services:
  samtools:
    build:
      args:
        SAMTOOLS_VERSION: ${SAMTOOLS_VERSION}
      tags: ['samtools:${SAMTOOLS_VERSION}']
```

#### Nextflow Script (`main.nf`)

```nextflow
include { dotenv } from "plugin/nf-dotenv"

process emit_samtools_version {
    container "samtools:${dotenv('SAMTOOLS_VERSION')}"
    output: stdout

    """
    samtools --version | head -n1
    """
}

workflow { emit_samtools_version() | view }
```

After building the Docker image with `docker compose build`, and after [enabling Docker for Nextflow](https://www.nextflow.io/docs/latest/docker.html#how-it-works), you will be able to use `nf-dotenv` to source the version tag of the container to use in your Nextflow process.
When the main Nextflow script is run with `nextflow run main.nf`, you will get the following output:

```console
❯ nextflow -quiet run main.nf
samtools 1.17
```

However, upgrade the dotenv variable `SAMTOOLS_VERSION` to `1.18` and you'll see:

```console
❯ nextflow -quiet run main.nf
samtools 1.18
```

Conveniently for debugging, local environment variables take precedence over dotenv variables:

```console
❯ SAMTOOLS_VERSION=1.16 nextflow -quiet run main.nf
samtools 1.16
```

## Testing the Plugin Locally

Execute the following to compile and run unit tests for the plugin:

```
make compile
make test
```

To install the plugin for use in local workflows (_e.g._ not internet connected), execute the following:

```
make install-local
```

## Developing the Plugin Locally


Execute the following to build the plugin along with Nextflow source files:

```
make compile-with-nextflow
```

Test your changes to the plugin on a Nextflow script like:

```bash
NXF_PLUGINS_DEV="${PWD}/plugins" nextflow/launch.sh run <script.nf> -plugins nf-dotenv
```

## Publishing to GitHub

After bumping the version of the plugin in the file [`MANIFEST.MF`](./plugins/nf-dotenv/src/resources/META-INF/MANIFEST.MF), execute the following:

```
GITHUB_TOKEN=... GITHUB_USERNAME=... GITHUB_COMMIT_EMAIL=... make publish-to-github
```
