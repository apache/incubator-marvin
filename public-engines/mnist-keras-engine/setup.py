from __future__ import print_function

from os.path import dirname, join
from setuptools import setup, find_packages
from setuptools.command.test import test as TestCommand


REQUIREMENT_TESTS = [
    'pytest>=2.6.4',
    'pytest-cov>=1.8.1',
    'mock>=2.0.0',
    'virtualenv>=15.0.1',
    'tox>=2.2.0'
]


def _get_version():
    """Return the project version from VERSION file."""

    with open(join(dirname(__file__), 'marvin_mnist_keras_engine/VERSION'), 'rb') as f:
        version = f.read().decode('ascii').strip()
    return version


class Tox(TestCommand):
    """Run the test cases using TOX command."""

    user_options = [('tox-args=', 'a', "Arguments to pass to tox")]

    def initialize_options(self):
        TestCommand.initialize_options(self)
        self.tox_args = None

    def finalize_options(self):
        TestCommand.finalize_options(self)
        self.test_args = []
        self.test_suite = True

    def run_tests(self):
        # Import here, cause outside the eggs aren't loaded
        import tox
        import shlex
        import sys
        args = self.tox_args
        if args:
            args = shlex.split(self.tox_args)
        else:
            # Run all tests by default
            args = ['-c', join(dirname(__file__), 'tox.ini'), 'tests']
        errno = tox.cmdline(args=args)
        sys.exit(errno)


setup(
    name='marvin_mnist_keras_engine',
    version=_get_version(),
    url='',
    description='Marvin engine',
    long_description=open(join(dirname(__file__), 'README.md')).read(),
    author='Marvin AI Community',
    maintainer='Marvin AI Community',
    maintainer_email='dev@marvin.apache.org',
    packages=find_packages(exclude=('tests', 'tests.*')),
    include_package_data=True,
    zip_safe=False,
    classifiers=[
        'Development Status :: 3 - Alpha',
        'Intended Audience :: Developers',
        'Programming Language :: Python',
        'Programming Language :: Python :: 2',
        'Programming Language :: Python :: 2.7',
        'Programming Language :: Python :: 3',
        'Programming Language :: Python :: 3.3',
        'Programming Language :: Python :: 3.4',
        'Programming Language :: Python :: 3.5',
        'Topic :: Software Development :: Libraries :: Python Modules',
    ],
    install_requires=[
        'scikit-learn==0.18.2',
        'scipy==0.19.1',
        'pandas==0.20.3',
        'matplotlib==2.0.2',
        'marvin-python-toolbox==0',
        'Fabric==1.14.0',
        'keras==2.1.4',
        'tensorflow==1.5.0',
        'np-utils==0.5.4',
        'h5py==2.7.1',
        'pytest==2.9.2',
        'pluggy==0.3.1',
        'opencv-python==3.4.0.12'
    ],
    dependency_links=['git+https://github.com/apache/incubator-marvin@master#subdirectory=python-toolbox#egg=marvin_python_toolbox-0'],
    tests_require=REQUIREMENT_TESTS,
    extras_require={
        'testing': REQUIREMENT_TESTS,
    },
    cmdclass={
        'test': Tox,
    },
)
