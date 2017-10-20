import os
from keras.models import load_model


class ModelSerializer(object):

    def serializer_load(self, object_file_path):
        if object_file_path.split(os.sep)[-1] == 'model':
            return load_model(object_file_path)
        else:
            return super(ModelSerializer, self).serializer_load(object_file_path)

    def serializer_dump(self, obj, object_file_path):
        if object_file_path.split(os.sep)[-1] == 'model':
            obj.save(object_file_path)
        else:
            super(ModelSerializer, self).serializer_dump(obj, object_file_path)
