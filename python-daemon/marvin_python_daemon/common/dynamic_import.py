import sys
import importlib
from .log import get_logger

logger = get_logger('utils.dynamic_import')

class RollbackImporter:
    def __init__(self):
        self.previousModules = sys.modules.copy()
        self.newModules = {}

    def dynamic_import(self, package, name):
        mod = __import__(package)
        self.newModules[package] = 1
        mod = getattr(mod, name)
        return mod

    def uninstall(self):
        for modname in self.newModules.keys():
            if not modname in self.previousModules:
                logger.debug("Uninstalling {} and all submodules...".format(modname))
                for module in list(sys.modules.keys()):
                    if modname in module:
                        del sys.modules[module]