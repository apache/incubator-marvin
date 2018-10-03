import os
from keras.models import load_model


class ModelSerializer(object):

    def _serializer_load(self, object_file_path):
        if object_file_path.split(os.sep)[-1] == 'model':
            return load_model(object_file_path)
        else:
            return super(ModelSerializer, self)._serializer_load(object_file_path)

    def _serializer_dump(self, obj, object_file_path):
        if object_file_path.split(os.sep)[-1] == 'model':
            obj.save(object_file_path)
        else:
            super(ModelSerializer, self)._serializer_dump(obj, object_file_path)
