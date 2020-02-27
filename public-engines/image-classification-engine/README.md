# image_classification v0.0.1

## Overview

**Image Classification**

It contains an example of how to perform image classification.
The dataset is divided into two classes according to VOC2012 Images.
- Airplane
- Not Airplane

## Installation

```
marvin make
```

## Development

It runs deep learning through Keras and tensorflow as backend.
Images are loaded using generator to save memory.

### Getting started

First, create a new virtualenv

```
mkvirtualenv marvin_image_classification_engine_env
```

Now install the development dependencies

```
make marvin
```

and to run the whole pipeline

```
marvin engine-dryrun
```

### Contact

name: danilo.nunes
email: dev@marvin.apache.org